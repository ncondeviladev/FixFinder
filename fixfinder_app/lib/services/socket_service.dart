import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';
import 'package:flutter/foundation.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';

/// Servicio centralizado para la comunicación bidireccional con el servidor Java.
/// 
/// Implementa un protocolo de bajo nivel basado en cabeceras de 4 bytes que
/// indican el tamaño del payload JSON. Gestiona la reconexión automática
/// y el emparejamiento de respuestas mediante un sistema de tickets (IDs).
class SocketService {
  static final SocketService _instance = SocketService._internal();
  factory SocketService() => _instance;
  SocketService._internal();

  Socket? _socket;

  /// Determina si el canal de comunicación está abierto actualmente.
  bool get isConectado => _socket != null;

  // Configuración de red (Resuelta dinámicamente según el entorno)
  String get _servidor {
    final env = dotenv.get('ENVIRONMENT', fallback: 'NUBE');
    final ip = dotenv.get(
      env == 'NUBE' ? 'SERVER_IP_NUBE' : 'SERVER_IP_LOCAL',
      fallback: '51.48.92.76',
    );
    return ip;
  }
  int get _puerto => int.parse(dotenv.get('PORT', fallback: '5000'));

  bool _estaConectando = false;
  
  // Almacén de promesas pendientes de respuesta por parte del servidor
  final Map<String, Completer<Map<String, dynamic>>> _peticionesPendientes = {};
  
  final StreamController<Map<String, dynamic>> _controladorRespuestas =
      StreamController.broadcast();

  /// Flujo continuo de mensajes entrantes (eventos de servidor o respuestas).
  Stream<Map<String, dynamic>> get respuestas => _controladorRespuestas.stream;

  // Semáforo para evitar colisiones en escrituras simultáneas al socket
  Future<void>? _operacionSendEnCurso;

  /// Envía una solicitud asíncrona y espera su respuesta única identificada por ticket.
  Future<Map<String, dynamic>> request(String accion, Map<String, dynamic> datos, {String? token, Duration timeout = const Duration(seconds: 15)}) async {
    final String idTicket = DateTime.now().microsecondsSinceEpoch.toString();
    final Completer<Map<String, dynamic>> completer = Completer();
    
    _peticionesPendientes[idTicket] = completer;

    final Map<String, dynamic> peticion = {
      'id_peticion': idTicket,
      'accion': accion,
      'datos': datos,
    };

    if (token != null) {
      peticion['token'] = token;
    }
    
    try {
      await send(peticion);
      return await completer.future.timeout(timeout);
    } catch (e) {
      _peticionesPendientes.remove(idTicket);
      rethrow;
    }
  }

  /// Realiza una prueba de conectividad rápida (Ping) al puerto del servidor.
  Future<bool> ping() async {
    try {
      final s = await Socket.connect(_servidor, _puerto,
          timeout: const Duration(seconds: 2));
      s.destroy();
      return true;
    } catch (_) {
      return false;
    }
  }

  /// Intenta establecer el socket TCP si no existe una sesión previa.
  Future<bool> connect() async {
    if (_socket != null) return true;
    if (_estaConectando) return false;

    _estaConectando = true;
    try {
      _socket = await Socket.connect(_servidor, _puerto,
          timeout: const Duration(seconds: 5));

      _socket!.listen(
        _onData,
        onError: _onError,
        onDone: _onDone,
        cancelOnError: false,
      );

      _estaConectando = false;
      return true;
    } catch (e) {
      debugPrint('[Socket] Error de conexión: $e');
      _socket = null;
      _estaConectando = false;
      return false;
    }
  }

  final List<int> _bufferBytes = [];

  /// Receptor primario de fragmentos binarios desde el socket.
  void _onData(Uint8List datos) {
    _bufferBytes.addAll(datos);
    _procesarBuffer();
  }

  /// Recompone los mensajes JSON a partir del flujo binario siguiendo el protocolo de longitud.
  void _procesarBuffer() {
    try {
      while (_bufferBytes.length >= 4) {
        // Lectura de la cabecera Big Endian de 4 bytes
        int longitud = (_bufferBytes[0] << 24) |
            (_bufferBytes[1] << 16) |
            (_bufferBytes[2] << 8) |
            _bufferBytes[3];

        if (longitud <= 0 || longitud > 10485760) {
          // Protección contra memoria insuficiente (límite 10MB)
          _bufferBytes.clear();
          return;
        }

        if (_bufferBytes.length < 4 + longitud) {
          return; // El mensaje aún no está completo en el buffer
        }

        final mensajeBytes = _bufferBytes.sublist(4, 4 + longitud);
        _bufferBytes.removeRange(0, 4 + longitud);

        try {
          final String jsonStr =
              utf8.decode(mensajeBytes, allowMalformed: true);
          final Map<String, dynamic> respuesta = jsonDecode(jsonStr);
          
          // Resolución de peticiones asíncronas mediante ID de ticket
          final String? idTicket = respuesta['id_peticion']?.toString();
          if (idTicket != null && _peticionesPendientes.containsKey(idTicket)) {
            _peticionesPendientes[idTicket]!.complete(respuesta);
            _peticionesPendientes.remove(idTicket);
          } else {
            // Eventos Push e información de sistema
            _controladorRespuestas.add(respuesta);
          }
        } catch (e) {
          debugPrint('[Socket] Error al procesar mensaje: $e');
        }
      }
    } catch (e) {
      debugPrint('[Socket] Error crítico en tratamiento de buffer: $e');
      _bufferBytes.clear();
    }
  }

  /// Serializa y envía un mapa JSON al servidor precedido de su longitud.
  /// Implementa sincronización atómica para evitar colisiones de red.
  Future<void> send(Map<String, dynamic> peticion) async {
    // Encadenamos la nueva operación al final de la cola actual
    final Future<void>? operacionPrevia = _operacionSendEnCurso;
    final Completer<void> completer = Completer<void>();
    _operacionSendEnCurso = completer.future;

    // Esperamos a que todas las operaciones anteriores hayan terminado
    if (operacionPrevia != null) {
      try {
        await operacionPrevia;
      } catch (_) {
        // Ignoramos errores de peticiones previas para no bloquear la cola
      }
    }

    try {
      if (_socket == null) {
        if (!await connect()) throw Exception('Servidor no disponible');
      }

      final List<int> jsonBytes = utf8.encode(jsonEncode(peticion));
      final int len = jsonBytes.length;

      final Uint8List cabecera = Uint8List(4);
      cabecera[0] = (len >> 24) & 0xFF;
      cabecera[1] = (len >> 16) & 0xFF;
      cabecera[2] = (len >> 8) & 0xFF;
      cabecera[3] = len & 0xFF;

      final constructor = BytesBuilder();
      constructor.add(cabecera);
      constructor.add(jsonBytes);

      _socket!.add(constructor.toBytes());
      await _socket!.flush();
    } catch (e) {
      debugPrint('[Socket] Error envío: $e');
      disconnect();
      rethrow;
    } finally {
      // Liberamos el semáforo para la siguiente petición en cola
      completer.complete();
    }
  }

  void _onError(error) {
    debugPrint('[Socket] Error stream: $error');
    disconnect();
  }

  void _onDone() {
    disconnect();
  }

  /// Finaliza la conexión actual y libera los recursos del socket.
  void disconnect() {
    _socket?.destroy();
    _socket = null;
  }
}

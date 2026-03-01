// Servicio base de Sockets.
// Gestiona el canal de comunicación persistente con el Servidor Java empleando una cabecera de tamaño de 4 bytes.
import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';
import 'package:flutter/foundation.dart';

class SocketService {
  static final SocketService _instance = SocketService._internal();
  factory SocketService() => _instance;
  SocketService._internal();

  Socket? _socket;
  // IP para el servidor: 10.0.2.2 es la IP del host en el emulador de Android
  // 127.0.0.1 es para Windows/Web.
  final String _servidor = Platform.isAndroid ? '10.0.2.2' : '127.0.0.1';
  final int _puerto = 5000;

  bool _estaConectando = false;
  final StreamController<Map<String, dynamic>> _controladorRespuestas =
      StreamController.broadcast();

  Stream<Map<String, dynamic>> get respuestas => _controladorRespuestas.stream;

  Future<bool> connect() async {
    if (_socket != null) return true;
    if (_estaConectando) return false;

    _estaConectando = true;
    try {
      debugPrint('Conectando al servidor en $_servidor:$_puerto...');
      _socket = await Socket.connect(_servidor, _puerto,
          timeout: const Duration(seconds: 5));
      debugPrint('Conectado exitosamente.');

      _socket!.listen(
        _onData,
        onError: _onError,
        onDone: _onDone,
        cancelOnError: false,
      );

      _estaConectando = false;
      return true;
    } catch (e) {
      debugPrint('Error de conexión: $e');
      _socket = null;
      _estaConectando = false;
      return false;
    }
  }

  final List<int> _bufferBytes = [];

  void _onData(Uint8List datos) {
    _bufferBytes.addAll(datos);
    _procesarBuffer();
  }

  void _procesarBuffer() {
    try {
      while (_bufferBytes.length >= 4) {
        // Leemos la longitud (4 bytes big-endian)
        int longitud = (_bufferBytes[0] << 24) |
            (_bufferBytes[1] << 16) |
            (_bufferBytes[2] << 8) |
            _bufferBytes[3];

        if (longitud <= 0 || longitud > 10485760) {
          // Buffer corrupto o longitud inválida
          _bufferBytes.clear();
          return;
        }

        if (_bufferBytes.length < 4 + longitud) {
          // No tenemos el mensaje completo todavía
          return;
        }
        // Parseo silencioso
        // _logger.d('Procesando mensaje de longitud: $longitud');

        // Extraemos el mensaje
        final mensajeBytes = _bufferBytes.sublist(4, 4 + longitud);

        // Eliminamos del buffer lo ya procesado ANTES de decodificar
        _bufferBytes.removeRange(0, 4 + longitud);

        String jsonStr;
        try {
          // allowMalformed: true para tolerar surrogates de Java (emojis en UTF-16)
          jsonStr = utf8.decode(mensajeBytes, allowMalformed: true);
        } catch (e) {
          debugPrint(
              'Error decodificando bytes a UTF-8: $e — se descarta el mensaje');
          continue;
        }

        try {
          final Map<String, dynamic> respuesta = jsonDecode(jsonStr);
          _controladorRespuestas.add(respuesta);
        } catch (e) {
          debugPrint(
              'Error al decodificar JSON: $e | raw: ${jsonStr.substring(0, jsonStr.length.clamp(0, 100))}');
        }
      }
    } catch (e) {
      debugPrint('Error al procesar buffer del socket: $e — limpiando buffer');
      _bufferBytes.clear();
    }
  }

  Future<void> send(Map<String, dynamic> peticion) async {
    if (_socket == null) {
      bool conectado = await connect();
      if (!conectado) throw Exception('No se pudo conectar al servidor');
    }

    try {
      final String jsonStr = jsonEncode(peticion);
      final List<int> jsonBytes = utf8.encode(jsonStr);

      // Preparar los 4 bytes de longitud (Big Endian)
      final int len = jsonBytes.length;
      final Uint8List cabecera = Uint8List(4);
      cabecera[0] = (len >> 24) & 0xFF;
      cabecera[1] = (len >> 16) & 0xFF;
      cabecera[2] = (len >> 8) & 0xFF;
      cabecera[3] = len & 0xFF;

      final BytesBuilder constructorBites = BytesBuilder();
      constructorBites.add(cabecera);
      constructorBites.add(jsonBytes);

      _socket!.add(constructorBites.toBytes());
      await _socket!.flush();
    } catch (e) {
      debugPrint('Error al enviar mensaje: $e');
      disconnect();
      rethrow;
    }
  }

  void _onError(error) {
    debugPrint('Error en el socket: $error');
    disconnect();
  }

  void _onDone() {
    // Conexión cerrada
    disconnect();
  }

  void disconnect() {
    _socket?.destroy();
    _socket = null;
  }
}

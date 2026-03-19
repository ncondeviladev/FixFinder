import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';
import 'package:flutter/foundation.dart';

/**
 * Servicio centralizado para la comunicación por Sockets con el servidor Java.
 * 
 * Implementa el protocolo de comunicación basado en cabeceras de 4 bytes 
 * (Big Endian) que indican la longitud del mensaje JSON siguiente.
 * Sigue el patrón Singleton para garantizar una única conexión activa.
 */
class SocketService {
  static final SocketService _instance = SocketService._internal();
  factory SocketService() => _instance;
  SocketService._internal();

  Socket? _socket;

  // Configuración de red
  final String _servidor = '192.168.0.13';
  final int _puerto = 5000;

  bool _estaConectando = false;
  final StreamController<Map<String, dynamic>> _controladorRespuestas =
      StreamController.broadcast();

  /**
   * Stream de respuestas entrantes desde el servidor.
   */
  Stream<Map<String, dynamic>> get respuestas => _controladorRespuestas.stream;

  /**
   * Establece la conexión con el servidor si no existe ya una activa.
   */
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

  /**
   * Callback para la recepción de fragmentos de datos.
   */
  void _onData(Uint8List datos) {
    _bufferBytes.addAll(datos);
    _procesarBuffer();
  }

  /**
   * Procesa el buffer acumulativo para extraer mensajes completos según el protocolo.
   */
  void _procesarBuffer() {
    try {
      while (_bufferBytes.length >= 4) {
        // Leemos la longitud big-endian de 4 bytes
        int longitud = (_bufferBytes[0] << 24) |
            (_bufferBytes[1] << 16) |
            (_bufferBytes[2] << 8) |
            _bufferBytes[3];

        if (longitud <= 0 || longitud > 10485760) {
          // Límite de 10MB para seguridad
          _bufferBytes.clear();
          return;
        }

        if (_bufferBytes.length < 4 + longitud) {
          return; // Esperando más fragmentos
        }

        final mensajeBytes = _bufferBytes.sublist(4, 4 + longitud);
        _bufferBytes.removeRange(0, 4 + longitud);

        try {
          final String jsonStr =
              utf8.decode(mensajeBytes, allowMalformed: true);
          final Map<String, dynamic> respuesta = jsonDecode(jsonStr);
          _controladorRespuestas.add(respuesta);
        } catch (e) {
          debugPrint('[Socket] Error al procesar mensaje: $e');
        }
      }
    } catch (e) {
      debugPrint('[Socket] Error crítico en buffer: $e');
      _bufferBytes.clear();
    }
  }

  /**
   * Envía una petición JSON al servidor.
   */
  Future<void> send(Map<String, dynamic> peticion) async {
    if (_socket == null) {
      if (!await connect()) throw Exception('Servidor no disponible');
    }

    try {
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
    }
  }

  void _onError(error) {
    debugPrint('[Socket] Error stream: $error');
    disconnect();
  }

  void _onDone() {
    disconnect();
  }

  /**
   * Cierra la conexión y libera recursos.
   */
  void disconnect() {
    _socket?.destroy();
    _socket = null;
  }
}

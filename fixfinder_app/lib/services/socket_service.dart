import 'dart:async';
import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:http/http.dart' as http;
import 'package:stomp_dart_client/stomp_dart_client.dart';

/// Servicio de comunicación adaptado para el nuevo backend Spring Boot.
/// Reemplaza la conexión Socket manual por REST y WebSockets (STOMP).
class SocketService {
  static final SocketService _instance = SocketService._internal();
  factory SocketService() => _instance;
  SocketService._internal() {
    _initStomp();
  }

  StompClient? _stompClient;
  
  bool get isConectado => _stompClient?.connected ?? false;

  String get _baseUrl {
    final env = dotenv.get('ENVIRONMENT', fallback: 'NUBE');
    final ip = dotenv.get(
      env == 'NUBE' ? 'SERVER_IP_NUBE' : 'SERVER_IP_LOCAL',
      fallback: '127.0.0.1',
    );
    final port = dotenv.get('PORT', fallback: '8080');
    return 'http://$ip:$port/api';
  }

  String get _wsUrl {
    final env = dotenv.get('ENVIRONMENT', fallback: 'NUBE');
    final ip = dotenv.get(
      env == 'NUBE' ? 'SERVER_IP_NUBE' : 'SERVER_IP_LOCAL',
      fallback: '127.0.0.1',
    );
    final port = dotenv.get('PORT', fallback: '8080');
    return 'ws://$ip:$port/ws';
  }

  final StreamController<Map<String, dynamic>> _controladorRespuestas =
      StreamController.broadcast();

  Stream<Map<String, dynamic>> get respuestas => _controladorRespuestas.stream;

  void _initStomp() {
    _stompClient = StompClient(
      config: StompConfig(
        url: _wsUrl,
        onConnect: (frame) {
          debugPrint('[STOMP] Conectado');
          _stompClient?.subscribe(
            destination: '/topic/trabajos',
            callback: (frame) => _onWsMessage(frame.body),
          );
          _stompClient?.subscribe(
            destination: '/topic/notificaciones',
            callback: (frame) => _onWsMessage(frame.body),
          );
        },
        onWebSocketError: (error) => debugPrint('[STOMP] Error: $error'),
      ),
    );
    _stompClient?.activate();
  }

  void _onWsMessage(String? body) {
    if (body != null) {
      final data = jsonDecode(body);
      _controladorRespuestas.add(data);
    }
  }

  Future<bool> connect() async {
    if (!isConectado) {
      _stompClient?.activate();
    }
    return true;
  }

  /// Realiza una petición REST y devuelve el resultado en el formato esperado por el App.
  Future<Map<String, dynamic>> request(String accion, Map<String, dynamic> datos, {String? token, Duration timeout = const Duration(seconds: 15)}) async {
    String path = _mapAccionToPath(accion, datos);
    final url = Uri.parse('$_baseUrl$path');
    
    try {
      final response = await http.post(
        url,
        headers: {
          'Content-Type': 'application/json',
          if (token != null) 'Authorization': 'Bearer $token',
        },
        body: jsonEncode(datos),
      ).timeout(timeout);

      final decoded = jsonDecode(response.body);
      
      // Adaptar respuesta al formato anterior del Socket
      return {
        'status': response.statusCode,
        'accion': accion,
        'datos': decoded,
        'mensaje': response.statusCode >= 400 ? (decoded['error'] ?? 'Error') : 'OK',
      };
    } catch (e) {
      return {
        'status': 500,
        'mensaje': 'Error de conexión: $e',
      };
    }
  }

  String _mapAccionToPath(String accion, Map<String, dynamic> datos) {
    switch (accion) {
      case 'LOGIN': return '/auth/login';
      case 'REGISTRO': return '/auth/register';
      case 'CREAR_TRABAJO': return '/trabajos/solicitar';
      case 'LISTAR_TRABAJOS': return '/trabajos/cliente/${datos['idCliente']}';
      default: return '/';
    }
  }

  Future<bool> ping() async {
    try {
      final response = await http.get(Uri.parse('$_baseUrl/empresas')).timeout(const Duration(seconds: 5));
      return response.statusCode == 200;
    } catch (_) {
      return false;
    }
  }

  void disconnect() {
    _stompClient?.deactivate();
  }
}

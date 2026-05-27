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

  int? _idUsuarioLogueado;
  dynamic _userSubscription;

  void suscribirCanalUsuario(int idUsuario) {
    _idUsuarioLogueado = idUsuario;
    _suscribirCanalUsuarioActual();
  }

  void _suscribirCanalUsuarioActual() {
    final idTemp = _idUsuarioLogueado;
    desuscribirCanalUsuario();
    _idUsuarioLogueado = idTemp;
    
    final bool isActuallyConnected = isConectado || (_stompClient?.connected ?? false);
    debugPrint('[STOMP-DEBUG] _suscribirCanalUsuarioActual: isConectado=$isConectado, isActuallyConnected=$isActuallyConnected, _idUsuarioLogueado=$_idUsuarioLogueado');
    
    if (isActuallyConnected && _idUsuarioLogueado != null) {
      try {
        _userSubscription = _stompClient?.subscribe(
          destination: '/topic/usuario/$_idUsuarioLogueado',
          callback: (frame) => _onWsMessage(frame.body),
        );
        debugPrint('[STOMP] Suscrito a canal privado de usuario: /topic/usuario/$_idUsuarioLogueado');
      } catch (e) {
        debugPrint('[STOMP-ERROR] Error subscribiendo canal privado: $e');
      }
    }
  }

  void desuscribirCanalUsuario() {
    if (_userSubscription != null) {
      try {
        _userSubscription();
      } catch (_) {}
      _userSubscription = null;
    }
    _idUsuarioLogueado = null;
  }

  void _initStomp() {
    _stompClient = StompClient(
      config: StompConfig(
        url: _wsUrl,
        reconnectDelay: const Duration(seconds: 5),
        onConnect: (frame) {
          debugPrint('[STOMP] Conectado');
          _stompClient?.subscribe(
            destination: '/topic/trabajos',
            callback: (frame) => _onWsMessage(frame.body),
          );
          _stompClient?.subscribe(
            destination: '/topic/presupuestos',
            callback: (frame) => _onWsMessage(frame.body),
          );
          _stompClient?.subscribe(
            destination: '/topic/usuarios',
            callback: (frame) => _onWsMessage(frame.body),
          );
          
          if (_idUsuarioLogueado != null) {
            _suscribirCanalUsuarioActual();
          }
        },
        onWebSocketError: (error) => debugPrint('[STOMP] Error: $error'),
      ),
    );
    _stompClient?.activate();
  }

  void _onWsMessage(String? body) {
    if (body != null) {
      debugPrint('[STOMP-MESSAGE] Received body: $body');
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
    final method = _getMethodForAccion(accion);
    
    try {
      final headers = {
        'Content-Type': 'application/json',
        if (token != null) 'Authorization': 'Bearer $token',
      };

      http.Response response;
      if (method == 'GET') {
        response = await http.get(url, headers: headers).timeout(timeout);
      } else if (method == 'PUT') {
        response = await http.put(url, headers: headers, body: jsonEncode(datos)).timeout(timeout);
      } else {
        response = await http.post(url, headers: headers, body: jsonEncode(datos)).timeout(timeout);
      }

      final decoded = jsonDecode(response.body);
      
      // Adaptar respuesta al formato anterior del Socket
      return {
        'status': response.statusCode,
        'accion': accion,
        'datos': decoded,
        'mensaje': response.statusCode >= 400 ? (decoded['error'] ?? decoded['mensaje'] ?? 'Error') : 'OK',
      };
    } catch (e) {
      return {
        'status': 500,
        'mensaje': 'Error de conexión: $e',
      };
    }
  }

  String _getMethodForAccion(String accion) {
    switch (accion) {
      case 'LISTAR_TRABAJOS':
      case 'LISTAR_PRESUPUESTOS':
        return 'GET';
      case 'MODIFICAR_TRABAJO':
      case 'MODIFICAR_USUARIO':
        return 'PUT';
      default:
        return 'POST';
    }
  }

  String _mapAccionToPath(String accion, Map<String, dynamic> datos) {
    switch (accion) {
      case 'LOGIN': return '/auth/login';
      case 'REGISTRO': return '/auth/register';
      case 'CREAR_TRABAJO': return '/trabajos/solicitar';
      case 'LISTAR_TRABAJOS':
        final rol = datos['rol'];
        final idUsuario = datos['idUsuario'] ?? datos['idCliente'] ?? datos['idOperario'];
        if (rol == 'OPERARIO') {
          return '/trabajos/operario/$idUsuario';
        } else if (rol == 'GERENTE') {
          final idEmpresa = datos['idEmpresa'];
          return '/trabajos/pendientes?idEmpresa=$idEmpresa';
        } else {
          return '/trabajos/cliente/$idUsuario';
        }
      case 'ASIGNAR_OPERARIO':
        return '/trabajos/${datos['idTrabajo']}/asignar/${datos['idOperario']}';
      case 'FINALIZAR_TRABAJO':
        return '/trabajos/${datos['idTrabajo']}/finalizar';
      case 'LISTAR_PRESUPUESTOS':
        return '/presupuestos/trabajo/${datos['idTrabajo']}';
      case 'ACEPTAR_PRESUPUESTO':
        return '/presupuestos/${datos['idPresupuesto']}/aceptar';
      case 'RECHAZAR_PRESUPUESTO':
        return '/presupuestos/${datos['idPresupuesto']}/rechazar';
      case 'VALORAR_TRABAJO':
        return '/trabajos/${datos['idTrabajo']}/valorar';
      case 'CANCELAR_TRABAJO':
        return '/trabajos/${datos['idTrabajo']}/cancelar';
      case 'MODIFICAR_TRABAJO':
        return '/trabajos/${datos['idTrabajo']}';
      case 'ACTUALIZAR_FOTO_PERFIL':
        return '/usuarios/${datos['idUsuario']}/foto';
      case 'MODIFICAR_USUARIO':
        return '/usuarios/${datos['id']}';
      case 'GET_OPERARIOS':
        return '/empresas/${datos['idEmpresa']}/operarios';
      case 'MODIFICAR_OPERARIO':
        return '/operarios/${datos['id']}';
      case 'MODIFICAR_EMPRESA':
        return '/empresas/${datos['id']}';
      case 'CREAR_PRESUPUESTO':
        return '/presupuestos';
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

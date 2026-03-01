// Servicio de Autenticación.
// Gestiona el login, logout y la persistencia de sesión mediante SharedPreferences.
import 'dart:async';
import 'package:shared_preferences/shared_preferences.dart';
import 'dart:convert';
import '../models/usuario.dart';
import 'socket_service.dart';

class AuthService {
  static final AuthService _instance = AuthService._internal();
  factory AuthService() => _instance;
  AuthService._internal();

  final SocketService _socket = SocketService();

  Usuario? _usuarioActual;
  Usuario? get usuarioActual => _usuarioActual;

  Future<bool> login(String email, String password) async {
    final Map<String, dynamic> peticion = {
      'accion': 'LOGIN',
      'datos': {
        'email': email,
        'password': password,
      }
    };

    StreamSubscription? suscripcion;
    try {
      await _socket.connect();

      final completer = Completer<Map<String, dynamic>>();

      suscripcion = _socket.respuestas.listen((respuesta) {
        final msg = respuesta['mensaje']?.toString() ?? '';
        final status = respuesta['status'];
        // Solo completar si es una respuesta de LOGIN: tiene token o es un error de auth
        if (status == 200 && respuesta['token'] != null) {
          if (!completer.isCompleted) completer.complete(respuesta);
        } else if (status == 401 && msg.contains('redencial')) {
          if (!completer.isCompleted) completer.complete(respuesta);
        } else if (status == 500 && msg.contains('nterno')) {
          if (!completer.isCompleted) completer.complete(respuesta);
        }
      });

      await _socket.send(peticion);

      final respuesta =
          await completer.future.timeout(const Duration(seconds: 10));

      if (respuesta['status'] == 200) {
        final datosUsuario = respuesta['datos'];
        final String token = respuesta['token'] ?? '';

        _usuarioActual = Usuario.fromJson({
          ...datosUsuario,
          'token': token,
        });

        // INTENTO DE PERSISTENCIA (Si falla por los plugins de Windows, lo ignoramos)
        try {
          final prefs = await SharedPreferences.getInstance();
          await prefs.setString(
              'userData', jsonEncode(_usuarioActual!.toJson()));
        } catch (e) {
          // No se pudo persistir la sesión, continuamos en memoria
        }

        return true;
      }
      return false;
    } catch (e) {
      return false;
    } finally {
      await suscripcion?.cancel();
    }
  }

  Future<void> logout() async {
    _usuarioActual = null;
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.clear();
    } catch (e) {
      // Ignorar
    }
    _socket.disconnect();
  }

  Future<bool> tryAutoLogin() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final userDataStr = prefs.getString('userData');
      if (userDataStr == null) return false;

      final userData = jsonDecode(userDataStr);
      if (userData['token'] == null || userData['id'] == null) return false;

      _usuarioActual = Usuario.fromJson(userData);
      return true;
    } catch (e) {
      // Ignorar errores de plugin
    }
    return false;
  }
}

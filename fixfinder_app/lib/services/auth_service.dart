import 'dart:async';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:logger/logger.dart';
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
        if (respuesta['status'] != null &&
            (respuesta['mensaje']?.toString().contains('Login') == true ||
                respuesta['token'] != null ||
                respuesta['status'] == 401)) {
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
          await prefs.setString('token', token);
          await prefs.setInt('userId', _usuarioActual!.id);
          await prefs.setString('userRol', _usuarioActual!.rol.name);
          await prefs.setString('userEmail', _usuarioActual!.email);
          await prefs.setString('userNombre', _usuarioActual!.nombreCompleto);
        } catch (e) {
          Logger().w(
              'No se pudo persistir la sesión (error de plugin), pero continuamos en memoria: $e');
        }

        return true;
      }
      return false;
    } catch (e) {
      Logger().e('Error durante el proceso de login: $e');
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
      Logger().w('Error al limpiar SharedPreferences: $e');
    }
    _socket.disconnect();
  }

  Future<bool> tryAutoLogin() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final token = prefs.getString('token');
      if (token == null) return false;

      final idUsuario = prefs.getInt('userId');
      final rolUsuario = prefs.getString('userRol');
      final email = prefs.getString('userEmail') ?? '';
      final nombre = prefs.getString('userNombre') ?? '';

      // Si faltan datos críticos, forzamos login para evitar perfil vacío
      if (idUsuario == null || rolUsuario == null || email.isEmpty)
        return false;

      _usuarioActual = Usuario(
        id: idUsuario,
        email: email,
        nombreCompleto: nombre,
        rol: Rol.values
            .firstWhere((e) => e.name == rolUsuario, orElse: () => Rol.CLIENTE),
        token: token,
      );
      return true;
    } catch (e) {
      // Ignorar errores de plugin en el arranque
      Logger().w('Error en auto-login (plugin no listo): $e');
    }
    return false;
  }
}

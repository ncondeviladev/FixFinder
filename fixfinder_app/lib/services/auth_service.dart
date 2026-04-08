// Servicio de Autenticación.
// Gestiona el login, logout y la persistencia de sesión mediante SharedPreferences.
import 'dart:async';
import 'package:flutter/foundation.dart';
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
    try {
      final respuesta = await _socket.request('LOGIN', {
        'email': email.trim(),
        'password': password.trim(),
      });

      if (respuesta['status'] == 200) {
        final datosUsuario = respuesta['datos'];
        final String token = respuesta['token'] ?? '';

        _usuarioActual = Usuario.fromJson({
          ...datosUsuario,
          'token': token,
        });

        // Intentar persistir la sesión
        try {
          final prefs = await SharedPreferences.getInstance();
          await prefs.setString(
              'userData', jsonEncode(_usuarioActual!.toJson()));
        } catch (_) {}

        return true;
      }
      return false;
    } catch (e) {
      debugPrint('[AuthService] Error en login: $e');
      return false;
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

  Future<bool> actualizarFotoPerfil(String urlFoto) async {
    if (_usuarioActual == null) return false;

    try {
      final respuesta = await _socket.request(
        'ACTUALIZAR_FOTO_PERFIL',
        {
          'idUsuario': _usuarioActual!.id,
          'url_foto': urlFoto,
        },
        token: _usuarioActual!.token,
      );

      if (respuesta['status'] == 200) {
        _usuarioActual!.urlFoto = urlFoto;
        try {
          final prefs = await SharedPreferences.getInstance();
          await prefs.setString(
              'userData', jsonEncode(_usuarioActual!.toJson()));
        } catch (_) {}
        return true;
      }
      return false;
    } catch (e) {
      debugPrint('[AuthService] Error actualizando foto: $e');
      return false;
    }
  }

  Future<Map<String, dynamic>> registrar({
    required String nombre,
    required String dni,
    required String email,
    required String telefono,
    required String direccion,
    required String password,
    String? urlFoto,
  }) async {
    try {
      final respuesta = await _socket.request('REGISTRO_USUARIO', {
        'esOperario': false,
        'nombreCompleto': nombre,
        'dni': dni,
        'email': email,
        'password': password,
        'telefono': telefono,
        'direccion': direccion,
        'url_foto': urlFoto ?? '',
      });
      return respuesta;
    } catch (e) {
      return {'status': 500, 'mensaje': 'Error de conexión: $e'};
    }
  }
}

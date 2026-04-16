import 'dart:async';
import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'dart:convert';
import '../models/usuario.dart';
import 'socket_service.dart';

/// Servicio de Autenticación.
/// 
/// Se encarga de gestionar el ciclo de vida de la sesión del usuario:
/// login, cierre de sesión, registro y persistencia local de datos.
class AuthService {
  static final AuthService _instance = AuthService._internal();
  factory AuthService() => _instance;
  AuthService._internal();

  final SocketService _socket = SocketService();

  Usuario? _usuarioActual;
  
  /// El usuario que ha iniciado sesión actualmente. Si es null, no hay sesión activa.
  Usuario? get usuarioActual => _usuarioActual;

  /// Inicia el proceso de autenticación con el servidor.
  /// 
  /// Envía las credenciales y, en caso de éxito, persiste el token y los datos
  /// del usuario en SharedPreferences para futuras sesiones.
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

  /// Intenta recuperar una sesión previa desde el almacenamiento persistente.
  /// 
  /// Devuelve true si el token y los datos básicos son válidos.
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
      return false;
    }
  }

  /// Actualiza la fotografía de perfil del usuario en el servidor y localmente.
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

  /// Modifica los datos personales del perfil del usuario.
  /// 
  /// Refresca el estado en memoria si el servidor confirma los cambios.
  Future<bool> actualizarPerfil({
    required String nombre,
    required String email,
    required String telefono,
    required String direccion,
  }) async {
    if (_usuarioActual == null) return false;

    try {
      final respuesta = await _socket.request(
        'MODIFICAR_USUARIO',
        {
          'id': _usuarioActual!.id,
          'nombre': nombre,
          'email': email,
          'telefono': telefono,
          'direccion': direccion,
        },
        token: _usuarioActual!.token,
      );

      if (respuesta['status'] == 200) {
        // Creamos una nueva instancia para respetar la inmutabilidad (campos final)
        _usuarioActual = Usuario(
          id: _usuarioActual!.id,
          email: email,
          nombreCompleto: nombre,
          rol: _usuarioActual!.rol,
          token: _usuarioActual!.token,
          telefono: telefono,
          direccion: direccion,
          dni: _usuarioActual!.dni,
          urlFoto: _usuarioActual!.urlFoto,
          fechaRegistro: _usuarioActual!.fechaRegistro,
        );

        // Persistimos los cambios localmente
        try {
          final prefs = await SharedPreferences.getInstance();
          await prefs.setString(
              'userData', jsonEncode(_usuarioActual!.toJson()));
        } catch (_) {}
        return true;
      }
      return false;
    } catch (e) {
      debugPrint('[AuthService] Error actualizando perfil: $e');
      return false;
    }
  }

  /// Registra un nuevo cliente en el sistema FixFinder.
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
      final respuesta = await _socket.request('REGISTRO', {
        'tipo': 'CLIENTE',
        'nombre': nombre,
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

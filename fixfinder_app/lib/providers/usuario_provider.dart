import 'package:flutter/material.dart';
import '../models/usuario.dart';
import '../services/auth_service.dart';
import '../services/socket_service.dart';

/// Provider encargado de gestionar el estado del usuario actual de forma reactiva.
/// 
/// Permite que la interfaz de usuario (Drawer, Perfil, etc.) se actualice 
/// automáticamente cuando cambian los datos del usuario sin necesidad de refrescos manuales.
class UsuarioProvider with ChangeNotifier {
  Usuario? _usuario;

  Usuario? get usuario => _usuario;

  UsuarioProvider() {
    SocketService().respuestas.listen((respuesta) {
      if (respuesta['accion'] == 'BROADCAST') {
        final datos = respuesta['datos'] ?? {};
        if (datos['categoria'] == 'USUARIO' || datos['categoria'] == 'DATOS') {
          if (_usuario != null && datos['idUsuario'] == _usuario!.id) {
            final nuevoUsuario = Usuario(
              id: _usuario!.id,
              email: datos['email'] ?? _usuario!.email,
              nombreCompleto: datos['nombre'] ?? _usuario!.nombreCompleto,
              rol: _usuario!.rol,
              token: _usuario!.token,
              telefono: datos['telefono'] ?? _usuario!.telefono,
              direccion: datos['direccion'] ?? _usuario!.direccion,
              dni: _usuario!.dni,
              urlFoto: datos.containsKey('url_foto') ? datos['url_foto'] : (datos.containsKey('urlFoto') ? datos['urlFoto'] : _usuario!.urlFoto),
              fechaRegistro: _usuario!.fechaRegistro,
              idEmpresa: _usuario!.idEmpresa,
            );
            establecerUsuario(nuevoUsuario);
            AuthService().actualizarUsuarioLocal(nuevoUsuario);
          }
        }
      }
    });
  }

  /// Inicializa el provider con el usuario autenticado actualmente si existe.
  void inicializarDesdeAuth() {
    _usuario = AuthService().usuarioActual;
    notifyListeners();
  }

  /// Actualiza el usuario en el provider y notifica a los escuchas.
  void establecerUsuario(Usuario? nuevoUsuario) {
    _usuario = nuevoUsuario;
    notifyListeners();
  }

  /// Método de conveniencia para refrescar los datos desde el servicio.
  void refrescar() {
    _usuario = AuthService().usuarioActual;
    notifyListeners();
  }

  /// Limpia los datos del usuario (al cerrar sesión).
  void limpiar() {
    _usuario = null;
    notifyListeners();
  }
}

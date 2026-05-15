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

  /// Inicializa el provider con el usuario autenticado actualmente si existe.
  void inicializarDesdeAuth() {
    _usuario = AuthService().usuarioActual;
    _inicializarEscuchaSockets();
    notifyListeners();
  }

  /// Escucha actualizaciones de perfil enviadas por el servidor (Push).
  void _inicializarEscuchaSockets() {
    SocketService().respuestas.listen((respuesta) {
      if (respuesta['accion'] == 'BROADCAST') {
        final datos = respuesta['datos'] ?? {};
        if (datos['categoria'] == 'USUARIO') {
          final int idUpdate = datos['idUsuario'] ?? 0;
          if (_usuario != null && idUpdate == _usuario!.id) {
            debugPrint('👤 [USUARIO-UPDATE] Actualizando datos de perfil...');
            
            // Actualizamos los campos recibidos en el broadcast usando inmutabilidad
            _usuario = _usuario!.copyWith(
              urlFoto: datos['url_foto'],
              nombreCompleto: datos['nombre'],
              email: datos['email'],
              telefono: datos['telefono'],
              direccion: datos['direccion'],
            );
            
            // Sincronizamos con el singleton de AuthService para persistencia
            AuthService().actualizarUsuarioLocal(_usuario!);
            notifyListeners();
          }
        }
      }
    });
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

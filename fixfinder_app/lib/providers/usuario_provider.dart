import 'package:flutter/material.dart';
import '../models/usuario.dart';
import '../services/auth_service.dart';

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

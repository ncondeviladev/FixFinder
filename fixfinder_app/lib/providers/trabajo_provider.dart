import 'dart:async';
import 'package:flutter/material.dart';
import '../models/trabajo.dart';
import '../services/socket_service.dart';
import '../services/auth_service.dart';
import '../services/job_api_service.dart';
import '../models/presupuesto.dart';

/// Proveedor de estado (Provider) para la gestión del ciclo de vida de los Trabajos.
/// 
/// Orquestra el estado de la lista de incidencias para el usuario activo,
/// gestiona el refresco automático (polling) y delega las operaciones de red
/// al JobApiService.
class TrabajoProvider with ChangeNotifier {
  final JobApiService _api = JobApiService();
  final SocketService _socket = SocketService();
  final AuthService _auth = AuthService();

  List<Trabajo> _trabajos = [];
  bool _estaCargando = false;
  Timer? _pollingTimer;

  // --- Getters ---
  List<Trabajo> get trabajos => _trabajos;
  bool get estaCargando => _estaCargando;

  TrabajoProvider() {
    _inicializarEscuchaSockets();
  }

  /// Configura la escucha de eventos en tiempo real.
  void _inicializarEscuchaSockets() {
    _socket.respuestas.listen((respuesta) {
      if (respuesta['event'] == 'NEW_JOB_ASSIGNED') {
        obtenerTrabajos();
      }
    });
  }

  // --- Gestión de Polling ---

  /// Inicia el ciclo de refresco automático.
  void startPolling({int intervaloSegundos = 15}) {
    _pollingTimer?.cancel();
    _pollingTimer = Timer.periodic(
      Duration(seconds: intervaloSegundos),
      (_) => _actualizarEstadoSilencioso(),
    );
  }

  /// Detiene el ciclo de refresco automático.
  void stopPolling() {
    _pollingTimer?.cancel();
    _pollingTimer = null;
  }

  /// Actualiza la lista de trabajos sin disparar indicadores de carga invasivos.
  /// Solo notifica si detecta cambios críticos de estado.
  Future<void> _actualizarEstadoSilencioso() async {
    final usuario = _auth.usuarioActual;
    if (usuario == null) return;

    try {
      final nuevaLista = await _api.fetchTrabajos(usuario);
      if (_detectarCambiosRelevantes(nuevaLista)) {
        _trabajos = nuevaLista;
        notifyListeners();
      }
    } catch (_) {
      // Ignorar errores en refresco silencioso
    }
  }

  bool _detectarCambiosRelevantes(List<Trabajo> nuevaLista) {
    if (nuevaLista.length != _trabajos.length) return true;
    for (int i = 0; i < nuevaLista.length; i++) {
      final nuevo = nuevaLista[i];
      final actual =
          _trabajos.firstWhere((t) => t.id == nuevo.id, orElse: () => nuevo);
      if (actual.estado != nuevo.estado) return true;
      if (actual.presupuesto?.estado != nuevo.presupuesto?.estado) return true;
    }
    return false;
  }

  // --- Operaciones de Negocio ---

  /// Carga completa de trabajos con indicador de carga.
  Future<void> obtenerTrabajos() async {
    final usuario = _auth.usuarioActual;
    if (usuario == null) return;

    _estaCargando = true;
    notifyListeners();

    try {
      final lista = await _api.fetchTrabajos(usuario);
      _trabajos =
          lista.where((t) => t.estado != EstadoTrabajo.CANCELADO).toList();
      _ordenarTrabajosPorPrioridad();
    } catch (e) {
      debugPrint('Error obteniendo trabajos: $e');
    } finally {
      _estaCargando = false;
      notifyListeners();
    }
  }

  void _ordenarTrabajosPorPrioridad() {
    _trabajos
        .sort((a, b) => _obtenerPesoEstado(a).compareTo(_obtenerPesoEstado(b)));
  }

  Future<bool> crearTrabajo(Map<String, dynamic> datosTrabajo) async {
    final usuario = _auth.usuarioActual;
    if (usuario == null) return false;
    return await _api.createTrabajo(usuario, datosTrabajo);
  }

  Future<bool> actualizarEstadoTrabajo(int id, EstadoTrabajo nuevoEstado,
      {String? informe, List<String>? fotos}) async {
    final usuario = _auth.usuarioActual;
    if (usuario == null) return false;

    final exito = await _api.finalizeTrabajo(
        usuario, id, informe ?? 'Finalizado desde App', fotos);

    if (exito) await obtenerTrabajos();
    return exito;
  }

  Future<bool> cancelarTrabajo(int idTrabajo) async {
    final usuario = _auth.usuarioActual;
    if (usuario == null) return false;

    final exito = await _api.cancelTrabajo(
        usuario, idTrabajo, 'Cancelado por el usuario');
    if (exito) await obtenerTrabajos();
    return exito;
  }

  Future<bool> modificarTrabajo(
      int idTrabajo, Map<String, dynamic> datosTrabajo) async {
    final usuario = _auth.usuarioActual;
    if (usuario == null) return false;

    final exito = await _api.modifyTrabajo(usuario, idTrabajo, datosTrabajo);
    if (exito) await obtenerTrabajos();
    return exito;
  }

  Future<List<Presupuesto>> obtenerPresupuestos(int idTrabajo) async {
    final usuario = _auth.usuarioActual;
    if (usuario == null) return [];
    return await _api.fetchPresupuestos(usuario, idTrabajo);
  }

  Future<bool> aceptarPresupuesto(int idPresupuesto) async {
    final usuario = _auth.usuarioActual;
    if (usuario == null) return false;

    final exito = await _api.acceptPresupuesto(usuario, idPresupuesto);
    if (exito) await obtenerTrabajos();
    return exito;
  }

  Future<bool> valorarTrabajo(
      int idTrabajo, int valoracion, String comentario) async {
    final usuario = _auth.usuarioActual;
    if (usuario == null) return false;
    return await _api.valorateTrabajo(
        usuario, idTrabajo, valoracion, comentario);
  }

  // --- Utilidades ---

  int _obtenerPesoEstado(Trabajo trabajo) {
    if (trabajo.estado == EstadoTrabajo.FINALIZADO && trabajo.valoracion == 0) {
      return 0; // Máxima prioridad: pendiente de valorar
    }

    switch (trabajo.estado) {
      case EstadoTrabajo.PRESUPUESTADO:
      case EstadoTrabajo.ACEPTADO:
      case EstadoTrabajo.ASIGNADO:
        return 0;
      case EstadoTrabajo.PENDIENTE:
        return 1;
      case EstadoTrabajo.REALIZADO:
        return 2;
      case EstadoTrabajo.FINALIZADO:
      case EstadoTrabajo.PAGADO:
        return 3;
      case EstadoTrabajo.CANCELADO:
        return 4;
    }
  }
}

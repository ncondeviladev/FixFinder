import 'dart:async';
import 'package:flutter/material.dart';
import '../models/trabajo.dart';
import '../services/socket_service.dart';
import '../services/auth_service.dart';
import '../services/job_api_service.dart';
import '../models/presupuesto.dart';

/// Gestor de estado para todo lo relacionado con los Trabajos (Incidencias).
/// 
/// Esta clase es el "cerebro" de la UI: centraliza la lista de trabajos,
/// controla los estados de carga, maneja el refresco automático (polling)
/// y reacciona a eventos en tiempo real mediante Sockets.
class TrabajoProvider with ChangeNotifier {
  final JobApiService _api = JobApiService();
  final SocketService _socket = SocketService();
  final AuthService _auth = AuthService();

  List<Trabajo> _trabajos = [];
  bool _estaCargando = false;
  Timer? _pollingTimer;

  // --- Getters ---
  
  /// Lista de trabajos filtrada del usuario actual.
  List<Trabajo> get trabajos => _trabajos;
  
  /// Flag para mostrar spinners de carga en la UI.
  bool get estaCargando => _estaCargando;

  /// Constructor que inicia la escucha de eventos en tiempo real.
  TrabajoProvider() {
    _inicializarEscuchaSockets();
  }

  /// Suscribe el provider a eventos del socket para reaccionar a cambios externos
  /// (ej: cuando un gerente asigna un operario a un trabajo).
  void _inicializarEscuchaSockets() {
    _socket.respuestas.listen((respuesta) {
      if (respuesta['event'] == 'NEW_JOB_ASSIGNED') {
        obtenerTrabajos();
      }
    });
  }

  // --- Gestión de Refresco (Polling) ---

  /// Activa un temporizador para refrescar la lista periódicamente.
  /// Útil para capturar cambios si el socket llegara a fallar silenciosamente.
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

  /// Compara la lista nueva con la actual para evitar renders innecesarios.
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

  // --- Operaciones con el Servidor ---

  /// Carga principal de trabajos. Activa el spinner global de carga.
  Future<void> obtenerTrabajos() async {
    final usuario = _auth.usuarioActual;
    if (usuario == null) return;

    _estaCargando = true;
    notifyListeners();

    try {
      final lista = await _api.fetchTrabajos(usuario);
      // Filtramos los cancelados para mantener la vista limpia
      _trabajos =
          lista.where((t) => t.estado != EstadoTrabajo.CANCELADO).toList();
      _ordenarTrabajosPorPrioridad();
    } catch (e) {
      debugPrint('Error obteniendo trabajos: $e');
      if (e.toString().contains('SESSION_EXPIRED')) {
        await _auth.logout();
        // Notificamos para que la UI sepa que el usuario es null y reditija
        notifyListeners();
      }
    } finally {
      _estaCargando = false;
      notifyListeners();
    }
  }

  /// Organiza la lista para que los trabajos más urgentes o recientes aparezcan arriba.
  void _ordenarTrabajosPorPrioridad() {
    _trabajos
        .sort((a, b) => _obtenerPesoEstado(a).compareTo(_obtenerPesoEstado(b)));
  }

  /// Crea una nueva solicitud de trabajo.
  Future<bool> crearTrabajo(Map<String, dynamic> datosTrabajo) async {
    final usuario = _auth.usuarioActual;
    if (usuario == null) return false;
    return await _api.createTrabajo(usuario, datosTrabajo);
  }

  /// Marca un trabajo como finalizado. 
  /// Permite adjuntar fotos del resultado y un informe técnico.
  Future<bool> actualizarEstadoTrabajo(int id, EstadoTrabajo nuevoEstado,
      {String? informe, List<String>? fotos}) async {
    final usuario = _auth.usuarioActual;
    if (usuario == null) return false;

    final exito = await _api.finalizeTrabajo(
        usuario, id, informe ?? 'Finalizado desde App', fotos);

    if (exito) await obtenerTrabajos();
    return exito;
  }

  /// Cancela una incidencia de forma lógica.
  Future<bool> cancelarTrabajo(int idTrabajo) async {
    final usuario = _auth.usuarioActual;
    if (usuario == null) return false;

    final exito = await _api.cancelTrabajo(
        usuario, idTrabajo, 'Cancelado por el usuario');
    if (exito) await obtenerTrabajos();
    return exito;
  }

  /// Modifica los metadatos de un trabajo (título, descripción, etc).
  Future<bool> modificarTrabajo(
      int idTrabajo, Map<String, dynamic> datosTrabajo) async {
    final usuario = _auth.usuarioActual;
    if (usuario == null) return false;

    final exito = await _api.modifyTrabajo(usuario, idTrabajo, datosTrabajo);
    if (exito) await obtenerTrabajos();
    return exito;
  }

  /// Obtiene la lista de presupuestos asociados a un trabajo.
  Future<List<Presupuesto>> obtenerPresupuestos(int idTrabajo) async {
    final usuario = _auth.usuarioActual;
    if (usuario == null) return [];
    return await _api.fetchPresupuestos(usuario, idTrabajo);
  }

  /// Acepta el presupuesto seleccionado y cierra la fase de puja.
  Future<bool> aceptarPresupuesto(int idPresupuesto) async {
    final usuario = _auth.usuarioActual;
    if (usuario == null) return false;

    final exito = await _api.acceptPresupuesto(usuario, idPresupuesto);
    if (exito) await obtenerTrabajos();
    return exito;
  }

  /// Rechaza un presupuesto recibido para una incidencia.
  /// Notifica al servidor para liberar la oferta y actualizar el listado.
  Future<bool> rechazarPresupuesto(int idPresupuesto) async {
    final usuario = _auth.usuarioActual;
    if (usuario == null) return false;

    final exito = await _api.rejectPresupuesto(usuario, idPresupuesto);
    if (exito) await obtenerTrabajos();
    return exito;
  }

  /// Envía la reseña final del cliente.
  Future<bool> valorarTrabajo(
      int idTrabajo, int valoracion, String comentario) async {
    final usuario = _auth.usuarioActual;
    if (usuario == null) return false;
    return await _api.valorateTrabajo(
        usuario, idTrabajo, valoracion, comentario);
  }

  // --- Lógica de Ordenación ---

  /// Calcula la importancia visual de un trabajo según su estado actual.
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

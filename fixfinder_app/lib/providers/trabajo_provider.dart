import 'dart:async';
import 'package:flutter/material.dart';
import '../main.dart'; // Para acceder a messengerKey
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
  bool _peticionEnCurso = false; // Semáforo para evitar peticiones redundantes
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
      if (respuesta['accion'] == 'BROADCAST') {
        final datos = respuesta['datos'] ?? {};
        final categoria = datos['categoria'] ?? 'SISTEMA';
        final info = datos['info'] ?? 'Actualización del sistema';

        debugPrint('🔔 [SOCKET-BROADCAST] $categoria: $info');

        // Refrescamos los trabajos en segundo plano para no bloquear la UI con spinners
        _actualizarEstadoSilencioso();

        // Mostramos notificación visual en la app con el nuevo estilo corporativo
        _mostrarNotificacionUI(categoria, info);
      }
    });
  }

  /// Muestra un SnackBar elegante con fondo semi-transparente y borde naranja.
  void _mostrarNotificacionUI(String categoria, String mensaje) {
    if (messengerKey.currentState == null) return;

    IconData icono;
    // Naranja corporativo de FixFinder
    const Color colorNaranja = Color(0xFFFF9800);

    switch (categoria) {
      case 'TRABAJO':
        icono = Icons.build_circle_outlined;
        break;
      case 'PRESUPUESTO':
        icono = Icons.monetization_on_outlined;
        break;
      default:
        icono = Icons.info_outline;
    }

    messengerKey.currentState!.hideCurrentSnackBar(); // Limpiar previas
    messengerKey.currentState!.showSnackBar(
      SnackBar(
        content: Container(
          padding: const EdgeInsets.symmetric(vertical: 8),
          decoration: const BoxDecoration(
            border: Border(left: BorderSide(color: colorNaranja, width: 4)),
          ),
          child: Row(
            children: [
              const SizedBox(width: 8),
              Icon(icono, color: colorNaranja),
              const SizedBox(width: 12),
              Expanded(
                  child: Text(mensaje,
                      style: const TextStyle(
                          color: Colors.white, fontWeight: FontWeight.w500))),
            ],
          ),
        ),
        backgroundColor: Colors.black.withOpacity(0.75), // Semi-transparente
        behavior: SnackBarBehavior.floating,
        duration: const Duration(seconds: 4),
        margin: const EdgeInsets.all(16),
        elevation: 0,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(12),
          side: BorderSide(color: colorNaranja.withOpacity(0.5), width: 1),
        ),
      ),
    );
  }

  // --- Gestión de Refresco (Polling) ---

  /// Activa un temporizador para refrescar la lista periódicamente.
  /// Útil para capturar cambios si el socket llegara a fallar silenciosamente.
  void iniciarSincronizacion({int intervaloSegundos = 15}) {
    _pollingTimer?.cancel();
    _pollingTimer = Timer.periodic(
      Duration(seconds: intervaloSegundos),
      (_) => _actualizarEstadoSilencioso(),
    );
  }

  /// Detiene el ciclo de refresco automático.
  void detenerSincronizacion() {
    _pollingTimer?.cancel();
    _pollingTimer = null;
  }

  /// Actualiza la lista de trabajos sin disparar indicadores de carga invasivos.
  /// Solo notifica si detecta cambios críticos de estado.
  Future<void> _actualizarEstadoSilencioso() async {
    final usuario = _auth.usuarioActual;
    if (usuario == null) return;

    try {
      final nuevaLista = await _api.obtenerTrabajos(usuario);
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

  /// Carga principal de trabajos. Activa el spinner global de carga solo si no es un refresco silencioso.
  Future<void> obtenerTrabajos({bool silencioso = false}) async {
    final usuario = _auth.usuarioActual;
    if (usuario == null) return;
    if (_peticionEnCurso) return; // Si ya hay una carga, ignoramos esta

    _peticionEnCurso = true;

    // Solo mostramos cargando si NO es silencioso O si la lista está vacía (carga inicial)
    if (!silencioso || _trabajos.isEmpty) {
      _estaCargando = true;
      notifyListeners();
    }

    try {
      final lista = await _api.obtenerTrabajos(usuario);
      // Solo actualizamos la lista si llegamos aquí (éxito)
      _trabajos =
          lista.where((t) => t.estado != EstadoTrabajo.CANCELADO).toList();
      _ordenarTrabajosPorPrioridad();
    } catch (e) {
      debugPrint('Error obteniendo trabajos: $e');
      if (e.toString().contains('SESSION_EXPIRED')) {
        await _auth.logout();
        notifyListeners();
      }
      // NOTA: Si falla por red, no vaciamos _trabajos. Se mantienen los datos anteriores.
    } finally {
      _peticionEnCurso = false;
      if (_estaCargando) {
        _estaCargando = false;
        notifyListeners();
      }
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
    return await _api.crearTrabajo(usuario, datosTrabajo);
  }

  /// Marca un trabajo como finalizado.
  /// Permite adjuntar fotos del resultado y un informe técnico.
  Future<bool> actualizarEstadoTrabajo(int id, EstadoTrabajo nuevoEstado,
      {String? informe, List<String>? fotos}) async {
    final usuario = _auth.usuarioActual;
    if (usuario == null) return false;

    final exito = await _api.finalizarTrabajo(
        usuario, id, informe ?? 'Finalizado desde App', fotos);

    if (exito) await obtenerTrabajos();
    return exito;
  }

  /// Cancela una incidencia de forma lógica.
  Future<bool> cancelarTrabajo(int idTrabajo) async {
    final usuario = _auth.usuarioActual;
    if (usuario == null) return false;

    final exito = await _api.cancelarTrabajo(
        usuario, idTrabajo, 'Cancelado por el usuario');
    if (exito) await obtenerTrabajos();
    return exito;
  }

  /// Modifica los metadatos de un trabajo (título, descripción, etc).
  Future<bool> modificarTrabajo(
      int idTrabajo, Map<String, dynamic> datosTrabajo) async {
    final usuario = _auth.usuarioActual;
    if (usuario == null) return false;

    final exito = await _api.modificarTrabajo(usuario, idTrabajo, datosTrabajo);
    if (exito) await obtenerTrabajos();
    return exito;
  }

  /// Obtiene la lista de presupuestos asociados a un trabajo.
  Future<List<Presupuesto>> obtenerPresupuestos(int idTrabajo) async {
    final usuario = _auth.usuarioActual;
    if (usuario == null) return [];
    return await _api.obtenerPresupuestos(usuario, idTrabajo);
  }

  /// Acepta el presupuesto seleccionado y cierra la fase de puja.
  Future<bool> aceptarPresupuesto(int idPresupuesto) async {
    final usuario = _auth.usuarioActual;
    if (usuario == null) return false;

    final exito = await _api.aceptarPresupuesto(usuario, idPresupuesto);
    if (exito) await obtenerTrabajos();
    return exito;
  }

  /// Rechaza un presupuesto recibido para una incidencia.
  /// Notifica al servidor para liberar la oferta y actualizar el listado.
  Future<bool> rechazarPresupuesto(int idPresupuesto) async {
    final usuario = _auth.usuarioActual;
    if (usuario == null) return false;

    final exito = await _api.rechazarPresupuesto(usuario, idPresupuesto);
    if (exito) await obtenerTrabajos();
    return exito;
  }

  /// Envía la reseña final del cliente.
  Future<bool> valorarTrabajo(
      int idTrabajo, int valoracion, String comentario) async {
    final usuario = _auth.usuarioActual;
    if (usuario == null) return false;
    final exito =
        await _api.valorarTrabajo(usuario, idTrabajo, valoracion, comentario);
    if (exito) await obtenerTrabajos();
    return exito;
  }

  // --- Lógica de Ordenación ---

  /// Calcula la importancia visual de un trabajo según su estado actual.
  int _obtenerPesoEstado(Trabajo trabajo) {
    // PRIORIDAD 0: Finalizado pero el cliente aún no lo ha valorado (Acción crítica de cierre)
    if (trabajo.estado == EstadoTrabajo.FINALIZADO && trabajo.valoracion == 0) {
      return 0;
    }

    // PRIORIDAD 1: Tiene presupuestos recibidos que el cliente debe revisar (Acción crítica de decisión)
    if (trabajo.estado == EstadoTrabajo.PRESUPUESTADO) {
      return 1;
    }

    // PRIORIDAD 2: Trabajos en curso (Asignados o Aceptados)
    if (trabajo.estado == EstadoTrabajo.ACEPTADO ||
        trabajo.estado == EstadoTrabajo.ASIGNADO) {
      return 2;
    }

    // PRIORIDAD 3: Recién creados esperando técnicos
    if (trabajo.estado == EstadoTrabajo.PENDIENTE) {
      return 3;
    }

    // PRIORIDAD 4: Completados y valorados o realizados
    if (trabajo.estado == EstadoTrabajo.REALIZADO ||
        trabajo.estado == EstadoTrabajo.FINALIZADO ||
        trabajo.estado == EstadoTrabajo.PAGADO) {
      return 4;
    }

    // PRIORIDAD 5: Cancelados
    return 5;
  }
}

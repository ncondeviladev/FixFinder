import 'dart:async';
import 'package:flutter/material.dart';
import '../models/trabajo.dart';
import '../services/socket_service.dart';
import '../services/auth_service.dart';
import '../models/presupuesto.dart';

class TrabajoProvider with ChangeNotifier {
  final SocketService _socket = SocketService();
  final AuthService _auth = AuthService();

  List<Trabajo> _trabajos = [];
  bool _estaCargando = false;
  Timer? _pollingTimer;

  List<Trabajo> get trabajos => _trabajos;
  bool get estaCargando => _estaCargando;

  TrabajoProvider() {
    // Escuchar actualizaciones en tiempo real del socket (eventos push)
    _socket.respuestas.listen((respuesta) {
      if (respuesta['event'] == 'NEW_JOB_ASSIGNED' ||
          respuesta['accion'] == 'LISTAR_TRABAJOS') {
        obtenerTrabajos();
      }
    });
  }

  /// Inicia el refresco automático cada [intervaloSegundos] segundos.
  /// Llama a esto justo después de que el usuario hace login y carga el dashboard.
  void startPolling({int intervaloSegundos = 15}) {
    _pollingTimer?.cancel();
    _pollingTimer = Timer.periodic(
      Duration(seconds: intervaloSegundos),
      (_) => _actualizarSiHayCambios(),
    );
  }

  /// Para el polling (llamar al hacer logout o al salir del dashboard).
  void stopPolling() {
    _pollingTimer?.cancel();
    _pollingTimer = null;
  }

  /// Recarga silenciosamente y solo notifica si algo cambió (evita parpadeos innecesarios).
  Future<void> _actualizarSiHayCambios() async {
    final usuario = _auth.usuarioActual;
    if (usuario == null) return;

    final Map<String, dynamic> peticion = {
      'accion': 'LISTAR_TRABAJOS',
      'token': usuario.token,
      'datos': {'idUsuario': usuario.id, 'rol': usuario.rol.name},
    };

    try {
      final completer = Completer<Map<String, dynamic>>();
      final suscripcion = _socket.respuestas.listen((respuesta) {
        if (respuesta['status'] != null) {
          if (!completer.isCompleted) completer.complete(respuesta);
        }
      });

      await _socket.send(peticion);
      final respuesta =
          await completer.future.timeout(const Duration(seconds: 8));
      suscripcion.cancel();

      if (respuesta['status'] == 200 && respuesta['datos'] is List) {
        final nuevaLista = (respuesta['datos'] as List)
            .map((json) => Trabajo.fromJson(json))
            .toList();

        // Solo notificar si hay cambios reales (estados distintos)
        bool hayCambios = _hayCambiosDeEstado(nuevaLista);
        _trabajos = nuevaLista;
        if (hayCambios) {
          notifyListeners();
        }
      }
    } catch (_) {
      // Polling silencioso — ignorar errores de red temporales
    }
  }

  bool _hayCambiosDeEstado(List<Trabajo> nuevaLista) {
    if (nuevaLista.length != _trabajos.length) return true;
    for (int i = 0; i < nuevaLista.length; i++) {
      final nuevo = nuevaLista[i];
      try {
        final actual = _trabajos.firstWhere((t) => t.id == nuevo.id);
        if (actual.estado != nuevo.estado) return true;
        if (actual.presupuesto?.estado != nuevo.presupuesto?.estado)
          return true;
      } catch (_) {
        return true; // Trabajo nuevo en la lista
      }
    }
    return false;
  }

  Future<void> obtenerTrabajos() async {
    _estaCargando = true;
    notifyListeners();

    final usuario = _auth.usuarioActual;
    if (usuario == null) return;

    final Map<String, dynamic> peticion = {
      'accion': 'LISTAR_TRABAJOS',
      'token': usuario.token,
      'datos': {
        'idUsuario': usuario.id,
        'rol': usuario.rol.name,
      }
    };

    try {
      final completer = Completer<Map<String, dynamic>>();
      final suscripcion = _socket.respuestas.listen((respuesta) {
        // Buscamos específicamente el status de la respuesta de esta acción
        if (respuesta['accion'] == 'LISTAR_TRABAJOS' ||
            respuesta['status'] != null) {
          if (!completer.isCompleted) completer.complete(respuesta);
        }
      });

      await _socket.send(peticion);
      final respuesta =
          await completer.future.timeout(const Duration(seconds: 10));
      suscripcion.cancel();

      if (respuesta['status'] == 200 && respuesta['datos'] is List) {
        final List<dynamic> listaJson = respuesta['datos'] ?? [];
        _trabajos = listaJson.map((json) {
          try {
            return Trabajo.fromJson(json);
          } catch (e) {
            print('Error parseando trabajo ID ${json['id']}: $e');
            rethrow;
          }
        }).toList();
      } else {
        // print('Respuesta no válida o vacía: ${respuesta['status']}');
      }
    } catch (e) {
      print('Error al obtener trabajos: $e');
      // _logger.e('Error al obtener trabajos: $e');
    } finally {
      _estaCargando = false;
      notifyListeners();
    }
  }

  Future<bool> crearTrabajo(Map<String, dynamic> datosTrabajo) async {
    final usuario = _auth.usuarioActual;
    if (usuario == null) return false;

    // Añadimos el idCliente que falta y que el servidor exige
    datosTrabajo['idCliente'] = usuario.id;

    final Map<String, dynamic> peticion = {
      'accion': 'CREAR_TRABAJO',
      'token': usuario.token,
      'datos': datosTrabajo,
    };

    try {
      final completer = Completer<Map<String, dynamic>>();
      final suscripcion = _socket.respuestas.listen((respuesta) {
        if (respuesta['status'] == 201 ||
            (respuesta['mensaje']?.toString().contains('creado') == true)) {
          if (!completer.isCompleted) completer.complete(respuesta);
        }
      });

      await _socket.send(peticion);
      final respuesta =
          await completer.future.timeout(const Duration(seconds: 5));
      suscripcion.cancel();

      if (respuesta['status'] == 201) {
        obtenerTrabajos(); // Recargamos la lista
        return true;
      }
      return false;
    } catch (e) {
      print('Error al crear trabajo: $e');
      return false;
    }
  }

  Future<bool> actualizarEstadoTrabajo(
      int idTrabajo, EstadoTrabajo nuevoEstado) async {
    final usuario = _auth.usuarioActual;
    if (usuario == null) return false;

    // Según ProcesadorTrabajos.java, solo hay FINALIZAR_TRABAJO que hace todo el flujo
    final Map<String, dynamic> peticion = {
      'accion': 'FINALIZAR_TRABAJO',
      'token': usuario.token,
      'datos': {'idTrabajo': idTrabajo, 'informe': 'Finalizado desde App Móvil'}
    };

    try {
      await _socket.send(peticion);
      obtenerTrabajos();
      return true;
    } catch (e) {
      return false;
    }
  }

  Future<List<Presupuesto>> obtenerPresupuestos(int idTrabajo) async {
    final usuario = _auth.usuarioActual;
    if (usuario == null) return [];

    final Map<String, dynamic> peticion = {
      'accion': 'LISTAR_PRESUPUESTOS',
      'token': usuario.token,
      'datos': {'idTrabajo': idTrabajo}
    };

    try {
      final completer = Completer<Map<String, dynamic>>();
      final suscripcion = _socket.respuestas.listen((respuesta) {
        if (respuesta['accion'] == 'LISTAR_PRESUPUESTOS' ||
            (respuesta['mensaje']?.toString().contains('presupuestos') ==
                true)) {
          if (!completer.isCompleted) completer.complete(respuesta);
        }
      });

      await _socket.send(peticion);
      final respuesta =
          await completer.future.timeout(const Duration(seconds: 5));
      suscripcion.cancel();

      if (respuesta['status'] == 200) {
        final List<dynamic> listaJson = respuesta['datos'] ?? [];
        return listaJson.map((json) => Presupuesto.fromJson(json)).toList();
      }
    } catch (e) {
      print('Error al obtener presupuestos: $e');
    }
    return [];
  }

  Future<bool> aceptarPresupuesto(int idPresupuesto) async {
    final usuario = _auth.usuarioActual;
    if (usuario == null) return false;

    final Map<String, dynamic> peticion = {
      'accion': 'ACEPTAR_PRESUPUESTO',
      'token': usuario.token,
      'datos': {'idPresupuesto': idPresupuesto}
    };

    try {
      await _socket.send(peticion);
      obtenerTrabajos();
      return true;
    } catch (e) {
      return false;
    }
  }

  Future<bool> cancelarTrabajo(int idTrabajo) async {
    final usuario = _auth.usuarioActual;
    if (usuario == null) return false;

    final Map<String, dynamic> peticion = {
      'accion':
          'CANCELAR_TRABAJO', // Nota: Asegúrate de que el servidor soporte esta acción
      'token': usuario.token,
      'datos': {'idTrabajo': idTrabajo, 'motivo': 'Cancelado por el usuario'}
    };

    try {
      await _socket.send(peticion);
      obtenerTrabajos();
      return true;
    } catch (e) {
      return false;
    }
  }
}

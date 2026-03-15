import 'dart:async';
import '../models/trabajo.dart';
import '../services/socket_service.dart';
import '../models/presupuesto.dart';
import '../models/usuario.dart';

/**
 * Servicio encargado de la comunicación con el servidor para temas relacionados con Trabajos.
 * Desacopla la lógica de red de los Providers de Flutter.
 */
class JobApiService {
  final SocketService _socket = SocketService();

  /**
   * Obtiene la lista de trabajos para un usuario y rol específicos.
   */
  Future<List<Trabajo>> fetchTrabajos(Usuario usuario) async {
    final Map<String, dynamic> peticion = {
      'accion': 'LISTAR_TRABAJOS',
      'token': usuario.token,
      'datos': {
        'idUsuario': usuario.id,
        'rol': usuario.rol.name,
      }
    };

    final completer = Completer<Map<String, dynamic>>();
    final suscripcion = _socket.respuestas.listen((respuesta) {
      if (respuesta['accion'] == 'LISTAR_TRABAJOS') {
        if (!completer.isCompleted) completer.complete(respuesta);
      }
    });

    await _socket.send(peticion);
    final respuesta =
        await completer.future.timeout(const Duration(seconds: 10));
    suscripcion.cancel();

    if (respuesta['status'] == 200 && respuesta['datos'] is List) {
      return (respuesta['datos'] as List)
          .map((json) => Trabajo.fromJson(json))
          .toList();
    }
    return [];
  }

  /**
   * Envía la creación de un nuevo trabajo.
   */
  Future<bool> createTrabajo(
      Usuario usuario, Map<String, dynamic> datosTrabajo) async {
    datosTrabajo['idCliente'] = usuario.id;
    final Map<String, dynamic> peticion = {
      'accion': 'CREAR_TRABAJO',
      'token': usuario.token,
      'datos': datosTrabajo,
    };

    await _socket.send(peticion);
    // Espera opcional para permitir que el servidor procese la transacción
    await Future.delayed(const Duration(milliseconds: 500));
    return true;
  }

  /**
   * Finaliza un trabajo con informe y fotos.
   */
  Future<bool> finalizeTrabajo(Usuario usuario, int idTrabajo, String informe,
      List<String>? fotos) async {
    final Map<String, dynamic> peticion = {
      'accion': 'FINALIZAR_TRABAJO',
      'token': usuario.token,
      'datos': {
        'idTrabajo': idTrabajo,
        'informe': informe,
        'fotos': fotos,
      }
    };

    await _socket.send(peticion);
    await Future.delayed(const Duration(milliseconds: 500));
    return true;
  }

  /**
   * Obtiene presupuestos asociados a un trabajo.
   */
  Future<List<Presupuesto>> fetchPresupuestos(
      Usuario usuario, int idTrabajo) async {
    final Map<String, dynamic> peticion = {
      'accion': 'LISTAR_PRESUPUESTOS',
      'token': usuario.token,
      'datos': {'idTrabajo': idTrabajo}
    };

    final completer = Completer<Map<String, dynamic>>();
    final suscripcion = _socket.respuestas.listen((respuesta) {
      if (respuesta['accion'] == 'LISTAR_PRESUPUESTOS' ||
          (respuesta['mensaje']?.toString().contains('presupuestos') == true)) {
        if (!completer.isCompleted) completer.complete(respuesta);
      }
    });

    await _socket.send(peticion);
    final respuesta =
        await completer.future.timeout(const Duration(seconds: 8));
    suscripcion.cancel();

    if (respuesta['status'] == 200 && respuesta['datos'] is List) {
      return (respuesta['datos'] as List)
          .map((json) => Presupuesto.fromJson(json))
          .toList();
    }
    return [];
  }

  /**
   * Acepta un presupuesto específico.
   */
  Future<bool> acceptPresupuesto(Usuario usuario, int idPresupuesto) async {
    final Map<String, dynamic> peticion = {
      'accion': 'ACEPTAR_PRESUPUESTO',
      'token': usuario.token,
      'datos': {'idPresupuesto': idPresupuesto}
    };

    await _socket.send(peticion);
    await Future.delayed(const Duration(milliseconds: 500));
    return true;
  }

  /**
   * Registra la valoración de un cliente.
   */
  Future<bool> valorateTrabajo(
      Usuario usuario, int idTrabajo, int valoracion, String comentario) async {
    final Map<String, dynamic> peticion = {
      'accion': 'VALORAR_TRABAJO',
      'token': usuario.token,
      'datos': {
        'idTrabajo': idTrabajo,
        'valoracion': valoracion,
        'comentarioCliente': comentario
      }
    };

    await _socket.send(peticion);
    await Future.delayed(const Duration(milliseconds: 500));
    return true;
  }

  /**
   * Cancela un trabajo con un motivo específico.
   */
  Future<bool> cancelTrabajo(
      Usuario usuario, int idTrabajo, String motivo) async {
    final Map<String, dynamic> peticion = {
      'accion': 'CANCELAR_TRABAJO',
      'token': usuario.token,
      'datos': {'idTrabajo': idTrabajo, 'motivo': motivo}
    };

    await _socket.send(peticion);
    await Future.delayed(const Duration(milliseconds: 500));
    return true;
  }

  /**
   * Modifica los datos de un trabajo existente.
   */
  Future<bool> modifyTrabajo(
      Usuario usuario, int idTrabajo, Map<String, dynamic> datos) async {
    datos['idTrabajo'] = idTrabajo;
    final Map<String, dynamic> peticion = {
      'accion': 'MODIFICAR_TRABAJO',
      'token': usuario.token,
      'datos': datos,
    };

    await _socket.send(peticion);
    await Future.delayed(const Duration(milliseconds: 500));
    return true;
  }
}

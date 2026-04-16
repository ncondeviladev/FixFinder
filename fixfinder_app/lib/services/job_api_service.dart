import 'dart:async';
import 'package:flutter/foundation.dart';
import '../models/trabajo.dart';
import '../services/socket_service.dart';
import '../models/presupuesto.dart';
import '../models/usuario.dart';

/// Servicio encargado de la comunicación con el servidor para temas relacionados con Trabajos.
/// Desacopla la lógica de red de los Providers de Flutter.
class JobApiService {
  final SocketService _socket = SocketService();
  /// Obtiene la lista de trabajos para un usuario y rol específicos.
  Future<List<Trabajo>> fetchTrabajos(Usuario usuario) async {
    try {
      final respuesta = await _socket.request(
        'LISTAR_TRABAJOS',
        {
          'idUsuario': usuario.id,
          'rol': usuario.rol.name,
        },
        token: usuario.token,
      );

      if (respuesta['status'] == 200 && respuesta['datos'] is List) {
        return (respuesta['datos'] as List)
            .map((json) => Trabajo.fromJson(json))
            .toList();
      }
      
      // Si el servidor nos dice que no estamos autorizados (token inválido/caducado)
      if (respuesta['status'] == 401 || (respuesta['mensaje'] != null && respuesta['mensaje'].toString().contains('no válida'))) {
        throw Exception('SESSION_EXPIRED');
      }
    } catch (e) {
      debugPrint('[JobApi] Error listando trabajos: $e');
      if (e.toString().contains('SESSION_EXPIRED')) rethrow;
    }
    return [];
  }

  /// Envía la creación de un nuevo trabajo.
  Future<bool> createTrabajo(
      Usuario usuario, Map<String, dynamic> datosTrabajo) async {
    datosTrabajo['idCliente'] = usuario.id;
    try {
      final respuesta = await _socket.request(
        'CREAR_TRABAJO',
        datosTrabajo,
        token: usuario.token,
      );
      return respuesta['status'] == 200 || respuesta['status'] == 201;
    } catch (e) {
      return false;
    }
  }

  /// Finaliza un trabajo con informe y fotos.
  Future<bool> finalizeTrabajo(Usuario usuario, int idTrabajo, String informe,
      List<String>? fotos) async {
    try {
      final respuesta = await _socket.request(
        'FINALIZAR_TRABAJO',
        {
          'idTrabajo': idTrabajo,
          'informe': informe,
          'fotos': fotos,
        },
        token: usuario.token,
      );
      return respuesta['status'] == 200;
    } catch (e) {
      return false;
    }
  }

  /// Obtiene presupuestos asociados a un trabajo.
  Future<List<Presupuesto>> fetchPresupuestos(
      Usuario usuario, int idTrabajo) async {
    try {
      final respuesta = await _socket.request(
        'LISTAR_PRESUPUESTOS',
        {'idTrabajo': idTrabajo},
        token: usuario.token,
      );

      if (respuesta['status'] == 200 && respuesta['datos'] is List) {
        return (respuesta['datos'] as List)
            .map((json) => Presupuesto.fromJson(json))
            .toList();
      }
    } catch (e) {
      debugPrint('[JobApi] Error listando presupuestos: $e');
    }
    return [];
  }

  /// Acepta un presupuesto específico.
  Future<bool> acceptPresupuesto(Usuario usuario, int idPresupuesto) async {
    try {
      final respuesta = await _socket.request(
        'ACEPTAR_PRESUPUESTO',
        {'idPresupuesto': idPresupuesto},
        token: usuario.token,
      );
      return respuesta['status'] == 200;
    } catch (e) {
      return false;
    }
  }

  /// Rechaza un presupuesto específico.
  Future<bool> rejectPresupuesto(Usuario usuario, int idPresupuesto) async {
    try {
      final respuesta = await _socket.request(
        'RECHAZAR_PRESUPUESTO',
        {'idPresupuesto': idPresupuesto},
        token: usuario.token,
      );
      return respuesta['status'] == 200;
    } catch (e) {
      return false;
    }
  }

  /// Registra la valoración de un cliente.
  Future<bool> valorateTrabajo(
      Usuario usuario, int idTrabajo, int valoracion, String comentario) async {
    try {
      final respuesta = await _socket.request(
        'VALORAR_TRABAJO',
        {
          'idTrabajo': idTrabajo,
          'valoracion': valoracion,
          'comentarioCliente': comentario,
        },
        token: usuario.token,
      );
      return respuesta['status'] == 200;
    } catch (e) {
      return false;
    }
  }

  /// Cancela un trabajo con un motivo específico.
  Future<bool> cancelTrabajo(
      Usuario usuario, int idTrabajo, String motivo) async {
    try {
      final respuesta = await _socket.request(
        'CANCELAR_TRABAJO',
        {'idTrabajo': idTrabajo, 'motivo': motivo},
        token: usuario.token,
      );
      return respuesta['status'] == 200;
    } catch (e) {
      return false;
    }
  }

  /// Modifica los datos de un trabajo existente.
  Future<bool> modifyTrabajo(
      Usuario usuario, int idTrabajo, Map<String, dynamic> datos) async {
    datos['idTrabajo'] = idTrabajo;
    try {
      final respuesta = await _socket.request(
        'MODIFICAR_TRABAJO',
        datos,
        token: usuario.token,
      );
      return respuesta['status'] == 200;
    } catch (e) {
      return false;
    }
  }
}

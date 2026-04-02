// Pantalla que muestra el detalle de la incidencia.
// Permite aprobar presupuestos a clientes o finalizar tareas a operarios.
import 'dart:io';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:image_picker/image_picker.dart';
import 'package:firebase_storage/firebase_storage.dart';
import '../models/trabajo.dart';
import '../models/usuario.dart';
import '../models/presupuesto.dart';
import '../providers/trabajo_provider.dart';
import '../services/auth_service.dart';
import '../widgets/detalle_trabajo/detalle_info_card.dart';
import '../widgets/detalle_trabajo/detalle_resumen_final.dart';
import '../widgets/detalle_trabajo/detalle_seccion_presupuestos.dart';
import '../widgets/detalle_trabajo/dialogos_trabajo.dart';
import 'crear_trabajo_pantalla.dart';

class DetalleTrabajoPantalla extends StatefulWidget {
  final Trabajo trabajo;
  const DetalleTrabajoPantalla({super.key, required this.trabajo});

  @override
  State<DetalleTrabajoPantalla> createState() => _DetalleTrabajoPantallaState();
}

class _DetalleTrabajoPantallaState extends State<DetalleTrabajoPantalla> {
  bool _procesando = false;
  List<Presupuesto> _presupuestos = [];
  bool _cargandoPresupuestos = false;

  @override
  void initState() {
    super.initState();
    if (widget.trabajo.estado == EstadoTrabajo.PENDIENTE ||
        widget.trabajo.estado == EstadoTrabajo.PRESUPUESTADO) {
      _cargarPresupuestos();
    }
  }

  Future<void> _cargarPresupuestos() async {
    setState(() => _cargandoPresupuestos = true);
    final lista = await context
        .read<TrabajoProvider>()
        .obtenerPresupuestos(widget.trabajo.id);
    if (mounted) {
      setState(() {
        _presupuestos = lista;
        _cargandoPresupuestos = false;
      });
    }
  }

  Future<void> _aceptarPresupuesto(int idPresupuesto) async {
    setState(() => _procesando = true);
    final exito =
        await context.read<TrabajoProvider>().aceptarPresupuesto(idPresupuesto);
    if (mounted) {
      setState(() => _procesando = false);
      if (exito) {
        Navigator.pop(context);
      }
    }
  }

  Future<void> _handleFinalizar(int idTrabajo) async {
    final datos = await DialogosTrabajo.mostrarDialogoFinalizar(context);
    if (datos != null) {
      _finalizarTrabajo(idTrabajo, datos['material']!, datos['horas']!,
          datos['fotos'] as List<XFile>?);
    }
  }

  Future<void> _handleBorrar(int idTrabajo) async {
    final confirmar = await DialogosTrabajo.mostrarDialogoBorrar(context);
    if (confirmar) {
      setState(() => _procesando = true);
      final exito =
          await context.read<TrabajoProvider>().cancelarTrabajo(idTrabajo);
      if (mounted) {
        setState(() => _procesando = false);
        if (exito) {
          // Pop simple: el .then() del dashboard gestiona la recarga
          Navigator.pop(context);
        }
      }
    }
  }

  Future<void> _handleValorar(int idTrabajo) async {
    final datos = await DialogosTrabajo.mostrarDialogoValorar(context);
    if (datos != null) {
      setState(() => _procesando = true);
      final exito = await context
          .read<TrabajoProvider>()
          .valorarTrabajo(idTrabajo, datos['estrellas'], datos['comentario']);
      if (mounted) {
        setState(() => _procesando = false);
        if (!exito) {
          // Si falla mostramos mensaje de error antes de salir
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(
              content:
                  Text('Error al enviar la valoración. Inténtalo de nuevo.'),
              backgroundColor: Colors.red,
            ),
          );
          return;
        }
        // Éxito: pop al dashboard, el .then() del dashboard recarga
        Navigator.pop(context);
      }
    }
  }

  Future<void> _finalizarTrabajo(
      int idTrabajo, String material, String horas, List<XFile>? fotos) async {
    setState(() => _procesando = true);

    List<String> urlsFotos = [];

    // 1. Subir fotos a Firebase si hay
    if (fotos != null && fotos.isNotEmpty) {
      try {
        for (var foto in fotos) {
          final fileName =
              'trabajos/$idTrabajo/final_${DateTime.now().millisecondsSinceEpoch}_${foto.name}';
          final storageRef = FirebaseStorage.instance.ref().child(fileName);
          await storageRef.putFile(File(foto.path));
          final url = await storageRef.getDownloadURL();
          urlsFotos.add(url);
        }
      } catch (e) {
        debugPrint('Error subiendo fotos finales: $e');
      }
    }

    String informeFinal = 'Finalizado desde App Móvil.';
    List<String> partesInforme = [];

    if (material.trim().isNotEmpty) {
      partesInforme.add('Material gastado: $material');
    }
    if (horas.trim().isNotEmpty) {
      partesInforme.add('Horas trabajadas: $horas');
    }

    if (partesInforme.isNotEmpty) {
      informeFinal = '${partesInforme.join('. ')}.';
    }

    final exito = await context.read<TrabajoProvider>().actualizarEstadoTrabajo(
        idTrabajo, EstadoTrabajo.FINALIZADO,
        informe: informeFinal, fotos: urlsFotos);
    if (mounted) {
      setState(() => _procesando = false);
      if (exito) {
        Navigator.pop(context);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final usuario = AuthService().usuarioActual;
    final esCliente = usuario?.rol == Rol.CLIENTE;
    final esOperario = usuario?.rol == Rol.OPERARIO;

    return Consumer<TrabajoProvider>(
      builder: (context, provider, child) {
        // Siempre usamos el trabajo actualizado del provider
        final trabajoActual = provider.trabajos.firstWhere(
          (t) => t.id == widget.trabajo.id,
          orElse: () => widget.trabajo,
        );

        return Scaffold(
          appBar: AppBar(
            title: const Text(''),
            actions: [
              if ((esCliente &&
                      (trabajoActual.estado == EstadoTrabajo.PENDIENTE ||
                          trabajoActual.estado ==
                              EstadoTrabajo.PRESUPUESTADO)) ||
                  (esOperario &&
                      trabajoActual.estado == EstadoTrabajo.ASIGNADO))
                PopupMenuButton<String>(
                  onSelected: (val) {
                    if (val == 'modificar') {
                      Navigator.push(
                        context,
                        MaterialPageRoute(
                          builder: (context) => CrearTrabajoPantalla(
                              trabajoAEditar: trabajoActual),
                        ),
                      );
                    } else if (val == 'borrar') {
                      _handleBorrar(trabajoActual.id);
                    }
                  },
                  itemBuilder: (context) => [
                    const PopupMenuItem(
                      value: 'modificar',
                      child: Text('Modificar Incidencia'),
                    ),
                    if (esCliente)
                      const PopupMenuItem(
                        value: 'borrar',
                        child: Text('Borrar Incidencia',
                            style: TextStyle(color: Colors.red)),
                      ),
                  ],
                )
            ],
          ),
          body: SingleChildScrollView(
            padding: const EdgeInsets.all(16.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                DetalleInfoCard(trabajo: trabajoActual),
                const SizedBox(height: 24),
                if (esCliente &&
                    (trabajoActual.estado == EstadoTrabajo.PENDIENTE ||
                        trabajoActual.estado == EstadoTrabajo.PRESUPUESTADO))
                  DetalleSeccionPresupuestos(
                    presupuestos: _presupuestos,
                    cargando: _cargandoPresupuestos,
                    procesando: _procesando,
                    onRefresh: _cargarPresupuestos,
                    onAceptar: _aceptarPresupuesto,
                  ),
                if (esOperario &&
                    (trabajoActual.estado == EstadoTrabajo.ASIGNADO ||
                        trabajoActual.estado == EstadoTrabajo.REALIZADO))
                  _buildBotonFinalizar(trabajoActual.id),
                if (trabajoActual.estado == EstadoTrabajo.FINALIZADO)
                  DetalleResumenFinal(trabajo: trabajoActual),
                if (esCliente &&
                    (trabajoActual.estado == EstadoTrabajo.FINALIZADO ||
                        trabajoActual.estado == EstadoTrabajo.REALIZADO) &&
                    trabajoActual.valoracion == 0)
                  _buildBotonValorar(trabajoActual.id),
                if (_procesando)
                  const Center(
                      child: Padding(
                    padding: EdgeInsets.all(8.0),
                    child: CircularProgressIndicator(),
                  )),
              ],
            ),
          ),
        );
      },
    );
  }

  Widget _buildBotonFinalizar(int idTrabajo) {
    return SizedBox(
      width: double.infinity,
      child: ElevatedButton.icon(
        onPressed: _procesando ? null : () => _handleFinalizar(idTrabajo),
        icon: const Icon(Icons.check_circle),
        label: const Text('MARCAR COMO FINALIZADO'),
        style: ElevatedButton.styleFrom(
          backgroundColor: Colors.green,
          foregroundColor: Colors.white,
          padding: const EdgeInsets.symmetric(vertical: 12),
        ),
      ),
    );
  }

  Widget _buildBotonValorar(int idTrabajo) {
    return Container(
      margin: const EdgeInsets.only(top: 16),
      width: double.infinity,
      child: ElevatedButton.icon(
        onPressed: _procesando ? null : () => _handleValorar(idTrabajo),
        icon: const Icon(Icons.star),
        label: const Text('VALORAR SERVICIO'),
        style: ElevatedButton.styleFrom(
          backgroundColor: Colors.blue,
          foregroundColor: Colors.white,
          padding: const EdgeInsets.symmetric(vertical: 12),
        ),
      ),
    );
  }
}

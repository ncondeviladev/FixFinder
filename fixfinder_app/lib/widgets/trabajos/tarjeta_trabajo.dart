import 'package:flutter/material.dart';
import '../../models/trabajo.dart';
import '../comunes/estado_badge.dart';

/// Bloque interactivo que representa una incidencia en el Dashboard.
/// Visualiza el estado, la urgencia y los datos principales del trabajo.
class TarjetaTrabajo extends StatefulWidget {
  const TarjetaTrabajo({
    super.key,
    required this.trabajo,
    required this.esCliente,
    required this.accionPendiente,
    required this.onTap,
    required this.onCancelar,
    required this.onModificar,
    required this.obtenerIconoCategoria,
  });

  /// Instancia de la incidencia a mostrar.
  final Trabajo trabajo;
  
  /// Indica si la vista es desde la perspectiva del cliente o del operario.
  final bool esCliente;
  
  /// Flag para indicar si hay una notificación activa sobre esta tarjeta.
  final bool accionPendiente;
  
  /// Callback disparado al pulsar sobre la tarjeta.
  final VoidCallback onTap;
  
  /// Acción para solicitar la anulación definitiva del trabajo.
  final VoidCallback onCancelar;
  
  /// Acción para abrir el formulario de edición de la incidencia.
  final VoidCallback onModificar;
  
  /// Función inyectada para resolver el icono visual de la categoría.
  final Icon Function(CategoriaServicio) obtenerIconoCategoria;

  @override
  State<TarjetaTrabajo> createState() => _TarjetaTrabajoState();
}

class _TarjetaTrabajoState extends State<TarjetaTrabajo>
    with SingleTickerProviderStateMixin {
  late AnimationController _pulseController;
  late Animation<double> _pulseAnimation;

  @override
  void initState() {
    super.initState();
    _pulseController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 900),
    );
    _pulseAnimation = Tween<double>(begin: 1.0, end: 1.25).animate(
      CurvedAnimation(parent: _pulseController, curve: Curves.easeInOut),
    );
    if (widget.accionPendiente) {
      _pulseController.repeat(reverse: true);
    }
  }

  @override
  void didUpdateWidget(TarjetaTrabajo oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.accionPendiente && !_pulseController.isAnimating) {
      _pulseController.repeat(reverse: true);
    } else if (!widget.accionPendiente && _pulseController.isAnimating) {
      _pulseController.stop();
    }
  }

  @override
  void dispose() {
    _pulseController.dispose();
    super.dispose();
  }

  /// Calcula y retorna el texto descriptivo del aviso según el estado.
  String _textoBadge() {
    if (widget.esCliente) {
      if (widget.trabajo.estado == EstadoTrabajo.PRESUPUESTADO) {
        return '¡Presupuesto recibido!';
      }
      if (widget.trabajo.estado == EstadoTrabajo.REALIZADO ||
          (widget.trabajo.estado == EstadoTrabajo.FINALIZADO &&
              widget.trabajo.valoracion == 0)) {
        return '¡Pendiente de valorar!';
      }
    } else {
      if (widget.trabajo.estado == EstadoTrabajo.ACEPTADO) {
        return '¡Presupuesto aceptado!';
      }
    }
    return '';
  }

  @override
  Widget build(BuildContext context) {
    final trabajo = widget.trabajo;

    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      shape: widget.accionPendiente
          ? RoundedRectangleBorder(
              side: BorderSide(color: Colors.orange.shade400, width: 1.8),
              borderRadius: BorderRadius.circular(12),
            )
          : null,
      child: InkWell(
        borderRadius: BorderRadius.circular(12),
        onTap: widget.onTap,
        child: Padding(
          padding: const EdgeInsets.all(12.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // ── Fila superior: icono + título + badge campana + menú ──
              Row(
                children: [
                  widget.obtenerIconoCategoria(trabajo.categoria),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Row(
                      children: [
                        if (trabajo.urgencia >= 3)
                          Container(
                            padding: const EdgeInsets.symmetric(
                                horizontal: 6, vertical: 2),
                            margin: const EdgeInsets.only(right: 8),
                            decoration: BoxDecoration(
                              color: Colors.red,
                              borderRadius: BorderRadius.circular(4),
                            ),
                            child: const Text('URGENTE',
                                style: TextStyle(
                                    color: Colors.white,
                                    fontSize: 10,
                                    fontWeight: FontWeight.bold)),
                          ),
                        Expanded(
                          child: Text(
                            trabajo.titulo,
                            style: const TextStyle(
                                fontSize: 16, fontWeight: FontWeight.bold),
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                      ],
                    ),
                  ),
                  // Campana animada cuando hay acción pendiente
                  if (widget.accionPendiente)
                    ScaleTransition(
                      scale: _pulseAnimation,
                      child: const Icon(
                        Icons.notifications_active,
                        color: Colors.orange,
                        size: 22,
                      ),
                    ),
                  if (widget.esCliente &&
                      (trabajo.estado == EstadoTrabajo.PENDIENTE ||
                          trabajo.estado == EstadoTrabajo.PRESUPUESTADO))
                    PopupMenuButton<String>(
                      onSelected: (val) {
                        if (val == 'cancelar') widget.onCancelar();
                        if (val == 'modificar') widget.onModificar();
                      },
                      itemBuilder: (context) => [
                        const PopupMenuItem(
                          value: 'modificar',
                          child: Text('Modificar Incidencia'),
                        ),
                        const PopupMenuItem(
                          value: 'cancelar',
                          child: Text('Borrar Incidencia',
                              style: TextStyle(color: Colors.red)),
                        ),
                      ],
                      child: const Icon(Icons.more_vert),
                    ),
                ],
              ),

              // ── Banner de aviso ──
              if (widget.accionPendiente) ...[
                const SizedBox(height: 6),
                Container(
                  width: double.infinity,
                  padding:
                      const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                  decoration: BoxDecoration(
                    color: Colors.orange.shade50,
                    borderRadius: BorderRadius.circular(6),
                    border: Border.all(color: Colors.orange.shade300, width: 1),
                  ),
                  child: Row(
                    children: [
                      const Icon(Icons.touch_app,
                          size: 14, color: Colors.orange),
                      const SizedBox(width: 6),
                      Text(
                        _textoBadge(),
                        style: TextStyle(
                          fontSize: 12,
                          color: Colors.orange.shade800,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ],
                  ),
                ),
              ],

              const SizedBox(height: 8),
              Text(
                trabajo.descripcion,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
                style: TextStyle(color: Colors.grey[700]),
              ),
              const SizedBox(height: 12),

              // ── Fila inferior: ubicación + chip de estado ──
              Row(
                children: [
                  const Icon(Icons.location_on, size: 16, color: Colors.grey),
                  const SizedBox(width: 4),
                  Expanded(
                    child: Text(
                      trabajo.direccion,
                      style: const TextStyle(fontSize: 12, color: Colors.grey),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                  EstadoBadge(estado: trabajo.estado),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}

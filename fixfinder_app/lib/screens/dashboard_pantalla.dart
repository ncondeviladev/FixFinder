import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/trabajo_provider.dart';
import '../services/auth_service.dart';
import '../models/trabajo.dart';
import '../models/usuario.dart';
import 'crear_trabajo_pantalla.dart';
import 'detalle_trabajo_pantalla.dart';

class DashboardPantalla extends StatefulWidget {
  const DashboardPantalla({super.key});

  @override
  State<DashboardPantalla> createState() => _DashboardPantallaState();
}

class _DashboardPantallaState extends State<DashboardPantalla> {
  TrabajoProvider? _provider;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _provider = context.read<TrabajoProvider>();
      _provider!.obtenerTrabajos();
      // Polling cada 15 segundos para detectar cambios (presupuestos, estados...)
      _provider!.startPolling(intervaloSegundos: 15);
    });
  }

  @override
  void dispose() {
    // Usamos la referencia guardada, NO context (que ya está deactivado)
    _provider?.stopPolling();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final usuario = AuthService().usuarioActual;
    final esCliente = usuario?.rol == Rol.CLIENTE;

    return Scaffold(
      appBar: AppBar(
        title: const Text('FIXFINDER'),
        actions: [
          IconButton(
            icon: const Icon(Icons.person),
            onPressed: () {
              Navigator.pushNamed(context, '/perfil');
            },
          ),
          IconButton(
            icon: const Icon(Icons.logout),
            onPressed: () {
              AuthService().logout();
              Navigator.pushReplacementNamed(context, '/login');
            },
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: () => context.read<TrabajoProvider>().obtenerTrabajos(),
        child: Consumer<TrabajoProvider>(
          builder: (context, provider, child) {
            if (provider.estaCargando) {
              return const Center(child: CircularProgressIndicator());
            }

            if (provider.trabajos.isEmpty) {
              return const Center(child: Text('No hay trabajos disponibles.'));
            }

            return ListView.builder(
              padding: const EdgeInsets.all(8),
              itemCount: provider.trabajos.length,
              itemBuilder: (context, index) {
                final trabajo = provider.trabajos[index];
                final accionPendiente =
                    _tieneAccionPendiente(trabajo, esCliente);
                return _TarjetaTrabajo(
                  trabajo: trabajo,
                  esCliente: esCliente,
                  accionPendiente: accionPendiente,
                  onTap: () {
                    Navigator.push(
                      context,
                      MaterialPageRoute(
                        builder: (context) =>
                            DetalleTrabajoPantalla(trabajo: trabajo),
                      ),
                    );
                  },
                  onCancelar: () => _confirmarCancelacion(context, trabajo.id),
                  obtenerIconoCategoria: _obtenerIconoCategoria,
                  colorEstado: _colorEstado,
                );
              },
            );
          },
        ),
      ),
      floatingActionButton: esCliente
          ? FloatingActionButton(
              onPressed: () {
                Navigator.push(
                  context,
                  MaterialPageRoute(
                      builder: (context) => const CrearTrabajoPantalla()),
                );
              },
              child: const Icon(Icons.add),
            )
          : null,
    );
  }

  /// Determina si la tarjeta debe mostrar el badge de acción pendiente.
  bool _tieneAccionPendiente(Trabajo trabajo, bool esCliente) {
    if (esCliente) {
      // El cliente debe actuar si hay un presupuesto esperando aceptación
      // o si el trabajo está realizado y pende valoración.
      return trabajo.estado == EstadoTrabajo.PRESUPUESTADO ||
          trabajo.estado == EstadoTrabajo.REALIZADO;
    } else {
      // El operario debe actuar si el cliente aceptó el presupuesto
      return trabajo.estado == EstadoTrabajo.ACEPTADO;
    }
  }

  void _confirmarCancelacion(BuildContext context, int idTrabajo) {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('¿Cancelar incidencia?'),
        content: const Text('Esta acción no se puede deshacer.'),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(ctx), child: const Text('NO')),
          TextButton(
            onPressed: () {
              context.read<TrabajoProvider>().cancelarTrabajo(idTrabajo);
              Navigator.pop(ctx);
            },
            child:
                const Text('SÍ, CANCELAR', style: TextStyle(color: Colors.red)),
          ),
        ],
      ),
    );
  }

  Icon _obtenerIconoCategoria(CategoriaServicio cat) {
    switch (cat) {
      case CategoriaServicio.ELECTRICIDAD:
        return const Icon(Icons.electrical_services);
      case CategoriaServicio.FONTANERIA:
        return const Icon(Icons.water_drop);
      case CategoriaServicio.CLIMATIZACION:
        return const Icon(Icons.ac_unit);
      case CategoriaServicio.PINTURA:
        return const Icon(Icons.format_paint);
      case CategoriaServicio.ALBANILERIA:
        return const Icon(Icons.foundation);
      default:
        return const Icon(Icons.build);
    }
  }

  Color _colorEstado(EstadoTrabajo e) {
    switch (e) {
      case EstadoTrabajo.PENDIENTE:
        return Colors.orange;
      case EstadoTrabajo.PRESUPUESTADO:
        return Colors.purple;
      case EstadoTrabajo.ACEPTADO:
        return Colors.teal;
      case EstadoTrabajo.ASIGNADO:
        return Colors.blue;
      case EstadoTrabajo.REALIZADO:
        return Colors.green;
      case EstadoTrabajo.FINALIZADO:
        return Colors.grey;
    }
  }
}

// ---------------------------------------------------------------------------
// Widget separado para la tarjeta, con animación de pulso en el badge
// ---------------------------------------------------------------------------
class _TarjetaTrabajo extends StatefulWidget {
  final Trabajo trabajo;
  final bool esCliente;
  final bool accionPendiente;
  final VoidCallback onTap;
  final VoidCallback onCancelar;
  final Icon Function(CategoriaServicio) obtenerIconoCategoria;
  final Color Function(EstadoTrabajo) colorEstado;

  const _TarjetaTrabajo({
    required this.trabajo,
    required this.esCliente,
    required this.accionPendiente,
    required this.onTap,
    required this.onCancelar,
    required this.obtenerIconoCategoria,
    required this.colorEstado,
  });

  @override
  State<_TarjetaTrabajo> createState() => _TarjetaTrabajoState();
}

class _TarjetaTrabajoState extends State<_TarjetaTrabajo>
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
  void didUpdateWidget(_TarjetaTrabajo oldWidget) {
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

  String _textoBadge() {
    if (widget.esCliente) {
      if (widget.trabajo.estado == EstadoTrabajo.PRESUPUESTADO) {
        return '¡Presupuesto recibido!';
      }
      if (widget.trabajo.estado == EstadoTrabajo.REALIZADO) {
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
    final colorE = widget.colorEstado(trabajo.estado);

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
                      trabajo.estado == EstadoTrabajo.PENDIENTE)
                    PopupMenuButton<String>(
                      onSelected: (val) {
                        if (val == 'cancelar') widget.onCancelar();
                      },
                      itemBuilder: (context) => [
                        const PopupMenuItem(
                          value: 'cancelar',
                          child: Text('Cancelar Incidencia',
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
                  Container(
                    padding:
                        const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                    decoration: BoxDecoration(
                      color: colorE.withValues(alpha: 0.1),
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(color: colorE),
                    ),
                    child: Text(
                      trabajo.estado.name,
                      style: TextStyle(
                        fontSize: 12,
                        color: colorE,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}

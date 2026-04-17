import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/trabajo_provider.dart';
import '../providers/usuario_provider.dart';
import '../services/auth_service.dart';
import '../models/trabajo.dart';
import '../models/usuario.dart';
import 'crear_trabajo_pantalla.dart';
import 'detalle_trabajo_pantalla.dart';
import '../widgets/trabajos/tarjeta_trabajo.dart';

/// Pantalla principal (Dashboard) que muestra la lista de trabajos del usuario.
/// 
/// Esta vista es dinámica y se adapta al rol del usuario (Cliente u Operario).
/// Centraliza el acceso al listado de incidencias y perfil.
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
      // Sin polling automático: se recarga al entrar, al volver de pantallas y con pull-to-refresh
    });
  }

  @override
  void dispose() {
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final usuario = context.watch<UsuarioProvider>().usuario;
    
    // Si la sesión se ha cerrado (ej: por token expirado), volvemos al login
    if (usuario == null) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        Navigator.pushReplacementNamed(context, '/login');
      });
      return const SizedBox.shrink(); 
    }

    final esCliente = usuario.rol == Rol.CLIENTE;

    return Scaffold(
      appBar: AppBar(
        leading: Navigator.canPop(context)
            ? null
            : Padding(
                padding: const EdgeInsets.all(8.0),
                child: ClipOval(child: Image.asset('assets/images/logo.png')),
              ),
        title: const Text('FIXFINDER'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            tooltip: 'Actualizar',
            onPressed: () => context.read<TrabajoProvider>().obtenerTrabajos(),
          ),
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
              context.read<UsuarioProvider>().limpiar();
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
              // CustomScrollView para que el pull-to-refresh funcione en pantalla vacía
              return CustomScrollView(
                physics: const AlwaysScrollableScrollPhysics(),
                slivers: [
                  SliverFillRemaining(
                    child: Center(
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          const Icon(Icons.inbox, size: 60, color: Colors.grey),
                          const SizedBox(height: 16),
                          const Text('No hay trabajos disponibles.',
                              style: TextStyle(color: Colors.grey)),
                          const SizedBox(height: 16),
                          TextButton.icon(
                            onPressed: () => context
                                .read<TrabajoProvider>()
                                .obtenerTrabajos(),
                            icon: const Icon(Icons.refresh),
                            label: const Text('Actualizar'),
                          )
                        ],
                      ),
                    ),
                  ),
                ],
              );
            }

            return ListView.builder(
              padding: const EdgeInsets.all(8),
              itemCount: provider.trabajos.length,
              itemBuilder: (context, index) {
                final trabajo = provider.trabajos[index];
                final accionPendiente =
                    _tieneAccionPendiente(trabajo, esCliente);
                return TarjetaTrabajo(
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
                    ).then((_) async {
                      // Esperamos a que el servidor procese la acción
                      await Future.delayed(const Duration(milliseconds: 900));
                      if (context.mounted) {
                        context.read<TrabajoProvider>().obtenerTrabajos();
                      }
                    });
                  },
                  onCancelar: () => _confirmarCancelacion(context, trabajo.id),
                  onModificar: () {
                    Navigator.push(
                      context,
                      MaterialPageRoute(
                        builder: (context) =>
                            CrearTrabajoPantalla(trabajoAEditar: trabajo),
                      ),
                    ).then((_) async {
                      await Future.delayed(const Duration(milliseconds: 500));
                      if (context.mounted) {
                        context.read<TrabajoProvider>().obtenerTrabajos();
                      }
                    });
                  },
                  obtenerIconoCategoria: _obtenerIconoCategoria,
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
                ).then((_) {
                  context.read<TrabajoProvider>().obtenerTrabajos();
                });
              },
              child: const Icon(Icons.add),
            )
          : null,
    );
  }

  /// Determina si la tarjeta debe mostrar el badge de acción pendiente.
  bool _tieneAccionPendiente(Trabajo trabajo, bool esCliente) {
    if (esCliente) {
      // Presupuesto pendiente de aceptar
      if (trabajo.estado == EstadoTrabajo.PRESUPUESTADO) return true;
      // Trabajo realizado pendiente de valorar (y aún sin valorar)
      if (trabajo.estado == EstadoTrabajo.REALIZADO && trabajo.valoracion == 0) {
        return true;
      }
      // Trabajo finalizado por operario y el cliente aún no ha valorado
      if (trabajo.estado == EstadoTrabajo.FINALIZADO && trabajo.valoracion == 0) {
        return true;
      }
      return false;
    } else {
      // El operario debe actuar si tiene un trabajo asignado o si hay uno aceptado listo para empezar
      return trabajo.estado == EstadoTrabajo.ACEPTADO ||
          trabajo.estado == EstadoTrabajo.ASIGNADO;
    }
  }

  /// Muestra un cuadro de diálogo para que el cliente confirme la cancelación.
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

  /// Mapea la categoría del servicio a un icono representativo de la interfaz.
  /// 
  /// Permite una rápida identificación visual del tipo de avería en el listado.
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
}

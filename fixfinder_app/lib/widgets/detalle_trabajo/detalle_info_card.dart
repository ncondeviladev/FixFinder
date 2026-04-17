// Tarjeta principal de la pantalla de detalles.
// Muestra la información básica del trabajo, estado, categoría y contactos asignados.
import 'package:flutter/material.dart';
import '../../models/trabajo.dart';
import '../../models/usuario.dart';
import 'package:provider/provider.dart';
import '../../providers/usuario_provider.dart';
import '../../services/auth_service.dart';
import '../comunes/dato_fila.dart';
import '../comunes/estado_badge.dart';
import '../trabajos/tarjeta_contacto.dart';
import '../trabajos/galeria_fotos.dart';
import '../../services/external_launcher_service.dart';

/// Tarjeta informativa que agrupa los datos básicos de una incidencia.
/// Muestra título, descripción, ubicación y contactos relevantes según el rol.
class DetalleInfoCard extends StatelessWidget {
  final Trabajo trabajo;

  const DetalleInfoCard({super.key, required this.trabajo});

  @override
  Widget build(BuildContext context) {
    return Card(
      elevation: 4,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(trabajo.titulo,
                style:
                    const TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
            const Divider(),
            Row(
              children: [
                const Text('Estado: ',
                    style: TextStyle(fontWeight: FontWeight.bold)),
                EstadoBadge(estado: trabajo.estado),
              ],
            ),
            const SizedBox(height: 8),
            DatoFila(etiqueta: 'Categoría', valor: trabajo.categoria.name),
            Row(
              children: [
                const Text('Dirección: ', style: TextStyle(fontWeight: FontWeight.bold)),
                Expanded(child: Text(trabajo.direccion, overflow: TextOverflow.ellipsis)),
                IconButton(
                  icon: const Icon(Icons.map, size: 20, color: Colors.blue),
                  onPressed: () => ExternalLauncherService.abrirMapa(trabajo.direccion),
                  tooltip: 'Ver en mapa',
                ),
              ],
            ),
            const SizedBox(height: 12),
            const Text('Descripción:',
                style: TextStyle(fontWeight: FontWeight.bold)),
            Text(trabajo.descripcion),
            const SizedBox(height: 16),
            if (trabajo.urlsFotos.isNotEmpty) ...[
              GaleriaFotos(urls: trabajo.urlsFotos),
            ],
            const SizedBox(height: 16),
            if (context.watch<UsuarioProvider>().usuario?.rol == Rol.OPERARIO &&
                trabajo.cliente != null)
              TarjetaContacto(
                  titulo: 'Datos del Cliente:', usuario: trabajo.cliente!),
            if (context.watch<UsuarioProvider>().usuario?.rol == Rol.CLIENTE &&
                trabajo.operarioAsignado != null)
              TarjetaContacto(
                  titulo: 'Técnico Asignado:',
                  usuario: trabajo.operarioAsignado!,
                  esOperario: true),
          ],
        ),
      ),
    );
  }
}

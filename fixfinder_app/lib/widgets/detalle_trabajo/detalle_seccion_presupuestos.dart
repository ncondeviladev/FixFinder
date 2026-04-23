
import 'package:flutter/material.dart';
import '../../services/external_launcher_service.dart';
import '../../models/presupuesto.dart';
import '../../theme/fixfinder_theme.dart';

/// Sección que gestiona y visualiza la lista de presupuestos recibidos para una incidencia.
/// Permite al cliente comparar ofertas y tomar decisiones de aceptación o rechazo.
class DetalleSeccionPresupuestos extends StatelessWidget {
  final List<Presupuesto> presupuestos;
  final bool cargando;
  final bool procesando;
  final VoidCallback onRefresh;
  final Function(int) onAceptar;
  final Function(int) onRechazar;

  const DetalleSeccionPresupuestos({
    super.key,
    required this.presupuestos,
    required this.cargando,
    required this.procesando,
    required this.onRefresh,
    required this.onAceptar,
    required this.onRechazar,
  });

  void _verDetallesEmpresa(BuildContext context, Presupuesto p) {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Información de la Empresa'),
        content: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Cabecera del diálogo con Logo y Nombre
              Center(
                child: Column(
                  children: [
                    CircleAvatar(
                      radius: 40,
                      backgroundColor: Theme.of(context).colorScheme.primary.withOpacity(0.1),
                      backgroundImage: (p.urlFotoEmpresa != null && p.urlFotoEmpresa!.isNotEmpty)
                          ? NetworkImage(p.urlFotoEmpresa!)
                          : null,
                      child: (p.urlFotoEmpresa == null || p.urlFotoEmpresa!.isEmpty)
                          ? Icon(Icons.business, size: 40, color: Theme.of(context).colorScheme.primary)
                          : null,
                    ),
                    const SizedBox(height: 12),
                    Text(
                      p.nombreEmpresa ?? "Desconocido",
                      textAlign: TextAlign.center,
                      style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 20),
              const Divider(),
              const SizedBox(height: 10),
              
              // Bloques de información interactiva
              _crearFilaInfo(
                context, 
                Icons.assignment_ind, 
                'CIF', 
                p.cifEmpresa ?? "No disponible"
              ),
              
              _crearFilaInfo(
                context, 
                Icons.email, 
                'Email', 
                p.emailEmpresa ?? "No disponible"
              ),

              _crearFilaInfo(
                context, 
                Icons.phone, 
                'Teléfono', 
                p.telefonoEmpresa ?? "No disponible",
                onTap: () => ExternalLauncherService.llamarTelefono(p.telefonoEmpresa),
                color: Theme.of(context).colorScheme.primary,
              ),

              _crearFilaInfo(
                context, 
                Icons.location_on, 
                'Dirección', 
                p.direccionEmpresa ?? "No especificada",
                trailing: IconButton(
                  icon: const Icon(Icons.map_outlined, color: Colors.blue),
                  onPressed: () => ExternalLauncherService.abrirMapa(p.direccionEmpresa),
                ),
              ),

              const SizedBox(height: 15),
              const Divider(),
              const SizedBox(height: 10),
              const Text('Propuesta Técnica:',
                  style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold, color: Colors.grey)),
              const SizedBox(height: 4),
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: Colors.grey.withOpacity(0.05),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Text(p.notas ?? "Sin notas adicionales", 
                    style: const TextStyle(fontSize: 14, fontStyle: FontStyle.italic)),
              ),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: const Text('CERRAR'),
          ),
        ],
      ),
    );
  }

  Widget _crearFilaInfo(BuildContext context, IconData icono, String etiqueta, String valor, {VoidCallback? onTap, Widget? trailing, Color? color}) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8.0),
      child: Row(
        children: [
          Icon(icono, size: 20, color: Colors.blueGrey),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(etiqueta, style: const TextStyle(fontSize: 11, color: Colors.grey, fontWeight: FontWeight.bold)),
                GestureDetector(
                  onTap: onTap,
                  child: Text(
                    valor, 
                    style: TextStyle(
                      fontSize: 14, 
                      fontWeight: FontWeight.w500,
                      decoration: onTap != null ? TextDecoration.underline : null,
                      color: color ?? Theme.of(context).textTheme.bodyLarge?.color,
                    )
                  ),
                ),
              ],
            ),
          ),
          if (trailing != null) trailing,
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            const Text('PRESUPUESTOS RECIBIDOS',
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
            IconButton(
                onPressed: procesando ? null : onRefresh,
                icon: const Icon(Icons.refresh)),
          ],
        ),
        const SizedBox(height: 8),
        if (cargando)
          const Center(child: CircularProgressIndicator())
        else if (presupuestos.isEmpty)
          Padding(
            padding: const EdgeInsets.symmetric(vertical: 20),
            child: Text('Aún no hay presupuestos para esta incidencia.',
                style: TextStyle(
                    color: Theme.of(context).disabledColor,
                    fontStyle: FontStyle.italic)),
          )
        else
          ...presupuestos
              .where((p) => p.estado != 'RECHAZADO')
              .map((p) => _crearTarjetaPresupuesto(context, p)),
      ],
    );
  }

  Widget _crearTarjetaPresupuesto(BuildContext context, Presupuesto p) {
    final bool esPendiente = p.estado == 'PENDIENTE';

    return Card(
      elevation: 3,
      margin: const EdgeInsets.only(bottom: 12),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Cabecera de la tarjeta: Empresa y Precio
          ListTile(
            leading: CircleAvatar(
              backgroundColor: Theme.of(context).colorScheme.secondary.withOpacity(0.1),
              backgroundImage: (p.urlFotoEmpresa != null && p.urlFotoEmpresa!.isNotEmpty)
                ? NetworkImage(p.urlFotoEmpresa!)
                : null,
              child: (p.urlFotoEmpresa == null || p.urlFotoEmpresa!.isEmpty)
                ? Icon(Icons.business, color: Theme.of(context).colorScheme.primary)
                : null,
            ),
            title: Text(
              p.nombreEmpresa ?? "Empresa",
              style: const TextStyle(fontWeight: FontWeight.bold),
            ),
            trailing: Text(
              '${p.monto.toStringAsFixed(2)}€',
              style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                  color: FixFinderTheme.successColor),
            ),
          ),

          // Cuerpo de la tarjeta: Propuesta técnica
          if (p.notas != null && p.notas!.isNotEmpty)
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16.0),
              child: Container(
                padding: const EdgeInsets.all(12),
                width: double.infinity,
                decoration: BoxDecoration(
                  color: Theme.of(context).colorScheme.secondary.withOpacity(0.05),
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(
                      color: Theme.of(context).colorScheme.secondary.withOpacity(0.1)),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('Propuesta técnica:',
                        style: TextStyle(
                            fontSize: 12,
                            fontWeight: FontWeight.bold,
                            color: Theme.of(context).hintColor)),
                    const SizedBox(height: 4),
                    Text(p.notas!,
                        style: const TextStyle(fontSize: 14, height: 1.3)),
                  ],
                ),
              ),
            ),

          // Botones de acción
          Padding(
            padding: const EdgeInsets.all(12.0),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.end,
              children: [
                TextButton.icon(
                  onPressed: () => _verDetallesEmpresa(context, p),
                  icon: const Icon(Icons.info_outline),
                  label: const Text('INFO'),
                ),
                const Spacer(),
                if (esPendiente) ...[
                  OutlinedButton(
                    onPressed: procesando ? null : () => onRechazar(p.id),
                    style: OutlinedButton.styleFrom(
                      foregroundColor: Theme.of(context).colorScheme.error,
                      side:
                          BorderSide(color: Theme.of(context).colorScheme.error),
                    ),
                    child: const Text('RECHAZAR'),
                  ),
                  const SizedBox(width: 8),
                  ElevatedButton(
                    onPressed: procesando ? null : () => onAceptar(p.id),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: FixFinderTheme.successColor,
                      foregroundColor: Theme.of(context).colorScheme.onPrimary,
                    ),
                    child: const Text('ACEPTAR'),
                  ),
                ] else
                  Chip(
                    label: Text(p.estado),
                    backgroundColor: p.estado == 'ACEPTADO'
                        ? FixFinderTheme.successColor.withOpacity(0.2)
                        : Theme.of(context).disabledColor.withOpacity(0.2),
                  ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

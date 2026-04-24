import 'package:flutter/material.dart';
import '../../models/presupuesto.dart';
import '../../services/external_launcher_service.dart';
import '../../theme/fixfinder_theme.dart';

/// Widget unificado para mostrar la información de una empresa (vía presupuesto).
/// Se utiliza tanto en el listado de presupuestos como en la cabecera cuando ya está aceptado.
class TarjetaEmpresaPresupuesto extends StatelessWidget {
  final Presupuesto presupuesto;
  final bool mostrarAcciones;
  final bool mostrarNotas;
  final bool procesando;
  final Function(int)? onAceptar;
  final Function(int)? onRechazar;

  const TarjetaEmpresaPresupuesto({
    super.key,
    required this.presupuesto,
    this.mostrarAcciones = true,
    this.mostrarNotas = true,
    this.procesando = false,
    this.onAceptar,
    this.onRechazar,
  });

  @override
  Widget build(BuildContext context) {
    final bool esPendiente = presupuesto.estado == 'PENDIENTE';

    return Card(
      elevation: 3,
      margin: const EdgeInsets.only(bottom: 12),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Cabecera: Logo, Nombre y Monto
          ListTile(
            leading: CircleAvatar(
              backgroundColor:
                  Theme.of(context).colorScheme.secondary.withOpacity(0.1),
              backgroundImage: (presupuesto.urlFotoEmpresa != null &&
                      presupuesto.urlFotoEmpresa!.isNotEmpty)
                  ? NetworkImage(presupuesto.urlFotoEmpresa!)
                  : null,
              child: (presupuesto.urlFotoEmpresa == null ||
                      presupuesto.urlFotoEmpresa!.isEmpty)
                  ? Icon(Icons.business,
                      color: Theme.of(context).colorScheme.primary)
                  : null,
            ),
            title: Text(
              presupuesto.nombreEmpresa ?? "Empresa",
              style: const TextStyle(fontWeight: FontWeight.bold),
            ),
            trailing: Text(
              '${presupuesto.monto.toStringAsFixed(2)}€',
              style: const TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                  color: FixFinderTheme.successColor),
            ),
          ),

          // Notas / Propuesta técnica (opcional)
          if (mostrarNotas &&
              presupuesto.notas != null &&
              presupuesto.notas!.isNotEmpty)
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16.0),
              child: Container(
                padding: const EdgeInsets.all(12),
                width: double.infinity,
                decoration: BoxDecoration(
                  color:
                      Theme.of(context).colorScheme.secondary.withOpacity(0.05),
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(
                      color: Theme.of(context)
                          .colorScheme
                          .secondary
                          .withOpacity(0.1)),
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
                    Text(presupuesto.notas!,
                        style: const TextStyle(fontSize: 14, height: 1.3)),
                  ],
                ),
              ),
            ),

          // Botones: INFO (siempre a la izquierda) + ACEPTAR/RECHAZAR (si mostrarAcciones es true)
          Padding(
            padding: const EdgeInsets.all(12.0),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.start,
              children: [
                TextButton.icon(
                  onPressed: () => _verDetallesEmpresa(context),
                  icon: const Icon(Icons.info_outline),
                  label: const Text('INFO'),
                ),
                if (mostrarAcciones) ...[
                  const Spacer(),
                  if (esPendiente) ...[
                    OutlinedButton(
                      onPressed: procesando
                          ? null
                          : () => onRechazar?.call(presupuesto.id),
                      style: OutlinedButton.styleFrom(
                        foregroundColor: Theme.of(context).colorScheme.error,
                        side: BorderSide(
                            color: Theme.of(context).colorScheme.error),
                      ),
                      child: const Text('RECHAZAR'),
                    ),
                    const SizedBox(width: 8),
                    ElevatedButton(
                      onPressed: procesando
                          ? null
                          : () => onAceptar?.call(presupuesto.id),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: FixFinderTheme.successColor,
                        foregroundColor:
                            Theme.of(context).colorScheme.onPrimary,
                      ),
                      child: const Text('ACEPTAR'),
                    ),
                  ] else
                    Chip(
                      label: Text(presupuesto.estado),
                      backgroundColor: presupuesto.estado == 'ACEPTADO'
                          ? FixFinderTheme.successColor.withOpacity(0.2)
                          : Theme.of(context).disabledColor.withOpacity(0.2),
                    ),
                ],
              ],
            ),
          ),
        ],
      ),
    );
  }

  /// Diálogo unificado de información legal y contacto.
  void _verDetallesEmpresa(BuildContext context) {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Información de la Empresa'),
        content: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Center(
                child: Column(
                  children: [
                    CircleAvatar(
                      radius: 40,
                      backgroundColor: Theme.of(context)
                          .colorScheme
                          .primary
                          .withOpacity(0.1),
                      backgroundImage: (presupuesto.urlFotoEmpresa != null &&
                              presupuesto.urlFotoEmpresa!.isNotEmpty)
                          ? NetworkImage(presupuesto.urlFotoEmpresa!)
                          : null,
                      child: (presupuesto.urlFotoEmpresa == null ||
                              presupuesto.urlFotoEmpresa!.isEmpty)
                          ? Icon(Icons.business,
                              size: 40,
                              color: Theme.of(context).colorScheme.primary)
                          : null,
                    ),
                    const SizedBox(height: 12),
                    Text(
                      presupuesto.nombreEmpresa ?? "Desconocido",
                      textAlign: TextAlign.center,
                      style: const TextStyle(
                          fontSize: 18, fontWeight: FontWeight.bold),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 20),
              const Divider(),
              const SizedBox(height: 10),
              _crearFilaInfo(context, Icons.assignment_ind, 'CIF',
                  presupuesto.cifEmpresa ?? "No disponible"),
              _crearFilaInfo(context, Icons.email, 'Email',
                  presupuesto.emailEmpresa ?? "No disponible"),
              _crearFilaInfo(
                context,
                Icons.phone,
                'Teléfono',
                presupuesto.telefonoEmpresa ?? "No disponible",
                onTap: () => ExternalLauncherService.llamarTelefono(
                    presupuesto.telefonoEmpresa),
                color: Theme.of(context).colorScheme.primary,
              ),
              _crearFilaInfo(
                context,
                Icons.location_on,
                'Dirección',
                presupuesto.direccionEmpresa ?? "No especificada",
                trailing: IconButton(
                  icon: const Icon(Icons.map_outlined, color: Colors.blue),
                  onPressed: () => ExternalLauncherService.abrirMapa(
                      presupuesto.direccionEmpresa),
                ),
              ),
              if (mostrarNotas) ...[
                const SizedBox(height: 15),
                const Divider(),
                const SizedBox(height: 10),
                const Text('Propuesta Técnica:',
                    style: TextStyle(
                        fontSize: 14,
                        fontWeight: FontWeight.bold,
                        color: Colors.grey)),
                const SizedBox(height: 4),
                Container(
                  width: double.infinity,
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: Colors.grey.withOpacity(0.05),
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: Text(presupuesto.notas ?? "Sin notas adicionales",
                      style: const TextStyle(
                          fontSize: 14, fontStyle: FontStyle.italic)),
                ),
              ],
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

  Widget _crearFilaInfo(
      BuildContext context, IconData icono, String etiqueta, String valor,
      {VoidCallback? onTap, Widget? trailing, Color? color}) {
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
                Text(etiqueta,
                    style: const TextStyle(
                        fontSize: 11,
                        color: Colors.grey,
                        fontWeight: FontWeight.bold)),
                GestureDetector(
                  onTap: onTap,
                  child: Text(valor,
                      style: TextStyle(
                        fontSize: 14,
                        fontWeight: FontWeight.w500,
                        decoration:
                            onTap != null ? TextDecoration.underline : null,
                        color: color ??
                            Theme.of(context).textTheme.bodyLarge?.color,
                      )),
                ),
              ],
            ),
          ),
          if (trailing != null) trailing,
        ],
      ),
    );
  }
}

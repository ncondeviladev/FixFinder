// Widget que se muestra únicamente cuando un trabajo ha sido marcado como FINALIZADO.
// Resume el cierre de la tarea mostrando coste, fecha de fin y la valoración.
import 'package:flutter/material.dart';
import '../../models/trabajo.dart';
import '../comunes/dato_fila.dart';
import '../../theme/fixfinder_theme.dart';
import '../../utils/date_format_utils.dart';

/// Widget que resume el cierre de un trabajo una vez finalizado.
/// Muestra detalles como la fecha de fin, el coste pactado y las valoraciones.
class DetalleResumenFinal extends StatelessWidget {
  final Trabajo trabajo;

  const DetalleResumenFinal({super.key, required this.trabajo});

  @override
  Widget build(BuildContext context) {
    return Card(
      color: FixFinderTheme.successColor.withOpacity(0.12),
      margin: const EdgeInsets.only(top: 8),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: BorderSide(color: FixFinderTheme.successColor.withOpacity(0.4)),
      ),
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Row(
              children: [
                Icon(Icons.check_circle, color: FixFinderTheme.successColor),
                SizedBox(width: 8),
                Text(
                  'Trabajo Completado',
                  style: TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                    color: FixFinderTheme.successColor,
                  ),
                ),
              ],
            ),
            const Divider(height: 24),
            if (trabajo.fechaFinalizacion != null)
              DatoFila(
                  etiqueta: 'Finalizado',
                  valor: DateFormatUtils.formatIsoString(
                      trabajo.fechaFinalizacion),
                  color: Theme.of(context).hintColor),
            if (trabajo.presupuesto != null)
              DatoFila(
                  etiqueta: 'Precio final',
                  valor: '${trabajo.presupuesto!.monto.toStringAsFixed(2)} €',
                  color: FixFinderTheme.successColor),
            if (trabajo.valoracion > 0) ...[
              const SizedBox(height: 8),
              const Text('Valoración:',
                  style: TextStyle(fontWeight: FontWeight.bold)),
              Row(
                children: List.generate(
                    5,
                    (i) => Icon(
                          i < trabajo.valoracion
                              ? Icons.star
                              : Icons.star_border,
                          color: Colors.amber,
                        )),
              ),
            ],
            if (trabajo.comentarioCliente != null &&
                trabajo.comentarioCliente!.isNotEmpty) ...[
              const SizedBox(height: 8),
              Text('"${trabajo.comentarioCliente}"',
                  style: TextStyle(
                      fontStyle: FontStyle.italic,
                      color: Theme.of(context).hintColor)),
            ],
          ],
        ),
      ),
    );
  }
}

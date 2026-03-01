// Widget que se muestra únicamente cuando un trabajo ha sido marcado como FINALIZADO.
// Resume el cierre de la tarea mostrando coste, fecha de fin y la valoración.
import 'package:flutter/material.dart';
import '../../models/trabajo.dart';
import '../comunes/dato_fila.dart';

class DetalleResumenFinal extends StatelessWidget {
  final Trabajo trabajo;

  const DetalleResumenFinal({super.key, required this.trabajo});

  @override
  Widget build(BuildContext context) {
    return Card(
      color: Colors.green.shade50,
      margin: const EdgeInsets.only(top: 8),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: BorderSide(color: Colors.green.shade200),
      ),
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(Icons.check_circle, color: Colors.green.shade700),
                const SizedBox(width: 8),
                Text(
                  'Trabajo Completado',
                  style: TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                    color: Colors.green.shade700,
                  ),
                ),
              ],
            ),
            const Divider(height: 24),
            if (trabajo.fechaFinalizacion != null)
              DatoFila(
                  etiqueta: 'Finalizado',
                  valor: trabajo.fechaFinalizacion!,
                  color: Colors.grey[700]),
            if (trabajo.presupuesto != null)
              DatoFila(
                  etiqueta: 'Precio final',
                  valor: '${trabajo.presupuesto!.monto.toStringAsFixed(2)} €',
                  color: Colors.green.shade800),
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
                  style: const TextStyle(
                      fontStyle: FontStyle.italic, color: Colors.grey)),
            ],
          ],
        ),
      ),
    );
  }
}

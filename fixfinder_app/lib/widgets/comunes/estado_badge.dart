// Widget que muestra el estado de un trabajo mediante una etiqueta coloreada.
// Los colores cambian dinámicamente según el estado actual de la incidencia.
import 'package:flutter/material.dart';
import '../../models/trabajo.dart';

class EstadoBadge extends StatelessWidget {
  final EstadoTrabajo estado;

  const EstadoBadge({super.key, required this.estado});

  Color _obtenerColor(EstadoTrabajo e) {
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
      case EstadoTrabajo.PAGADO:
        return Colors.indigo;
      case EstadoTrabajo.CANCELADO:
        return Colors.red;
    }
  }

  @override
  Widget build(BuildContext context) {
    final color = _obtenerColor(estado);
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: color.withOpacity(0.1),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: color),
      ),
      child: Text(
        estado.name,
        style: TextStyle(
          fontSize: 12,
          color: color,
          fontWeight: FontWeight.bold,
        ),
      ),
    );
  }
}

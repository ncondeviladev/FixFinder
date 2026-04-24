import 'package:flutter/material.dart';
import '../../models/trabajo.dart';

/// Etiqueta visual que representa el estado evolutivo de un trabajo.
/// Utiliza una paleta de colores semánticos para indicar la fase actual de la incidencia.
class EstadoBadge extends StatelessWidget {
  final EstadoTrabajo estado;

  const EstadoBadge({super.key, required this.estado});

  Color _obtenerColor(BuildContext context, EstadoTrabajo e) {
    switch (e) {
      case EstadoTrabajo.PENDIENTE:
        return const Color(0xFFF59E0B); // Amarillo
      case EstadoTrabajo.PRESUPUESTADO:
        return const Color(0xFF38BDF8); // Azul Claro
      case EstadoTrabajo.ACEPTADO:
        return const Color(0xFF3B82F6); // Azul Medio
      case EstadoTrabajo.ASIGNADO:
        return const Color(0xFFA855F7); // Morado
      case EstadoTrabajo.REALIZADO:
      case EstadoTrabajo.FINALIZADO:
        return const Color(0xFF22C55E); // Verde
      case EstadoTrabajo.PAGADO:
        return Theme.of(context).colorScheme.secondary;
      case EstadoTrabajo.CANCELADO:
        return Theme.of(context).colorScheme.error;
    }
  }

  @override
  Widget build(BuildContext context) {
    final color = _obtenerColor(context, estado);
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

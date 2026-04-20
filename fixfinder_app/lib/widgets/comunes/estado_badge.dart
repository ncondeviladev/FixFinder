import 'package:flutter/material.dart';
import '../../models/trabajo.dart';
import '../../theme/fixfinder_theme.dart';

/// Etiqueta visual que representa el estado evolutivo de un trabajo.
/// Utiliza una paleta de colores semánticos para indicar la fase actual de la incidencia.
class EstadoBadge extends StatelessWidget {
  final EstadoTrabajo estado;

  const EstadoBadge({super.key, required this.estado});

  Color _obtenerColor(BuildContext context, EstadoTrabajo e) {
    switch (e) {
      case EstadoTrabajo.PENDIENTE:
        return Theme.of(context).colorScheme.primary;
      case EstadoTrabajo.PRESUPUESTADO:
        return FixFinderTheme.adminColor;
      case EstadoTrabajo.ACEPTADO:
        return FixFinderTheme.infoColor;
      case EstadoTrabajo.ASIGNADO:
        return Theme.of(context).colorScheme.tertiary;
      case EstadoTrabajo.REALIZADO:
        return FixFinderTheme.successColor;
      case EstadoTrabajo.FINALIZADO:
        return Theme.of(context).disabledColor;
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

// Widget reutilizable para mostrar una etiqueta y un valor en formato fila.
// Usado intensivamente en pantallas de detalle para mostrar información ordenada.
import 'package:flutter/material.dart';

/// Representación minimalista de un par clave-valor en formato fila.
/// Se utiliza para visualizar datos técnicos y metadatos de forma estructurada.
class DatoFila extends StatelessWidget {
  final String etiqueta;
  final String valor;
  final Color? color;

  const DatoFila({
    super.key,
    required this.etiqueta,
    required this.valor,
    this.color,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 2.0),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            '$etiqueta: ',
            style: const TextStyle(fontWeight: FontWeight.bold),
          ),
          Expanded(
            child: Text(
              valor,
              style: TextStyle(
                color: color,
                fontWeight: color != null ? FontWeight.bold : FontWeight.normal,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

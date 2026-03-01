// Widget reutilizable para mostrar una etiqueta y un valor en formato fila.
// Usado intensivamente en pantallas de detalle para mostrar información ordenada.
import 'package:flutter/material.dart';

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
      padding: const EdgeInsets.symmetric(vertical: 4.0),
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

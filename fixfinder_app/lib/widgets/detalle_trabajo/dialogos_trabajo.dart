// Clase utilitaria que contiene componentes de diálogos superpuestos.
// Reúne los popup para confirmar borrado, ingresar horas y gastos al finalizar, o valorar servicio.
import 'package:flutter/material.dart';

class DialogosTrabajo {
  static Future<bool> mostrarDialogoBorrar(BuildContext context) async {
    final confirmacion = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Borrar Incidencia'),
        content: const Text(
            '¿Estás seguro de que deseas borrar esta incidencia? Esta acción no se puede deshacer.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: const Text('CANCELAR'),
          ),
          ElevatedButton(
            onPressed: () => Navigator.pop(ctx, true),
            style: ElevatedButton.styleFrom(backgroundColor: Colors.red),
            child: const Text('BORRAR', style: TextStyle(color: Colors.white)),
          ),
        ],
      ),
    );
    return confirmacion ?? false;
  }

  static Future<Map<String, String>?> mostrarDialogoFinalizar(
      BuildContext context) async {
    final materialController = TextEditingController();
    final horasController = TextEditingController();

    return showDialog<Map<String, String>>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Finalizar Trabajo'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
                '¿Has completado esta incidencia? Añade el material gastado y horas (opcional):'),
            const SizedBox(height: 12),
            TextField(
              controller: materialController,
              decoration: const InputDecoration(
                labelText: 'Material y recambios usados',
                hintText: 'Ej: 2 tubos PVC, cableado...',
              ),
              maxLines: 2,
            ),
            const SizedBox(height: 12),
            TextField(
              controller: horasController,
              keyboardType: TextInputType.number,
              decoration: const InputDecoration(
                labelText: 'Horas trabajadas',
                hintText: 'Ej: 2',
              ),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, null),
            child: const Text('CANCELAR'),
          ),
          ElevatedButton(
            onPressed: () {
              Navigator.pop(ctx, {
                'material': materialController.text,
                'horas': horasController.text,
              });
            },
            style: ElevatedButton.styleFrom(backgroundColor: Colors.green),
            child:
                const Text('FINALIZAR', style: TextStyle(color: Colors.white)),
          ),
        ],
      ),
    );
  }

  static Future<Map<String, dynamic>?> mostrarDialogoValorar(
      BuildContext context) async {
    final comentarioController = TextEditingController();
    int estrellasSeleccionadas = 5;

    return showDialog<Map<String, dynamic>>(
      context: context,
      builder: (ctx) {
        return StatefulBuilder(
          builder: (context, setDialogState) {
            return AlertDialog(
              title: const Text('Valorar Servicio'),
              content: SingleChildScrollView(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const Text('¿Qué tal el servicio?'),
                    const SizedBox(height: 16),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: List.generate(5, (index) {
                        return IconButton(
                          icon: Icon(
                            index < estrellasSeleccionadas
                                ? Icons.star
                                : Icons.star_border,
                            color: Colors.amber,
                            size: 32,
                          ),
                          onPressed: () {
                            setDialogState(() {
                              estrellasSeleccionadas = index + 1;
                            });
                          },
                        );
                      }),
                    ),
                    const SizedBox(height: 16),
                    TextField(
                      controller: comentarioController,
                      maxLines: 3,
                      keyboardType: TextInputType.multiline,
                      decoration: const InputDecoration(
                        labelText: 'Comentario',
                        hintText: 'Ej: Muy profesional y rápido.',
                        border: OutlineInputBorder(),
                      ),
                    ),
                  ],
                ),
              ),
              actions: [
                TextButton(
                  onPressed: () => Navigator.pop(ctx, null),
                  child: const Text('CANCELAR'),
                ),
                ElevatedButton(
                  onPressed: () {
                    Navigator.pop(ctx, {
                      'estrellas': estrellasSeleccionadas,
                      'comentario': comentarioController.text,
                    });
                  },
                  style: ElevatedButton.styleFrom(backgroundColor: Colors.blue),
                  child: const Text('ENVIAR',
                      style: TextStyle(color: Colors.white)),
                ),
              ],
            );
          },
        );
      },
    );
  }
}

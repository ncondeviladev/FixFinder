// Clase utilitaria que contiene componentes de diálogos superpuestos.
// Reúne los popup para confirmar borrado, ingresar horas y gastos al finalizar, o valorar servicio.
import 'dart:io';
import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import '../../theme/fixfinder_theme.dart';

class DialogosTrabajo {
  /// Muestra un cuadro de confirmación para eliminar una incidencia.
  /// Retorna true si el usuario confirma la eliminación.
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
            style: ElevatedButton.styleFrom(
                backgroundColor: Theme.of(context).colorScheme.error),
            child: Text('BORRAR',
                style: TextStyle(color: Theme.of(context).colorScheme.onPrimary)),
          ),
        ],
      ),
    );
    return confirmacion ?? false;
  }

  /// Despliega el formulario para que el operario informe sobre la finalización del trabajo.
  /// Permite capturar materiales, horas y fotos del resultado.
  static Future<Map<String, dynamic>?> mostrarDialogoFinalizar(
      BuildContext context) async {
    final materialController = TextEditingController();
    final horasController = TextEditingController();
    final List<XFile> fotosSeleccionadas = [];

    return showDialog<Map<String, dynamic>>(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (context, setDialogState) {
          return AlertDialog(
            title: const Text('Finalizar Trabajo'),
            content: SingleChildScrollView(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                      '¿Has completado esta incidencia? Añade material, horas y fotos del resultado:'),
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
                  const SizedBox(height: 16),
                  const Text('Fotos del trabajo finalizado:',
                      style: TextStyle(fontWeight: FontWeight.bold)),
                  const SizedBox(height: 8),
                  Wrap(
                    spacing: 8,
                    runSpacing: 8,
                    children: [
                      ...fotosSeleccionadas.map((foto) => Stack(
                            children: [
                              ClipRRect(
                                borderRadius: BorderRadius.circular(8),
                                child: Image.file(File(foto.path),
                                    width: 70, height: 70, fit: BoxFit.cover),
                              ),
                              Positioned(
                                right: -10,
                                top: -10,
                                child: IconButton(
                                  icon: Icon(Icons.cancel,
                                      color: Theme.of(context).colorScheme.error,
                                      size: 20),
                                  onPressed: () {
                                    setDialogState(() {
                                      fotosSeleccionadas.remove(foto);
                                    });
                                  },
                                ),
                              ),
                            ],
                          )),
                      GestureDetector(
                        onTap: () async {
                          final picker = ImagePicker();
                          final XFile? image = await picker.pickImage(
                            source: ImageSource.gallery,
                            imageQuality: 70,
                          );
                          if (image != null) {
                            setDialogState(() {
                              fotosSeleccionadas.add(image);
                            });
                          }
                        },
                        child: Container(
                          width: 70,
                          height: 70,
                          decoration: BoxDecoration(
                            color: Theme.of(context).dividerColor.withOpacity(0.1),
                            borderRadius: BorderRadius.circular(8),
                            border: Border.all(color: Theme.of(context).dividerColor),
                          ),
                          child: Icon(Icons.add_a_photo,
                              color: Theme.of(context).hintColor),
                        ),
                      ),
                    ],
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
                    'material': materialController.text,
                    'horas': horasController.text,
                    'fotos': fotosSeleccionadas,
                  });
                },
                style: ElevatedButton.styleFrom(
                    backgroundColor: FixFinderTheme.successColor),
                child: Text('FINALIZAR',
                    style: TextStyle(
                        color: Theme.of(context).colorScheme.onPrimary)),
              ),
            ],
          );
        },
      ),
    );
  }

  /// Abre el diálogo de valoración para que el cliente califique el servicio con estrellas y comentario.
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
                  style: ElevatedButton.styleFrom(
                      backgroundColor: Theme.of(context).colorScheme.tertiary),
                  child: Text('ENVIAR',
                      style: TextStyle(
                          color: Theme.of(context).colorScheme.onPrimary)),
                ),
              ],
            );
          },
        );
      },
    );
  }
}

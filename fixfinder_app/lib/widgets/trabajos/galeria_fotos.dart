// Componente visual para mostrar una tira horizontal de imágenes adjuntas.
// Permite ampliar cualquier foto seleccionada dentro de un modal emergente.
import 'package:flutter/material.dart';

/// Componente para visualizar una colección de imágenes asociadas a un trabajo.
/// Permite navegar por las fotos y verlas en detalle a pantalla completa.
class GaleriaFotos extends StatelessWidget {
  final List<String> urls;

  const GaleriaFotos({super.key, required this.urls});

  void _verImagenGrande(BuildContext context, String url) {
    showDialog(
      context: context,
      builder: (ctx) => Dialog(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Image.network(url, fit: BoxFit.contain),
            TextButton(
              onPressed: () => Navigator.pop(ctx),
              child: const Text('CERRAR'),
            ),
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    if (urls.isEmpty) return const SizedBox.shrink();

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text('Imágenes adjuntas:',
            style: TextStyle(fontWeight: FontWeight.bold)),
        const SizedBox(height: 8),
        SizedBox(
          height: 120,
          child: ListView.builder(
            scrollDirection: Axis.horizontal,
            itemCount: urls.length,
            itemBuilder: (context, index) {
              return Padding(
                padding: const EdgeInsets.only(right: 8.0),
                child: GestureDetector(
                  onTap: () => _verImagenGrande(context, urls[index]),
                  child: ClipRRect(
                    borderRadius: BorderRadius.circular(8),
                    child: Image.network(
                      urls[index],
                      width: 120,
                      height: 120,
                      fit: BoxFit.cover,
                      errorBuilder: (context, error, stackTrace) => Container(
                        width: 120,
                        color: Colors.grey.shade300,
                        child: const Icon(Icons.broken_image),
                      ),
                    ),
                  ),
                ),
              );
            },
          ),
        ),
      ],
    );
  }
}

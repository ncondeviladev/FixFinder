import 'package:flutter/material.dart';
import '../../theme/fixfinder_theme.dart';

/// Widget que descompone una descripción textual en bloques visuales coloreados.
/// Replica la lógica del Historial Visual del Dashboard JavaFX, separando
/// las intervenciones de Cliente, Gerente (Empresa) y Operario.
class BloquesDescripcion extends StatelessWidget {
  final String descripcion;

  const BloquesDescripcion({super.key, required this.descripcion});

  @override
  Widget build(BuildContext context) {
    if (descripcion.trim().isEmpty) return const SizedBox.shrink();

    final bloques = _parsearDescripcion(descripcion);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: bloques.map((bloque) => _buildBloque(bloque)).toList(),
    );
  }

  /// Estructura de datos temporal para representar un fragmento de la historia.
  List<_DatosBloque> _parsearDescripcion(String desc) {
    List<_DatosBloque> resultado = [];
    String descLimpia = desc.replaceAll('==============================', '');
    
    final marcadores = {
      '📝 CLIENTE:': FixFinderTheme.bgBlockCliente,
      '💰 GERENTE:': FixFinderTheme.bgBlockGerente,
      '🛠 OPERARIO:': FixFinderTheme.bgBlockOperario,
    };

    // Buscamos las posiciones de todos los marcadores presentes
    List<MapEntry<int, String>> posiciones = [];
    for (var marcador in marcadores.keys) {
      int index = descLimpia.indexOf(marcador);
      if (index != -1) {
        posiciones.add(MapEntry(index, marcador));
      }
    }

    // Ordenamos por posición de aparición
    posiciones.sort((a, b) => a.key.compareTo(b.key));

    if (posiciones.isEmpty) {
      // Fallback: Si no hay marcadores, mostrar todo en un bloque por defecto
      resultado.add(_DatosBloque(
        titulo: 'DESCRIPCIÓN:',
        contenido: descLimpia.trim(),
        colorFondo: FixFinderTheme.bgBlockDefault,
      ));
      return resultado;
    }

    for (int i = 0; i < posiciones.length; i++) {
      final inicio = posiciones[i].key;
      final marcador = posiciones[i].value;
      final fin = (i + 1 < posiciones.length) ? posiciones[i + 1].key : descLimpia.length;

      String contenido = descLimpia.substring(inicio + marcador.length, fin).trim();
      
      // Solo añadimos si tiene contenido real (ignora "(Sin información)")
      if (contenido.isNotEmpty && !contenido.contains('(Sin ')) {
        resultado.add(_DatosBloque(
          titulo: marcador,
          contenido: contenido,
          colorFondo: marcadores[marcador] ?? FixFinderTheme.bgBlockDefault,
        ));
      }
    }

    return resultado;
  }

  Widget _buildBloque(_DatosBloque bloque) {
    return Container(
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: bloque.colorFondo,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(
          color: bloque.colorFondo.withOpacity(0.5),
          width: 1,
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            bloque.titulo,
            style: const TextStyle(
              color: FixFinderTheme.primaryColor,
              fontWeight: FontWeight.bold,
              fontSize: 11,
              letterSpacing: 0.5,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            bloque.contenido,
            style: const TextStyle(
              color: Color(0xFFEEEEEE),
              fontSize: 14,
              height: 1.4,
            ),
          ),
        ],
      ),
    );
  }
}

class _DatosBloque {
  final String titulo;
  final String contenido;
  final Color colorFondo;

  _DatosBloque({
    required this.titulo,
    required this.contenido,
    required this.colorFondo,
  });
}

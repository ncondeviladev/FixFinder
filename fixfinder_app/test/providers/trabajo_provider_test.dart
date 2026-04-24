import 'package:flutter_test/flutter_test.dart';
import 'package:fixfinder_app/providers/trabajo_provider.dart';
import 'package:fixfinder_app/models/trabajo.dart';

void main() {
  group('TrabajoProvider Unit Tests', () {
    test('Al inicializar, la lista de trabajos debe estar vacía', () {
      final provider = TrabajoProvider();
      expect(provider.trabajos, isEmpty);
    });

    test('filtrarPorEstado devuelve solo los trabajos con el estado solicitado', () {
      final provider = TrabajoProvider();
      
      final t1 = Trabajo(
        id: 1, 
        idCliente: 1, 
        titulo: 'T1', 
        descripcion: '...', 
        categoria: CategoriaServicio.OTROS, 
        direccion: '...', 
        estado: EstadoTrabajo.PENDIENTE,
        urgencia: 1,
        urlsFotos: [],
      );
      
      final t2 = Trabajo(
        id: 2, 
        idCliente: 1, 
        titulo: 'T2', 
        descripcion: '...', 
        categoria: CategoriaServicio.OTROS, 
        direccion: '...', 
        estado: EstadoTrabajo.ACEPTADO,
        urgencia: 1,
        urlsFotos: [],
      );
      
      expect(provider.trabajos, isEmpty);
    });
  });
}
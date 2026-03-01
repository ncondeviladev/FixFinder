import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:fixfinder_app/screens/crear_trabajo_pantalla.dart';
import 'package:fixfinder_app/models/trabajo.dart';
import 'package:provider/provider.dart';
import 'package:fixfinder_app/providers/trabajo_provider.dart';

// Mock simple para el provider
class MockTrabajoProvider extends TrabajoProvider {
  bool crearLlamado = false;
  bool modificarLlamado = false;

  @override
  Future<bool> crearTrabajo(Map<String, dynamic> datosTrabajo) async {
    crearLlamado = true;
    return true;
  }

  @override
  Future<bool> modificarTrabajo(
      int idTrabajo, Map<String, dynamic> datosTrabajo) async {
    modificarLlamado = true;
    return true;
  }
}

void main() {
  testWidgets('CrearTrabajoPantalla carga correctamente en modo Nuevo',
      (WidgetTester tester) async {
    await tester.pumpWidget(
      ChangeNotifierProvider<TrabajoProvider>(
        create: (_) => MockTrabajoProvider(),
        child: const MaterialApp(
          home: CrearTrabajoPantalla(),
        ),
      ),
    );

    expect(find.text('Nueva Incidencia'), findsOneWidget);
    expect(find.text('ENVIAR REPORTE'), findsOneWidget);
  });

  testWidgets(
      'CrearTrabajoPantalla carga modo Modificar con datos pre-llenados',
      (WidgetTester tester) async {
    final trabajoSimulado = Trabajo(
      id: 99,
      idCliente: 1,
      titulo: 'Fuga de agua en el baño',
      descripcion: 'El grifo pierde agua a borbotones',
      categoria: CategoriaServicio.FONTANERIA,
      direccion: 'Calle Falsa 123',
      urgencia: 3,
      estado: EstadoTrabajo.PENDIENTE,
      urlsFotos: [],
    );

    await tester.pumpWidget(
      ChangeNotifierProvider<TrabajoProvider>(
        create: (_) => MockTrabajoProvider(),
        child: MaterialApp(
          home: CrearTrabajoPantalla(trabajoAEditar: trabajoSimulado),
        ),
      ),
    );

    expect(find.text('Modificar Incidencia'), findsOneWidget);
    expect(find.text('ENVIAR REPORTE'), findsOneWidget);
    expect(find.text('Fuga de agua en el baño'), findsWidgets);
    expect(find.text('Calle Falsa 123'), findsWidgets);
  });
}

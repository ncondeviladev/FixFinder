import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';
import 'package:fixfinder_app/screens/detalle_trabajo_pantalla.dart';
import 'package:fixfinder_app/models/trabajo.dart';
import 'package:fixfinder_app/models/usuario.dart';
import 'package:fixfinder_app/providers/trabajo_provider.dart';
import 'package:fixfinder_app/providers/usuario_provider.dart';

class MockTrabajoProvider extends TrabajoProvider {
  @override
  List<Trabajo> get trabajos => [];
}

class MockUsuarioProvider extends UsuarioProvider {
  final Usuario? mockUsuario;
  MockUsuarioProvider(this.mockUsuario);
  
  @override
  Usuario? get usuario => mockUsuario;
}

void main() {
  testWidgets('DetalleTrabajoPantalla muestra botón de valorar si el usuario es Cliente y el trabajo está FINALIZADO', (WidgetTester tester) async {
    final trabajoFinalizado = Trabajo(
      id: 1,
      titulo: 'Test Finalizado',
      descripcion: '...',
      categoria: CategoriaServicio.FONTANERIA,
      direccion: '...',
      estado: EstadoTrabajo.FINALIZADO,
      idCliente: 10,
      valoracion: 0,
      urgencia: 1,
      urlsFotos: [],
    );

    final cliente = Usuario(
      id: 10,
      nombreCompleto: 'Marta Cliente',
      email: 'marta@test.com',
      rol: Rol.CLIENTE,
    );

    await tester.pumpWidget(
      MultiProvider(
        providers: [
          ChangeNotifierProvider<TrabajoProvider>(create: (_) => MockTrabajoProvider()),
          ChangeNotifierProvider<UsuarioProvider>(create: (_) => MockUsuarioProvider(cliente)),
        ],
        child: MaterialApp(
          home: DetalleTrabajoPantalla(trabajo: trabajoFinalizado),
        ),
      ),
    );

    expect(find.text('VALORAR SERVICIO'), findsOneWidget);
  });

  testWidgets('DetalleTrabajoPantalla NO muestra botón de valorar si el trabajo ya tiene valoración', (WidgetTester tester) async {
    final trabajoValorado = Trabajo(
      id: 1,
      titulo: 'Test Valorado',
      descripcion: '...',
      categoria: CategoriaServicio.FONTANERIA,
      direccion: '...',
      estado: EstadoTrabajo.FINALIZADO,
      idCliente: 10,
      valoracion: 5,
      urgencia: 1,
      urlsFotos: [],
    );

    final cliente = Usuario(
      id: 10,
      nombreCompleto: 'Marta Cliente',
      email: 'marta@test.com',
      rol: Rol.CLIENTE,
    );

    await tester.pumpWidget(
      MultiProvider(
        providers: [
          ChangeNotifierProvider<TrabajoProvider>(create: (_) => MockTrabajoProvider()),
          ChangeNotifierProvider<UsuarioProvider>(create: (_) => MockUsuarioProvider(cliente)),
        ],
        child: MaterialApp(
          home: DetalleTrabajoPantalla(trabajo: trabajoValorado),
        ),
      ),
    );

    expect(find.text('VALORAR SERVICIO'), findsNothing);
  });
}

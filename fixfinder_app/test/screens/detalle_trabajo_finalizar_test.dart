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
  testWidgets('DetalleTrabajoPantalla muestra botón de finalizar si el usuario es Operario y el trabajo está ASIGNADO', (WidgetTester tester) async {
    final trabajoAsignado = Trabajo(
      id: 1,
      titulo: 'Test Asignado',
      descripcion: '...',
      categoria: CategoriaServicio.FONTANERIA,
      direccion: '...',
      estado: EstadoTrabajo.ASIGNADO,
      idCliente: 10,
      urgencia: 1,
      urlsFotos: [],
    );

    final operario = Usuario(
      id: 2,
      nombreCompleto: 'Paco Operario',
      email: 'paco@test.com',
      rol: Rol.OPERARIO,
    );

    await tester.pumpWidget(
      MultiProvider(
        providers: [
          ChangeNotifierProvider<TrabajoProvider>(create: (_) => MockTrabajoProvider()),
          ChangeNotifierProvider<UsuarioProvider>(create: (_) => MockUsuarioProvider(operario)),
        ],
        child: MaterialApp(
          home: DetalleTrabajoPantalla(trabajo: trabajoAsignado),
        ),
      ),
    );

    expect(find.text('MARCAR COMO FINALIZADO'), findsOneWidget);
  });

  testWidgets('DetalleTrabajoPantalla NO muestra botón de finalizar si el trabajo ya está FINALIZADO', (WidgetTester tester) async {
    final trabajoFinalizado = Trabajo(
      id: 1,
      titulo: 'Test Finalizado',
      descripcion: '...',
      categoria: CategoriaServicio.FONTANERIA,
      direccion: '...',
      estado: EstadoTrabajo.FINALIZADO,
      idCliente: 10,
      urgencia: 1,
      urlsFotos: [],
    );

    final operario = Usuario(
      id: 2,
      nombreCompleto: 'Paco Operario',
      email: 'paco@test.com',
      rol: Rol.OPERARIO,
    );

    await tester.pumpWidget(
      MultiProvider(
        providers: [
          ChangeNotifierProvider<TrabajoProvider>(create: (_) => MockTrabajoProvider()),
          ChangeNotifierProvider<UsuarioProvider>(create: (_) => MockUsuarioProvider(operario)),
        ],
        child: MaterialApp(
          home: DetalleTrabajoPantalla(trabajo: trabajoFinalizado),
        ),
      ),
    );

    expect(find.text('MARCAR COMO FINALIZADO'), findsNothing);
  });
}

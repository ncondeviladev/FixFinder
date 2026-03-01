import 'package:flutter_test/flutter_test.dart';
import 'package:flutter/material.dart';
import 'package:fixfinder_app/widgets/trabajos/tarjeta_contacto.dart';
import 'package:fixfinder_app/models/usuario.dart';

void main() {
  testWidgets('TarjetaContacto muestra la info del usuario',
      (WidgetTester tester) async {
    final usuario = Usuario(
      id: 1,
      nombreCompleto: 'Juan Antonio',
      email: 'juan@test.com',
      rol: Rol.CLIENTE,
      telefono: '600123456',
    );

    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: TarjetaContacto(
            titulo: 'Cliente Info',
            usuario: usuario,
          ),
        ),
      ),
    );

    expect(find.text('Cliente Info'), findsOneWidget);
    expect(find.text('Juan Antonio'), findsOneWidget);
    expect(find.text('Tel: 600123456'), findsOneWidget);
    expect(find.text('Email: juan@test.com'), findsOneWidget);
  });
}

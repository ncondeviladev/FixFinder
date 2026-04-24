import 'package:flutter_test/flutter_test.dart';
import 'package:flutter/material.dart';
import 'package:fixfinder_app/widgets/comunes/estado_badge.dart';
import 'package:fixfinder_app/models/trabajo.dart';

void main() {
  testWidgets(
      'EstadoBadge muestra correctamente el estado y el color (PENDIENTE = naranja)',
      (WidgetTester tester) async {
    await tester.pumpWidget(
      const MaterialApp(
        home: Scaffold(
          body: EstadoBadge(
            estado: EstadoTrabajo.PENDIENTE,
          ),
        ),
      ),
    );

    expect(find.text('PENDIENTE'), findsOneWidget);

    final Text textWidget = tester.widget(find.text('PENDIENTE'));
    expect(textWidget.style?.color, const Color(0xFFF59E0B));
  });
}

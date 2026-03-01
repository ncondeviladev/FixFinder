import 'package:flutter_test/flutter_test.dart';
import 'package:flutter/material.dart';
import 'package:fixfinder_app/widgets/comunes/dato_fila.dart';

void main() {
  testWidgets('DatoFila muestra correctamente la etiqueta y el valor',
      (WidgetTester tester) async {
    await tester.pumpWidget(
      const MaterialApp(
        home: Scaffold(
          body: DatoFila(
            etiqueta: 'Estado',
            valor: 'PENDIENTE',
            color: Colors.orange,
          ),
        ),
      ),
    );

    expect(find.text('Estado: '), findsOneWidget);
    expect(find.text('PENDIENTE'), findsOneWidget);

    final Text valorText = tester.widget(find.text('PENDIENTE'));
    expect(valorText.style?.color, Colors.orange);
    expect(valorText.style?.fontWeight, FontWeight.bold);
  });
}

import 'package:flutter_test/flutter_test.dart';
import 'package:flutter/material.dart';
import 'package:fixfinder_app/widgets/trabajos/galeria_fotos.dart';

void main() {
  testWidgets('GaleriaFotos se muestra correctamente si hay urls',
      (WidgetTester tester) async {
    final urls = ['https://test.com/foto1.jpg', 'https://test.com/foto2.jpg'];

    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: GaleriaFotos(urls: urls),
        ),
      ),
    );

    expect(find.text('Imágenes adjuntas:'), findsOneWidget);
    // Hay que hacer un pumpAndSettle porque usa Image.network o tener cuidado
    // con el error de red en los test, pero podemos buscar por tipo
    expect(find.byType(Image), findsNWidgets(2));
  });

  testWidgets('GaleriaFotos no muestra nada si la lista es vacia',
      (WidgetTester tester) async {
    await tester.pumpWidget(
      const MaterialApp(
        home: Scaffold(
          body: GaleriaFotos(urls: []),
        ),
      ),
    );

    expect(find.text('Imágenes adjuntas:'), findsNothing);
  });
}

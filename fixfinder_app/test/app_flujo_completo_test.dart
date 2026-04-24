import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';
import 'package:fixfinder_app/main.dart';
import 'package:fixfinder_app/screens/login_pantalla.dart';
import 'package:fixfinder_app/screens/dashboard_pantalla.dart';
import 'package:fixfinder_app/providers/trabajo_provider.dart';
import 'package:fixfinder_app/providers/usuario_provider.dart';

import 'package:shared_preferences/shared_preferences.dart';

/// Test de Integración a nivel de Widget de la aplicación FixFinder.
/// Este test verifica el arranque de la app y la llegada a la pantalla de Login.
void main() {
  setUp(() async {
    // Inicializamos SharedPreferences con valores vacíos para evitar errores
    SharedPreferences.setMockInitialValues({});
  });

  testWidgets('Flujo completo: Verificación de pantalla de Login',
      (WidgetTester tester) async {
    // Ajustamos el tamaño de la pantalla para evitar problemas de overflow en tests
    tester.view.physicalSize = const Size(1080, 1920);
    tester.view.devicePixelRatio = 1.0;

    // 1. Cargamos la aplicación con sus Providers (necesario para que no falle el árbol de widgets)
    await tester.pumpWidget(
      MultiProvider(
        providers: [
          ChangeNotifierProvider(create: (_) => TrabajoProvider()),
          ChangeNotifierProvider(create: (_) => UsuarioProvider()),
        ],
        child: const FixFinderApp(),
      ),
    );

    // Esperamos a que el AuthWrapper decida qué pantalla mostrar (por defecto Login si no hay usuario)
    await tester.pumpAndSettle();

    // 2. Verificamos que estamos en la pantalla de Login
    expect(find.byType(LoginPantalla), findsOneWidget);

    // 3. Verificamos elementos clave del diseño (Logo, Campos, Botón)
    expect(find.byType(TextField), findsAtLeastNWidgets(2)); // Email y Password

    // Buscamos el botón de entrar por su texto (asumiendo que es "ENTRAR" o "Login")
    final loginButton = find.byType(ElevatedButton);
    expect(loginButton, findsAtLeastNWidgets(1));
  });
}

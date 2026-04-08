import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'screens/login_pantalla.dart';
import 'screens/dashboard_pantalla.dart';
import 'screens/perfil_pantalla.dart';
import 'screens/splash_pantalla.dart';
import 'providers/trabajo_provider.dart';

import 'package:firebase_core/firebase_core.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'firebase_options.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  debugPrint("🚀 Iniciando FixFinder App...");

  // Cargar variables de entorno securizadas
  try {
    debugPrint("📂 Cargando .env...");
    await dotenv.load(fileName: ".env");
    debugPrint("✅ .env cargado.");
  } catch (e) {
    debugPrint("⚠️ Advertencia: No se pudo cargar el archivo .env: $e");
  }

  try {
    debugPrint("🔥 Inicializando Firebase...");
    await Firebase.initializeApp(
      options: DefaultFirebaseOptions.currentPlatform,
    );
    debugPrint("✅ Firebase listo.");
  } catch (e) {
    debugPrint("❌ Error Firebase (ignorado): $e");
  }

  debugPrint("🏗️ Arrancando aplicación...");
  runApp(
    MultiProvider(
      providers: [
        ChangeNotifierProvider(create: (_) => TrabajoProvider()),
      ],
      child: const FixFinderApp(),
    ),
  );
}

class FixFinderApp extends StatelessWidget {
  const FixFinderApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'FixFinder',
      debugShowCheckedModeBanner: false,
      themeMode: ThemeMode.dark,
      theme: ThemeData(
        useMaterial3: true,
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xFFFF6D00),
          brightness: Brightness.light,
        ),
      ),
      darkTheme: ThemeData(
        useMaterial3: true,
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xFFFF6D00), // naranja técnico
          brightness: Brightness.dark,
        ).copyWith(
          surface: const Color(0xFF1A1A1A),
          onSurface: const Color(0xFFEEEEEE),
          surfaceContainerHighest: const Color(0xFF242424),
          primary: const Color(0xFFFF6D00),
          onPrimary: Colors.white,
          secondary: const Color(0xFFFFAB40),
          onSecondary: Colors.black,
          tertiary: const Color(0xFF90CAF9),
          error: const Color(0xFFEF5350),
        ),
        scaffoldBackgroundColor: const Color(0xFF121212),
        cardColor: const Color(0xFF1E1E1E),
        dividerColor: const Color(0xFF2A2A2A),
        appBarTheme: const AppBarTheme(
          backgroundColor: Color(0xFF1A1A1A),
          foregroundColor: Color(0xFFFF6D00),
          elevation: 0,
          centerTitle: true,
          titleTextStyle: TextStyle(
            color: Color(0xFFFF6D00),
            fontSize: 20,
            fontWeight: FontWeight.bold,
          ),
        ),
        cardTheme: CardThemeData(
          color: const Color(0xFF1E1E1E),
          elevation: 2,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12),
            side: const BorderSide(color: Color(0xFF2A2A2A)),
          ),
        ),
        elevatedButtonTheme: ElevatedButtonThemeData(
          style: ElevatedButton.styleFrom(
            backgroundColor: const Color(0xFFFF6D00),
            foregroundColor: Colors.white,
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(10),
            ),
          ),
        ),
        inputDecorationTheme: InputDecorationTheme(
          filled: true,
          fillColor: const Color(0xFF242424),
          border: OutlineInputBorder(
            borderRadius: BorderRadius.circular(10),
            borderSide: const BorderSide(color: Color(0xFF3A3A3A)),
          ),
          enabledBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(10),
            borderSide: const BorderSide(color: Color(0xFF3A3A3A)),
          ),
          focusedBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(10),
            borderSide: const BorderSide(color: Color(0xFFFF6D00), width: 2),
          ),
          labelStyle: const TextStyle(color: Color(0xFFAAAAAA)),
          prefixIconColor: const Color(0xFFFF6D00),
        ),
        textTheme: const TextTheme(
          bodyLarge: TextStyle(color: Color(0xFFEEEEEE)),
          bodyMedium: TextStyle(color: Color(0xFFCCCCCC)),
          bodySmall: TextStyle(color: Color(0xFF999999)),
          titleLarge:
              TextStyle(color: Color(0xFFFFFFFF), fontWeight: FontWeight.bold),
          titleMedium: TextStyle(color: Color(0xFFEEEEEE)),
        ),
        iconTheme: const IconThemeData(color: Color(0xFFFF6D00)),
        listTileTheme: const ListTileThemeData(
          textColor: Color(0xFFEEEEEE),
          iconColor: Color(0xFFFF6D00),
        ),
        chipTheme: ChipThemeData(
          backgroundColor: const Color(0xFF242424),
          selectedColor: const Color(0xFFFF6D00),
          labelStyle: const TextStyle(color: Color(0xFFEEEEEE)),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
        ),
        floatingActionButtonTheme: const FloatingActionButtonThemeData(
          backgroundColor: Color(0xFFFF6D00),
          foregroundColor: Colors.white,
        ),
      ),
      initialRoute: '/splash',
      routes: {
        '/splash': (context) => const SplashPantalla(),
        '/login': (context) => const LoginPantalla(),
        '/dashboard': (context) => const DashboardPantalla(),
        '/perfil': (context) => const PerfilPantalla(),
      },
    );
  }
}

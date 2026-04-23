import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'screens/login_pantalla.dart';
import 'screens/dashboard_pantalla.dart';
import 'screens/perfil_pantalla.dart';
import 'screens/auth_wrapper.dart';
import 'package:flutter_native_splash/flutter_native_splash.dart';
import 'providers/trabajo_provider.dart';
import 'providers/usuario_provider.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'firebase_options.dart';
import 'theme/fixfinder_theme.dart';

final GlobalKey<ScaffoldMessengerState> messengerKey = GlobalKey<ScaffoldMessengerState>();

/// Punto de entrada principal de la aplicación FixFinder.
void main() async {
  WidgetsBinding widgetsBinding = WidgetsFlutterBinding.ensureInitialized();
  FlutterNativeSplash.preserve(widgetsBinding: widgetsBinding);
  debugPrint("🚀 Iniciando FixFinder App...");

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
        ChangeNotifierProvider(create: (_) => UsuarioProvider()),
      ],
      child: const FixFinderApp(),
    ),
  );
}

/// Clase principal de la aplicación que define la estructura global y el tema visual.
class FixFinderApp extends StatelessWidget {
  const FixFinderApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'FixFinder',
      scaffoldMessengerKey: messengerKey,
      debugShowCheckedModeBanner: false,
      themeMode: ThemeMode.dark,
      theme: ThemeData(
        useMaterial3: true,
        colorScheme: ColorScheme.fromSeed(
          seedColor: FixFinderTheme.primaryColor,
          brightness: Brightness.light,
        ),
      ),
      darkTheme: FixFinderTheme.darkTheme,
      home: const AuthWrapper(),
      routes: {
        '/login': (context) => const LoginPantalla(),
        '/dashboard': (context) => const DashboardPantalla(),
        '/perfil': (context) => const PerfilPantalla(),
      },
    );
  }
}


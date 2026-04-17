import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:flutter_native_splash/flutter_native_splash.dart';
import '../services/auth_service.dart';
import '../providers/usuario_provider.dart';

/// Componente que se encarga de retener la pantalla de Splash nativa
/// mientras decide silenciosamente a qué pantalla redirigir según el estado de la sesión.
class AuthWrapper extends StatefulWidget {
  const AuthWrapper({super.key});

  @override
  State<AuthWrapper> createState() => _AuthWrapperState();
}

class _AuthWrapperState extends State<AuthWrapper> {
  @override
  void initState() {
    super.initState();
    _iniciarApp();
  }

  Future<void> _iniciarApp() async {
    // Verificamos si hay un token válido guardado
    final logueado = await AuthService().tryAutoLogin();

    if (!mounted) return;

    if (logueado) {
      // Sincronizamos el estado global del usuario
      context.read<UsuarioProvider>().inicializarDesdeAuth();
      Navigator.pushReplacementNamed(context, '/dashboard');
    } else {
      // Sin sesión válida, mandamos al login
      Navigator.pushReplacementNamed(context, '/login');
    }

    // Una vez que ya hemos ordenado la redirección,
    // quitamos el velo del Splash Screen Nativo
    FlutterNativeSplash.remove();
  }

  @override
  Widget build(BuildContext context) {
    // Este Scaffold gris puro nunca se llegará a ver porque
    // el Splash Nativo estará por encima tapándolo hasta que hagamos remove().
    return const Scaffold(
      backgroundColor: Color(0xFF121212),
    );
  }
}

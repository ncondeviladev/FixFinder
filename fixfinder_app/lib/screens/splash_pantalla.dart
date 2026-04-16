import 'package:flutter/material.dart';
import '../services/auth_service.dart';

/// Pantalla de bienvenida (Splash) que se muestra al arrancar la App.
/// 
/// Realiza dos tareas críticas:
/// 1. Muestra la identidad visual de FixFinder con una animación de entrada.
/// 2. Gestiona el "Auto-Login" recuperando la sesión guardada para decidir
///    si redirigir al Dashboard o a la pantalla de Login.
class SplashPantalla extends StatefulWidget {
  const SplashPantalla({super.key});

  @override
  State<SplashPantalla> createState() => _SplashPantallaState();
}

class _SplashPantallaState extends State<SplashPantalla>
    with SingleTickerProviderStateMixin {
  late AnimationController _controller;
  late Animation<double> _fadeAnimation;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      vsync: this,
      duration: const Duration(seconds: 2),
    );
    _fadeAnimation = Tween<double>(begin: 0.0, end: 1.0).animate(_controller);

    _controller.forward();

    _iniciarApp();
  }

  /// Lógica de inicialización de la aplicación.
  /// Espera a que la animación termine y verifica si hay una sesión activa.
  Future<void> _iniciarApp() async {
    // Retardo intencionado para dar presencia a la marca y cargar recursos
    await Future.delayed(const Duration(seconds: 3));

    if (!mounted) return;

    // Comprobamos si el usuario ya estaba logueado previamente (token en SharedPreferences)
    final logueado = await AuthService().tryAutoLogin();

    if (!mounted) return;

    if (logueado) {
      // Si hay sesión, saltamos directamente al panel principal
      Navigator.pushReplacementNamed(context, '/dashboard');
    } else {
      // De lo contrario, toca identificarse
      Navigator.pushReplacementNamed(context, '/login');
    }
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF121212), // Fondo oscuro corporativo
      body: Center(
        child: FadeTransition(
          opacity: _fadeAnimation,
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              ClipOval(
                child: Image.asset(
                  'assets/images/logo.png',
                  width: 150,
                  height: 150,
                ),
              ),
              const SizedBox(height: 24),
              const Text(
                'FIXFINDER',
                style: TextStyle(
                  color: Color(0xFFFF6D00), // Naranja característico
                  fontSize: 32,
                  fontWeight: FontWeight.bold,
                  letterSpacing: 4,
                ),
              ),
              const SizedBox(height: 10),
              const Text(
                'Servicios Técnicos Profesionales',
                style: TextStyle(
                  color: Colors.white70,
                  fontSize: 14,
                  letterSpacing: 1.2,
                ),
              ),
              const SizedBox(height: 48),
              const CircularProgressIndicator(
                valueColor: AlwaysStoppedAnimation<Color>(Color(0xFFFF6D00)),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

import 'package:flutter/material.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import '../services/auth_service.dart';
import '../services/socket_service.dart';
import 'registro_pantalla.dart';
import 'dart:async';

/// Pantalla de inicio de sesión de la aplicación móvil.
/// 
/// Recibe las credenciales del usuario y gestiona el flujo de acceso
/// interactuando con el [AuthService].
class LoginPantalla extends StatefulWidget {
  const LoginPantalla({super.key});

  @override
  State<LoginPantalla> createState() => _LoginPantallaState();
}

class _LoginPantallaState extends State<LoginPantalla> {
  final _emailController = TextEditingController();
  final _passController = TextEditingController();
  final _emailFocus = FocusNode();
  final _passFocus = FocusNode();
  bool _cargando = false;
  String? _error;
  bool? _isConectado;
  Timer? _pingTimer;

  @override
  void initState() {
    super.initState();
    _checkStatus();
    _pingTimer = Timer.periodic(const Duration(seconds: 10), (timer) {
      _checkStatus();
    });
  }

  /// Verifica el estado de conexión con el servidor Socket.
  Future<void> _checkStatus() async {
    final status = await SocketService().ping();
    if (mounted) {
      setState(() {
        _isConectado = status;
      });
    }
  }

  @override
  void dispose() {
    _pingTimer?.cancel();
    _emailController.dispose();
    _passController.dispose();
    _emailFocus.dispose();
    _passFocus.dispose();
    super.dispose();
  }

  /// Evalúa el resultado del intento de autenticación.
  /// 
  /// Si la validación es exitosa, redirige al Dashboard y establece la
  /// persistencia de la sesión. En caso contrario, muestra el error capturado.
  Future<void> _login() async {
    setState(() {
      _cargando = true;
      _error = null;
    });

    final exito = await AuthService().login(
      _emailController.text.trim(),
      _passController.text,
    );

    if (mounted) {
      setState(() => _cargando = false);
      if (exito) {
        Navigator.pushReplacementNamed(context, '/dashboard');
      } else {
        setState(() => _error = 'Credenciales inválidas o error de conexión');
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      resizeToAvoidBottomInset: true,
      appBar: AppBar(
        leading: Navigator.canPop(context)
            ? null
            : Padding(
                padding: const EdgeInsets.all(8.0),
                child: ClipOval(child: Image.asset('assets/images/logo.png')),
              ),
        title: const Text('Iniciar Sesión'),
        actions: [
          Padding(
            padding: const EdgeInsets.only(right: 16.0),
            child: _ConnectionStatusDot(isConectado: _isConectado),
          ),
        ],
      ),
      body: SingleChildScrollView(
        child: Padding(
          padding: const EdgeInsets.all(24.0),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const SizedBox(height: 40),
              ClipOval(
                child: Image.asset('assets/images/logo.png',
                    width: 100, height: 100),
              ),
              const SizedBox(height: 32),
              TextField(
                controller: _emailController,
                focusNode: _emailFocus,
                keyboardType: TextInputType.emailAddress,
                textInputAction: TextInputAction.next,
                onSubmitted: (_) =>
                    FocusScope.of(context).requestFocus(_passFocus),
                decoration: const InputDecoration(
                  labelText: 'Correo Electrónico',
                  border: OutlineInputBorder(),
                  prefixIcon: Icon(Icons.email),
                ),
              ),
              const SizedBox(height: 16),
              TextField(
                controller: _passController,
                focusNode: _passFocus,
                obscureText: true,
                keyboardType: TextInputType.visiblePassword,
                textInputAction: TextInputAction.done,
                onSubmitted: (_) => _cargando ? null : _login(),
                decoration: const InputDecoration(
                  labelText: 'Contraseña',
                  border: OutlineInputBorder(),
                  prefixIcon: Icon(Icons.lock),
                ),
              ),
              if (_error != null)
                Padding(
                  padding: const EdgeInsets.only(top: 16.0),
                  child:
                      Text(_error!, style: const TextStyle(color: Colors.red)),
                ),
              const SizedBox(height: 32),
              SizedBox(
                width: double.infinity,
                height: 50,
                child: ElevatedButton(
                  onPressed: _cargando ? null : _login,
                  child: _cargando
                      ? const CircularProgressIndicator()
                      : const Text('ACCEDER'),
                ),
              ),
              const SizedBox(height: 24),
              TextButton(
                onPressed: () {
                  Navigator.push(
                    context,
                    MaterialPageRoute(builder: (context) => const RegistroPantalla()),
                  );
                },
                child: const Text('¿No tienes cuenta? Regístrate aquí'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _ConnectionStatusDot extends StatelessWidget {
  final bool? isConectado;

  const _ConnectionStatusDot({this.isConectado});

  @override
  Widget build(BuildContext context) {
    Color color;
    if (isConectado == null || !isConectado!) {
      color = Colors.grey;
    } else {
      final isNube = dotenv.get('ENVIRONMENT', fallback: 'LOCAL') == 'NUBE';
      color = isNube ? Colors.green : Colors.blue;
    }

    return Container(
      width: 10,
      height: 10,
      decoration: BoxDecoration(
        color: color,
        shape: BoxShape.circle,
        boxShadow: [
          BoxShadow(
            color: color.withOpacity(0.5),
            blurRadius: 4,
            spreadRadius: 1,
          ),
        ],
      ),
    );
  }
}

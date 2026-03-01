// Pantalla de inicio de sesión de la aplicación móvil.
// Recibe las credenciales y las valida mediante el AuthService.
import 'package:flutter/material.dart';
import '../services/auth_service.dart';

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

  @override
  void dispose() {
    _emailController.dispose();
    _passController.dispose();
    _emailFocus.dispose();
    _passFocus.dispose();
    super.dispose();
  }

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
      appBar: AppBar(title: const Text('Iniciar Sesión')),
      body: SingleChildScrollView(
        child: Padding(
          padding: const EdgeInsets.all(24.0),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const SizedBox(height: 40),
              const Icon(Icons.build_circle, size: 80, color: Colors.blue),
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
            ],
          ),
        ),
      ),
    );
  }
}

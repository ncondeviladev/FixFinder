
import 'dart:io';
import 'package:flutter/material.dart';
import '../services/auth_service.dart';
import '../services/image_service.dart';

/// Pantalla de registro para nuevos clientes en FixFinder.
class RegistroPantalla extends StatefulWidget {
  const RegistroPantalla({super.key});

  @override
  State<RegistroPantalla> createState() => _RegistroPantallaState();
}

class _RegistroPantallaState extends State<RegistroPantalla> {
  final _formKey = GlobalKey<FormState>();
  
  final _nombreController = TextEditingController();
  final _dniController = TextEditingController();
  final _emailController = TextEditingController();
  final _telefonoController = TextEditingController();
  final _direccionController = TextEditingController();
  final _passController = TextEditingController();
  
  bool _cargando = false;
  String? _error;
  File? _fotoSeleccionada;

  @override
  void dispose() {
    _nombreController.dispose();
    _dniController.dispose();
    _emailController.dispose();
    _telefonoController.dispose();
    _direccionController.dispose();
    _passController.dispose();
    super.dispose();
  }

  /// Abre el selector de archivos para que el usuario escoja su foto de perfil.
  Future<void> _seleccionarImagen() async {
    final file = await ImageService().elegirImagen();
    if (file != null) {
      setState(() => _fotoSeleccionada = file);
    }
  }

  /// Ejecuta el proceso de registro enviando los datos y la imagen al servidor.
  Future<void> _registrar() async {
    if (!_formKey.currentState!.validate()) return;

    setState(() {
      _cargando = true;
      _error = null;
    });

    String? urlFoto;
    if (_fotoSeleccionada != null) {
      final String nombreUnico = 'dni_${_dniController.text}_${DateTime.now().millisecondsSinceEpoch}';
      urlFoto = await ImageService().subirImagen(_fotoSeleccionada!, 'perfiles', nombreUnico);
    }

    final resultado = await AuthService().registrar(
      nombre: _nombreController.text.trim(),
      dni: _dniController.text.trim(),
      email: _emailController.text.trim(),
      telefono: _telefonoController.text.trim(),
      direccion: _direccionController.text.trim(),
      password: _passController.text,
      urlFoto: urlFoto, // Añadir este parámetro
    );

    if (mounted) {
      setState(() => _cargando = false);
      
      if (resultado['status'] == 200 || resultado['status'] == 201) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('¡Registro completado! Ya puedes iniciar sesión.'),
            backgroundColor: Colors.green,
          ),
        );
        Navigator.pop(context); // Volver al login
      } else {
        setState(() => _error = resultado['mensaje'] ?? 'Error al registrarse');
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Crear Cuenta')),
      body: SingleChildScrollView(
        child: Padding(
          padding: const EdgeInsets.all(24.0),
          child: Form(
            key: _formKey,
            child: Column(
              children: [
                GestureDetector(
                  onTap: _seleccionarImagen,
                  child: Stack(
                    alignment: Alignment.bottomRight,
                    children: [
                      CircleAvatar(
                        radius: 50,
                        backgroundColor: Colors.grey[800],
                        backgroundImage: _fotoSeleccionada != null ? FileImage(_fotoSeleccionada!) : null,
                        child: _fotoSeleccionada == null 
                          ? const Icon(Icons.person, size: 50, color: Colors.grey)
                          : null,
                      ),
                      const CircleAvatar(
                        radius: 18,
                        backgroundColor: Colors.orange,
                        child: Icon(Icons.camera_alt, size: 18, color: Colors.white),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 12),
                const Text('Foto de perfil', style: TextStyle(color: Colors.grey)),
                const SizedBox(height: 32),
                
                TextFormField(
                  controller: _nombreController,
                  decoration: const InputDecoration(
                    labelText: 'Nombre Completo',
                    prefixIcon: Icon(Icons.person),
                    border: OutlineInputBorder(),
                  ),
                  validator: (v) => v!.isEmpty ? 'Campo obligatorio' : null,
                ),
                const SizedBox(height: 16),
                
                TextFormField(
                  controller: _dniController,
                  decoration: const InputDecoration(
                    labelText: 'DNI / NIE',
                    prefixIcon: Icon(Icons.badge),
                    border: OutlineInputBorder(),
                  ),
                  validator: (v) => v!.isEmpty ? 'Campo obligatorio' : null,
                ),
                const SizedBox(height: 16),
                
                TextFormField(
                  controller: _emailController,
                  keyboardType: TextInputType.emailAddress,
                  decoration: const InputDecoration(
                    labelText: 'Correo Electrónico',
                    prefixIcon: Icon(Icons.email),
                    border: OutlineInputBorder(),
                  ),
                  validator: (v) => v!.contains('@') ? null : 'Email inválido',
                ),
                const SizedBox(height: 16),
                
                TextFormField(
                  controller: _telefonoController,
                  keyboardType: TextInputType.phone,
                  decoration: const InputDecoration(
                    labelText: 'Teléfono',
                    prefixIcon: Icon(Icons.phone),
                    border: OutlineInputBorder(),
                  ),
                  validator: (v) => v!.length >= 9 ? null : 'Teléfono inválido',
                ),
                const SizedBox(height: 16),
                
                TextFormField(
                  controller: _direccionController,
                  decoration: const InputDecoration(
                    labelText: 'Dirección (Opcional)',
                    prefixIcon: Icon(Icons.location_on),
                    border: OutlineInputBorder(),
                  ),
                ),
                const SizedBox(height: 16),
                
                TextFormField(
                  controller: _passController,
                  obscureText: true,
                  decoration: const InputDecoration(
                    labelText: 'Contraseña',
                    prefixIcon: Icon(Icons.lock),
                    border: OutlineInputBorder(),
                  ),
                  validator: (v) => v!.length >= 4 ? null : 'Mínimo 4 caracteres',
                ),
                
                if (_error != null)
                  Padding(
                    padding: const EdgeInsets.only(top: 16),
                    child: Text(_error!, style: const TextStyle(color: Colors.red)),
                  ),
                  
                const SizedBox(height: 32),
                
                SizedBox(
                  width: double.infinity,
                  height: 50,
                  child: ElevatedButton(
                    onPressed: _cargando ? null : _registrar,
                    child: _cargando 
                      ? const CircularProgressIndicator(color: Colors.white)
                      : const Text('REGISTRARSE'),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

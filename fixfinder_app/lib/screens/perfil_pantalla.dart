import 'dart:io';
import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'package:firebase_storage/firebase_storage.dart';
import '../models/usuario.dart';
import '../services/auth_service.dart';

class PerfilPantalla extends StatefulWidget {
  const PerfilPantalla({super.key});

  @override
  State<PerfilPantalla> createState() => _PerfilPantallaState();
}

class _PerfilPantallaState extends State<PerfilPantalla> {
  bool _subiendoFoto = false;

  Color _colorRol(Rol rol) {
    switch (rol) {
      case Rol.CLIENTE:
        return Colors.blue;
      case Rol.OPERARIO:
        return Colors.green;
      case Rol.GERENTE:
        return Colors.orange;
      case Rol.ADMIN:
        return Colors.purple;
    }
  }

  Future<void> _cambiarFotoPerfil() async {
    final usuario = AuthService().usuarioActual;
    if (usuario == null) return;
    if (usuario.rol != Rol.CLIENTE) return;

    final picker = ImagePicker();
    final XFile? image = await picker.pickImage(
      source: ImageSource.gallery,
      imageQuality: 70,
    );

    if (image == null) return;

    setState(() => _subiendoFoto = true);

    try {
      final fileName =
          'perfiles/${usuario.id}_${DateTime.now().millisecondsSinceEpoch}_${image.name}';
      final storageRef = FirebaseStorage.instance.ref().child(fileName);

      await storageRef.putFile(File(image.path));
      final downloadUrl = await storageRef.getDownloadURL();

      final exito = await AuthService().actualizarFotoPerfil(downloadUrl);

      if (mounted) {
        setState(() => _subiendoFoto = false);
        if (exito) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(
                content: Text('Foto de perfil actualizada'),
                backgroundColor: Colors.green),
          );
        } else {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(
                content: Text('Error al actualizar en el servidor'),
                backgroundColor: Colors.red),
          );
        }
      }
    } catch (e) {
      if (mounted) {
        setState(() => _subiendoFoto = false);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
              content: Text('Error al subir foto: $e'),
              backgroundColor: Colors.red),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final usuario = AuthService().usuarioActual;

    if (usuario == null) {
      return Scaffold(
        appBar: AppBar(title: const Text('Mi Perfil')),
        body: const Center(child: Text('No hay sesión activa')),
      );
    }

    return Scaffold(
      appBar: AppBar(title: const Text('Mi Perfil')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            Center(
              child: Stack(
                alignment: Alignment.bottomRight,
                children: [
                  CircleAvatar(
                    radius: 55,
                    backgroundColor:
                        _colorRol(usuario.rol).withValues(alpha: 0.15),
                    backgroundImage: usuario.urlFoto != null
                        ? NetworkImage(usuario.urlFoto!)
                        : null,
                    child: _subiendoFoto
                        ? const CircularProgressIndicator()
                        : (usuario.urlFoto == null
                            ? Icon(Icons.person,
                                size: 55, color: _colorRol(usuario.rol))
                            : null),
                  ),
                  if (usuario.rol == Rol.CLIENTE)
                    Positioned(
                      bottom: 0,
                      right: 0,
                      child: GestureDetector(
                        onTap: _subiendoFoto ? null : _cambiarFotoPerfil,
                        child: Container(
                          padding: const EdgeInsets.all(8),
                          decoration: const BoxDecoration(
                            color: Colors.blue,
                            shape: BoxShape.circle,
                          ),
                          child: const Icon(Icons.camera_alt,
                              color: Colors.white, size: 20),
                        ),
                      ),
                    ),
                ],
              ),
            ),
            const SizedBox(height: 16),

            Text(
              usuario.nombreCompleto.isNotEmpty
                  ? usuario.nombreCompleto
                  : 'Sin nombre',
              style: const TextStyle(fontSize: 22, fontWeight: FontWeight.bold),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 8),

            // Badge de Rol
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
              decoration: BoxDecoration(
                color: _colorRol(usuario.rol).withValues(alpha: 0.15),
                borderRadius: BorderRadius.circular(20),
                border: Border.all(color: _colorRol(usuario.rol)),
              ),
              child: Text(
                usuario.rol.name,
                style: TextStyle(
                  color: _colorRol(usuario.rol),
                  fontWeight: FontWeight.bold,
                ),
              ),
            ),
            const SizedBox(height: 24),
            const Divider(),
            const SizedBox(height: 8),

            // Datos del usuario
            _infoTile(Icons.email, 'Email', usuario.email),
            if (usuario.telefono != null)
              _infoTile(Icons.phone, 'Teléfono', usuario.telefono!),
            if (usuario.dni != null)
              _infoTile(Icons.badge, 'DNI', usuario.dni!),
            if (usuario.direccion != null)
              _infoTile(Icons.home, 'Dirección', usuario.direccion!),
            if (usuario.fechaRegistro != null)
              _infoTile(Icons.calendar_today, 'Miembro desde',
                  usuario.fechaRegistro!),

            const SizedBox(height: 32),

            // Botón Cerrar Sesión
            SizedBox(
              width: double.infinity,
              child: OutlinedButton.icon(
                icon: const Icon(Icons.logout, color: Colors.red),
                label: const Text('CERRAR SESIÓN',
                    style: TextStyle(color: Colors.red)),
                onPressed: () {
                  AuthService().logout();
                  Navigator.pushReplacementNamed(context, '/login');
                },
                style: OutlinedButton.styleFrom(
                  side: const BorderSide(color: Colors.red),
                  padding: const EdgeInsets.symmetric(vertical: 14),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _infoTile(IconData icon, String label, String valor) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8.0),
      child: Row(
        children: [
          Icon(icon, color: Colors.grey, size: 20),
          const SizedBox(width: 12),
          Text('$label: ', style: const TextStyle(fontWeight: FontWeight.bold)),
          Expanded(child: Text(valor, overflow: TextOverflow.ellipsis)),
        ],
      ),
    );
  }
}

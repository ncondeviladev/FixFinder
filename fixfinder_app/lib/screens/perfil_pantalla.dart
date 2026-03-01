// Pantalla de Perfil de Usuario.
// Muestra los datos personales y de cuenta del cliente u operario que tiene sesión activa.
import 'package:flutter/material.dart';
import '../models/usuario.dart';
import '../services/auth_service.dart';

class PerfilPantalla extends StatelessWidget {
  const PerfilPantalla({super.key});

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
            // Avatar
            Center(
              child: CircleAvatar(
                radius: 55,
                backgroundColor: _colorRol(usuario.rol).withValues(alpha: 0.15),
                backgroundImage: usuario.urlFoto != null
                    ? NetworkImage(usuario.urlFoto!)
                    : null,
                child: usuario.urlFoto == null
                    ? Icon(Icons.person,
                        size: 55, color: _colorRol(usuario.rol))
                    : null,
              ),
            ),
            const SizedBox(height: 16),

            // Nombre
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

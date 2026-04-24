import 'dart:io';
import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'package:firebase_storage/firebase_storage.dart';
import '../models/usuario.dart';
import '../services/auth_service.dart';
import 'package:provider/provider.dart';
import '../providers/usuario_provider.dart';
import '../theme/fixfinder_theme.dart';

/// Pantalla de Perfil de Usuario.
/// Permite visualizar la información personal, cambiar la foto de perfil
/// (subiéndola a Firebase) y editar datos básicos (nombre, email, tlf, dirección).
/// Se comunica con [AuthService] para la persistencia y sincronización con el servidor.
class PerfilPantalla extends StatefulWidget {
  const PerfilPantalla({super.key});

  @override
  State<PerfilPantalla> createState() => _PerfilPantallaState();
}

class _PerfilPantallaState extends State<PerfilPantalla> {
  bool _subiendoFoto = false;
  bool _guardandoDatos = false;

  /// Obtiene el color temático según el rol del usuario (consistencia visual con Dashboard).
  Color _obtenerColorRol(Rol rol) {
    switch (rol) {
      case Rol.CLIENTE:
        return Theme.of(context).colorScheme.tertiary;
      case Rol.OPERARIO:
        return FixFinderTheme.successColor;
      case Rol.GERENTE:
        return Theme.of(context).colorScheme.primary;
      case Rol.ADMIN:
        return FixFinderTheme.adminColor;
    }
  }

  /// Despliega un diálogo para editar la información personal del usuario.
  /// Implementa validación de campos y feedback visual durante el envío.
  void _abrirDialogoGestionPerfil() {
    final usuario = context.read<UsuarioProvider>().usuario;
    if (usuario == null) return;

    final formKey = GlobalKey<FormState>();
    final nombreCtrl = TextEditingController(text: usuario.nombreCompleto);
    final emailCtrl = TextEditingController(text: usuario.email);
    final telefonoCtrl = TextEditingController(text: usuario.telefono ?? '');
    final direccionCtrl = TextEditingController(text: usuario.direccion ?? '');

    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (context) => StatefulBuilder(
        builder: (context, setDialogState) => AlertDialog(
          title: const Text('Editar Perfil'),
          contentPadding: const EdgeInsets.fromLTRB(24, 20, 24, 0),
          content: Container(
            width: double.maxFinite,
            constraints:
                const BoxConstraints(maxWidth: 450), // Ancho máximo profesional
            child: Form(
              key: formKey,
              child: SingleChildScrollView(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('Modificar datos de: ${usuario.nombreCompleto}',
                        style:
                            const TextStyle(fontSize: 13, color: Colors.grey)),
                    const SizedBox(height: 20),
                    _crearSeccionEdicion(
                        'Nombre completo', nombreCtrl, Icons.person,
                        obligatorio: true),
                    _crearSeccionEdicion(
                        'Correo electrónico', emailCtrl, Icons.email,
                        teclado: TextInputType.emailAddress, obligatorio: true),
                    _crearSeccionEdicion('Teléfono', telefonoCtrl, Icons.phone,
                        teclado: TextInputType.phone),
                    _crearSeccionEdicion(
                        'Dirección', direccionCtrl, Icons.home),
                  ],
                ),
              ),
            ),
          ),
          actionsPadding:
              const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
          actions: [
            TextButton(
              onPressed: _guardandoDatos ? null : () => Navigator.pop(context),
              child: const Text('CANCELAR',
                  style: TextStyle(
                      color: Colors.grey, fontWeight: FontWeight.bold)),
            ),
            ElevatedButton(
              onPressed: _guardandoDatos
                  ? null
                  : () async {
                      if (formKey.currentState!.validate()) {
                        setDialogState(() => _guardandoDatos = true);
                        final exito = await AuthService().actualizarPerfil(
                          nombre: nombreCtrl.text.trim(),
                          email: emailCtrl.text.trim(),
                          telefono: telefonoCtrl.text.trim(),
                          direccion: direccionCtrl.text.trim(),
                        );

                        if (mounted) {
                          setState(() => _guardandoDatos = false);
                          Navigator.pop(context);
                          if (exito) {
                            if (context.mounted) {
                              context.read<UsuarioProvider>().refrescar();
                              ScaffoldMessenger.of(context).showSnackBar(
                                const SnackBar(
                                    content: Text(
                                        'Perfil actualizado correctamente'),
                                    backgroundColor:
                                        FixFinderTheme.successColor),
                              );
                            }
                          } else {
                            ScaffoldMessenger.of(context).showSnackBar(
                              SnackBar(
                                  content: const Text(
                                      'Error al actualizar perfil. El servidor rechazó la acción.'),
                                  backgroundColor:
                                      Theme.of(context).colorScheme.error),
                            );
                          }
                        }
                      }
                    },
              child: _guardandoDatos
                  ? const SizedBox(
                      width: 20,
                      height: 20,
                      child: CircularProgressIndicator(
                          strokeWidth: 2, color: Colors.white))
                  : const Text('GUARDAR CAMBIOS'),
            ),
          ],
        ),
      ),
    );
  }

  /// Helper para crear campos de edición con estilo uniforme.
  Widget _crearSeccionEdicion(
      String titulo, TextEditingController ctrl, IconData icono,
      {TextInputType? teclado, bool obligatorio = false}) {
    return Padding(
      padding: const EdgeInsets.all(8.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(titulo,
              style: const TextStyle(
                  fontSize: 12,
                  fontWeight: FontWeight.bold,
                  color: Colors.blueGrey)),
          const SizedBox(height: 6),
          TextFormField(
            controller: ctrl,
            keyboardType: teclado,
            decoration: InputDecoration(
              prefixIcon: Icon(icono, size: 18),
              contentPadding:
                  const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
              border: const OutlineInputBorder(),
            ),
            validator: (v) {
              if (obligatorio && (v == null || v.trim().isEmpty))
                return 'Campo requerido';
              return null;
            },
          ),
        ],
      ),
    );
  }

  /// Abre el selector de imágenes de la galería para actualizar el avatar.
  /// Sube la imagen a Firebase Storage y actualiza la URL en el servidor.
  Future<void> _gestionarFotoPerfil() async {
    final usuario = context.read<UsuarioProvider>().usuario;
    if (usuario == null) return;

    final picker = ImagePicker();
    final image =
        await picker.pickImage(source: ImageSource.gallery, imageQuality: 70);
    if (image == null) return;

    setState(() => _subiendoFoto = true);
    try {
      final ruta =
          'perfiles/${usuario.id}_${DateTime.now().millisecondsSinceEpoch}';
      final ref = FirebaseStorage.instance.ref().child(ruta);
      await ref.putFile(File(image.path));
      final url = await ref.getDownloadURL();
      await AuthService().actualizarFotoPerfil(url);
      if (mounted) {
        context.read<UsuarioProvider>().refrescar();
        setState(() => _subiendoFoto = false);
      }
    } catch (e) {
      debugPrint("❌ Error al gestionar foto: $e");
      if (mounted) {
        setState(() => _subiendoFoto = false);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
              content: Text('Error al subir imagen: $e'),
              backgroundColor: Theme.of(context).colorScheme.error),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final usuario = context.watch<UsuarioProvider>().usuario;
    if (usuario == null)
      return const Scaffold(body: Center(child: Text('Sesión no encontrada')));

    return Scaffold(
      appBar: AppBar(
        title: const Text('Mi Perfil'),
        actions: [
          IconButton(
            icon: const Icon(Icons.edit),
            onPressed: _abrirDialogoGestionPerfil,
            tooltip: 'Modificar perfil',
          ),
        ],
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          children: [
            _construirAvatar(usuario),
            const SizedBox(height: 16),
            Text(usuario.nombreCompleto,
                style:
                    const TextStyle(fontSize: 22, fontWeight: FontWeight.bold)),
            const SizedBox(height: 8),
            _construirBadgeRol(usuario),
            const SizedBox(height: 24),
            const Divider(),
            _bloqueInfo(Icons.email, 'Email', usuario.email),
            _bloqueInfo(
                Icons.phone, 'Teléfono', usuario.telefono ?? 'No especificado'),
            _bloqueInfo(Icons.badge, 'DNI', usuario.dni ?? 'No especificado'),
            _bloqueInfo(Icons.home, 'Dirección',
                usuario.direccion ?? 'No especificada'),
            const SizedBox(height: 40),
            _botonCerrarSesion(),
          ],
        ),
      ),
    );
  }

  /// Construye el widget del avatar circular con opción de edición.
  Widget _construirAvatar(Usuario usuario) {
    return Center(
      child: Stack(
        alignment: Alignment.bottomRight,
        children: [
          CircleAvatar(
            radius: 55,
            backgroundColor: _obtenerColorRol(usuario.rol).withOpacity(0.15),
            backgroundImage:
                usuario.urlFoto != null ? NetworkImage(usuario.urlFoto!) : null,
            child: _subiendoFoto
                ? const CircularProgressIndicator()
                : (usuario.urlFoto == null
                    ? Icon(Icons.person,
                        size: 55, color: _obtenerColorRol(usuario.rol))
                    : null),
          ),
          Positioned(
            child: GestureDetector(
              onTap: _subiendoFoto ? null : _gestionarFotoPerfil,
              child: Container(
                padding: const EdgeInsets.all(8),
                decoration: BoxDecoration(
                  color: Theme.of(context).primaryColor,
                  shape: BoxShape.circle,
                  boxShadow: [
                    BoxShadow(
                        color: Colors.black.withOpacity(0.3), blurRadius: 4)
                  ],
                ),
                child:
                    const Icon(Icons.camera_alt, color: Colors.white, size: 20),
              ),
            ),
          ),
        ],
      ),
    );
  }

  /// Genera una etiqueta visual estilizada con el rol del usuario.
  ///
  /// Utiliza un esquema de colores diferenciado por tipo de perfil para
  /// una jerarquía visual clara.
  Widget _construirBadgeRol(Usuario usuario) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
      decoration: BoxDecoration(
        color: _obtenerColorRol(usuario.rol).withOpacity(0.15),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: _obtenerColorRol(usuario.rol)),
      ),
      child: Text(usuario.rol.name,
          style: TextStyle(
              color: _obtenerColorRol(usuario.rol),
              fontWeight: FontWeight.bold)),
    );
  }

  /// Widget informativo para mostrar un par de datos (Icono + Etiqueta + Valor).
  ///
  /// Estructura la información personal de forma legible y alineada.
  Widget _bloqueInfo(IconData icono, String etiqueta, String valor) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 12.0),
      child: Row(
        children: [
          Icon(icono, color: Colors.grey, size: 20),
          const SizedBox(width: 12),
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(etiqueta,
                  style: const TextStyle(
                      fontSize: 11,
                      color: Colors.grey,
                      fontWeight: FontWeight.bold)),
              Text(valor,
                  style: const TextStyle(
                      fontSize: 15, fontWeight: FontWeight.w500)),
            ],
          ),
        ],
      ),
    );
  }

  /// Despliega el botón de cierre de sesión con validación de seguridad.
  ///
  /// Finaliza la sesión activa, limpia la persistencia local y redirige al Login.
  Widget _botonCerrarSesion() {
    return SizedBox(
      width: double.infinity,
      child: OutlinedButton.icon(
        icon: Icon(Icons.logout, color: Theme.of(context).colorScheme.error),
        label: Text('CERRAR SESIÓN',
            style: TextStyle(
                color: Theme.of(context).colorScheme.error,
                fontWeight: FontWeight.bold)),
        onPressed: () {
          AuthService().logout();
          Navigator.pushReplacementNamed(context, '/login');
        },
        style: OutlinedButton.styleFrom(
          side: BorderSide(color: Theme.of(context).colorScheme.error),
          padding: const EdgeInsets.symmetric(vertical: 14),
          shape:
              RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
        ),
      ),
    );
  }
}

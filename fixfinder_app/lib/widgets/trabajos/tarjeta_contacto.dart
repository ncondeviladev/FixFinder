// Widget reutilizable en forma de tarjeta para mostrar información de contacto de un usuario.
// Usualmente expone los datos de un operario a su cliente, o de un cliente al operario.
import 'package:flutter/material.dart';
import '../../models/usuario.dart';

/// Tarjeta informativa para mostrar los datos de perfil y contacto de un usuario.
/// Se utiliza para facilitar la comunicación entre clientes y operarios asignados.
class TarjetaContacto extends StatelessWidget {
  final String titulo;
  final Usuario usuario;
  final bool esOperario;

  const TarjetaContacto({
    super.key,
    required this.titulo,
    required this.usuario,
    this.esOperario = false,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Divider(),
        Text(
          titulo,
          style: const TextStyle(
            fontWeight: FontWeight.bold,
            fontSize: 16,
          ),
        ),
        const SizedBox(height: 8),
        ListTile(
          contentPadding: EdgeInsets.zero,
          leading: CircleAvatar(
            backgroundColor: esOperario ? Colors.blue.shade100 : null,
            backgroundImage:
                usuario.urlFoto != null ? NetworkImage(usuario.urlFoto!) : null,
            child: usuario.urlFoto == null
                ? Icon(esOperario ? Icons.engineering : Icons.person,
                    color: esOperario ? Colors.blue : null)
                : null,
          ),
          title: Text(usuario.nombreCompleto),
          subtitle: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              if (usuario.telefono != null) Text('Tel: ${usuario.telefono}'),
              if (usuario.email.isNotEmpty) Text('Email: ${usuario.email}'),
            ],
          ),
          trailing: usuario.telefono != null
              ? IconButton(
                  icon: const Icon(Icons.phone, color: Colors.green),
                  onPressed: () {
                    ScaffoldMessenger.of(context).showSnackBar(
                      SnackBar(
                          content: Text('Llamando a ${usuario.telefono}...')),
                    );
                  },
                )
              : null,
        ),
      ],
    );
  }
}

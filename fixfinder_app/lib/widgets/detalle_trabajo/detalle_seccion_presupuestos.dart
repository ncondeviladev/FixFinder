// Sección de la pantalla de detalles dedicada a la gestión de presupuestos.
// Permite al cliente listar, ver detalles y aceptar los presupuestos recibidos.
import 'package:flutter/material.dart';
import '../../models/presupuesto.dart';

class DetalleSeccionPresupuestos extends StatelessWidget {
  final List<Presupuesto> presupuestos;
  final bool cargando;
  final bool procesando;
  final VoidCallback onRefresh;
  final Function(int) onAceptar;

  const DetalleSeccionPresupuestos({
    super.key,
    required this.presupuestos,
    required this.cargando,
    required this.procesando,
    required this.onRefresh,
    required this.onAceptar,
  });

  void _verDetallesEmpresa(BuildContext context, Presupuesto p) {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Información de la Empresa'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Nombre: ${p.nombreEmpresa ?? "Desconocido"}',
                style: const TextStyle(fontWeight: FontWeight.bold)),
            const SizedBox(height: 8),
            if (p.emailEmpresa != null) ...[
              Row(
                children: [
                  const Icon(Icons.email, size: 16),
                  const SizedBox(width: 8),
                  Expanded(child: Text(p.emailEmpresa!)),
                ],
              ),
              const SizedBox(height: 8),
            ],
            if (p.telefonoEmpresa != null) ...[
              Row(
                children: [
                  const Icon(Icons.phone, size: 16),
                  const SizedBox(width: 8),
                  Text(p.telefonoEmpresa!),
                ],
              ),
              const SizedBox(height: 8),
            ],
            const Divider(),
            const Text('Detalles del Presupuesto:',
                style: TextStyle(fontWeight: FontWeight.bold)),
            Text(p.notas ?? "Sin notas adicionales"),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: const Text('CERRAR'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            const Text('PRESUPUESTOS RECIBIDOS',
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
            IconButton(onPressed: onRefresh, icon: const Icon(Icons.refresh)),
          ],
        ),
        const SizedBox(height: 8),
        if (cargando)
          const Center(child: CircularProgressIndicator())
        else if (presupuestos.isEmpty)
          const Text('Aún no hay presupuestos para esta incidencia.')
        else
          ...presupuestos.map((p) => Card(
                margin: const EdgeInsets.only(bottom: 8),
                child: ListTile(
                  title: Row(
                    children: [
                      Expanded(
                          child: Text(
                              '${p.monto}€ - ${p.nombreEmpresa ?? "Empresa"}')),
                      IconButton(
                        icon:
                            const Icon(Icons.info_outline, color: Colors.blue),
                        onPressed: () => _verDetallesEmpresa(context, p),
                        tooltip: 'Ver detalles de la empresa',
                      ),
                    ],
                  ),
                  subtitle: Text('Estado: ${p.estado}'),
                  trailing: p.estado == 'PENDIENTE'
                      ? ElevatedButton(
                          onPressed: procesando ? null : () => onAceptar(p.id),
                          child: const Text('ACEPTAR'),
                        )
                      : null,
                ),
              )),
      ],
    );
  }
}

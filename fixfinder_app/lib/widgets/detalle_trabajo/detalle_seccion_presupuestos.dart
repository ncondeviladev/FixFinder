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
  final Function(int) onRechazar;

  const DetalleSeccionPresupuestos({
    super.key,
    required this.presupuestos,
    required this.cargando,
    required this.procesando,
    required this.onRefresh,
    required this.onAceptar,
    required this.onRechazar,
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
            if (p.cifEmpresa != null) ...[
              Row(
                children: [
                  const Icon(Icons.assignment_ind, size: 16),
                  const SizedBox(width: 8),
                  Text('CIF: ${p.cifEmpresa!}'),
                ],
              ),
              const SizedBox(height: 8),
            ],
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
            if (p.direccionEmpresa != null) ...[
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Icon(Icons.location_on, size: 16),
                  const SizedBox(width: 8),
                  Expanded(child: Text(p.direccionEmpresa!)),
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
            IconButton(
                onPressed: procesando ? null : onRefresh,
                icon: const Icon(Icons.refresh)),
          ],
        ),
        const SizedBox(height: 8),
        if (cargando)
          const Center(child: CircularProgressIndicator())
        else if (presupuestos.isEmpty)
          const Padding(
            padding: EdgeInsets.symmetric(vertical: 20),
            child: Text('Aún no hay presupuestos para esta incidencia.',
                style: TextStyle(color: Colors.grey, fontStyle: FontStyle.italic)),
          )
        else
          ...presupuestos
              .where((p) => p.estado != 'RECHAZADO')
              .map((p) => _buildPresupuestoCard(context, p)),
      ],
    );
  }

  Widget _buildPresupuestoCard(BuildContext context, Presupuesto p) {
    final bool esPendiente = p.estado == 'PENDIENTE';

    return Card(
      elevation: 3,
      margin: const EdgeInsets.only(bottom: 12),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Cabecera de la tarjeta: Empresa y Precio
          ListTile(
            leading: const CircleAvatar(
              backgroundColor: Colors.blueAccent,
              child: Icon(Icons.business, color: Colors.white),
            ),
            title: Text(
              p.nombreEmpresa ?? "Empresa",
              style: const TextStyle(fontWeight: FontWeight.bold),
            ),
            subtitle: Text('ID Oferta: #${p.id}'),
            trailing: Text(
              '${p.monto.toStringAsFixed(2)}€',
              style: const TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                  color: Colors.green),
            ),
          ),

          // Cuerpo de la tarjeta: Propuesta técnica
          if (p.notas != null && p.notas!.isNotEmpty)
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16.0),
              child: Container(
                padding: const EdgeInsets.all(12),
                width: double.infinity,
                decoration: BoxDecoration(
                  color: Colors.indigo.withOpacity(0.05),
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(color: Colors.indigo.withOpacity(0.1)),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text('Propuesta técnica:',
                        style: TextStyle(
                            fontSize: 12,
                            fontWeight: FontWeight.bold,
                            color: Colors.blueGrey)),
                    const SizedBox(height: 4),
                    Text(p.notas!,
                        style: const TextStyle(fontSize: 14, height: 1.3)),
                  ],
                ),
              ),
            ),

          // Botones de acción
          Padding(
            padding: const EdgeInsets.all(12.0),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.end,
              children: [
                TextButton.icon(
                  onPressed: () => _verDetallesEmpresa(context, p),
                  icon: const Icon(Icons.info_outline),
                  label: const Text('INFO'),
                ),
                const Spacer(),
                if (esPendiente) ...[
                  OutlinedButton(
                    onPressed: procesando ? null : () => onRechazar(p.id),
                    style: OutlinedButton.styleFrom(
                      foregroundColor: Colors.red,
                      side: const BorderSide(color: Colors.red),
                    ),
                    child: const Text('RECHAZAR'),
                  ),
                  const SizedBox(width: 8),
                  ElevatedButton(
                    onPressed: procesando ? null : () => onAceptar(p.id),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: Colors.blue,
                      foregroundColor: Colors.white,
                    ),
                    child: const Text('ACEPTAR'),
                  ),
                ] else
                  Chip(
                    label: Text(p.estado),
                    backgroundColor: p.estado == 'ACEPTADO'
                        ? Colors.green[100]
                        : Colors.grey[200],
                  ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

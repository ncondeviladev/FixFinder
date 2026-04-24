import 'package:flutter/material.dart';
import 'tarjeta_empresa_presupuesto.dart';
import '../../models/presupuesto.dart';

/// Sección que gestiona y visualiza la lista de presupuestos recibidos para una incidencia.
/// Permite al cliente comparar ofertas y tomar decisiones de aceptación o rechazo.
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
          Padding(
            padding: const EdgeInsets.symmetric(vertical: 20),
            child: Text('Aún no hay presupuestos para esta incidencia.',
                style: TextStyle(
                    color: Theme.of(context).disabledColor,
                    fontStyle: FontStyle.italic)),
          )
        else
          ...presupuestos.where((p) => p.estado != 'RECHAZADO').map(
                (p) => TarjetaEmpresaPresupuesto(
                  presupuesto: p,
                  procesando: procesando,
                  onAceptar: onAceptar,
                  onRechazar: onRechazar,
                ),
              ),
      ],
    );
  }
}

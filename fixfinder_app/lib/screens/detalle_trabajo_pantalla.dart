import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../models/trabajo.dart';
import '../models/usuario.dart';
import '../models/presupuesto.dart';
import '../providers/trabajo_provider.dart';
import '../services/auth_service.dart';

class DetalleTrabajoPantalla extends StatefulWidget {
  final Trabajo trabajo;
  const DetalleTrabajoPantalla({super.key, required this.trabajo});

  @override
  State<DetalleTrabajoPantalla> createState() => _DetalleTrabajoPantallaState();
}

class _DetalleTrabajoPantallaState extends State<DetalleTrabajoPantalla> {
  bool _procesando = false;
  List<Presupuesto> _presupuestos = [];
  bool _cargandoPresupuestos = false;

  @override
  void initState() {
    super.initState();
    if (widget.trabajo.estado == EstadoTrabajo.PENDIENTE ||
        widget.trabajo.estado == EstadoTrabajo.PRESUPUESTADO) {
      _cargarPresupuestos();
    }
  }

  Future<void> _cargarPresupuestos() async {
    setState(() => _cargandoPresupuestos = true);
    final lista = await context
        .read<TrabajoProvider>()
        .obtenerPresupuestos(widget.trabajo.id);
    if (mounted) {
      setState(() {
        _presupuestos = lista;
        _cargandoPresupuestos = false;
      });
    }
  }

  Future<void> _aceptarPresupuesto(int idPresupuesto) async {
    setState(() => _procesando = true);
    final exito =
        await context.read<TrabajoProvider>().aceptarPresupuesto(idPresupuesto);
    if (mounted) {
      setState(() => _procesando = false);
      if (exito) {
        Navigator.pop(context);
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
              content: Text(
                  'Presupuesto aceptado. Esperando asignación de operario.')),
        );
      }
    }
  }

  Future<void> _finalizarTrabajo(int idTrabajo) async {
    setState(() => _procesando = true);
    final exito = await context
        .read<TrabajoProvider>()
        .actualizarEstadoTrabajo(idTrabajo, EstadoTrabajo.FINALIZADO);
    if (mounted) {
      setState(() => _procesando = false);
      if (exito) {
        Navigator.pop(context);
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Trabajo finalizado correctamente.')),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final usuario = AuthService().usuarioActual;
    final esCliente = usuario?.rol == Rol.CLIENTE;
    final esOperario = usuario?.rol == Rol.OPERARIO;

    return Scaffold(
      appBar: AppBar(title: Text('Incidencia #${widget.trabajo.id}')),
      body: Consumer<TrabajoProvider>(
        builder: (context, provider, child) {
          // Buscamos el trabajo actualizado en el provider
          final trabajoActual = provider.trabajos.firstWhere(
            (t) => t.id == widget.trabajo.id,
            orElse: () => widget.trabajo,
          );

          return SingleChildScrollView(
            padding: const EdgeInsets.all(16.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                _buildInfoCard(trabajoActual),
                const SizedBox(height: 24),
                if (esCliente &&
                    (trabajoActual.estado == EstadoTrabajo.PENDIENTE ||
                        trabajoActual.estado == EstadoTrabajo.PRESUPUESTADO))
                  _buildSeccionPresupuestos(trabajoActual.id),
                if (esOperario &&
                    (trabajoActual.estado == EstadoTrabajo.ASIGNADO ||
                        trabajoActual.estado == EstadoTrabajo.REALIZADO))
                  _buildBotonFinalizar(trabajoActual.id),
                if (trabajoActual.estado == EstadoTrabajo.FINALIZADO)
                  _buildResumenFinal(trabajoActual),
                if (_procesando)
                  const Center(
                      child: Padding(
                    padding: EdgeInsets.all(8.0),
                    child: CircularProgressIndicator(),
                  )),
              ],
            ),
          );
        },
      ),
    );
  }

  Widget _buildInfoCard(Trabajo trabajo) {
    return Card(
      elevation: 4,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(trabajo.titulo,
                style:
                    const TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
            const Divider(),
            _dato('Estado', trabajo.estado.name,
                color: _colorEstado(trabajo.estado)),
            _dato('Categoría', trabajo.categoria.name),
            _dato('Dirección', trabajo.direccion),
            const SizedBox(height: 12),
            const Text('Descripción:',
                style: TextStyle(fontWeight: FontWeight.bold)),
            Text(trabajo.descripcion),
            const SizedBox(height: 16),
            if (trabajo.urlsFotos.isNotEmpty)
              _buildGaleriaFotos(trabajo.urlsFotos),
            const SizedBox(height: 16),
            if (AuthService().usuarioActual?.rol == Rol.OPERARIO)
              _buildDatosCliente(context, trabajo.cliente),
            if (AuthService().usuarioActual?.rol == Rol.CLIENTE)
              _buildDatosOperario(context, trabajo.operarioAsignado),
          ],
        ),
      ),
    );
  }

  Widget _buildDatosCliente(BuildContext context, Usuario? cliente) {
    if (cliente == null) return const SizedBox.shrink();

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Divider(),
        const Text('Datos del Cliente:',
            style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
        const SizedBox(height: 8),
        ListTile(
          contentPadding: EdgeInsets.zero,
          leading: CircleAvatar(
            backgroundImage:
                cliente.urlFoto != null ? NetworkImage(cliente.urlFoto!) : null,
            child: cliente.urlFoto == null ? const Icon(Icons.person) : null,
          ),
          title: Text(cliente.nombreCompleto),
          subtitle: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              if (cliente.telefono != null) Text('Tel: ${cliente.telefono}'),
              if (cliente.email.isNotEmpty) Text('Email: ${cliente.email}'),
            ],
          ),
          trailing: cliente.telefono != null
              ? IconButton(
                  icon: const Icon(Icons.phone, color: Colors.green),
                  onPressed: () {
                    // Aquí irá url_launcher en el futuro
                    ScaffoldMessenger.of(context).showSnackBar(
                      SnackBar(
                          content: Text('Llamando a ${cliente.telefono}...')),
                    );
                  },
                )
              : null,
        ),
      ],
    );
  }

  Widget _buildGaleriaFotos(List<String> urls) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text('Imágenes adjuntas:',
            style: TextStyle(fontWeight: FontWeight.bold)),
        const SizedBox(height: 8),
        SizedBox(
          height: 120,
          child: ListView.builder(
            scrollDirection: Axis.horizontal,
            itemCount: urls.length,
            itemBuilder: (context, index) {
              return Padding(
                padding: const EdgeInsets.only(right: 8.0),
                child: GestureDetector(
                  onTap: () => _verImagenGrande(urls[index]),
                  child: ClipRRect(
                    borderRadius: BorderRadius.circular(8),
                    child: Image.network(
                      urls[index],
                      width: 120,
                      height: 120,
                      fit: BoxFit.cover,
                      errorBuilder: (context, error, stackTrace) => Container(
                        width: 120,
                        color: Colors.grey.shade300,
                        child: const Icon(Icons.broken_image),
                      ),
                    ),
                  ),
                ),
              );
            },
          ),
        ),
      ],
    );
  }

  void _verImagenGrande(String url) {
    showDialog(
      context: context,
      builder: (ctx) => Dialog(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Image.network(url, fit: BoxFit.contain),
            TextButton(
              onPressed: () => Navigator.pop(ctx),
              child: const Text('CERRAR'),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildDatosOperario(BuildContext context, Usuario? operario) {
    if (operario == null) return const SizedBox.shrink();

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Divider(),
        const Text('Técnico Asignado:',
            style: TextStyle(
                fontWeight: FontWeight.bold, fontSize: 16, color: Colors.blue)),
        const SizedBox(height: 8),
        ListTile(
          contentPadding: EdgeInsets.zero,
          leading: CircleAvatar(
            backgroundColor: Colors.blue.shade100,
            backgroundImage: operario.urlFoto != null
                ? NetworkImage(operario.urlFoto!)
                : null,
            child: operario.urlFoto == null
                ? const Icon(Icons.engineering, color: Colors.blue)
                : null,
          ),
          title: Text(operario.nombreCompleto),
          subtitle: Text(operario.telefono ?? 'Sin teléfono de contacto'),
        ),
      ],
    );
  }

  Widget _buildResumenFinal(Trabajo trabajo) {
    return Card(
      color: Colors.green.shade50,
      margin: const EdgeInsets.only(top: 8),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: BorderSide(color: Colors.green.shade200),
      ),
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(Icons.check_circle, color: Colors.green.shade700),
                const SizedBox(width: 8),
                Text(
                  'Trabajo Completado',
                  style: TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                    color: Colors.green.shade700,
                  ),
                ),
              ],
            ),
            const Divider(height: 24),
            if (trabajo.fechaFinalizacion != null)
              _dato('Finalizado', trabajo.fechaFinalizacion!,
                  color: Colors.grey[700]),
            if (trabajo.presupuesto != null)
              _dato('Precio final',
                  '${trabajo.presupuesto!.monto.toStringAsFixed(2)} €',
                  color: Colors.green.shade800),
            if (trabajo.valoracion > 0) ...[
              const SizedBox(height: 8),
              const Text('Valoración:',
                  style: TextStyle(fontWeight: FontWeight.bold)),
              Row(
                children: List.generate(
                    5,
                    (i) => Icon(
                          i < trabajo.valoracion
                              ? Icons.star
                              : Icons.star_border,
                          color: Colors.amber,
                        )),
              ),
            ],
            if (trabajo.comentarioCliente != null &&
                trabajo.comentarioCliente!.isNotEmpty) ...[
              const SizedBox(height: 8),
              Text('"${trabajo.comentarioCliente}"',
                  style: const TextStyle(
                      fontStyle: FontStyle.italic, color: Colors.grey)),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildSeccionPresupuestos(int idTrabajo) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            const Text('PRESUPUESTOS RECIBIDOS',
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
            IconButton(
                onPressed: _cargarPresupuestos,
                icon: const Icon(Icons.refresh)),
          ],
        ),
        const SizedBox(height: 8),
        if (_cargandoPresupuestos)
          const Center(child: CircularProgressIndicator())
        else if (_presupuestos.isEmpty)
          const Text('Aún no hay presupuestos para esta incidencia.')
        else
          ..._presupuestos.map((p) => Card(
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
                        onPressed: () => _verDetallesEmpresa(p),
                        tooltip: 'Ver detalles de la empresa',
                      ),
                    ],
                  ),
                  subtitle: Text('Estado: ${p.estado}'),
                  trailing: p.estado == 'PENDIENTE'
                      ? ElevatedButton(
                          onPressed: _procesando
                              ? null
                              : () => _aceptarPresupuesto(p.id),
                          child: const Text('ACEPTAR'),
                        )
                      : null,
                ),
              )),
      ],
    );
  }

  Widget _buildBotonFinalizar(int idTrabajo) {
    return SizedBox(
      width: double.infinity,
      child: ElevatedButton.icon(
        onPressed: _procesando ? null : () => _finalizarTrabajo(idTrabajo),
        icon: const Icon(Icons.check_circle),
        label: const Text('MARCAR COMO FINALIZADO'),
        style: ElevatedButton.styleFrom(
          backgroundColor: Colors.green,
          foregroundColor: Colors.white,
          padding: const EdgeInsets.symmetric(vertical: 12),
        ),
      ),
    );
  }

  Color _colorEstado(EstadoTrabajo e) {
    switch (e) {
      case EstadoTrabajo.PENDIENTE:
        return Colors.orange;
      case EstadoTrabajo.FINALIZADO:
        return Colors.grey;
      case EstadoTrabajo.ASIGNADO:
        return Colors.blue;
      case EstadoTrabajo.REALIZADO:
        return Colors.green;
      default:
        return Colors.black;
    }
  }

  Widget _dato(String etiqueta, String valor, {Color? color}) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4.0),
      child: Row(
        children: [
          Text('$etiqueta: ',
              style: const TextStyle(fontWeight: FontWeight.bold)),
          Expanded(
              child: Text(valor,
                  style: TextStyle(
                      color: color,
                      fontWeight: color != null
                          ? FontWeight.bold
                          : FontWeight.normal))),
        ],
      ),
    );
  }

  void _verDetallesEmpresa(Presupuesto p) {
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
}

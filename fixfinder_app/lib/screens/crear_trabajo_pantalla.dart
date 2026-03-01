// Pantalla que incluye el formulario para crear o modificar una nueva incidencia.
// Permite al cliente llenar titulo, descripción, ubicación y agregar fotos (futuro).
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/trabajo_provider.dart';
import '../models/trabajo.dart';

class CrearTrabajoPantalla extends StatefulWidget {
  final Trabajo? trabajoAEditar;
  const CrearTrabajoPantalla({super.key, this.trabajoAEditar});

  @override
  State<CrearTrabajoPantalla> createState() => _CrearTrabajoPantallaState();
}

class _CrearTrabajoPantallaState extends State<CrearTrabajoPantalla> {
  final _formKey = GlobalKey<FormState>();
  final _tituloController = TextEditingController();
  final _descripcionController = TextEditingController();
  final _direccionController = TextEditingController();

  CategoriaServicio _categoriaSeleccionada = CategoriaServicio.OTROS;
  int _urgenciaSeleccionada = 1; // 1=Normal, 2=Prioridad, 3=Urgente
  bool _enviando = false;
  List<String> _urlsFotos = [];

  @override
  void initState() {
    super.initState();
    if (widget.trabajoAEditar != null) {
      _tituloController.text = widget.trabajoAEditar!.titulo;
      _descripcionController.text = widget.trabajoAEditar!.descripcion;
      _direccionController.text = widget.trabajoAEditar!.direccion;
      _categoriaSeleccionada = widget.trabajoAEditar!.categoria;
      _urgenciaSeleccionada = widget.trabajoAEditar!.urgencia;
      _urlsFotos = List.from(widget.trabajoAEditar!.urlsFotos);
    }
  }

  Future<void> _guardar() async {
    if (!_formKey.currentState!.validate()) return;

    setState(() => _enviando = true);

    final Map<String, dynamic> datos = {
      'titulo': _tituloController.text,
      'descripcion': _descripcionController.text,
      // Solo enviamos la dirección si el usuario ha escrito algo
      if (_direccionController.text.trim().isNotEmpty)
        'direccion': _direccionController.text.trim(),
      'categoria': _categoriaSeleccionada.name,
      'urgencia': _urgenciaSeleccionada,
      'fechaCreacion': DateTime.now().toIso8601String(),
      if (widget.trabajoAEditar != null) 'idTrabajo': widget.trabajoAEditar!.id,
      if (_urlsFotos.isNotEmpty) 'urls_fotos': _urlsFotos,
    };

    bool exito;
    if (widget.trabajoAEditar != null) {
      exito = await context
          .read<TrabajoProvider>()
          .modificarTrabajo(widget.trabajoAEditar!.id, datos);
    } else {
      exito = await context.read<TrabajoProvider>().crearTrabajo(datos);
    }

    if (mounted) {
      setState(() => _enviando = false);
      final esEdicion = widget.trabajoAEditar != null;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(exito
              ? (esEdicion
                  ? 'Incidencia modificada correctamente.'
                  : 'Incidencia enviada correctamente.')
              : (esEdicion
                  ? 'Error al modificar. ¿Solo se puede editar en estado PENDIENTE?'
                  : 'Error al enviar la incidencia.')),
          backgroundColor: exito ? Colors.green : Colors.red,
        ),
      );
      if (exito) Navigator.pop(context);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.trabajoAEditar == null
            ? 'Nueva Incidencia'
            : 'Modificar Incidencia'),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16.0),
        child: Form(
          key: _formKey,
          child: Column(
            children: [
              TextFormField(
                controller: _tituloController,
                decoration:
                    const InputDecoration(labelText: 'Título del problema'),
                validator: (v) => v!.isEmpty ? 'Campo obligatorio' : null,
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _descripcionController,
                decoration:
                    const InputDecoration(labelText: 'Descripción detallada'),
                maxLines: 3,
                validator: (v) => v!.isEmpty ? 'Campo obligatorio' : null,
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _direccionController,
                decoration: const InputDecoration(
                  labelText: 'Dirección del servicio (opcional)',
                  hintText: 'Si se deja vacío, se usa tu dirección registrada',
                ),
                // Campo opcional: no tiene validador obligatorio
              ),
              const SizedBox(height: 16),
              DropdownButtonFormField<CategoriaServicio>(
                initialValue: _categoriaSeleccionada,
                decoration: const InputDecoration(labelText: 'Categoría'),
                items: CategoriaServicio.values.map((cat) {
                  return DropdownMenuItem(value: cat, child: Text(cat.name));
                }).toList(),
                onChanged: (val) =>
                    setState(() => _categoriaSeleccionada = val!),
              ),
              const SizedBox(height: 16),
              // Selector de urgencia
              DropdownButtonFormField<int>(
                value: _urgenciaSeleccionada,
                decoration: const InputDecoration(
                  labelText: 'Urgencia',
                  prefixIcon: Icon(Icons.warning_amber),
                ),
                items: const [
                  DropdownMenuItem(value: 1, child: Text('Normal')),
                  DropdownMenuItem(value: 2, child: Text('⚡ Prioridad')),
                  DropdownMenuItem(value: 3, child: Text('🚨 Urgente')),
                ],
                onChanged: (val) =>
                    setState(() => _urgenciaSeleccionada = val!),
              ),
              const SizedBox(height: 24),
              // -- SECCIÓN DE FOTOS (Preparación UI para Firebase) --
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: Colors.grey.withValues(alpha: 0.1),
                  borderRadius: BorderRadius.circular(12),
                  border:
                      Border.all(color: Colors.orange.withValues(alpha: 0.5)),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text('Imágenes y Fotos',
                        style: TextStyle(fontWeight: FontWeight.bold)),
                    const SizedBox(height: 8),
                    const Text(
                        'Próximamente: Sube fotos del problema para ayudar al operario.',
                        style: TextStyle(fontSize: 12, color: Colors.grey)),
                    const SizedBox(height: 12),
                    OutlinedButton.icon(
                      icon: const Icon(Icons.add_a_photo),
                      label:
                          const Text('Subir Foto (Firebase en construcción)'),
                      onPressed: () {
                        ScaffoldMessenger.of(context).showSnackBar(
                          const SnackBar(
                              content:
                                  Text('Firebase Storage no disponible aún.')),
                        );
                      },
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 32),
              SizedBox(
                width: double.infinity,
                height: 50,
                child: ElevatedButton(
                  onPressed: _enviando ? null : _guardar,
                  child: _enviando
                      ? const CircularProgressIndicator()
                      : const Text('ENVIAR REPORTE'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

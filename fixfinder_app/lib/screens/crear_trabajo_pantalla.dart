// Pantalla que incluye el formulario para crear o modificar una nueva incidencia.
// Permite al cliente llenar titulo, descripción, ubicación y agregar fotos (futuro).
import 'dart:io';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:image_picker/image_picker.dart';
import 'package:firebase_storage/firebase_storage.dart';
import '../providers/trabajo_provider.dart';
import '../models/trabajo.dart';
import '../widgets/trabajos/galeria_fotos.dart';

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
  bool _subiendoFoto = false;
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

  Future<void> _seleccionarYSubirFoto() async {
    final picker = ImagePicker();
    final XFile? image = await picker.pickImage(
      source: ImageSource.gallery,
      imageQuality: 70, // Reducir calidad para optimizar subida
    );

    if (image == null) return; // cancelado por usuario

    setState(() => _subiendoFoto = true);

    try {
      final fileName =
          'trabajos/${DateTime.now().millisecondsSinceEpoch}_${image.name}';
      final storageRef = FirebaseStorage.instance.ref().child(fileName);

      await storageRef.putFile(File(image.path));
      final downloadUrl = await storageRef.getDownloadURL();

      if (mounted) {
        setState(() {
          _urlsFotos.add(downloadUrl);
          _subiendoFoto = false;
        });
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
                  ? 'Error al modificar. ¿Se puede editar solo en PENDIENTE o ASIGNADO?'
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
                        'Añade fotos para que el operario vea el problema.',
                        style: TextStyle(fontSize: 12, color: Colors.grey)),
                    const SizedBox(height: 12),
                    if (_urlsFotos.isNotEmpty)
                      Padding(
                        padding: const EdgeInsets.only(bottom: 12.0),
                        child: GaleriaFotos(urls: _urlsFotos),
                      ),
                    OutlinedButton.icon(
                      icon: _subiendoFoto
                          ? const SizedBox(
                              width: 16,
                              height: 16,
                              child: CircularProgressIndicator(strokeWidth: 2))
                          : const Icon(Icons.add_a_photo),
                      label: Text(_subiendoFoto
                          ? 'Subiendo imagen...'
                          : 'Añadir nueva foto'),
                      onPressed: _subiendoFoto ? null : _seleccionarYSubirFoto,
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

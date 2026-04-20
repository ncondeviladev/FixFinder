import 'dart:io';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:image_picker/image_picker.dart';
import '../../theme/fixfinder_theme.dart';
import 'package:firebase_storage/firebase_storage.dart';
import '../providers/trabajo_provider.dart';
import '../models/trabajo.dart';
import '../widgets/trabajos/galeria_fotos.dart';

/// Pantalla para la creación y edición de incidencias de servicio.
/// 
/// Proporciona un formulario completo para capturar el título, descripción,
/// ubicación y evidencias visuales de un problema técnico. Gestiona tanto
/// el alta de nuevos trabajos como la modificación de borradores existentes.
class CrearTrabajoPantalla extends StatefulWidget {
  /// Instancia de trabajo opcional para el modo edición.
  final Trabajo? trabajoAEditar;
  
  const CrearTrabajoPantalla({super.key, this.trabajoAEditar});

  @override
  State<CrearTrabajoPantalla> createState() => _CrearTrabajoPantallaState();
}

class _CrearTrabajoPantallaState extends State<CrearTrabajoPantalla> {
  /// Clave global para validación integrada del formulario.
  final _formKey = GlobalKey<FormState>();
  
  /// Controlador para el título corto de la incidencia.
  final _tituloController = TextEditingController();
  
  /// Controlador para la explicación detallada del problema.
  final _descripcionController = TextEditingController();
  
  /// Controlador para la ubicación física del servicio.
  final _direccionController = TextEditingController();

  /// Categoría técnica del servicio solicitado.
  CategoriaServicio _categoriaSeleccionada = CategoriaServicio.OTROS;
  
  /// Nivel de urgencia percibido (1 a 3).
  int _urgenciaSeleccionada = 1; 
  
  /// Flag de control para bloquear la UI durante el envío.
  bool _enviando = false;
  
  /// Estado del proceso de carga de archivos a la nube.
  bool _subiendoFoto = false;
  
  /// Lista de enlaces públicos a las imágenes asociadas.
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

  /// Gestiona la selección de una imagen desde la galería y su posterior
  /// persistencia en Firebase Storage para ser asociada al trabajo.
  Future<void> _seleccionarYSubirFoto() async {
    final picker = ImagePicker();
    final XFile? image = await picker.pickImage(
      source: ImageSource.gallery,
      imageQuality: 70, // Reducción de peso para optimizar la transferencia
    );

    if (image == null) return;

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
              backgroundColor: Theme.of(context).colorScheme.error),
        );
      }
    }
  }

  /// Valida y persiste los datos del formulario en el servidor.
  /// 
  /// Determina si debe realizar una creación (POST) o actualización (PUT)
  /// basándose en la presencia de [widget.trabajoAEditar].
  Future<void> _guardar() async {
    if (!_formKey.currentState!.validate()) return;

    setState(() => _enviando = true);

    final Map<String, dynamic> datos = {
      'titulo': _tituloController.text,
      'descripcion': _descripcionController.text,
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
          backgroundColor:
              exito ? FixFinderTheme.successColor : Theme.of(context).colorScheme.error,
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
                  hintText: 'Ej: Calle Mayor 10, Gandia',
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
                initialValue: _urgenciaSeleccionada,
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
                  color: Theme.of(context).dividerColor.withOpacity(0.1),
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(
                      color: Theme.of(context).colorScheme.primary.withOpacity(0.5)),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text('Imágenes y Fotos',
                        style: TextStyle(fontWeight: FontWeight.bold)),
                    const SizedBox(height: 8),
                    Text('Añade fotos para que el operario vea el problema.',
                        style: TextStyle(
                            fontSize: 12, color: Theme.of(context).hintColor)),
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

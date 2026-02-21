import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/trabajo_provider.dart';
import '../models/trabajo.dart';

class CrearTrabajoPantalla extends StatefulWidget {
  const CrearTrabajoPantalla({super.key});

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

  Future<void> _guardar() async {
    if (!_formKey.currentState!.validate()) return;

    setState(() => _enviando = true);

    final Map<String, dynamic> datos = {
      'titulo': _tituloController.text,
      'descripcion': _descripcionController.text,
      'direccion': _direccionController.text,
      'categoria': _categoriaSeleccionada.name,
      'urgencia': _urgenciaSeleccionada,
      'fechaCreacion': DateTime.now().toIso8601String(),
    };

    final exito = await context.read<TrabajoProvider>().crearTrabajo(datos);

    if (mounted) {
      setState(() => _enviando = false);
      if (exito) {
        Navigator.pop(context);
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Incidencia enviada correctamente')),
        );
      } else {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Error al enviar la incidencia')),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Nueva Incidencia')),
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
                decoration:
                    const InputDecoration(labelText: 'Dirección del servicio'),
                validator: (v) => v!.isEmpty ? 'Campo obligatorio' : null,
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

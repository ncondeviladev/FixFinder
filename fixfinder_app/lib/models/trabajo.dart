import 'usuario.dart';
import 'presupuesto.dart';

/// Define los posibles estados por los que puede pasar una incidencia.
/// Controla la lógica de qué acciones puede realizar el usuario sobre el trabajo.
enum EstadoTrabajo {
  PENDIENTE,
  PRESUPUESTADO,
  ACEPTADO,
  ASIGNADO,
  REALIZADO,
  FINALIZADO,
  PAGADO,
  CANCELADO
}

/// Categorías de servicios técnicos ofrecidos en la plataforma.
enum CategoriaServicio {
  ELECTRICIDAD,
  FONTANERIA,
  CLIMATIZACION,
  ALBANILERIA,
  PINTURA,
  CERRAJERIA,
  LIMPIEZA,
  OTROS
}

/// Almacena coordenadas geográficas asociadas a una solicitud de trabajo.
class Ubicacion {
  final double lat;
  final double lon;

  Ubicacion({required this.lat, required this.lon});

  factory Ubicacion.fromJson(Map<String, dynamic> json) {
    return Ubicacion(
      lat: (json['lat'] as num).toDouble(),
      lon: (json['lon'] as num).toDouble(),
    );
  }

  Map<String, dynamic> toJson() => {'lat': lat, 'lon': lon};
}

/// Modelo central del sistema que representa una "Incidencia" o "Trabajo".
/// 
/// Vincula al cliente con el servicio solicitado, el estado actual,
/// los presupuestos recibidos y el operario que finalmente lo realiza.
class Trabajo {
  final int id;
  final int idCliente;
  final int? idOperario;
  final String titulo;
  final CategoriaServicio categoria;
  final String descripcion;
  final String direccion;
  final int urgencia; // Nivel de prioridad (1-5)
  final EstadoTrabajo estado;
  final List<String> urlsFotos;
  final Ubicacion? ubicacion;
  final int valoracion; // Estrellas otorgadas por el cliente (0-5)
  final String? comentarioCliente;
  final String? fechaFinalizacion;
  
  // Objetos relacionados (hidratados por el servidor en listados detallados)
  final Usuario? cliente;
  final Usuario? operarioAsignado;
  final Presupuesto? presupuesto;
  final List<Presupuesto> presupuestos;

  Trabajo({
    required this.id,
    required this.idCliente,
    this.idOperario,
    required this.titulo,
    required this.categoria,
    required this.descripcion,
    required this.direccion,
    required this.urgencia,
    required this.estado,
    required this.urlsFotos,
    this.ubicacion,
    this.valoracion = 0,
    this.comentarioCliente,
    this.fechaFinalizacion,
    this.cliente,
    this.operarioAsignado,
    this.presupuesto,
    this.presupuestos = const [],
  });

  /// Mapea el JSON del servidor a una instancia de Trabajo.
  /// Maneja fallbacks para nombres de campos que varían entre la BD y el JSON de red.
  factory Trabajo.fromJson(Map<String, dynamic> json) {
    return Trabajo(
      id: json['id'],
      idCliente: json['id_cliente'] != null
          ? json['id_cliente'] as int
          : (json['idCliente'] != null ? json['idCliente'] as int : 0),
      idOperario: json['id_operario'] ?? json['idOperario'],
      titulo: json['titulo'] ?? 'Sin título',
      categoria: _parseCategoria(json['categoria']),
      descripcion: json['descripcion'] ?? '',
      direccion:
          json['direccion'] ?? json['direccionCliente'] ?? 'No especificada',
      urgencia: json['urgencia'] ?? 1,
      estado: _parseEstado(json['estado']),
      urlsFotos:
          List<String>.from(json['urls_fotos'] ?? json['urlsFotos'] ?? []),
      ubicacion: json['ubicacion'] != null && json['ubicacion']['lat'] != null
          ? Ubicacion.fromJson(json['ubicacion'])
          : null,
      valoracion: json['valoracion'] is int
          ? json['valoracion']
          : (int.tryParse(json['valoracion']?.toString() ?? '0') ?? 0),
      comentarioCliente: json['comentarioCliente']?.toString(),
      fechaFinalizacion: json['fechaFinalizacion']?.toString(),
      cliente:
          json['cliente'] != null ? Usuario.fromJson(json['cliente']) : null,
      operarioAsignado: json['operarioAsignado'] != null
          ? Usuario.fromJson(json['operarioAsignado'])
          : null,
      presupuesto: json['presupuesto'] != null
          ? Presupuesto.fromJson(json['presupuesto'])
          : null,
      presupuestos: (json['presupuestos'] as List? ?? [])
          .map((p) => Presupuesto.fromJson(p))
          .toList(),
    );
  }

  /// Convierte el estado representado como String en el backend al Enum de la aplicación.
  /// 
  /// Si el valor no coincide con ningún estado conocido, se establece como PENDIENTE por defecto.
  static EstadoTrabajo _parseEstado(String estado) {
    return EstadoTrabajo.values.firstWhere(
      (e) => e.name == estado.toUpperCase(),
      orElse: () => EstadoTrabajo.PENDIENTE,
    );
  }

  /// Convierte la categoría recibida como String al Enum de la aplicación.
  /// 
  /// Garantiza la integridad de los datos incluso si el servidor envía valores inesperados.
  static CategoriaServicio _parseCategoria(String? categoria) {
    if (categoria == null) return CategoriaServicio.OTROS;
    return CategoriaServicio.values.firstWhere(
      (e) => e.name == categoria.toUpperCase(),
      orElse: () => CategoriaServicio.OTROS,
    );
  }

  /// Serialización para envío o persistencia local.
  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'id_cliente': idCliente,
      'id_operario': idOperario,
      'titulo': titulo,
      'categoria': categoria.name,
      'descripcion': descripcion,
      'direccion': direccion,
      'urgencia': urgencia, 
      'estado': estado.name,
      'urls_fotos': urlsFotos,
      'ubicacion': ubicacion?.toJson(),
      'valoracion': valoracion,
      'comentarioCliente': comentarioCliente,
      'fechaFinalizacion': fechaFinalizacion,
      'presupuesto': presupuesto?.toJson(),
      'presupuestos': presupuestos.map((p) => p.toJson()).toList(),
    };
  }
}

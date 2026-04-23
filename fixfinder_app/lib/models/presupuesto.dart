/// Representa una oferta económica o estimación de costes vinculada a un trabajo.
/// 
/// Incluye la información de la empresa que emite el presupuesto para que
/// el cliente pueda contactar con ellos directamente.
class Presupuesto {
  final int id;
  final double monto;
  final String estado;
  final String? fechaEnvio;
  final String? notas;
  
  // Información de la empresa (se aplana desde el JSON para acceso rápido en la UI)
  final String? nombreEmpresa;
  final String? emailEmpresa;
  final String? telefonoEmpresa;
  final String? direccionEmpresa;
  final String? cifEmpresa;
  final String? urlFotoEmpresa;

  Presupuesto({
    required this.id,
    required this.monto,
    required this.estado,
    this.fechaEnvio,
    this.notas,
    this.nombreEmpresa,
    this.emailEmpresa,
    this.telefonoEmpresa,
    this.direccionEmpresa,
    this.cifEmpresa,
    this.urlFotoEmpresa,
  });

  /// Construye un presupuesto a partir de un mapa de datos JSON del servidor.
  /// 
  /// Gestiona de forma flexible diferentes nombres de campos (monto/precioTotal)
  /// para garantizar la compatibilidad con distintas versiones del backend.
  factory Presupuesto.fromJson(Map<String, dynamic> json) {
    return Presupuesto(
      id: json['id'],
      monto: (json['monto'] ?? json['precioTotal'] ?? 0).toDouble(), 
      estado: json['estado'] ?? 'PENDIENTE',
      fechaEnvio: json['fechaEnvio'] ?? json['fechaValidez'],
      notas: json['notas'] ?? json['detalles'],
      nombreEmpresa:
          json['empresa'] != null ? json['empresa']['nombre'] : 'Empresa',
      emailEmpresa: json['empresa'] != null ? json['empresa']['email'] : null,
      telefonoEmpresa:
          json['empresa'] != null ? json['empresa']['telefono'] : null,
      direccionEmpresa:
          json['empresa'] != null ? json['empresa']['direccion'] : null,
      cifEmpresa: json['empresa'] != null ? json['empresa']['cif'] : null,
      urlFotoEmpresa: json['empresa'] != null ? json['empresa']['url_foto'] : null,
    );
  }

  /// Serializa el presupuesto a JSON para su persistencia o envío.
  /// 
  /// Nota: Los datos de la empresa no se serializan por ser informativos y de solo lectura.
  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'monto': monto,
      'estado': estado,
      'fechaEnvio': fechaEnvio,
      'notas': notas,
    };
  }
}

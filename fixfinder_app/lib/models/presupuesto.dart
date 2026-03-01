// Modelo de datos para Presupuesto.
// Contiene la estimación de costes o facturación asociada a un trabajo.
class Presupuesto {
  final int id;
  final double monto;
  final String estado;
  final String? fechaEnvio;
  final String? notas;
  final String? nombreEmpresa;
  final String? emailEmpresa;
  final String? telefonoEmpresa;

  Presupuesto({
    required this.id,
    required this.monto,
    required this.estado,
    this.fechaEnvio,
    this.notas,
    this.nombreEmpresa,
    this.emailEmpresa,
    this.telefonoEmpresa,
  });

  factory Presupuesto.fromJson(Map<String, dynamic> json) {
    return Presupuesto(
      id: json['id'],
      monto: (json['monto'] ?? json['precioTotal'] ?? 0)
          .toDouble(), // Soporta 'monto' y 'precioTotal'
      estado: json['estado'] ?? 'PENDIENTE',
      fechaEnvio: json['fechaEnvio'] ?? json['fechaValidez'], // Soporta ambos
      notas: json['notas'] ?? json['detalles'], // Soporta 'notas' y 'detalles'
      nombreEmpresa:
          json['empresa'] != null ? json['empresa']['nombre'] : 'Empresa',
      emailEmpresa: json['empresa'] != null ? json['empresa']['email'] : null,
      telefonoEmpresa:
          json['empresa'] != null ? json['empresa']['telefono'] : null,
    );
  }
  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'monto': monto,
      'estado': estado,
      'fechaEnvio': fechaEnvio,
      'notas': notas,
      // No re-exportamos info de empresa para no redundar o porque no se requiere al enviar
    };
  }
}

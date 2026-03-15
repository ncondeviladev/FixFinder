// Modelo de datos para Usuario.
// Define los diferentes roles (Cliente, Operario, etc.) e información personal.
enum Rol { ADMIN, GERENTE, OPERARIO, CLIENTE }

class Usuario {
  final int id;
  final String email;
  final String nombreCompleto;
  final Rol rol;
  final String? token;
  final String? telefono;
  final String? direccion;
  final String? dni;
  String? urlFoto;
  final String? fechaRegistro;

  Usuario({
    required this.id,
    required this.email,
    required this.nombreCompleto,
    required this.rol,
    this.token,
    this.telefono,
    this.direccion,
    this.dni,
    this.urlFoto,
    this.fechaRegistro,
  });

  factory Usuario.fromJson(Map<String, dynamic> json) {
    return Usuario(
      id: json['id'],
      email: json['email'],
      nombreCompleto: json['nombreCompleto'] ??
          json['nombre_completo'] ??
          json['nombre'] ??
          '',
      rol: _parseRol(json['rol']),
      token: json['token'],
      telefono: json['telefono'],
      direccion: json['direccion'],
      dni: json['dni'],
      urlFoto: _sanitizarUrl(json['url_foto'] ?? json['urlFoto']),
      fechaRegistro: json['fecha_registro'] ?? json['fechaRegistro'],
    );
  }

  static String? _sanitizarUrl(String? url) {
    if (url == null || url.trim().isEmpty) return null;
    if (url.startsWith('http://') || url.startsWith('https://')) return url;
    return null;
  }

  static Rol _parseRol(String? rol) {
    if (rol == null) return Rol.CLIENTE;
    return Rol.values.firstWhere(
      (e) => e.name == rol.toUpperCase(),
      orElse: () => Rol.CLIENTE,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'email': email,
      'nombreCompleto': nombreCompleto,
      'rol': rol.name,
      'token': token,
      'telefono': telefono,
      'direccion': direccion,
      'dni': dni,
      'urlFoto': urlFoto,
      'fechaRegistro': fechaRegistro,
    };
  }
}

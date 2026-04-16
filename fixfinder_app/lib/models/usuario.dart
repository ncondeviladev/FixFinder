/// Roles disponibles en el sistema. Determina los permisos y vistas en la App.
enum Rol { ADMIN, GERENTE, OPERARIO, CLIENTE }

/// Representa a un usuario autenticado en el sistema FixFinder.
/// 
/// Contiene la información personal, el rol asignado y el token de sesión
/// activo para las peticiones al servidor Java.
class Usuario {
  /// Identificador único del usuario en la base de datos central.
  final int id;
  
  /// Dirección de correo electrónico (se utiliza como identificador de sesión).
  final String email;
  
  /// Nombre y apellidos para identificación visual en la plataforma.
  final String nombreCompleto;
  
  /// Tipo de cuenta (Cliente, Operario, etc.) que determina los permisos.
  final Rol rol;
  
  /// Token JWT o identificador de sesión único para autorizar peticiones Socket.
  final String? token;
  
  /// Número de contacto telefónico para coordinación de servicios.
  final String? telefono;
  
  /// Localización física registrada para el envío de suministros o servicios.
  final String? direccion;
  
  /// Documento Nacional de Identidad o equivalente legal.
  final String? dni;
  
  /// Enlace público a la imagen de avatar en Firebase Storage.
  String? urlFoto;
  
  /// Marca de tiempo de la creación de la cuenta en formato ISO8601.
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

  /// Factory para construir el usuario desde un mapa JSON recibido por el socket.
  /// Soporta diferentes snake_case y camelCase para compatibilidad con el servidor.
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

  /// Valida que la URL de la foto sea una dirección HTTP/S válida.
  static String? _sanitizarUrl(String? url) {
    if (url == null || url.trim().isEmpty) return null;
    if (url.startsWith('http://') || url.startsWith('https://')) return url;
    return null;
  }

  /// Convierte el String del rol recibido desde el backend en el Enum correspondiente.
  /// 
  /// Por defecto asigna el rol CLIENTE si el valor es nulo o desconocido.
  static Rol _parseRol(String? rol) {
    if (rol == null) return Rol.CLIENTE;
    return Rol.values.firstWhere(
      (e) => e.name == rol.toUpperCase(),
      orElse: () => Rol.CLIENTE,
    );
  }

  /// Serializa el objeto a JSON para persistencia local o envío al servidor.
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

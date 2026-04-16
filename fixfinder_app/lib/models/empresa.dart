/// Modelo para representar una Empresa colaboradora en FixFinder.
/// 
/// Contiene la ficha técnica del negocio, contacto y sus áreas de especialización.
class Empresa {
  final int id;
  final String nombre;
  final String cif;
  final String direccion;
  final String telefono;
  final String email;
  final String? urlFoto;
  final List<String> especialidades;

  Empresa({
    required this.id,
    required this.nombre,
    required this.cif,
    required this.direccion,
    required this.telefono,
    required this.email,
    this.urlFoto,
    required this.especialidades,
  });

  /// Construye la entidad Empresa a partir de la respuesta del servidor.
  /// 
  /// Asegura valores por defecto para campos obligatorios en la UI,
  /// evitando nulos inesperados durante el renderizado.
  factory Empresa.fromJson(Map<String, dynamic> json) {
    return Empresa(
      id: json['id'],
      nombre: json['nombre'] ?? 'Sin nombre',
      cif: json['cif'] ?? '',
      direccion: json['direccion'] ?? '',
      telefono: json['telefono'] ?? '',
      email: json['email'] ?? '',
      urlFoto: json['url_foto'],
      especialidades: List<String>.from(json['especialidades'] ?? []),
    );
  }
}

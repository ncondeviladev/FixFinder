// Modelo de datos para Empresa colaboradora.
// Información pública y de contacto sobre el negocio que presta el servicio.
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

import 'dart:io';
import 'package:firebase_storage/firebase_storage.dart';
import 'package:image_picker/image_picker.dart';
import 'package:flutter/foundation.dart';

/// Servicio central para la gestión de imágenes (Selección y Subida a Firebase).
class ImageService {
  static final ImageService _instance = ImageService._internal();
  factory ImageService() => _instance;
  ImageService._internal();

  final ImagePicker _picker = ImagePicker();
  final FirebaseStorage _storage = FirebaseStorage.instance;

  /// Abre la galería para seleccionar una imagen.
  Future<File?> elegirImagen({ImageSource source = ImageSource.gallery}) async {
    try {
      final XFile? pick = await _picker.pickImage(
        source: source,
        maxWidth: 1024,
        maxHeight: 1024,
        imageQuality: 85,
      );
      if (pick == null) return null;
      return File(pick.path);
    } catch (e) {
      debugPrint('[ImageService] Error seleccionando imagen: $e');
      return null;
    }
  }

  /// Sube un archivo a Firebase Storage y devuelve la URL descargable.
  /// 
  /// @param archivo El archivo File local.
  /// @param carpeta Carpeta destino en Storage (ej: 'perfiles').
  /// @param nombreArchivo Nombre único del archivo.
  Future<String?> subirImagen(File archivo, String carpeta, String nombreArchivo) async {
    try {
      final String extension = archivo.path.split('.').last;
      final String ruta = '$carpeta/$nombreArchivo.$extension';
      final Reference ref = _storage.ref().child(ruta);
      
      final UploadTask task = ref.putFile(archivo);
      final TaskSnapshot snap = await task;
      
      final String url = await snap.ref.getDownloadURL();
      return url;
    } catch (e) {
      debugPrint('[ImageService] Error al subir a Firebase Storage: $e');
      return null;
    }
  }
}

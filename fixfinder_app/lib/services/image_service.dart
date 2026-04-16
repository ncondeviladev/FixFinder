import 'dart:io';
import 'package:firebase_storage/firebase_storage.dart';
import 'package:image_picker/image_picker.dart';
import 'package:flutter/foundation.dart';

/// Servicio central para la gestión de imágenes (Selección y Subida a Firebase).
/// 
/// Administra la interacción con el selector de archivos del sistema y la transferencia
/// de datos binarios hacia el almacenamiento en la nube para evidencias de trabajos.
class ImageService {
  /// Singleton para acceso global al servicio de imágenes.
  static final ImageService _instance = ImageService._internal();
  
  /// Constructor alternativo (tipo Factory).
  factory ImageService() => _instance;
  
  /// Constructor privado para el patrón Singleton.
  ImageService._internal();

  /// Controlador para la interacción con la galería/cámara del dispositivo.
  final ImagePicker _picker = ImagePicker();
  
  /// Instancia de acceso al almacenamiento en la nube de Firebase.
  final FirebaseStorage _storage = FirebaseStorage.instance;

  /// Inicia el selector de imágenes del sistema (Galería o Cámara).
  /// 
  /// Aplica una pre-optimización de tamaño y calidad para reducir el consumo de red.
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

  /// Transfiere un archivo local a un bucket de Firebase Storage.
  /// 
  /// @param archivo Instancia del archivo File local a subir.
  /// @param carpeta Ruta virtual en el almacenamiento (ej: 'trabajos/id_1').
  /// @param nombreArchivo Identificador único para el archivo en la nube.
  /// 
  /// Devuelve la URL pública de acceso si la transferencia es exitosa.
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

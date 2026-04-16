import 'package:url_launcher/url_launcher.dart';
import 'dart:io';

class ExternalLauncherService {
  /// Abre el marcador telefónico con el número especificado.
  static Future<void> llamarTelefono(String? telefono) async {
    if (telefono == null || telefono.isEmpty) return;
    
    // Limpiamos el número de cualquier carácter que no sea dígito o '+'
    final String limpio = telefono.replaceAll(RegExp(r'[^0-9+]'), '');
    final Uri url = Uri.parse('tel:$limpio');
    
    try {
      if (await canLaunchUrl(url)) {
        await launchUrl(url);
      } else {
        print('No se pudo lanzar el marcador para: $telefono');
      }
    } catch (e) {
      print('Error al intentar llamar: $e');
    }
  }

  /// Abre la aplicación de mapas nativa con la dirección proporcionada.
  /// En Android abre Google Maps, en iOS abre Apple Maps.
  static Future<void> abrirMapa(String? direccion) async {
    if (direccion == null || direccion.isEmpty) return;

    final String query = Uri.encodeComponent(direccion);
    Uri url;

    if (Platform.isAndroid) {
      url = Uri.parse('https://www.google.com/maps/search/?api=1&query=$query');
    } else {
      url = Uri.parse('https://maps.apple.com/?q=$query');
    }

    try {
      if (await canLaunchUrl(url)) {
        await launchUrl(url, mode: LaunchMode.externalApplication);
      } else {
        // Fallback: Intentar abrir en el navegador si la app nativa no responde
        await launchUrl(url, mode: LaunchMode.platformDefault);
      }
    } catch (e) {
      print('Error al intentar abrir el mapa: $e');
    }
  }
}

import 'package:intl/intl.dart';

/**
 * Utilidades para el formateo de fechas consistente en toda la App FixFinder.
 * Sigue el estándar dd/MM/yyyy HH:mm
 */
class DateFormatUtils {
  
  /// Formatea una cadena ISO (ej: 2026-04-17T12:04:16) a formato legible europeo.
  static String formatIsoString(String? isoString) {
    if (isoString == null || isoString.isEmpty) return 'No disp.';
    
    try {
      // Intentar parsear el formato ISO
      DateTime dt = DateTime.parse(isoString.replaceAll(' ', 'T'));
      return DateFormat('dd/MM/yyyy HH:mm').format(dt);
    } catch (e) {
      // Fallback manual si el parseo falla
      return isoString.replaceFirst('T', ' ');
    }
  }

  /// Formatea un objeto DateTime.
  static String formatDateTime(DateTime? dt) {
    if (dt == null) return 'No disp.';
    return DateFormat('dd/MM/yyyy HH:mm').format(dt);
  }
}

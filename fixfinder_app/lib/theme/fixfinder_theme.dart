import 'package:flutter/material.dart';

/// Clase centralizada para el manejo del tema visual de FixFinder.
/// 
/// Define los colores corporativos (Naranja Técnico y escalas de grises oscuros)
/// y estandariza el estilo de los componentes (Card, AppBar, Inputs).
class FixFinderTheme {
  // Paleta de colores principal
  static const Color primaryColor = Color(0xFFFF6D00); // Naranja técnico
  static const Color accentColor = Color(0xFFFFAB40);
  static const Color bgDark = Color(0xFF121212);
  static const Color surfaceDark = Color(0xFF1A1A1A);
  static const Color cardDark = Color(0xFF1E1E1E);
  static const Color dividerDark = Color(0xFF2A2A2A);
  static const Color inputBg = Color(0xFF242424);
  static const Color textMain = Color(0xFFEEEEEE);
  static const Color textMuted = Color(0xFF999999);
  static const Color errorColor = Color(0xFFEF5350);
  static const Color successColor = Color(0xFF4CAF50);
  static const Color infoColor = Color(0xFF2196F3);
  static const Color adminColor = Color(0xFF9C27B0);

  // Colores para bloques de descripción (Historial Visual)
  static const Color bgBlockCliente = Color(0xFF2E3B4E);
  static const Color bgBlockGerente = Color(0xFF3E3B2E);
  static const Color bgBlockOperario = Color(0xFF2E3E34);
  static const Color bgBlockDefault = Color(0xFF2A2A2A);

  /// Genera el tema oscuro (Dark Mode) de la aplicación.
  static ThemeData get darkTheme {
    return ThemeData(
      useMaterial3: true,
      brightness: Brightness.dark,
      scaffoldBackgroundColor: bgDark,
      cardColor: cardDark,
      dividerColor: dividerDark,
      
      colorScheme: const ColorScheme.dark(
        primary: primaryColor,
        onPrimary: Colors.white,
        secondary: accentColor,
        onSecondary: Colors.black,
        surface: surfaceDark,
        onSurface: textMain,
        error: errorColor,
        tertiary: Color(0xFF6366F1),
        surfaceContainerHighest: Color(0xFF242424),
      ),

      appBarTheme: const AppBarTheme(
        backgroundColor: surfaceDark,
        foregroundColor: primaryColor,
        elevation: 0,
        centerTitle: true,
        titleTextStyle: TextStyle(
          color: primaryColor,
          fontSize: 20,
          fontWeight: FontWeight.bold,
        ),
      ),

      cardTheme: CardThemeData(
        color: cardDark,
        elevation: 2,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(12),
          side: const BorderSide(color: dividerDark),
        ),
      ),

      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          backgroundColor: primaryColor,
          foregroundColor: Colors.white,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(10),
          ),
        ),
      ),

      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: inputBg,
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(10),
          borderSide: const BorderSide(color: Color(0xFF3A3A3A)),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(10),
          borderSide: const BorderSide(color: Color(0xFF3A3A3A)),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(10),
          borderSide: const BorderSide(color: primaryColor, width: 2),
        ),
        labelStyle: const TextStyle(color: Color(0xFFAAAAAA)),
        prefixIconColor: primaryColor,
        suffixIconColor: primaryColor,
      ),

      textTheme: const TextTheme(
        bodyLarge: TextStyle(color: textMain),
        bodyMedium: TextStyle(color: Color(0xFFCCCCCC)),
        bodySmall: TextStyle(color: textMuted),
        titleLarge: TextStyle(color: Colors.white, fontWeight: FontWeight.bold),
        titleMedium: TextStyle(color: textMain),
      ),

      iconTheme: const IconThemeData(color: primaryColor),
      
      listTileTheme: const ListTileThemeData(
        textColor: textMain,
        iconColor: primaryColor,
      ),

      chipTheme: ChipThemeData(
        backgroundColor: inputBg,
        selectedColor: primaryColor,
        labelStyle: const TextStyle(color: textMain),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
      ),

      floatingActionButtonTheme: const FloatingActionButtonThemeData(
        backgroundColor: primaryColor,
        foregroundColor: Colors.white,
      ),
    );
  }
}

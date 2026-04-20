package com.fixfinder.ui.dashboard.componentes.tabla;

import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

/**
 * Clase de utilidad que centraliza la creación de componentes visuales comunes
 * y lógica de iconos/estilos para la tabla de incidencias del Dashboard.
 * 
 * Proporciona métodos estáticos para generar avatares, etiquetas de iconos
 * y mapeo de categorías a emojis.
 */
public class UtilidadesTabla {

    /**
     * Genera un pequeño círculo con las iniciales del nombre del usuario
     * actuando como un avatar genérico.
     * 
     * @param nombre El nombre completo del usuario.
     * @return Un StackPane que contiene el avatar visual.
     */
    public static StackPane generarMiniAvatar(String nombre) {
        StackPane avatar = new StackPane();
        avatar.setMinSize(24, 24);
        avatar.setMaxSize(24, 24);
        // Naranja corporativo suave con bordes redondeados
        avatar.getStyleClass().add("contenedor-mini-avatar");
        avatar.setStyle("-fx-background-radius: 12;"); // El radio depende del tamaño fijo aquí
        
        String[] partes = nombre.trim().split("\\s+");
        String iniciales = partes.length >= 2 
            ? ("" + partes[0].charAt(0) + partes[1].charAt(0)).toUpperCase()
            : nombre.substring(0, Math.min(2, nombre.length())).toUpperCase();
            
        Label lblIniciales = new Label(iniciales);
        lblIniciales.getStyleClass().add("iniciales-avatar");
        lblIniciales.setStyle("-fx-font-size: 9px;"); // Tamaño específico para mini avatar de tabla
        
        avatar.getChildren().add(lblIniciales);
        return avatar;
    }

    /**
     * Devuelve el emoji representativo para una categoría de servicio.
     * 
     * @param categoria El nombre de la categoría (ej: FONTANERIA).
     * @return El emoji correspondiente como String.
     */
    public static String obtenerIconoCategoria(String categoria) {
        if (categoria == null) return "🔧";
        
        return switch (categoria.toUpperCase()) {
            case "ELECTRICIDAD" -> "⚡";
            case "FONTANERIA" -> "💧";
            case "CLIMATIZACION" -> "❄";
            case "PINTURA" -> "🖌";
            case "ALBANILERIA" -> "🧱";
            case "LIMPIEZA" -> "🧹";
            case "CERRAJERIA" -> "🔑";
            default -> "🔧";
        };
    }

    /**
     * Crea un Label estilizado con un color y tamaño de fuente específico.
     * 
     * @param texto El contenido del Label.
     * @param colorHex El color en formato Hexadecimal (ej: #FFFFFF).
     * @param tamanio El tamaño de la fuente en píxeles.
     * @return El Label configurado.
     */
    public static Label crearEtiquetaEstilizada(String texto, String colorHex, double tamanio) {
        Label etiqueta = new Label(texto);
        etiqueta.getStyleClass().add("texto-tenue");
        if (tamanio > 0) etiqueta.setStyle("-fx-font-size: " + tamanio + "px;");
        return etiqueta;
    }
}

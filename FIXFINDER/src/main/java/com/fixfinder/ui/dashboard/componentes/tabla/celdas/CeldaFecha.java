package com.fixfinder.ui.dashboard.componentes.tabla.celdas;

import com.fixfinder.ui.dashboard.modelos.TrabajoFX;
import javafx.scene.control.TableCell;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Celda personalizada que formatea una fecha en formato ISO (2026-04-02T...)
 * a un formato humano legible (dd/MM/yyyy HH:mm).
 */
public class CeldaFecha extends TableCell<TrabajoFX, String> {

    private static final DateTimeFormatter FORMATO_ENTRADA = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter FORMATO_SALIDA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    protected void updateItem(String fechaIso, boolean vacio) {
        super.updateItem(fechaIso, vacio);

        if (vacio || fechaIso == null || fechaIso.isBlank()) {
            setText(null);
            setGraphic(null);
            return;
        }

        try {
            // Intentamos parsear la fecha ISO y transformarla
            LocalDateTime fecha = LocalDateTime.parse(fechaIso, FORMATO_ENTRADA);
            String fechaFormateada = fecha.format(FORMATO_SALIDA);
            setText(fechaFormateada);
            getStyleClass().add("texto-tenue-pequeno");
        } catch (Exception e) {
            // Si el formato no es ISO o falla, mostramos el texto original
            setText(fechaIso);
        }
    }
}

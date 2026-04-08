package com.fixfinder.ui.dashboard.componentes.tabla.celdas;

import com.fixfinder.ui.dashboard.modelos.TrabajoFX;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;

/**
 * Celda personalizada que renderiza el estado de un trabajo dentro de un "Badge"
 * (chip con color de fondo) para una mejor identificación visual.
 */
public class CeldaEstadoBadge extends TableCell<TrabajoFX, String> {

    @Override
    protected void updateItem(String estado, boolean vacio) {
        super.updateItem(estado, vacio);

        if (vacio || estado == null) {
            setGraphic(null);
            return;
        }

        Label badge = new Label(estado);
        // Aplica las clases CSS definidas en dashboard-principal.css
        // Ej: .badge y .badge-pendiente, .badge-aceptado, etc.
        badge.getStyleClass().addAll("badge", "badge-" + estado.toLowerCase());
        
        setGraphic(badge);
        setText(null);
    }
}

package com.fixfinder.ui.dashboard.componentes.tabla.celdas;

import com.fixfinder.ui.dashboard.componentes.tabla.UtilidadesTabla;
import com.fixfinder.ui.dashboard.modelos.TrabajoFX;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.layout.HBox;

/**
 * Celda personalizada que muestra el operario asignado junto a su avatar circular.
 * Si no hay operario, muestra el texto en cursiva y color gris.
 */
public class CeldaOperario extends TableCell<TrabajoFX, String> {

    @Override
    protected void updateItem(String nombreOperario, boolean vacio) {
        super.updateItem(nombreOperario, vacio);

        if (vacio || nombreOperario == null) {
            setGraphic(null);
            return;
        }

        if ("Sin asignar".equalsIgnoreCase(nombreOperario)) {
            Label lblSinAsignar = new Label(nombreOperario);
            lblSinAsignar.getStyleClass().add("texto-sin-asignar");
            setGraphic(lblSinAsignar);
        } else {
            HBox contenedor = new HBox(8);
            contenedor.setAlignment(Pos.CENTER_LEFT);
            contenedor.getChildren().add(
                UtilidadesTabla.crearEtiquetaEstilizada(nombreOperario, "#F8FAFC", 12.5)
            );
            setGraphic(contenedor);
        }
        setText(null);
    }
}

package com.fixfinder.ui.dashboard.componentes.tabla.celdas;

import com.fixfinder.ui.dashboard.componentes.tabla.UtilidadesTabla;
import com.fixfinder.ui.dashboard.modelos.TrabajoFX;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.layout.HBox;

/**
 * Celda personalizada que muestra la categoría del servicio mediante un emoji
 * y el nombre formateado.
 */
public class CeldaCategoria extends TableCell<TrabajoFX, String> {

    @Override
    protected void updateItem(String categoria, boolean vacio) {
        super.updateItem(categoria, vacio);

        if (vacio || categoria == null) {
            setGraphic(null);
            return;
        }

        HBox contenedor = new HBox(5);
        contenedor.setAlignment(Pos.CENTER_LEFT);
        
        // Icono (Emoji)
        Label lblIcono = new Label(UtilidadesTabla.obtenerIconoCategoria(categoria));
        lblIcono.getStyleClass().add("texto-tenue");
        
        // Texto con la primera letra en mayúscula
        String texto = categoria.charAt(0) + categoria.substring(1).toLowerCase();
        Label lblTexto = new Label(texto);
        lblTexto.getStyleClass().add("etiqueta-categoria");
        
        contenedor.getChildren().addAll(lblIcono, lblTexto);
        setGraphic(contenedor);
        setText(null);
    }
}

package com.fixfinder.ui.dashboard.componentes.tabla.celdas;

import com.fixfinder.ui.dashboard.modelos.TrabajoFX;
import com.fixfinder.ui.dashboard.dialogos.DialogoFichaCliente;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TableCell;

/**
 * Celda personalizada que muestra el nombre del cliente como un enlace interactivo.
 * Al hacer clic, se abre la ficha de detalle del cliente.
 */
public class CeldaCliente extends TableCell<TrabajoFX, String> {

    private final String cssUrl;

    public CeldaCliente(String cssUrl) {
        this.cssUrl = cssUrl;
    }

    @Override
    protected void updateItem(String nombreCliente, boolean vacio) {
        super.updateItem(nombreCliente, vacio);

        if (vacio || nombreCliente == null) {
            setGraphic(null);
            return;
        }

        Hyperlink enlace = new Hyperlink(nombreCliente);
        enlace.getStyleClass().add("enlace-cliente");
        
        enlace.setOnAction(e -> {
            TrabajoFX trabajo = getTableView().getItems().get(getIndex());
            new DialogoFichaCliente(cssUrl).mostrar(trabajo);
        });

        setGraphic(enlace);
        setText(null);
    }
}

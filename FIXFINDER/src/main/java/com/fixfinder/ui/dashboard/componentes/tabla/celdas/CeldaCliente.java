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
        // Estilo manual para mantener el diseño premium de la marca
        enlace.setStyle("-fx-text-fill: #38BDF8; -fx-underline: false; -fx-padding: 0;");
        
        enlace.setOnAction(e -> {
            TrabajoFX trabajo = getTableView().getItems().get(getIndex());
            new DialogoFichaCliente(cssUrl).mostrar(trabajo);
        });

        // Efectos de hover para mejorar la UX
        enlace.setOnMouseEntered(e -> 
            enlace.setStyle("-fx-text-fill: #7DD3FC; -fx-underline: true; -fx-padding: 0;"));
        enlace.setOnMouseExited(e -> 
            enlace.setStyle("-fx-text-fill: #38BDF8; -fx-underline: false; -fx-padding: 0;"));

        setGraphic(enlace);
        setText(null);
    }
}

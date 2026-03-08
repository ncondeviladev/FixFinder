package com.fixfinder.ui.dashboard.vistas;

import com.fixfinder.ui.dashboard.componentes.TablaIncidencias;
import com.fixfinder.ui.dashboard.modelos.OperarioFX;
import com.fixfinder.ui.dashboard.modelos.TrabajoFX;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.layout.*;

/**
 * Vista de la sección "Incidencias" del Dashboard.
 * Envuelve la TablaIncidencias con su layout de página completa.
 */
public class VistaIncidencias extends VBox {

    public VistaIncidencias(FilteredList<TrabajoFX> trabajos,
            ObservableList<OperarioFX> operarios,
            TablaIncidencias.AccionesCallback callback,
            String cssUrl) {
        getStyleClass().add("content-area");
        VBox.setVgrow(this, Priority.ALWAYS);

        TablaIncidencias tabla = new TablaIncidencias(trabajos, operarios, callback, cssUrl);
        VBox.setVgrow(tabla, Priority.ALWAYS);

        VBox body = new VBox();
        body.setPadding(new Insets(20, 24, 24, 24));
        VBox.setVgrow(body, Priority.ALWAYS);
        body.getChildren().add(tabla);

        VBox.setVgrow(body, Priority.ALWAYS);
        getChildren().add(body);
    }
}

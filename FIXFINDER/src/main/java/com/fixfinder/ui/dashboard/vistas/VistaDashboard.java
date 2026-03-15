package com.fixfinder.ui.dashboard.vistas;

import com.fixfinder.ui.dashboard.componentes.FilaIndicadores;
import com.fixfinder.ui.dashboard.componentes.PanelLateral;
import com.fixfinder.ui.dashboard.componentes.TablaIncidencias;
import com.fixfinder.ui.dashboard.modelos.OperarioFX;
import com.fixfinder.ui.dashboard.modelos.TrabajoFX;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;

/**
 * Vista principal del Dashboard: contiene la fila de KPIs, la tabla resumen de
 * incidencias
 * y el panel lateral de actividad reciente. Punto de entrada visual tras el
 * login.
 */
public class VistaDashboard extends VBox {

    private final FilaIndicadores filaIndicadores;
    private final PanelLateral panelLateral;

    public VistaDashboard(FilteredList<TrabajoFX> trabajos,
            ObservableList<OperarioFX> operarios,
            TablaIncidencias.AccionesCallback callback,
            Runnable onRefresh,
            String cssUrl) {
        getStyleClass().add("content-area");
        VBox.setVgrow(this, Priority.ALWAYS);

        filaIndicadores = new FilaIndicadores();
        panelLateral = new PanelLateral();

        HBox header = new HBox();
        header.setPadding(new Insets(0, 0, 10, 0));
        javafx.scene.control.Button btnRefresh = new javafx.scene.control.Button("🔄 Actualizar Datos");
        btnRefresh.getStyleClass().add("btn-secondary");

        btnRefresh.setOnAction(e -> {
            // Animación de pulso más elegante y discreta
            javafx.animation.ScaleTransition scale = new javafx.animation.ScaleTransition(
                    javafx.util.Duration.millis(200), btnRefresh);
            scale.setFromX(1.0);
            scale.setFromY(1.0);
            scale.setToX(1.05);
            scale.setToY(1.05);
            scale.setCycleCount(2);
            scale.setAutoReverse(true);
            scale.play();

            onRefresh.run();
        });
        header.getChildren().add(btnRefresh);

        TablaIncidencias tabla = new TablaIncidencias(trabajos, operarios, callback, cssUrl);
        HBox.setHgrow(tabla, Priority.ALWAYS);

        HBox mainZone = new HBox(16);
        VBox.setVgrow(mainZone, Priority.ALWAYS);
        mainZone.getChildren().addAll(tabla, panelLateral);

        VBox body = new VBox(16);
        body.setPadding(new Insets(20, 24, 24, 24));
        VBox.setVgrow(body, Priority.ALWAYS);
        body.getChildren().addAll(header, filaIndicadores, mainZone);

        ScrollPane scroll = new ScrollPane(body);
        scroll.getStyleClass().add("transparent-scroll");
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true); // Permitir que el contenido (tablas) se estire verticalmente
        VBox.setVgrow(scroll, Priority.ALWAYS);

        getChildren().add(scroll);
    }

    public void actualizarKpis(int activos, int pendientes, int completados, int presupuestados) {
        filaIndicadores.actualizar(activos, pendientes, completados, presupuestados);
        // Resumen lateral eliminado por petición del usuario
    }

    public PanelLateral getPanelLateral() {
        return panelLateral;
    }
}

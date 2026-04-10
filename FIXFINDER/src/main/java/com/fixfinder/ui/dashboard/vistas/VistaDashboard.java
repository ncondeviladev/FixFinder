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
 * Vista raíz del Dashboard.
 * Organiza los componentes de alto nivel: KPIs, Tabla y Panel Lateral con historial.
 * El Header ahora es global y reside en el controlador para persistencia.
 */
public class VistaDashboard extends VBox {

    private final FilaIndicadores filaIndicadores;
    private final TablaIncidencias tablaIncidencias;
    private final PanelLateral panelLateral;

    public VistaDashboard(FilteredList<TrabajoFX> trabajosFiltrados,
            ObservableList<OperarioFX> operarios,
            TablaIncidencias.AccionesCallback callback,
            String cssUrl,
            int idEmpresa) {
        
        getStyleClass().add("dashboard-view");
        
        this.filaIndicadores = new FilaIndicadores();
        this.panelLateral = new PanelLateral();
        this.tablaIncidencias = new TablaIncidencias(trabajosFiltrados, operarios, callback, cssUrl, idEmpresa);

        HBox.setHgrow(tablaIncidencias, Priority.ALWAYS);

        HBox mainZone = new HBox(16);
        VBox.setVgrow(mainZone, Priority.ALWAYS);
        mainZone.getChildren().addAll(tablaIncidencias, panelLateral);

        VBox body = new VBox(20); // Mayor espaciado entre KPIs y Tabla
        body.setPadding(new Insets(10, 24, 24, 24));
        VBox.setVgrow(body, Priority.ALWAYS);
        body.getChildren().addAll(filaIndicadores, mainZone);

        ScrollPane scroll = new ScrollPane(body);
        scroll.getStyleClass().add("transparent-scroll");
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        getChildren().add(scroll);
    }

    public void actualizarKpis(int activos, int pendientes, int completados, int presupuestados) {
        filaIndicadores.actualizar(activos, pendientes, completados, presupuestados);
    }

    public PanelLateral getPanelLateral() {
        return panelLateral;
    }

    public TablaIncidencias getTablaIncidencias() {
        return tablaIncidencias;
    }
}

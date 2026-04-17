package com.fixfinder.ui.dashboard.componentes;

import javafx.scene.control.Label;
import javafx.scene.layout.*;

/**
 * Fila horizontal de tarjetas KPI del Dashboard.
 */
public class FilaIndicadores extends HBox {

    private final Label lblActivos = new Label("0");
    private final Label lblPendientes = new Label("0");
    private final Label lblCompletados = new Label("0");
    private final Label lblPresupuestados = new Label("0");

    public FilaIndicadores() {
        setSpacing(16);
        getChildren().addAll(
                crearKpi("Incidencias Activas", lblActivos, "▲", "kpi-icon-orange", "#F97316"),
                crearKpi("Por Ofertar", lblPendientes, "!", "kpi-icon-amber", "#F59E0B"),
                crearKpi("Pujas Enviadas", lblPresupuestados, "$", "kpi-icon-teal", "#14B8A6"),
                crearKpi("Completadas", lblCompletados, "✓", "kpi-icon-green", "#22C55E"));
    }

    public void actualizar(int activos, int pendientes, int completados, int presupuestados) {
        lblActivos.setText(String.valueOf(activos));
        lblPendientes.setText(String.valueOf(pendientes));
        lblCompletados.setText(String.valueOf(completados));
        lblPresupuestados.setText(String.valueOf(presupuestados));
    }

    private VBox crearKpi(String titulo, Label lblValor, String icono, String claseIcono, String color) {
        VBox card = new VBox(8);
        card.getStyleClass().add("kpi-card");
        HBox.setHgrow(card, Priority.ALWAYS);

        HBox top = new HBox();
        VBox textos = new VBox(4);
        HBox.setHgrow(textos, Priority.ALWAYS);
        Label lTitulo = new Label(titulo);
        lTitulo.getStyleClass().add("kpi-label");
        lblValor.getStyleClass().add("kpi-value");
        textos.getChildren().addAll(lTitulo, lblValor);

        StackPane iconBox = new StackPane();
        iconBox.getStyleClass().addAll("kpi-icon-box", claseIcono);
        iconBox.setMinSize(40, 40);
        iconBox.setMaxSize(40, 40);
        Label lIcono = new Label(icono);
        lIcono.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 16px; -fx-font-weight: bold;");
        iconBox.getChildren().add(lIcono);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        top.getChildren().addAll(textos, spacer, iconBox);
        card.getChildren().add(top);
        return card;
    }
}

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
                crearKpi("Incidencias Activas", lblActivos, "▲", "icono-kpi-naranja", "#F97316"),
                crearKpi("Por Ofertar", lblPendientes, "!", "icono-kpi-ambar", "#F59E0B"),
                crearKpi("Pujas Enviadas", lblPresupuestados, "$", "icono-kpi-turquesa", "#14B8A6"),
                crearKpi("Completadas", lblCompletados, "✓", "icono-kpi-verde", "#22C55E"));
    }

    public void actualizar(int activos, int pendientes, int completados, int presupuestados) {
        lblActivos.setText(String.valueOf(activos));
        lblPendientes.setText(String.valueOf(pendientes));
        lblCompletados.setText(String.valueOf(completados));
        lblPresupuestados.setText(String.valueOf(presupuestados));
    }

    private VBox crearKpi(String titulo, Label lblValor, String icono, String claseIcono, String color) {
        VBox card = new VBox(8);
        card.getStyleClass().add("tarjeta-kpi");
        HBox.setHgrow(card, Priority.ALWAYS);

        HBox row = new HBox(); // Note: Changed top to row for clarity but kept structure
        VBox textos = new VBox(4);
        HBox.setHgrow(textos, Priority.ALWAYS);
        Label lTitulo = new Label(titulo);
        lTitulo.getStyleClass().add("etiqueta-kpi");
        lblValor.getStyleClass().add("valor-kpi");
        textos.getChildren().addAll(lTitulo, lblValor);

        StackPane iconBox = new StackPane();
        iconBox.getStyleClass().addAll("caja-icono-kpi", claseIcono);
        iconBox.setMinSize(40, 40);
        iconBox.setMaxSize(40, 40);
        Label lIcono = new Label(icono);
        lIcono.getStyleClass().add("etiqueta-icono-kpi");
        iconBox.getChildren().add(lIcono);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        row.getChildren().addAll(textos, spacer, iconBox); // Updated top to row
        card.getChildren().add(row);
        return card;
    }
}

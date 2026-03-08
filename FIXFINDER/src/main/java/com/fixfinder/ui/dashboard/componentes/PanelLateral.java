package com.fixfinder.ui.dashboard.componentes;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Panel lateral derecho del Dashboard.
 * Muestra el historial de últimas actualizaciones (actividad reciente del
 * sistema).
 */
public class PanelLateral extends VBox {

    private VBox listaActividadBox;

    public PanelLateral() {
        setSpacing(16);
        getStyleClass().add("right-panel");
        getChildren().addAll(construirCardActividad());
    }

    private VBox construirCardActividad() {
        VBox card = new VBox();
        card.getStyleClass().add("side-card");

        Label titulo = new Label("Últimas Actualizaciones");
        titulo.getStyleClass().add("side-card-title");

        listaActividadBox = new VBox(8);
        listaActividadBox.setPadding(new Insets(10, 16, 12, 16));
        listaActividadBox.getChildren().add(placeholder("Sin actividad reciente"));

        ScrollPane scroll = new ScrollPane(listaActividadBox);
        scroll.getStyleClass().add("transparent-scroll");
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(180);
        scroll.setMaxHeight(180);

        card.getChildren().addAll(titulo, scroll);
        return card;
    }

    public void agregarActividad(String mensaje) {
        if (listaActividadBox.getChildren().size() == 1
                && listaActividadBox.getChildren().get(0) instanceof Label l
                && "Sin actividad reciente".equals(l.getText())) {
            listaActividadBox.getChildren().clear();
        }
        HBox fila = new HBox(8);
        fila.setAlignment(Pos.TOP_LEFT);

        Label icono = new Label("●");
        icono.setStyle("-fx-text-fill: #F97316; -fx-font-size: 10px;");

        VBox textos = new VBox(2);
        Label txt = new Label(mensaje);
        txt.getStyleClass().add("activity-text");
        txt.setWrapText(true);
        Label hora = new Label(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        hora.getStyleClass().add("activity-time");
        textos.getChildren().addAll(txt, hora);

        fila.getChildren().addAll(icono, textos);
        listaActividadBox.getChildren().add(0, fila);
        if (listaActividadBox.getChildren().size() > 8) {
            listaActividadBox.getChildren().remove(8);
        }
    }

    private Label placeholder(String texto) {
        Label l = new Label(texto);
        l.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 12px; -fx-padding: 8 16 8 16;");
        return l;
    }
}

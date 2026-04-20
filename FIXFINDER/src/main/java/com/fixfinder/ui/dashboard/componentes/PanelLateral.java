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
        getStyleClass().add("panel-derecho");
        getChildren().addAll(construirCardActividad());
    }

    private VBox construirCardActividad() {
        VBox card = new VBox();
        card.getStyleClass().add("tarjeta-lateral");

        Label titulo = new Label("Últimas Actualizaciones");
        titulo.getStyleClass().add("titulo-tarjeta-lateral");

        listaActividadBox = new VBox(8);
        listaActividadBox.setPadding(new Insets(10, 16, 12, 16));
        listaActividadBox.getChildren().add(placeholder("Sin actividad reciente"));

        ScrollPane scroll = new ScrollPane(listaActividadBox);
        scroll.getStyleClass().add("scroll-transparente");
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
        icono.getStyleClass().add("punto-actividad");

        VBox textos = new VBox(2);
        Label txt = new Label(mensaje);
        txt.getStyleClass().add("texto-actividad");
        txt.setWrapText(true);
        Label hora = new Label(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        hora.getStyleClass().add("hora-actividad");
        textos.getChildren().addAll(txt, hora);

        fila.getChildren().addAll(icono, textos);
        listaActividadBox.getChildren().add(0, fila);
        if (listaActividadBox.getChildren().size() > 8) {
            listaActividadBox.getChildren().remove(8);
        }
    }

    private Label placeholder(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("marcador-vacio-lateral");
        return l;
    }
}

package com.fixfinder.ui.dashboard.vistas;

import com.fixfinder.ui.dashboard.modelos.OperarioFX;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;

/**
 * Vista de la sección "Empresa" del Dashboard.
 * Muestra datos legales de la empresa, información del gerente y las últimas
 * valoraciones recibidas de clientes.
 */
public class VistaEmpresa extends VBox {

    public VistaEmpresa(java.util.Map<String, Object> infoEmpresa,
            String gerenteNombre, String gerenteRol,
            ObservableList<OperarioFX> operarios) {
        getStyleClass().add("content-area");
        VBox.setVgrow(this, Priority.ALWAYS);

        ScrollPane scroll = new ScrollPane();
        scroll.getStyleClass().add("transparent-scroll");
        scroll.setFitToWidth(true);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox body = new VBox(20);
        body.setPadding(new Insets(24, 24, 24, 24));

        VBox card = new VBox(20);
        card.getStyleClass().add("table-card");
        card.setPadding(new Insets(28));
        card.setMaxWidth(640);

        Label titulo = new Label("Información de la Empresa");
        titulo.getStyleClass().add("card-title");

        HBox perfilRow = new HBox(20);
        perfilRow.setAlignment(Pos.CENTER_LEFT);

        StackPane imagenPlaceholder = new StackPane();
        imagenPlaceholder.setMinSize(80, 80);
        imagenPlaceholder.setMaxSize(80, 80);
        imagenPlaceholder.setStyle(
                "-fx-background-color: rgba(249,115,22,0.12);" +
                        "-fx-background-radius: 40;" +
                        "-fx-border-color: rgba(249,115,22,0.3);" +
                        "-fx-border-radius: 40;" +
                        "-fx-border-width: 2;");

        VBox imgInfo = new VBox(4);
        Label lblDatos = new Label("Datos Personales del Gerente");
        lblDatos.getStyleClass().add("modal-label");

        VBox camposGerente = new VBox(12);
        camposGerente.getChildren().addAll(
                fila("Nombre", gerenteNombre),
                fila("Rol en el sistema", gerenteRol));

        imgInfo.getChildren().addAll(lblDatos, camposGerente);

        Label lIniciales = new Label(iniciales(gerenteNombre));
        lIniciales.setStyle("-fx-text-fill: #F97316; -fx-font-size: 28px; -fx-font-weight: bold;");
        imagenPlaceholder.getChildren().add(lIniciales);

        perfilRow.getChildren().addAll(imagenPlaceholder, imgInfo);

        Separator sep1 = sep();

        Label lblEmp = new Label("Información Legal de Empresa");
        lblEmp.getStyleClass().add("modal-label");

        VBox camposEmp = new VBox(12);
        camposEmp.getChildren().addAll(
                fila("Denominación", String.valueOf(infoEmpresa.getOrDefault("nombre", "Desconocido"))),
                fila("CIF / NIF", String.valueOf(infoEmpresa.getOrDefault("cif", "No disponible"))),
                fila("Email Contacto", String.valueOf(infoEmpresa.getOrDefault("email", "No disponible"))),
                fila("Teléfono", String.valueOf(infoEmpresa.getOrDefault("telefono", "No disponible"))),
                fila("Dirección Física", String.valueOf(infoEmpresa.getOrDefault("direccion", "No disponible"))),
                fila("Fecha de Alta", String.valueOf(infoEmpresa.getOrDefault("fechaAlta", "No disponible"))),
                fila("Equipo Total", operarios.size() + " Operarios en plantilla"));

        card.getChildren().addAll(titulo, perfilRow, sep1, lblEmp, camposEmp);

        // --- SECCIÓN DE VALORACIONES ---
        if (infoEmpresa.containsKey("valoraciones")) {
            Separator sep2 = sep();
            Label lblVal = new Label("Últimas Valoraciones de Clientes");
            lblVal.getStyleClass().add("modal-label");

            VBox boxValoraciones = new VBox(12);
            Object vNode = infoEmpresa.get("valoraciones");
            if (vNode instanceof com.fasterxml.jackson.databind.JsonNode array && array.isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode v : array) {
                    HBox fVal = new HBox(10);
                    fVal.setAlignment(Pos.CENTER_LEFT);
                    fVal.setStyle(
                            "-fx-background-color: rgba(255,255,255,0.05); -fx-padding: 10; -fx-background-radius: 8;");

                    int estrellas = v.get("puntos").asInt();
                    Label lEstrellas = new Label("⭐".repeat(estrellas));
                    lEstrellas.setStyle("-fx-text-fill: #FBBF24; -fx-font-size: 14px;");

                    Label lCliente = new Label(v.get("cliente").asText());
                    lCliente.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

                    Label lComentario = new Label(v.get("comentario").asText());
                    lComentario.setStyle("-fx-text-fill: #94A3B8; -fx-font-style: italic;");
                    lComentario.setWrapText(true);

                    VBox vText = new VBox(2, new HBox(10, lCliente, lEstrellas), lComentario);
                    fVal.getChildren().add(vText);
                    boxValoraciones.getChildren().add(fVal);
                }
            }
            if (boxValoraciones.getChildren().isEmpty()) {
                boxValoraciones.getChildren().add(new Label("Aún no hay valoraciones disponibles."));
            }
            card.getChildren().addAll(sep2, lblVal, boxValoraciones);
        }

        body.getChildren().add(card);
        scroll.setContent(body);
        getChildren().add(scroll);
    }

    private HBox fila(String etiqueta, String valor) {
        HBox f = new HBox(8);
        Label lbl = new Label(etiqueta + ":");
        lbl.getStyleClass().add("modal-label");
        lbl.setMinWidth(180);
        Label val = new Label(valor);
        val.getStyleClass().add("modal-value");
        f.getChildren().addAll(lbl, val);
        return f;
    }

    private Separator sep() {
        Separator s = new Separator();
        s.setStyle("-fx-background-color: #2D3348;");
        return s;
    }

    private String iniciales(String nombre) {
        if (nombre == null || nombre.isBlank())
            return "?";
        String[] p = nombre.trim().split("\\s+");
        return p.length >= 2
                ? ("" + p[0].charAt(0) + p[1].charAt(0)).toUpperCase()
                : nombre.substring(0, Math.min(1, nombre.length())).toUpperCase();
    }
}

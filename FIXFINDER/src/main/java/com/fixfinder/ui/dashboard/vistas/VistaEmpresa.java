package com.fixfinder.ui.dashboard.vistas;

import com.fixfinder.ui.dashboard.modelos.OperarioFX;
import com.fasterxml.jackson.databind.JsonNode;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.util.Map;

/**
 * Vista de la sección "Empresa" del Dashboard.
 * Muestra datos legales de la empresa, información del gerente y las últimas valoraciones recibidas.
 */
public class VistaEmpresa extends VBox {

    public VistaEmpresa(Map<String, Object> infoEmpresa,
            String gerenteNombre, String gerenteRol,
            ObservableList<OperarioFX> operarios, Runnable onCambiarFotoGerente) {
        
        getStyleClass().add("area-contenido");
        VBox.setVgrow(this, Priority.ALWAYS);

        ScrollPane scroll = new ScrollPane();
        scroll.getStyleClass().add("scroll-transparente");
        scroll.setFitToWidth(true);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox body = new VBox(20);
        body.setPadding(new Insets(24));

        VBox card = new VBox(20);
        card.getStyleClass().add("tarjeta-tabla");
        card.setPadding(new Insets(28));
        card.setMaxWidth(640);

        Label titulo = new Label("Información de la Empresa");
        titulo.getStyleClass().add("titulo-tarjeta");

        HBox perfilRow = new HBox(20);
        perfilRow.setAlignment(Pos.CENTER_LEFT);

        StackPane imagenPlaceholder = new StackPane();
        imagenPlaceholder.getStyleClass().add("contenedor-imagen-perfil");

        VBox imgInfo = new VBox(4);
        Label lblDatos = new Label("Datos Personales del Gerente");
        lblDatos.getStyleClass().add("etiqueta-modal");

        VBox camposGerente = new VBox(12);
        camposGerente.getChildren().addAll(
                fila("Nombre", gerenteNombre),
                fila("Rol en el sistema", gerenteRol));

        imgInfo.getChildren().addAll(lblDatos, camposGerente);

        Label lIniciales = new Label(iniciales(gerenteNombre));
        lIniciales.getStyleClass().add("iniciales-perfil");

        String urlFotoGerente = (String) infoEmpresa.get("gerenteUrlFoto");
        boolean tieneFoto = false;
        if (urlFotoGerente != null && (urlFotoGerente.startsWith("http://") || urlFotoGerente.startsWith("https://"))) {
            try {
                ImageView iv = new ImageView(new Image(urlFotoGerente, 80, 80, true, true, false));
                iv.setFitWidth(80);
                iv.setFitHeight(80);
                iv.setClip(new Circle(40, 40, 40));
                imagenPlaceholder.getChildren().add(iv);
                tieneFoto = true;
            } catch (Exception ignored) {}
        }

        if (!tieneFoto) {
            imagenPlaceholder.getChildren().add(lIniciales);
        }

        Button btnCambiarFoto = new Button("📸");
        btnCambiarFoto.getStyleClass().add("btn-solo-icono");
        btnCambiarFoto.setOnAction(e -> {
            if (onCambiarFotoGerente != null) onCambiarFotoGerente.run();
        });
        StackPane.setAlignment(btnCambiarFoto, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(btnCambiarFoto, new Insets(0, -10, -10, 0));

        imagenPlaceholder.getChildren().add(btnCambiarFoto);
        perfilRow.getChildren().addAll(imagenPlaceholder, imgInfo);

        Separator sep1 = sep();
        Label lblEmp = new Label("Información Legal de Empresa");
        lblEmp.getStyleClass().add("etiqueta-modal");

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
        Separator sep2 = sep();
        Label lblVal = new Label("Últimas Valoraciones de Clientes");
        lblVal.getStyleClass().add("etiqueta-modal");
        VBox boxValoraciones = new VBox(12);

        // Detección exhaustiva de la clave que viene del servidor
        Object vRaw = null;
        String[] possibleKeys = {"valoraciones", "Valoraciones", "VALORACIONES", "reviews", "ratings", "valoracion"};
        for (String k : possibleKeys) {
            if (infoEmpresa.containsKey(k)) {
                vRaw = infoEmpresa.get(k);
                break;
            }
        }

        if (vRaw != null) {
            if (vRaw instanceof JsonNode array && array.isArray()) {
                for (JsonNode v : array) {
                    boxValoraciones.getChildren().add(crearFilaValoracion(
                        v.has("cliente") ? v.get("cliente").asText() : "Cliente",
                        v.has("puntos") ? v.get("puntos").asInt() : 0,
                        v.has("comentario") ? v.get("comentario").asText() : ""
                    ));
                }
            } else if (vRaw instanceof java.util.List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map m) {
                        boxValoraciones.getChildren().add(crearFilaValoracion(
                            String.valueOf(m.getOrDefault("cliente", "Cliente")),
                            Integer.parseInt(String.valueOf(m.getOrDefault("puntos", 0))),
                            String.valueOf(m.getOrDefault("comentario", ""))
                        ));
                    }
                }
            }
        }

        if (boxValoraciones.getChildren().isEmpty()) {
            Label noVal = new Label("Aún no se han recibido valoraciones para esta empresa.");
            noVal.getStyleClass().add("texto-tenue-cursiva");
            boxValoraciones.getChildren().add(noVal);
        }

        card.getChildren().addAll(sep2, lblVal, boxValoraciones);

        body.getChildren().add(card);
        scroll.setContent(body);
        getChildren().add(scroll);
    }

    private HBox crearFilaValoracion(String cliente, int puntos, String comentario) {
        HBox fVal = new HBox(12);
        fVal.setAlignment(Pos.CENTER_LEFT);
        fVal.getStyleClass().add("tarjeta-valoracion");

        Label lEstrellas = new Label("★".repeat(Math.max(0, Math.min(5, puntos))) + "☆".repeat(Math.max(0, 5 - puntos)));
        lEstrellas.getStyleClass().add("etiqueta-estrellas");

        Label lCliente = new Label(cliente);
        lCliente.getStyleClass().add("valoracion-cliente");

        Label lComentario = new Label(comentario.isBlank() ? "(Sin comentario)" : "\"" + comentario + "\"");
        lComentario.getStyleClass().add("valoracion-comentario");
        lComentario.setWrapText(true);

        VBox vText = new VBox(2, new HBox(10, lCliente, lEstrellas), lComentario);
        HBox.setHgrow(vText, Priority.ALWAYS);
        fVal.getChildren().add(vText);
        return fVal;
    }

    private HBox fila(String etiqueta, String valor) {
        HBox f = new HBox(8);
        Label lbl = new Label(etiqueta + ":");
        lbl.getStyleClass().add("etiqueta-modal");
        lbl.setMinWidth(180);
        Label val = new Label(valor);
        val.getStyleClass().add("valor-modal");
        f.getChildren().addAll(lbl, val);
        return f;
    }

    private Separator sep() {
        Separator s = new Separator();
        s.getStyleClass().add("separador-contenido");
        return s;
    }

    private String iniciales(String nombre) {
        if (nombre == null || nombre.isBlank()) return "?";
        String[] p = nombre.trim().split("\\s+");
        return p.length >= 2
                ? ("" + p[0].charAt(0) + p[1].charAt(0)).toUpperCase()
                : nombre.substring(0, Math.min(1, nombre.length())).toUpperCase();
    }
}

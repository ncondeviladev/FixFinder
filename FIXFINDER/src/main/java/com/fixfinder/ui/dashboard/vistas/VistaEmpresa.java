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
import javafx.scene.layout.Region;
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
            ObservableList<OperarioFX> operarios, 
            Runnable onCambiarFotoGerente, 
            Runnable onCambiarLogoEmpresa,
            Runnable onEditarEmpresa) {
        
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

        // --- CABECERA DE IDENTIDAD (LOGO + GERENTE) ---
        HBox cabeceraIdentidad = new HBox(25);
        cabeceraIdentidad.setAlignment(Pos.CENTER_LEFT);
        cabeceraIdentidad.setPadding(new Insets(0, 0, 10, 0));

        // 1. Logo Empresa
        StackPane logoPlaceholder = new StackPane();
        logoPlaceholder.getStyleClass().add("contenedor-imagen-perfil");
        logoPlaceholder.setMinWidth(90); logoPlaceholder.setMaxWidth(90);
        logoPlaceholder.setMinHeight(90); logoPlaceholder.setMaxHeight(90);

        String logoUrl = (String) infoEmpresa.get("url_foto");
        boolean tieneLogo = false;
        if (logoUrl != null && (logoUrl.startsWith("http://") || logoUrl.startsWith("https://"))) {
            try {
                // backgroundLoading = true para evitar congelamiento de UI
                Image img = new Image(logoUrl, 120, 120, true, true, true);
                ImageView ivLogo = new ImageView();
                ivLogo.setFitWidth(90);
                ivLogo.setFitHeight(90);
                ivLogo.setClip(new Circle(45, 45, 45));
                
                // Mientras carga, podemos poner un listener o dejar que aparezca sola
                ivLogo.imageProperty().bind(javafx.beans.binding.Bindings.when(img.progressProperty().isEqualTo(1))
                        .then(img).otherwise((Image)null));
                
                logoPlaceholder.getChildren().add(ivLogo);
                tieneLogo = true;
            } catch (Exception ignored) {}
        }
        Button btnCambiarLogo = new Button("📸");
        btnCambiarLogo.getStyleClass().add("btn-solo-icono");
        btnCambiarLogo.setOnAction(e -> {
            if (onCambiarLogoEmpresa != null) onCambiarLogoEmpresa.run();
        });
        StackPane.setAlignment(btnCambiarLogo, Pos.BOTTOM_RIGHT);
        logoPlaceholder.getChildren().add(btnCambiarLogo);

        if (!tieneLogo) {
            Label lIniLogo = new Label(iniciales(String.valueOf(infoEmpresa.getOrDefault("nombre", "EF"))));
            lIniLogo.getStyleClass().add("iniciales-perfil");
            logoPlaceholder.getChildren().add(lIniLogo);
        }

        // 2. Info Textual
        VBox infoTextos = new VBox(2);
        infoTextos.setAlignment(Pos.CENTER_LEFT);
        
        Label lblEmpresaNom = new Label(String.valueOf(infoEmpresa.getOrDefault("nombre", "Empresa")));
        lblEmpresaNom.getStyleClass().add("titulo-cabecera");
        lblEmpresaNom.setStyle("-fx-font-size: 24px;");

        Label lblGerenteNom = new Label("Responsable: " + gerenteNombre);
        lblGerenteNom.getStyleClass().add("texto-tenue");
        lblGerenteNom.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        infoTextos.getChildren().addAll(lblEmpresaNom, lblGerenteNom);

        cabeceraIdentidad.getChildren().addAll(logoPlaceholder, infoTextos);

        Separator sepHeader = sep();
        
        HBox headerAdmin = new HBox();
        headerAdmin.setAlignment(Pos.CENTER_LEFT);
        Label lblEmp = new Label("Información Corporativa y Legal");
        lblEmp.getStyleClass().add("etiqueta-modal");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button btnEditar = new Button("⚙ Editar Datos");
        btnEditar.getStyleClass().add("btn-transparente-naranja");
        btnEditar.setOnAction(e -> {
            if (onEditarEmpresa != null) onEditarEmpresa.run();
        });
        headerAdmin.getChildren().addAll(lblEmp, spacer, btnEditar);

        VBox camposEmp = new VBox(12);
        camposEmp.getChildren().addAll(
                fila("Denominación Social", String.valueOf(infoEmpresa.getOrDefault("nombre", "Desconocido"))),
                fila("CIF / Identificación Fiscal", String.valueOf(infoEmpresa.getOrDefault("cif", "No disponible"))),
                fila("Correo electrónico", String.valueOf(infoEmpresa.getOrDefault("email", "No disponible"))),
                fila("Teléfono de contacto", String.valueOf(infoEmpresa.getOrDefault("telefono", "No disponible"))),
                fila("Dirección Física", String.valueOf(infoEmpresa.getOrDefault("direccion", "No disponible"))),
                fila("Fecha de Alta", String.valueOf(infoEmpresa.getOrDefault("fechaAlta", "No disponible"))),
                fila("Equipo Total", operarios.size() + " Operarios en plantilla"));

        card.getChildren().addAll(titulo, cabeceraIdentidad, sepHeader, headerAdmin, camposEmp);

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

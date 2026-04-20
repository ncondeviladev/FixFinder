package com.fixfinder.ui.dashboard.dialogos;

import com.fixfinder.ui.dashboard.componentes.tabla.UtilidadesTabla;
import com.fixfinder.ui.dashboard.modelos.TrabajoFX;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;

/**
 * Diálogo modal de solo lectura que muestra los datos de contacto del cliente
 * de una incidencia.
 * Accesible desde el botón de ficha de cliente en la tabla de incidencias del
 * Dashboard.
 */
public class DialogoFichaCliente {

    private final String cssUrl;

    public DialogoFichaCliente(String cssUrl) {
        this.cssUrl = cssUrl;
    }

    public void mostrar(TrabajoFX t) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Ficha de Cliente");
        dialog.setHeaderText("Información de Contacto");

        if (cssUrl != null) {
            dialog.getDialogPane().getStylesheets().add(cssUrl);
        }
        dialog.getDialogPane().getStyleClass().add("panel-dialogo");

        VBox content = new VBox(20);
        content.setPadding(new Insets(12, 10, 10, 10));

        // Cabecera con avatar
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        StackPane avatar = new StackPane();
        avatar.setMinSize(64, 64);
        avatar.setMaxSize(64, 64);
        avatar.getStyleClass().add("contenedor-mini-avatar");
        avatar.setStyle("-fx-background-radius: 32; -fx-border-color: rgba(249,115,22,0.4); -fx-border-radius: 32; -fx-border-width: 2;");

        String fotoUrl = t.getClienteUrlFoto();
        if (fotoUrl != null && !fotoUrl.isBlank() && !fotoUrl.equals("null")) {
            try {
                // Cargamos la imagen de forma asíncrona
                Image img = new Image(fotoUrl, 128, 128, true, true, true);
                
                Circle circuloFoto = new Circle(32);
                circuloFoto.setSmooth(true);
                // Color de fondo (pincel temporal mientras carga)
                circuloFoto.setFill(Color.web("#2D3348"));

                // TRUCO: Cuando la imagen carga (progreso == 1), aplicamos el patrón de relleno
                img.progressProperty().addListener((obs, old, progress) -> {
                    if (progress.doubleValue() == 1.0 && !img.isError()) {
                        Platform.runLater(() -> {
                            circuloFoto.setFill(new ImagePattern(img));
                        });
                    }
                });
                
                // Si ya está cargada (caché o carga inmediata)
                if (img.getProgress() == 1.0 && !img.isError()) {
                    circuloFoto.setFill(new ImagePattern(img));
                }
                
                avatar.getChildren().add(circuloFoto);
            } catch (Exception e) {
                avatar.getChildren().add(UtilidadesTabla.generarMiniAvatar(t.getCliente()));
            }
        } else {
            avatar.getChildren().add(UtilidadesTabla.generarMiniAvatar(t.getCliente()));
        }

        VBox titulos = new VBox(2);
        Label lblNombre = new Label(t.getCliente());
        lblNombre.getStyleClass().add("titulo-modal");
        Label lblRol = new Label("Cliente de FixFinder");
        lblRol.getStyleClass().add("texto-sin-asignar");
        titulos.getChildren().addAll(lblNombre, lblRol);

        header.getChildren().addAll(avatar, titulos);

        // Datos Personales
        VBox datos = new VBox(15);
        datos.getChildren().addAll(
                fila(t, "Teléfono", t.getClienteTelefono().isBlank() ? "No proporcionado" : t.getClienteTelefono()),
                fila(t, "Email", t.getClienteEmail().isBlank() ? "No proporcionado" : t.getClienteEmail()),
                fila(t, "Dirección de la Incidencia", t.getDireccion().isBlank() ? "No especificada" : t.getDireccion()));

        content.getChildren().addAll(header, datos);
        dialog.getDialogPane().setContent(content);

        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        Button btnClose = (Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        btnClose.setText("Cerrar Ficha");
        btnClose.getStyleClass().add("btn-secundario");

        dialog.showAndWait();
    }

    private HBox fila(TrabajoFX t, String etiqueta, String valor) {
        HBox f = new HBox(10);
        Label lbl = new Label(etiqueta + ":");
        lbl.getStyleClass().add("etiqueta-modal");
        lbl.setMinWidth(160);
        Label val = new Label(valor);
        val.getStyleClass().add("valor-modal");
        val.setWrapText(true);

        // Si es dirección, lo hacemos interactivo para abrir mapas
        if (etiqueta.contains("Dirección")) {
            val.getStyleClass().add("enlace-mapa-modal");
            val.setOnMouseClicked(e -> {
                try {
                    String query = valor.replace(" ", "+");
                    String url = "https://www.google.com/maps/search/?api=1&query=" + query;
                    Runtime.getRuntime().exec("cmd /c start " + url.replace("&", "^&"));
                } catch (Exception ex) {
                    System.err.println("Error al abrir mapas: " + ex.getMessage());
                }
            });
        }

        f.getChildren().addAll(lbl, val);
        return f;
    }
}

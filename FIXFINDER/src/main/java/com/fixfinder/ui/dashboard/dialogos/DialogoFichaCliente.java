package com.fixfinder.ui.dashboard.dialogos;

import com.fixfinder.ui.dashboard.modelos.TrabajoFX;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

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
        dialog.getDialogPane().getStyleClass().add("dialog-pane");

        VBox content = new VBox(20);
        content.setPadding(new Insets(12, 10, 10, 10));

        // Cabecera con avatar
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        StackPane avatar = new StackPane();
        avatar.setMinSize(64, 64);
        avatar.setMaxSize(64, 64);
        avatar.setStyle(
                "-fx-background-color: rgba(249,115,22,0.15);" +
                        "-fx-background-radius: 32;" +
                        "-fx-border-color: rgba(249,115,22,0.4);" +
                        "-fx-border-radius: 32;" +
                        "-fx-border-width: 2;");
        Label lIni = new Label(iniciales(t.getCliente()));
        lIni.setStyle("-fx-text-fill: #F97316; -fx-font-size: 24px; -fx-font-weight: bold;");
        avatar.getChildren().add(lIni);

        VBox titulos = new VBox(2);
        Label lblNombre = new Label(t.getCliente());
        lblNombre.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
        Label lblRol = new Label("Cliente de FixFinder");
        lblRol.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 12px;");
        titulos.getChildren().addAll(lblNombre, lblRol);

        header.getChildren().addAll(avatar, titulos);

        // Datos Personales
        VBox datos = new VBox(15);
        datos.getChildren().addAll(
                fila("Teléfono", t.getClienteTelefono().isBlank() ? "No proporcionado" : t.getClienteTelefono()),
                fila("Email", t.getClienteEmail().isBlank() ? "No proporcionado" : t.getClienteEmail()),
                fila("Dirección de la Incidencia", t.getDireccion().isBlank() ? "No especificada" : t.getDireccion()));

        content.getChildren().addAll(header, datos);
        dialog.getDialogPane().setContent(content);

        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        Button btnClose = (Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        btnClose.setText("Cerrar Ficha");
        btnClose.getStyleClass().add("btn-secondary");

        dialog.showAndWait();
    }

    private HBox fila(String etiqueta, String valor) {
        HBox f = new HBox(10);
        Label lbl = new Label(etiqueta + ":");
        lbl.getStyleClass().add("modal-label");
        lbl.setMinWidth(160);
        Label val = new Label(valor);
        val.getStyleClass().add("modal-value");
        val.setWrapText(true);
        f.getChildren().addAll(lbl, val);
        return f;
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

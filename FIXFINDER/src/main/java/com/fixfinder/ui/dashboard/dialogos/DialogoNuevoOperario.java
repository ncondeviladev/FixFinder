package com.fixfinder.ui.dashboard.dialogos;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.Optional;

/**
 * Diálogo modal para registrar un nuevo operario en la empresa.
 * Recopila nombre, DNI, email, teléfono, contraseña y especialidad.
 */
public class DialogoNuevoOperario {

    public static class DatosOperario {
        public final String nombre;
        public final String dni;
        public final String email;
        public final String telefono;
        public final String password;
        public final String especialidad;

        public DatosOperario(String n, String d, String e, String t, String p, String esp) {
            nombre = n;
            dni = d;
            email = e;
            telefono = t;
            password = p;
            especialidad = esp;
        }
    }

    private final String cssUrl;

    public DialogoNuevoOperario(String cssUrl) {
        this.cssUrl = cssUrl;
    }

    public Optional<DatosOperario> mostrar() {
        Dialog<DatosOperario> dialog = new Dialog<>();
        dialog.setTitle("Nuevo Operario");
        dialog.setHeaderText("Añadir operario al equipo");
        if (cssUrl != null)
            dialog.getDialogPane().getStylesheets().add(cssUrl);
        dialog.getDialogPane().getStyleClass().add("dialog-pane");

        TextField txtNombre = new TextField();
        txtNombre.setPromptText("Nombre completo");
        TextField txtDni = new TextField();
        txtDni.setPromptText("DNI / NIE");
        TextField txtEmail = new TextField();
        txtEmail.setPromptText("Correo electrónico");
        TextField txtTelefono = new TextField();
        txtTelefono.setPromptText("Teléfono");
        PasswordField txtPass = new PasswordField();
        txtPass.setPromptText("Contraseña (temporal)");
        ComboBox<String> cbEsp = new ComboBox<>();
        cbEsp.getItems().addAll("ELECTRICIDAD", "FONTANERIA", "CLIMATIZACION", "PINTURA", "ALBANILERIA", "OTROS");
        cbEsp.getSelectionModel().selectFirst();
        cbEsp.setStyle(
                "-fx-font-family: 'Inter', sans-serif; -fx-text-fill: white; -fx-background-color: #1E2536; -fx-border-color: #3B4256; -fx-border-radius: 6; -fx-background-radius: 6;");

        for (Control c : new Control[] { txtNombre, txtDni, txtEmail, txtTelefono, txtPass }) {
            c.getStyleClass().add("modal-input");
            if (c instanceof TextField tf)
                tf.setMaxWidth(Double.MAX_VALUE);
        }
        cbEsp.setMaxWidth(Double.MAX_VALUE);

        VBox form = new VBox(12);
        form.setPadding(new Insets(10, 0, 10, 0));
        form.getChildren().addAll(
                seccion("Nombre completo", txtNombre),
                seccion("DNI", txtDni),
                seccion("Correo electrónico", txtEmail),
                seccion("Teléfono", txtTelefono),
                seccion("Contraseña", txtPass),
                seccion("Especialidad", cbEsp));

        Button btnSubirFoto = new Button("📸 Subir Fotografía del Operario");
        btnSubirFoto.getStyleClass().add("btn-secondary");
        btnSubirFoto.setMaxWidth(Double.MAX_VALUE);
        btnSubirFoto.setStyle(
                "-fx-border-style: dashed; -fx-border-color: #3B4256; -fx-background-color: transparent; -fx-text-fill: #94A3B8;");
        btnSubirFoto.setDisable(true); // Se habilitará al implementar Firebase

        Label infoImage = new Label(
                "La foto de perfil se asociará en el entorno al implementarse las subidas en Firestore.");
        infoImage.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 11px; -fx-font-style: italic;");
        infoImage.setWrapText(true);

        form.getChildren().addAll(seccion("Fotografía de Perfil", btnSubirFoto), infoImage);

        dialog.getDialogPane().setContent(form);

        ButtonType btnCrear = new ButtonType("Crear Operario", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnCrear, ButtonType.CANCEL);

        Button okBtn = (Button) dialog.getDialogPane().lookupButton(btnCrear);
        okBtn.getStyleClass().add("btn-primary");
        Button cancelBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelBtn.getStyleClass().add("btn-secondary");

        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            boolean errorValidacion = false;
            String msj = "";

            // if (txtNombre.getText().isBlank() || txtEmail.getText().isBlank() ||
            // txtPass.getText().isBlank()) {
            // msj = "Nombre, email y contraseña son obligatorios.";
            // errorValidacion = true;
            // } else if
            // (!txtEmail.getText().matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            // msj = "Formato de email incorrecto.";
            // errorValidacion = true;
            // } else if (!txtTelefono.getText().isBlank() &&
            // !txtTelefono.getText().matches("^\\d{9}$")) {
            // msj = "El teléfono debe contener exactamente 9 dígitos numéricos.";
            // errorValidacion = true;
            // }

            if (errorValidacion) {
                event.consume(); // Evita que se cierre la ventana
                new Alert(Alert.AlertType.ERROR, msj, ButtonType.OK).show();
            }
        });

        dialog.setResultConverter(b -> {
            if (b == btnCrear) {
                return new DatosOperario(txtNombre.getText(), txtDni.getText(), txtEmail.getText(),
                        txtTelefono.getText(), txtPass.getText(), cbEsp.getValue());
            }
            return null;
        });

        return dialog.showAndWait();
    }

    private VBox seccion(String titulo, Control input) {
        VBox sec = new VBox(5);
        Label lbl = new Label(titulo);
        lbl.getStyleClass().add("modal-label");
        sec.getChildren().addAll(lbl, input);
        return sec;
    }
}

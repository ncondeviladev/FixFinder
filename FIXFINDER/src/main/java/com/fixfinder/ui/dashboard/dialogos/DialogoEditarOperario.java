package com.fixfinder.ui.dashboard.dialogos;

import com.fixfinder.ui.dashboard.modelos.OperarioFX;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.Optional;

/**
 * Diálogo modal para editar los datos de un operario existente.
 * Pre-rellena los campos con los valores actuales del operario y devuelve los
 * datos modificados.
 */
public class DialogoEditarOperario {

    public static class DatosOperarioAct {
        public final String nombre;
        public final String dni;
        public final String email;
        public final String telefono;
        public final String especialidad;
        // Se podría enviar la password si la cambia, pero lo dejamos por defecto sin
        // forzar cambio.

        public DatosOperarioAct(String n, String d, String e, String t, String esp) {
            nombre = n;
            dni = d;
            email = e;
            telefono = t;
            especialidad = esp;
        }
    }

    private final String cssUrl;
    private final OperarioFX operarioActual;

    public DialogoEditarOperario(OperarioFX operario, String cssUrl) {
        this.operarioActual = operario;
        this.cssUrl = cssUrl;
    }

    public Optional<DatosOperarioAct> mostrar() {
        Dialog<DatosOperarioAct> dialog = new Dialog<>();
        dialog.setTitle("Editar Operario");
        dialog.setHeaderText("Modificar datos de: " + operarioActual.getNombre());
        if (cssUrl != null)
            dialog.getDialogPane().getStylesheets().add(cssUrl);
        dialog.getDialogPane().getStyleClass().add("dialog-pane");

        TextField txtNombre = new TextField(operarioActual.getNombre());
        txtNombre.setPromptText("Nombre completo");

        TextField txtDni = new TextField(operarioActual.getDni());
        txtDni.setPromptText("DNI / NIE");

        TextField txtEmail = new TextField(operarioActual.getEmail());
        txtEmail.setPromptText("Correo electrónico");

        TextField txtTelefono = new TextField(operarioActual.getTelefono());
        txtTelefono.setPromptText("Teléfono");

        ComboBox<String> cbEsp = new ComboBox<>();
        cbEsp.getItems().addAll("ELECTRICIDAD", "FONTANERIA", "CLIMATIZACION", "PINTURA", "ALBANILERIA", "OTROS");
        String espM = operarioActual.getEspecialidad() != null && !operarioActual.getEspecialidad().isBlank()
                ? operarioActual.getEspecialidad().toUpperCase()
                : "OTROS";
        if (cbEsp.getItems().contains(espM))
            cbEsp.getSelectionModel().select(espM);
        else
            cbEsp.getSelectionModel().selectFirst();

        cbEsp.setStyle(
                "-fx-font-family: 'Inter', sans-serif; -fx-text-fill: white; -fx-background-color: #1E2536; -fx-border-color: #3B4256; -fx-border-radius: 6; -fx-background-radius: 6;");

        for (Control c : new Control[] { txtNombre, txtDni, txtEmail, txtTelefono }) {
            c.getStyleClass().add("modal-input");
            if (c instanceof TextField) {
                ((TextField) c).setMaxWidth(Double.MAX_VALUE);
            }
        }
        cbEsp.setMaxWidth(Double.MAX_VALUE);

        VBox form = new VBox(12);
        form.setPadding(new Insets(10, 0, 10, 0));
        form.getChildren().addAll(
                seccion("Nombre completo", txtNombre),
                seccion("DNI", txtDni),
                seccion("Correo electrónico", txtEmail),
                seccion("Teléfono", txtTelefono),
                seccion("Especialidad", cbEsp));

        Button btnSubirFoto = new Button("📸 Modificar Fotografía del Operario");
        btnSubirFoto.getStyleClass().add("btn-secondary");
        btnSubirFoto.setMaxWidth(Double.MAX_VALUE);
        btnSubirFoto.setStyle(
                "-fx-border-style: dashed; -fx-border-color: #3B4256; -fx-background-color: transparent; -fx-text-fill: #94A3B8;");
        btnSubirFoto.setDisable(true);

        Label infoImage = new Label(
                "La foto de perfil podrá actualizarse cuando configuremos el servidor de media en Firebase.");
        infoImage.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 11px; -fx-font-style: italic;");
        infoImage.setWrapText(true);

        form.getChildren().addAll(seccion("Fotografía de Perfil", btnSubirFoto), infoImage);

        dialog.getDialogPane().setContent(form);

        ButtonType btnModificar = new ButtonType("Guardar Cambios", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnModificar, ButtonType.CANCEL);

        Button okBtn = (Button) dialog.getDialogPane().lookupButton(btnModificar);
        okBtn.getStyleClass().add("btn-primary");
        Button cancelBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelBtn.getStyleClass().add("btn-secondary");

        dialog.setResultConverter(b -> {
            if (b == btnModificar) {
                String nom = txtNombre.getText().isBlank() ? operarioActual.getNombre() : txtNombre.getText();
                String dni = txtDni.getText().isBlank() ? operarioActual.getDni() : txtDni.getText();
                String mail = txtEmail.getText().isBlank() ? operarioActual.getEmail() : txtEmail.getText();
                String tel = txtTelefono.getText().isBlank() ? operarioActual.getTelefono() : txtTelefono.getText();
                return new DatosOperarioAct(nom, dni, mail, tel, cbEsp.getValue());
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

package com.fixfinder.ui.dashboard.dialogos;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.Map;
import java.util.Optional;

/**
 * Diálogo para la edición de datos corporativos de la empresa.
 */
public class DialogoGestionEmpresa {

    public record ResultadoEmpresa(String nombre, String cif, String email, String telefono, String direccion) {}

    private final String cssUrl;
    private final Map<String, Object> infoActual;

    public DialogoGestionEmpresa(Map<String, Object> infoActual, String cssUrl) {
        this.infoActual = infoActual;
        this.cssUrl = cssUrl;
    }

    public Optional<ResultadoEmpresa> mostrar() {
        Dialog<ResultadoEmpresa> dialog = new Dialog<>();
        dialog.setTitle("Configuración de Empresa");
        dialog.setHeaderText("Modificar datos legales e información de contacto");

        if (cssUrl != null) {
            dialog.getDialogPane().getStylesheets().add(cssUrl);
        }
        dialog.getDialogPane().getStyleClass().add("panel-dialogo");

        // --- CAMPOS DE ENTRADA ---
        TextField txtNombre = new TextField(getString("nombre"));
        txtNombre.setPromptText("Nombre de la empresa");

        TextField txtCif = new TextField(getString("cif"));
        txtCif.setPromptText("CIF");

        TextField txtEmail = new TextField(getString("email"));
        txtEmail.setPromptText("Email corporativo");

        TextField txtTelefono = new TextField(getString("telefono"));
        txtTelefono.setPromptText("Teléfono");

        TextField txtDireccion = new TextField(getString("direccion"));
        txtDireccion.setPromptText("Dirección fiscal");

        Control[] inputs = {txtNombre, txtCif, txtEmail, txtTelefono, txtDireccion};
        for (Control c : inputs) {
            c.getStyleClass().add("entrada-modal");
            if (c instanceof TextField tf) tf.setMaxWidth(Double.MAX_VALUE);
        }

        // --- CONSTRUCCIÓN DEL FORMULARIO ---
        VBox form = new VBox(12);
        form.setPadding(new Insets(10, 0, 10, 0));
        form.setPrefWidth(400);

        form.getChildren().addAll(
                seccion("Denominación Social", txtNombre),
                seccion("CIF / Identificación Fiscal", txtCif),
                seccion("Correo electrónico de contacto", txtEmail),
                seccion("Teléfono de contacto", txtTelefono),
                seccion("Dirección completa", txtDireccion)
        );

        dialog.getDialogPane().setContent(form);

        // --- BOTONES ---
        ButtonType btnOk = new ButtonType("Guardar Cambios", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnOk, ButtonType.CANCEL);

        Button okBtn = (Button) dialog.getDialogPane().lookupButton(btnOk);
        okBtn.getStyleClass().add("btn-primario");

        Button cancelBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        if (cancelBtn != null) cancelBtn.getStyleClass().add("btn-secundario");

        // --- CONVERSOR ---
        dialog.setResultConverter(b -> {
            if (b == btnOk) {
                return new ResultadoEmpresa(
                        txtNombre.getText().trim(),
                        txtCif.getText().trim(),
                        txtEmail.getText().trim(),
                        txtTelefono.getText().trim(),
                        txtDireccion.getText().trim()
                );
            }
            return null;
        });

        return dialog.showAndWait();
    }

    private VBox seccion(String titulo, Control input) {
        VBox sec = new VBox(5);
        Label lbl = new Label(titulo);
        lbl.getStyleClass().add("etiqueta-modal");
        sec.getChildren().addAll(lbl, input);
        return sec;
    }

    private String getString(String key) {
        Object val = infoActual.get(key);
        return val != null ? val.toString() : "";
    }
}

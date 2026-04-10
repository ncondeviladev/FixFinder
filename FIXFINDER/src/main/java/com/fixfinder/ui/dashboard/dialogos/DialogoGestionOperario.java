package com.fixfinder.ui.dashboard.dialogos;

import com.fixfinder.ui.dashboard.modelos.*;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.Optional;

/**
 * Diálogo modular unificado para la gestión de operarios.
 * Permite tanto la creación de un nuevo operario como la edición de uno existente,
 * adaptando su interfaz (campos, títulos y botones) dinámicamente.
 */
public class DialogoGestionOperario {

    /**
     * Registro unificado de datos para el retorno del diálogo.
     */
    public record ResultadoOperario(String nombre, String dni, String email, 
            String telefono, String password, String especialidad) {}

    private final String cssUrl;
    private final OperarioFX operarioActual;
    private final boolean esModoEdicion;

    /**
     * Constructor para modo creación (campos vacíos).
     */
    public DialogoGestionOperario(String cssUrl) {
        this(null, cssUrl);
    }

    /**
     * Constructor para modo edición (campos pre-rellenados).
     * @param operario Operario a editar. Si es null, entra en modo creación.
     */
    public DialogoGestionOperario(OperarioFX operario, String cssUrl) {
        this.operarioActual = operario;
        this.cssUrl = cssUrl;
        this.esModoEdicion = operario != null;
    }

    public Optional<ResultadoOperario> mostrar() {
        Dialog<ResultadoOperario> dialog = new Dialog<>();
        dialog.setTitle(esModoEdicion ? "Editar Operario" : "Nuevo Operario");
        dialog.setHeaderText(esModoEdicion 
            ? "Modificar datos de: " + operarioActual.getNombre() 
            : "Añadir un nuevo operario al equipo");
        
        if (cssUrl != null) {
            dialog.getDialogPane().getStylesheets().add(cssUrl);
        }
        dialog.getDialogPane().getStyleClass().add("dialog-pane");

        // --- CAMPOS DE ENTRADA ---
        TextField txtNombre = new TextField(esModoEdicion ? operarioActual.getNombre() : "");
        txtNombre.setPromptText("Nombre completo");

        TextField txtDni = new TextField(esModoEdicion ? operarioActual.getDni() : "");
        txtDni.setPromptText("DNI / NIE");

        TextField txtEmail = new TextField(esModoEdicion ? operarioActual.getEmail() : "");
        txtEmail.setPromptText("Correo electrónico");

        TextField txtTelefono = new TextField(esModoEdicion ? operarioActual.getTelefono() : "");
        txtTelefono.setPromptText("Teléfono");

        PasswordField txtPass = new PasswordField();
        txtPass.setPromptText("Contraseña (temporal)");

        ComboBox<String> cbEsp = new ComboBox<>();
        cbEsp.getItems().addAll("ELECTRICIDAD", "FONTANERIA", "CLIMATIZACION", "PINTURA", "ALBANILERIA", "OTROS");
        
        if (esModoEdicion) {
            String espM = operarioActual.getEspecialidad() != null && !operarioActual.getEspecialidad().isBlank()
                    ? operarioActual.getEspecialidad().toUpperCase() : "OTROS";
            cbEsp.getSelectionModel().select(cbEsp.getItems().contains(espM) ? espM : "OTROS");
        } else {
            cbEsp.getSelectionModel().selectFirst();
        }

        // Estilo común para inputs
        Control[] inputs = esModoEdicion 
            ? new Control[]{txtNombre, txtDni, txtEmail, txtTelefono} 
            : new Control[]{txtNombre, txtDni, txtEmail, txtTelefono, txtPass};
            
        for (Control c : inputs) {
            c.getStyleClass().add("modal-input");
            if (c instanceof TextField tf) tf.setMaxWidth(Double.MAX_VALUE);
        }
        cbEsp.setMaxWidth(Double.MAX_VALUE);
        cbEsp.getStyleClass().add("modal-combo"); // Asumiendo estilo en CSS

        // --- CONSTRUCCIÓN DEL FORMULARIO ---
        VBox form = new VBox(12);
        form.setPadding(new Insets(10, 0, 10, 0));
        form.setPrefWidth(400);

        form.getChildren().addAll(
                seccion("Nombre completo", txtNombre),
                seccion("DNI", txtDni),
                seccion("Correo electrónico", txtEmail),
                seccion("Teléfono", txtTelefono)
        );

        // La contraseña SOLO se pide en modo creación
        if (!esModoEdicion) {
            form.getChildren().add(seccion("Contraseña de acceso", txtPass));
        }

        form.getChildren().add(seccion("Especialidad técnica", cbEsp));

        dialog.getDialogPane().setContent(form);

        // --- BOTONES DE ACCIÓN ---
        ButtonType btnOk = new ButtonType(esModoEdicion ? "Guardar Cambios" : "Crear Operario", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnOk, ButtonType.CANCEL);

        Button okBtn = (Button) dialog.getDialogPane().lookupButton(btnOk);
        okBtn.getStyleClass().add("btn-primary");
        
        Button cancelBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelBtn.getStyleClass().add("btn-secondary");

        // --- CONVERSOR DE RESULTADOS ---
        dialog.setResultConverter(b -> {
            if (b == btnOk) {
                return new ResultadoOperario(
                        txtNombre.getText().trim(),
                        txtDni.getText().trim(),
                        txtEmail.getText().trim(),
                        txtTelefono.getText().trim(),
                        esModoEdicion ? null : txtPass.getText(),
                        cbEsp.getValue()
                );
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

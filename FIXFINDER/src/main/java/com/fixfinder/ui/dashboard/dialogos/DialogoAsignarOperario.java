package com.fixfinder.ui.dashboard.dialogos;

import com.fixfinder.ui.dashboard.modelos.OperarioFX;
import com.fixfinder.ui.dashboard.modelos.TrabajoFX;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.util.List;
import java.util.Optional;

/**
 * Diálogo modal para asignar o desasignar un operario a una incidencia.
 * Muestra solo operarios activos de la empresa y permite desasignar el actual
 * si ya tiene uno.
 */
public class DialogoAsignarOperario {

    private final TrabajoFX trabajo;
    private final List<OperarioFX> operarios;
    private final String cssPath;

    public DialogoAsignarOperario(TrabajoFX trabajo, List<OperarioFX> operarios, String cssPath) {
        this.trabajo = trabajo;
        this.operarios = operarios;
        this.cssPath = cssPath;
    }

    public Optional<Integer> mostrar() {
        Dialog<Integer> dialog = new Dialog<>();
        dialog.setTitle("Asignar Operario");
        dialog.setHeaderText("Incidencia #" + trabajo.getId() + " — " + trabajo.getTitulo());

        dialog.getDialogPane().getStylesheets().add(cssPath);
        dialog.getDialogPane().getStyleClass().add("dialog-pane");

        VBox content = new VBox(12);
        content.setPadding(new Insets(16, 0, 8, 0));

        Label lblCliente = new Label("Cliente: " + trabajo.getCliente());
        lblCliente.getStyleClass().add("modal-value");

        Label lblEstado = new Label("Estado actual: " + trabajo.getEstado());
        lblEstado.getStyleClass().add("modal-value");

        Label lblSelect = new Label("Seleccionar técnico:");
        lblSelect.getStyleClass().add("modal-label");

        ComboBox<OperarioFX> comboOperarios = new ComboBox<>();
        // Solo mostramos los que no están de baja (activos)
        comboOperarios.getItems().addAll(operarios.stream().filter(OperarioFX::isActivo).toList());
        comboOperarios.setPromptText("Seleccionar técnico...");
        comboOperarios.getStyleClass().add("modal-combo");
        comboOperarios.setMaxWidth(Double.MAX_VALUE);

        if (trabajo.getIdOperario() > 0) {
            operarios.stream()
                    .filter(o -> o.getId() == trabajo.getIdOperario())
                    .findFirst()
                    .ifPresent(comboOperarios::setValue);
        }

        content.getChildren().addAll(lblCliente, lblEstado, lblSelect, comboOperarios);

        boolean tieneOperario = trabajo.getIdOperario() > 0;
        if (tieneOperario) {
            Button btnDesasignar = new Button("Desasignar operario actual");
            btnDesasignar.getStyleClass().add("btn-danger");
            btnDesasignar.setMaxWidth(Double.MAX_VALUE);
            btnDesasignar.setOnAction(e -> {
                dialog.setResult(-1);
                dialog.close();
            });
            content.getChildren().add(btnDesasignar);
        }

        dialog.getDialogPane().setContent(content);

        ButtonType btnAsignar = new ButtonType("Asignar", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(btnAsignar, btnCancelar);

        Button botonAsignar = (Button) dialog.getDialogPane().lookupButton(btnAsignar);
        botonAsignar.getStyleClass().add("btn-primary");
        Button botonCancelar = (Button) dialog.getDialogPane().lookupButton(btnCancelar);
        botonCancelar.getStyleClass().add("btn-secondary");

        dialog.setResultConverter(tipo -> {
            if (tipo == btnAsignar) {
                OperarioFX sel = comboOperarios.getValue();
                return sel != null ? sel.getId() : null;
            }
            return null;
        });

        return dialog.showAndWait();
    }
}

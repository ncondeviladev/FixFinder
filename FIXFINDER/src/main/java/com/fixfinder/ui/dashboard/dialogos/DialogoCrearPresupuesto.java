package com.fixfinder.ui.dashboard.dialogos;

import com.fixfinder.ui.dashboard.modelos.TrabajoFX;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.util.Optional;

/**
 * Diálogo modal para que el gerente cree un presupuesto para una incidencia.
 * Permite indicar precio, descripción del trabajoy actualizar la hoja
 * informativa del trabajo
 * que sirve de comunicación interna entre cliente, gerente y operario.
 */
public class DialogoCrearPresupuesto {

    private final TrabajoFX trabajo;
    private final String nombreEmpresa;
    private final String cssPath;

    public DialogoCrearPresupuesto(TrabajoFX trabajo, String nombreEmpresa, String cssPath) {
        this.trabajo = trabajo;
        this.nombreEmpresa = nombreEmpresa;
        this.cssPath = cssPath;
    }

    public record DatosPresupuesto(double monto, String nuevaDescripcion) {
    }

    public Optional<DatosPresupuesto> mostrar() {
        Dialog<DatosPresupuesto> dialog = new Dialog<>();
        dialog.setTitle("Crear Presupuesto");
        dialog.setHeaderText("Incidencia #" + trabajo.getId() + " — " + trabajo.getTitulo());

        dialog.getDialogPane().getStylesheets().add(cssPath);
        dialog.getDialogPane().getStyleClass().add("dialog-pane");

        VBox content = new VBox(12);
        content.setPadding(new Insets(16, 0, 8, 0));

        Label lblCliente = new Label("Cliente: " + trabajo.getCliente());
        lblCliente.getStyleClass().add("modal-value");

        Label lblEmpresa = new Label("Empresa: " + nombreEmpresa);
        lblEmpresa.getStyleClass().add("modal-value");

        Label lblCategoria = new Label("Categoría: " + trabajo.getCategoria());
        lblCategoria.getStyleClass().add("modal-value");

        Label lblMonto = new Label("Importe del presupuesto (€):");
        lblMonto.getStyleClass().add("modal-label");

        TextField txtMonto = new TextField();
        txtMonto.setPromptText("Ej: 250.00");
        txtMonto.getStyleClass().add("modal-input");
        txtMonto.setMaxWidth(Double.MAX_VALUE);

        Label lblNotas = new Label("Hoja Informativa del Trabajo (Compartida):");
        lblNotas.getStyleClass().add("modal-label");

        TextArea txtNotas = new TextArea();

        // Mostramos la descripción estructurada que nos llega del servidor
        txtNotas.setText(trabajo.getDescripcion());

        txtNotas.setPromptText("Edita la hoja informativa compartida...");
        txtNotas.setPrefRowCount(8);
        txtNotas.setWrapText(true);
        txtNotas.getStyleClass().add("modal-input");
        txtNotas.setMaxWidth(Double.MAX_VALUE);

        content.getChildren().addAll(lblCliente, lblEmpresa, lblCategoria, lblMonto, txtMonto, lblNotas, txtNotas);

        dialog.getDialogPane().setContent(content);

        ButtonType btnEnviar = new ButtonType("Enviar Presupuesto", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(btnEnviar, btnCancelar);

        Button botonEnviar = (Button) dialog.getDialogPane().lookupButton(btnEnviar);
        botonEnviar.getStyleClass().add("btn-primary");
        Button botonCancelar = (Button) dialog.getDialogPane().lookupButton(btnCancelar);
        botonCancelar.getStyleClass().add("btn-secondary");

        botonEnviar.setDisable(true);
        txtMonto.textProperty().addListener((obs, old, val) -> {
            try {
                double v = Double.parseDouble(val.replace(",", "."));
                botonEnviar.setDisable(v <= 0);
            } catch (NumberFormatException e) {
                botonEnviar.setDisable(true);
            }
        });

        dialog.setResultConverter(tipo -> {
            if (tipo == btnEnviar) {
                try {
                    double m = Double.parseDouble(txtMonto.getText().replace(",", "."));
                    return new DatosPresupuesto(m, txtNotas.getText());
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        });

        return dialog.showAndWait();
    }
}

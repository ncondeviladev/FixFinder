package com.fixfinder.ui.dashboard.dialogos;

import com.fixfinder.cliente.RespuestaServidor;
import com.fixfinder.cliente.ServicioCliente;
import com.fixfinder.config.GlobalConfig;
import com.fixfinder.utilidades.ClienteException;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class DialogoRegistroEmpresa {

    private final ServicioCliente servicioCliente;

    public DialogoRegistroEmpresa(ServicioCliente servicioCliente) {
        this.servicioCliente = servicioCliente;
    }

    public void mostrar() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Dar de Alta Nueva Empresa");

        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("panel-dialogo"); // Clase base para fondo oscuro

        Label lblTitulo = new Label("Registro de Empresa y Gerente");
        lblTitulo.getStyleClass().add("titulo-modal");
        
        Label lblSubtitulo = new Label("Estos datos crearán la ficha de tu empresa y el primer usuario (Gerente).");
        lblSubtitulo.getStyleClass().add("texto-sin-asignar");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);

        // --- DATOS EMPRESA ---
        Label lblEmpresa = new Label("1. DATOS DE LA EMPRESA");
        lblEmpresa.getStyleClass().add("titulo-seccion-modal-naranja");
        grid.add(lblEmpresa, 0, 0, 2, 1);

        TextField txtNombreEmpresa = crearCampo("Nombre Comercial");
        grid.add(crearEtiqueta("Nombre:"), 0, 1);
        grid.add(txtNombreEmpresa, 1, 1);

        TextField txtCif = crearCampo("CIF / NIF");
        grid.add(crearEtiqueta("CIF:"), 0, 2);
        grid.add(txtCif, 1, 2);

        TextField txtEmailEmpresa = crearCampo("Email Comercial");
        grid.add(crearEtiqueta("Email Empresa:"), 0, 3);
        grid.add(txtEmailEmpresa, 1, 3);

        TextField txtDireccion = crearCampo("Dirección Física");
        grid.add(crearEtiqueta("Dirección:"), 0, 4);
        grid.add(txtDireccion, 1, 4);

        TextField txtTelefonoEmpresa = crearCampo("Teléfono de la Empresa");
        grid.add(crearEtiqueta("Teléfono Emp:"), 0, 5);
        grid.add(txtTelefonoEmpresa, 1, 5);

        // --- DATOS GERENTE ---
        Label lblGerente = new Label("2. DATOS DEL GERENTE");
        lblGerente.getStyleClass().add("titulo-seccion-modal-verde");
        grid.add(lblGerente, 0, 6, 2, 1);

        TextField txtNombreGerente = crearCampo("Nombre Completo");
        grid.add(crearEtiqueta("Nombre:"), 0, 7);
        grid.add(txtNombreGerente, 1, 7);

        TextField txtDniGerente = crearCampo("DNI / NIE");
        grid.add(crearEtiqueta("DNI:"), 0, 8);
        grid.add(txtDniGerente, 1, 8);

        TextField txtTelefonoGerente = crearCampo("Teléfono del Gerente");
        grid.add(crearEtiqueta("Teléfono:"), 0, 9);
        grid.add(txtTelefonoGerente, 1, 9);

        TextField txtEmailGerente = crearCampo("Email de Acceso");
        grid.add(crearEtiqueta("Email Acceso:"), 0, 10);
        grid.add(txtEmailGerente, 1, 10);

        PasswordField txtPass = new PasswordField();
        txtPass.setPromptText("Contraseña Segura");
        grid.add(crearEtiqueta("Contraseña:"), 0, 11);
        grid.add(txtPass, 1, 11);

        Label lblFeedback = new Label("");
        lblFeedback.getStyleClass().add("estado-ppto-modal-error");

        Button btnRegistrar = new Button("Crear Empresa");
        btnRegistrar.getStyleClass().add("btn-primario");
        
        Button btnCancelar = new Button("Cancelar");
        btnCancelar.getStyleClass().add("btn-secundario");
        btnCancelar.setOnAction(e -> dialog.close());

        HBox btnBox = new HBox(15, btnCancelar, btnRegistrar);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        btnRegistrar.setOnAction(e -> {
            if (txtNombreEmpresa.getText().isEmpty() || txtCif.getText().isEmpty() || 
                txtEmailGerente.getText().isEmpty() || txtPass.getText().isEmpty()) {
                lblFeedback.setText("Por favor, rellena los campos obligatorios.");
                return;
            }

            lblFeedback.getStyleClass().setAll("estado-ppto-modal-ok");
            lblFeedback.setText("Conectando y registrando...");
            btnRegistrar.setDisable(true);

            new Thread(() -> {
                try {
                    if (!servicioCliente.isConectado()) {
                        servicioCliente.conectar(GlobalConfig.getServerIp(), GlobalConfig.PORT);
                    }

                    servicioCliente.setOnMensajeRecibido(json -> {
                        Platform.runLater(() -> {
                            try {
                                RespuestaServidor res = servicioCliente.parseRespuesta(json);
                                if (res.esExito()) {
                                    Alert exito = new Alert(Alert.AlertType.INFORMATION);
                                    exito.setTitle("Registro Completado");
                                    exito.setHeaderText(null);
                                    exito.setContentText("¡Empresa registrada con éxito!\nYa puedes hacer login con el email del gerente.");
                                    exito.showAndWait();
                                    dialog.close();
                                } else {
                                    lblFeedback.getStyleClass().setAll("estado-ppto-modal-error");
                                    lblFeedback.setText(res.getMensaje());
                                    btnRegistrar.setDisable(false);
                                }
                            } catch (ClienteException ex) {
                                lblFeedback.getStyleClass().setAll("estado-ppto-modal-error");
                                lblFeedback.setText("Error al procesar la respuesta.");
                                btnRegistrar.setDisable(false);
                            }
                        });
                    });

                    servicioCliente.enviarRegistroEmpresa(
                        txtNombreEmpresa.getText().trim(),
                        txtCif.getText().trim(),
                        txtEmailEmpresa.getText().trim(),
                        txtTelefonoEmpresa.getText().trim(),
                        txtDireccion.getText().trim(),
                        txtNombreGerente.getText().trim(),
                        txtEmailGerente.getText().trim(),
                        txtPass.getText(),
                        txtDniGerente.getText().trim(),
                        txtTelefonoGerente.getText().trim()
                    );
                } catch (IOException ex) {
                    Platform.runLater(() -> {
                        lblFeedback.getStyleClass().setAll("estado-ppto-modal-error");
                        lblFeedback.setText("Error de red: No se pudo contactar con el Servidor.");
                        btnRegistrar.setDisable(false);
                    });
                }
            }).start();
        });

        root.getChildren().addAll(lblTitulo, lblSubtitulo, grid, lblFeedback, btnBox);

        Scene scene = new Scene(root, 480, 660);
        String css = getClass().getResource("/com/fixfinder/ui/dashboard/dashboard-principal.css").toExternalForm();
        scene.getStylesheets().add(css);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private Label crearEtiqueta(String texto) {
        Label lbl = new Label(texto);
        lbl.getStyleClass().add("etiqueta-modal");
        return lbl;
    }

    private TextField crearCampo(String prompt) {
        TextField txt = new TextField();
        txt.setPromptText(prompt);
        txt.getStyleClass().add("entrada-modal");
        return txt;
    }
}

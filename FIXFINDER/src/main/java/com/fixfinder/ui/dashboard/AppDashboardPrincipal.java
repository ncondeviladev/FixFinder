package com.fixfinder.ui.dashboard;

import com.fixfinder.cliente.RespuestaServidor;
import com.fixfinder.cliente.ServicioCliente;
import com.fixfinder.utilidades.ClienteException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Punto de entrada JavaFX del Dashboard de escritorio.
 * Muestra la pantalla de login y, tras autenticación exitosa como
 * GERENTE/ADMIN,
 * abre la ventana principal del Dashboard ({@link DashboardPrincipal}).
 */
public class AppDashboardPrincipal extends Application {

    private static final String CSS_URL = AppDashboardPrincipal.class
            .getResource("/com/fixfinder/ui/dashboard/dashboard-principal.css")
            .toExternalForm();

    private ServicioCliente servicioCliente;
    private Stage loginStage;

    @Override
    public void start(Stage stage) {
        this.loginStage = stage;
        this.servicioCliente = new ServicioCliente();
        mostrarPantallaLogin(stage);
    }

    private void mostrarPantallaLogin(Stage stage) {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(48));
        root.setStyle("-fx-background-color: #0F1117;");

        StackPane logoBox = new StackPane();
        logoBox.setMinSize(56, 56);
        logoBox.setMaxSize(56, 56);
        logoBox.setStyle("-fx-background-color: #F97316; -fx-background-radius: 14;");
        Label letraLogo = new Label("F");
        letraLogo.setStyle("-fx-text-fill: white; -fx-font-size: 28px; -fx-font-weight: bold;");
        logoBox.getChildren().add(letraLogo);

        Label lblTitulo = new Label("FixFinder");
        lblTitulo.setStyle("-fx-text-fill: #F8FAFC; -fx-font-size: 26px; -fx-font-weight: bold;");

        Label lblSub = new Label("Panel de Gestión — Acceso Gerentes");
        lblSub.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 13px;");

        VBox formBox = new VBox(12);
        formBox.setMaxWidth(360);
        formBox.setPadding(new Insets(28));
        formBox.setStyle("-fx-background-color: #1A1D27; -fx-background-radius: 14; "
                + "-fx-border-color: #2D3348; -fx-border-radius: 14; -fx-border-width: 1;");

        Label lblEmail = new Label("Correo electrónico");
        lblEmail.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 12px; -fx-font-weight: bold;");

        TextField txtEmail = new TextField();
        txtEmail.setPromptText("gerente@empresa.com");
        txtEmail.getStyleClass().add("modal-input");
        txtEmail.setMaxWidth(Double.MAX_VALUE);

        Label lblPass = new Label("Contraseña");
        lblPass.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 12px; -fx-font-weight: bold;");

        PasswordField txtPass = new PasswordField();
        txtPass.setPromptText("Contraseña");
        txtPass.getStyleClass().add("modal-input");
        txtPass.setMaxWidth(Double.MAX_VALUE);

        Label lblError = new Label("");
        lblError.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 12px;");
        lblError.setWrapText(true);

        Button btnEntrar = new Button("Entrar al Panel");
        btnEntrar.getStyleClass().add("btn-primary");
        btnEntrar.setMaxWidth(Double.MAX_VALUE);

        Label lblConexion = new Label("Conectando al servidor...");
        lblConexion.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 11px;");
        lblConexion.setVisible(false);

        formBox.getChildren().addAll(
                lblEmail, txtEmail,
                lblPass, txtPass,
                lblError, btnEntrar, lblConexion);

        root.getChildren().addAll(logoBox, lblTitulo, lblSub, formBox);

        btnEntrar.setOnAction(e -> {
            String email = txtEmail.getText().trim();
            String pass = txtPass.getText();
            if (email.isEmpty() || pass.isEmpty()) {
                lblError.setText("Completa todos los campos.");
                return;
            }
            lblError.setText("");
            btnEntrar.setDisable(true);
            lblConexion.setVisible(true);
            realizarLogin(email, pass, lblError, btnEntrar, lblConexion);
        });

        txtPass.setOnAction(e -> btnEntrar.fire());

        Scene scene = new Scene(root, 480, 520);
        scene.getStylesheets().add(CSS_URL);
        stage.setTitle("FixFinder — Acceso");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    private void realizarLogin(String email, String pass,
            Label lblError, Button btnEntrar, Label lblConexion) {
        new Thread(() -> {
            try {
                if (!servicioCliente.isConectado()) {
                    servicioCliente.conectar("localhost", 5000);
                }

                servicioCliente.setOnMensajeRecibido(json -> Platform.runLater(() -> {
                    try {
                        RespuestaServidor respuesta = servicioCliente.interpretarRespuesta(json);
                        if (respuesta.esExito() && respuesta.getDatos() != null
                                && respuesta.getDatos().has("rol")) {

                            String rol = respuesta.getDatos().get("rol").asText();
                            if (!"GERENTE".equalsIgnoreCase(rol) && !"ADMIN".equalsIgnoreCase(rol)) {
                                lblError.setText("Acceso solo para Gerentes y Administradores.");
                                btnEntrar.setDisable(false);
                                lblConexion.setVisible(false);
                                return;
                            }

                            int id = respuesta.getDatos().get("id").asInt();
                            String nombre = respuesta.getDatos().has("nombreCompleto")
                                    ? respuesta.getDatos().get("nombreCompleto").asText()
                                    : email;
                            int idEmpresa = respuesta.getDatos().has("idEmpresa")
                                    ? respuesta.getDatos().get("idEmpresa").asInt()
                                    : 0;
                            String urlFoto = respuesta.getDatos().has("url_foto")
                                    ? respuesta.getDatos().get("url_foto").asText()
                                    : null;

                            loginStage.close();
                            DashboardPrincipal dashboard = new DashboardPrincipal(
                                    servicioCliente, id, nombre, rol, urlFoto, idEmpresa);
                            dashboard.mostrar();

                        } else {
                            lblError.setText(respuesta.getMensaje().isBlank()
                                    ? "Credenciales incorrectas."
                                    : respuesta.getMensaje());
                            btnEntrar.setDisable(false);
                            lblConexion.setVisible(false);
                        }
                    } catch (ClienteException ex) {
                        lblError.setText("Error procesando respuesta.");
                        btnEntrar.setDisable(false);
                        lblConexion.setVisible(false);
                    }
                }));

                servicioCliente.enviarLogin(email, pass);

            } catch (IOException ex) {
                Platform.runLater(() -> {
                    lblError.setText("No se pudo conectar con el servidor (puerto 5000).");
                    btnEntrar.setDisable(false);
                    lblConexion.setVisible(false);
                });
            }
        }, "hilo-login").start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

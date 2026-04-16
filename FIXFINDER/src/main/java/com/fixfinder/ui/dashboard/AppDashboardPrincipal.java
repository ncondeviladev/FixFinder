package com.fixfinder.ui.dashboard;

import com.fixfinder.cliente.RespuestaServidor;
import com.fixfinder.cliente.ServicioCliente;
import com.fixfinder.config.GlobalConfig;
import com.fixfinder.utilidades.ClienteException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
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

        Circle dot = new Circle(6, javafx.scene.paint.Color.GRAY);
        conectarAServer(dot);

        mostrarPantallaLogin(stage, dot);
    }

    private void mostrarPantallaLogin(Stage stage, Circle dot) {
        StackPane mainStack = new StackPane();
        mainStack.setStyle("-fx-background-color: #0F1117;");

        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(48));
        root.setStyle("-fx-background-color: transparent;");

        ImageView logoImg = new ImageView();
        try {
            Image img = new Image(getClass().getResourceAsStream("/com/fixfinder/ui/dashboard/imagenes/logo.png"));
            logoImg.setImage(img);
            logoImg.setFitWidth(64);
            logoImg.setFitHeight(64);
            logoImg.setPreserveRatio(true);

            // Hacer el logo redondo
            Circle clip = new Circle(32, 32, 32);
            logoImg.setClip(clip);

            stage.getIcons().add(img);
        } catch (Exception e) {
            // Fallback
            StackPane logoBox = new StackPane();
            logoBox.setMinSize(56, 56);
            logoBox.setMaxSize(56, 56);
            logoBox.setStyle("-fx-background-color: #F97316; -fx-background-radius: 14;");
            Label letraLogo = new Label("F");
            letraLogo.setStyle("-fx-text-fill: white; -fx-font-size: 28px; -fx-font-weight: bold;");
            logoBox.getChildren().add(letraLogo);
            root.getChildren().add(logoBox);
        }

        if (logoImg.getImage() != null) {
            root.getChildren().add(logoImg);
        }

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

        Button btnRegistrar = new Button("¿Nueva Empresa? Regístrala aquí");
        btnRegistrar.setStyle("-fx-background-color: transparent; -fx-text-fill: #3B82F6; -fx-underline: true; -fx-cursor: hand;");
        btnRegistrar.setMaxWidth(Double.MAX_VALUE);
        btnRegistrar.setOnAction(e -> {
            new com.fixfinder.ui.dashboard.dialogos.DialogoRegistroEmpresa(servicioCliente).mostrar();
        });

        formBox.getChildren().addAll(
                lblEmail, txtEmail,
                lblPass, txtPass,
                lblError, btnEntrar, btnRegistrar, lblConexion);

        root.getChildren().addAll(lblTitulo, lblSub, formBox);

        mainStack.getChildren().add(root);

        // El Semáforo en la esquina
        StackPane.setAlignment(dot, Pos.TOP_RIGHT);
        StackPane.setMargin(dot, new Insets(15));
        mainStack.getChildren().add(dot);

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

        Scene scene = new Scene(mainStack, 440, 500);
        scene.getStylesheets().add(CSS_URL);
        stage.setTitle("FixFinder — Acceso");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    private void conectarAServer(Circle dot) {
        if (!GlobalConfig.MODO_NUBE) {
            dot.setFill(javafx.scene.paint.Color.DODGERBLUE);
            return;
        }
        new Thread(() -> {
            try (java.net.Socket socket = new java.net.Socket()) {
                socket.connect(new java.net.InetSocketAddress(GlobalConfig.getServerIp(), GlobalConfig.PORT), 2000);
                Platform.runLater(() -> dot.setFill(javafx.scene.paint.Color.web("#22C55E")));
            } catch (Exception e) {
                Platform.runLater(() -> dot.setFill(javafx.scene.paint.Color.GRAY));
            }
        }).start();
    }

    private void realizarLogin(String email, String pass,
            Label lblError, Button btnEntrar, Label lblConexion) {
        new Thread(() -> {
            try {
                if (!servicioCliente.isConectado()) {
                    servicioCliente.conectar(GlobalConfig.getServerIp(), GlobalConfig.PORT);
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

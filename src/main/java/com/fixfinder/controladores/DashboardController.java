package com.fixfinder.controladores;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fixfinder.cliente.ClienteSocket;
import javafx.scene.control.Label;

import com.fixfinder.utilidades.ClienteException;
import java.io.IOException;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;

public class DashboardController {

    @FXML
    private Button btnConectar;
    @FXML
    private Button btnPing;
    @FXML
    private TextArea txtLog;
    @FXML
    private TextField txtStatus;

    // Campos Registro Empresa
    @FXML
    private TextField txtNombreEmpresa;
    @FXML
    private TextField txtCif;
    @FXML
    private TextField txtEmailEmpresa;
    @FXML
    private TextField txtDireccionEmpresa;
    @FXML
    private TextField txtNombreGerente;
    @FXML
    private TextField txtEmailGerente;
    @FXML
    private PasswordField txtPassGerente;
    @FXML
    private TextField txtDniGerente;
    @FXML
    private Button btnRegEmpresa;
    @FXML
    private Label lblStatusEmpresa;

    // Campos Registro Usuario Generico (Cli/Op)
    @FXML
    private RadioButton rbCliente;
    @FXML
    private RadioButton rbOperario;
    @FXML
    private TextField txtNombreUsuario;
    @FXML
    private TextField txtDniUsuario;
    @FXML
    private TextField txtEmailUsuario;
    @FXML
    private PasswordField txtPassUsuario;
    @FXML
    private TextField txtTelefonoUsuario;
    @FXML
    private TextField txtDireccionUsuario;

    @FXML
    private HBox boxIdEmpresa;
    @FXML
    private TextField txtIdEmpresaOperario;
    @FXML
    private Button btnRegUsuario;
    @FXML
    private Label lblStatusUsuario;

    // Campos Login
    @FXML
    private TextField txtLoginEmail;
    @FXML
    private PasswordField txtLoginPassword;
    @FXML
    private Button btnLogin;
    @FXML
    private Button btnLogout;
    @FXML
    private Label lblUsuarioLogueado;
    @FXML
    private Label lblStatusLogin;

    // Campos Crear Trabajo
    @FXML
    private TextField txtTituloTrabajo;
    @FXML
    private TextArea txtDescripcionTrabajo;
    @FXML
    private TextField txtDireccionTrabajo;
    @FXML
    private javafx.scene.control.ComboBox<String> comboCategoria;
    @FXML
    private javafx.scene.control.ComboBox<String> comboUrgencia;
    @FXML
    private Button btnCrearTrabajo;
    @FXML
    private Label lblStatusTrabajo;
    // Variables de estado para guardar la sesión en memoria
    private Integer usuarioLogueadoId = null;
    private String usuarioLogueadoNombre = null;
    private String usuarioLogueadoRol = null;

    private ClienteSocket cliente;
    private final ObjectMapper mapper = new ObjectMapper();
    private final ToggleGroup tipoUsuarioGroup = new ToggleGroup();

    public void initialize() {
        cliente = new ClienteSocket();

        // Configurar RadioButtons
        rbCliente.setToggleGroup(tipoUsuarioGroup);
        rbOperario.setToggleGroup(tipoUsuarioGroup);

        // Inicializar ComboBox Categorias
        if (comboCategoria != null) {
            comboCategoria.getItems().addAll("FONTANERIA", "ELECTRICIDAD", "ALBAÑILERIA", "CARPINTERIA", "PINTURA",
                    "LIMPIEZA", "OTROS");
            comboCategoria.getSelectionModel().selectFirst();
        }

        // Inicializar ComboBox Urgencia (Visual)
        if (comboUrgencia != null) {
            comboUrgencia.getItems().addAll("Normal", "Prioridad", "Urgente");
            comboUrgencia.getSelectionModel().selectFirst();
        }

        // Listener para habilitar/deshabilitar campo ID Empresa
        tipoUsuarioGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            boolean esOperario = rbOperario.isSelected();
            boxIdEmpresa.setDisable(!esOperario);
        });

        // Bloquear botones hasta conectar
        toggleBotones(true);
        btnConectar.setDisable(false);

        cliente.setOnMensajeRecibido(json -> {
            Platform.runLater(() -> {
                try {
                    procesarRespuesta(json);
                } catch (ClienteException e) {
                    log("❌ Error crítico: " + e.getMessage());
                    // Aquí en el futuro mostraremos un Alert(Error);
                }
            });
        });

    }

    @FXML
    private void onConectarClick() {
        try {
            if (!cliente.isConectado()) {
                log("⏳ Conectando...");
                cliente.conectar("localhost", 5000);
                txtStatus.setText("CONECTADO");
                txtStatus.setStyle("-fx-text-fill: green;");
                btnConectar.setText("Desconectar");
                toggleBotones(false); // Habilitar
                log("✅ Conexión establecida.");
            } else {
                cliente.desconectar();
                txtStatus.setText("DESCONECTADO");
                txtStatus.setStyle("-fx-text-fill: red;");
                btnConectar.setText("Conectar");
                toggleBotones(true); // Deshabilitar
                log("❌ Desconectado.");
            }
        } catch (IOException e) {
            log("🔥 Error de conexión: " + e.getMessage());
        }
    }

    private void toggleBotones(boolean disable) {
        btnPing.setDisable(disable);
        btnRegEmpresa.setDisable(disable);
        btnRegUsuario.setDisable(disable);
    }

    @FXML
    private void onPingClick() {
        enviarJson("PING", mapper.createObjectNode()); // Datos vacíos
    }

    @FXML
    private void onLimpiarLogClick() {
        txtLog.clear();
    }

    @FXML
    private void onLoginClick() {
        lblStatusLogin.setText("Conectando...");
        lblStatusLogin.setStyle("-fx-text-fill: gray;");

        ObjectNode datos = mapper.createObjectNode();

        datos.put("email", txtLoginEmail.getText());
        datos.put("password", txtLoginPassword.getText());
        enviarJson("LOGIN", datos);
    }

    @FXML
    private void onLogoutClick() {
        usuarioLogueadoId = null;
        usuarioLogueadoNombre = null;
        usuarioLogueadoRol = null;

        lblUsuarioLogueado.setText("Ninguna");
        lblUsuarioLogueado.setStyle("-fx-text-fill: #7f8c8d;");

        lblStatusLogin.setText("");
        btnLogin.setDisable(false);
        btnLogout.setDisable(true);

        // Deshabilitar Crear Trabajo
        btnCrearTrabajo.setDisable(true);
        lblStatusTrabajo.setText("Debes iniciar sesión para solicitar servicios");

        log("🔒 Sesión cerrada localmente.");
    }

    @FXML
    private void onCrearTrabajoClick() {
        if (usuarioLogueadoId == null) {
            log("⚠️ Error: No hay usuario logueado.");
            return;
        }

        ObjectNode datos = mapper.createObjectNode();
        datos.put("idCliente", usuarioLogueadoId);
        datos.put("titulo", txtTituloTrabajo.getText());
        datos.put("descripcion", txtDescripcionTrabajo.getText());
        datos.put("direccion", txtDireccionTrabajo.getText());

        // Convertir selección visual a entero (1=Normal, 2=Prioridad, 3=Urgente)
        int nivelUrgencia = 1;
        String seleccion = comboUrgencia.getValue();
        if ("Prioridad".equals(seleccion))
            nivelUrgencia = 2;
        if ("Urgente".equals(seleccion))
            nivelUrgencia = 3;
        datos.put("urgencia", nivelUrgencia);

        datos.put("categoria", comboCategoria.getValue());

        enviarJson("CREAR_TRABAJO", datos);
    }

    @FXML
    private void onRegistrarEmpresaClick() {
        ObjectNode datos = mapper.createObjectNode();
        datos.put("tipo", "EMPRESA");
        datos.put("nombreEmpresa", txtNombreEmpresa.getText());
        datos.put("cif", txtCif.getText());
        datos.put("emailEmpresa", txtEmailEmpresa.getText());
        datos.put("direccion", txtDireccionEmpresa.getText());

        // Datos Gerente
        datos.put("nombreGerente", txtNombreGerente.getText());
        datos.put("emailGerente", txtEmailGerente.getText());
        datos.put("password", txtPassGerente.getText());
        datos.put("dniGerente", txtDniGerente.getText());

        enviarJson("REGISTRO", datos);
    }

    @FXML
    private void onRegistrarUsuarioClick() {
        ObjectNode datos = mapper.createObjectNode();

        boolean esOperario = rbOperario.isSelected();
        datos.put("tipo", esOperario ? "OPERARIO" : "CLIENTE"); // Tipo dinámico

        // Mapeo de campos comunes a los nombres que espera el servidor
        if (esOperario) {
            datos.put("nombreOperario", txtNombreUsuario.getText());
            datos.put("dniOperario", txtDniUsuario.getText());
            datos.put("emailOperario", txtEmailUsuario.getText());
            datos.put("passwordOperario", txtPassUsuario.getText());
            datos.put("telefonoOperario", txtTelefonoUsuario.getText());
            datos.put("idEmpresa", txtIdEmpresaOperario.getText()); // Clave para operario
        } else {
            datos.put("nombre", txtNombreUsuario.getText());
            datos.put("dni", txtDniUsuario.getText());
            datos.put("email", txtEmailUsuario.getText());
            datos.put("password", txtPassUsuario.getText());
            datos.put("telefono", txtTelefonoUsuario.getText());
            datos.put("direccion", txtDireccionUsuario.getText());
        }

        enviarJson("REGISTRO", datos);
    }

    private void enviarJson(String accion, ObjectNode datos) {
        try {
            cliente.enviar(accion, datos);
            log("📤 Enviado (" + accion + "): " + (datos != null ? datos.toString() : "null"));
        } catch (IOException e) {
            log("Error enviando: " + e.getMessage());
        }
    }

    private void procesarRespuesta(String json) {
        try {
            JsonNode datos = mapper.readTree(json);

            int status = datos.has("status") ? datos.get("status").asInt() : 0;
            String mensaje = datos.has("mensaje") ? datos.get("mensaje").asText() : "";

            // 1. GESTIÓN DE LOGIN
            if (status == 200) {
                if (datos.has("datos") && datos.get("datos").has("id") && datos.get("datos").has("rol")) {
                    JsonNode datosUsuario = datos.get("datos");
                    usuarioLogueadoId = datosUsuario.get("id").asInt();
                    usuarioLogueadoNombre = datosUsuario.has("nombreCompleto")
                            ? datosUsuario.get("nombreCompleto").asText()
                            : "Usuario Desconocido";
                    usuarioLogueadoRol = datosUsuario.get("rol").asText();

                    lblUsuarioLogueado.setText(
                            usuarioLogueadoNombre + " (" + usuarioLogueadoRol + ") - ID: " + usuarioLogueadoId);
                    lblUsuarioLogueado.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");

                    // Limpiar errores previos
                    lblStatusLogin.setText("Login Correcto ✅");
                    lblStatusLogin.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");

                    // Activar Logout / Desactivar Login
                    btnLogin.setDisable(true);
                    btnLogout.setDisable(false);

                    // Habilitar Crear Trabajo si es CLIENTE
                    if ("CLIENTE".equalsIgnoreCase(usuarioLogueadoRol)) {
                        btnCrearTrabajo.setDisable(false);
                        lblStatusTrabajo.setText("Listo para solicitar servicios como " + usuarioLogueadoNombre);
                        lblStatusTrabajo.setStyle("-fx-text-fill: green;");
                    } else {
                        lblStatusTrabajo.setText(
                                "Solo los CLIENTES pueden solicitar servicios (Eres " + usuarioLogueadoRol + ")");
                        lblStatusTrabajo.setStyle("-fx-text-fill: orange;");
                    }

                    log("✅ Sesión iniciada correctamente.");
                }
            } else if (status == 401 || mensaje.toLowerCase().contains("credenciales")
                    || mensaje.toLowerCase().contains("login")) {
                // Error específico de Login o Autenticación
                lblStatusLogin.setText(mensaje + " ❌");
                lblStatusLogin.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                btnLogin.setDisable(false);
            }

            // 2. GESTIÓN DE FEEDBACK REGISTROS
            String colorStyle = (status >= 200 && status < 300) ? "-fx-text-fill: green;" : "-fx-text-fill: red;";

            if (mensaje.contains("Empresa")) {
                lblStatusEmpresa.setText(mensaje + (status >= 200 && status < 300 ? " ✅" : " ❌"));
                lblStatusEmpresa.setStyle(colorStyle + " -fx-font-weight: bold;");
            }

            if (mensaje.contains("Operario") || mensaje.contains("Cliente") || mensaje.contains("usuario")) {
                lblStatusUsuario.setText(mensaje + (status >= 200 && status < 300 ? " ✅" : " ❌"));
                lblStatusUsuario.setStyle(colorStyle + " -fx-font-weight: bold;");
            }

            // 4. FEEDBACK CREAR TRABAJO
            if (mensaje.contains("Trabajo creado")) {
                lblStatusTrabajo.setText(mensaje + " ✅");
                lblStatusTrabajo.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                // Limpiar campos
                txtTituloTrabajo.clear();
                txtDescripcionTrabajo.clear();
                txtDireccionTrabajo.clear();
            } else if (status >= 400 && usuarioLogueadoId != null && !mensaje.contains("login")
                    && !mensaje.contains("Empresa") && !mensaje.contains("Cliente")) {
                // Si hay error y estamos logueados, asumimos que puede ser de aquí
                lblStatusTrabajo.setText("Error: " + mensaje);
                lblStatusTrabajo.setStyle("-fx-text-fill: red;");
            }

            // 3. LOG ERRORES GENÉRICOS
            if (status >= 400) {
                log("⚠️ Servidor reporta error (" + status + "): " + mensaje);
            }
        } catch (JsonProcessingException e) {
            throw new ClienteException("Error de formato en el JSON recibido", e);
        } catch (Exception e) {
            throw new ClienteException("Error inesperado procesando la respuesta", e);
        }
    }

    private void log(String mensaje) {
        txtLog.appendText(mensaje + "\n");
    }
}

package com.fixfinder.controladores;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fixfinder.cliente.ClienteSocket;
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

    private ClienteSocket cliente;
    private final ObjectMapper mapper = new ObjectMapper();
    private final ToggleGroup tipoUsuarioGroup = new ToggleGroup();

    public void initialize() {
        cliente = new ClienteSocket();

        // Configurar RadioButtons
        rbCliente.setToggleGroup(tipoUsuarioGroup);
        rbOperario.setToggleGroup(tipoUsuarioGroup);

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
                log("📩 Servidor: " + json);
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

    private void log(String mensaje) {
        txtLog.appendText(mensaje + "\n");
    }
}

package com.fixfinder.controladores;

import com.fixfinder.cliente.ClienteSocket;
import java.io.IOException;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class DashboardController {

    @FXML
    private Button btnConectar;
    @FXML
    private Button btnPing;
    @FXML
    private TextArea txtLog;
    @FXML
    private TextField txtStatus;

    private ClienteSocket cliente;

    public void initialize() {
        cliente = new ClienteSocket();

        // Configurar qué hacer cuando llega un mensaje del servidor
        cliente.setOnMensajeRecibido(json -> {
            // JavaFX requiere que las actualizaciones de UI sean en su hilo principal
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
                btnPing.setDisable(false);
                log("✅ Conexión establecida.");
            } else {
                cliente.desconectar();
                txtStatus.setText("DESCONECTADO");
                txtStatus.setStyle("-fx-text-fill: red;");
                btnConectar.setText("Conectar");
                btnPing.setDisable(true);
                log("❌ Desconectado.");
            }
        } catch (IOException e) {
            log("🔥 Error de conexión: " + e.getMessage());
        }
    }

    @FXML
    private void onPingClick() {
        try {
            // Enviar un PING simple
            cliente.enviar("PING", null);
            log("📤 Enviado: PING");
        } catch (IOException e) {
            log("Error enviando ping: " + e.getMessage());
        }
    }

    private void log(String mensaje) {
        txtLog.appendText(mensaje + "\n");
    }
}

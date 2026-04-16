package com.fixfinder.panelprototipo;

import com.fixfinder.config.GlobalConfig;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * Cliente de red especializado para el Simulador.
 * Gestiona todas las peticiones asíncronas al servidor de forma centralizada.
 * Implementa el patrón Singleton para asegurar una única conexión.
 */
public class ClienteRedSimulador {

    private static ClienteRedSimulador instance;
    private final ObjectMapper mapper = new ObjectMapper();

    public static synchronized ClienteRedSimulador getInstance() {
        if (instance == null) {
            instance = new ClienteRedSimulador();
        }
        return instance;
    }

    public JsonNode enviarSolicitud(ObjectNode requestJson) {
        try (Socket socket = new Socket(GlobalConfig.getServerIp(), GlobalConfig.PORT);
                DataInputStream entrada = new DataInputStream(socket.getInputStream());
                DataOutputStream salida = new DataOutputStream(socket.getOutputStream())) {

            byte[] requestBytes = mapper.writeValueAsString(requestJson)
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8);
            salida.writeInt(requestBytes.length);
            salida.write(requestBytes);

            int resLength = entrada.readInt();
            if (resLength <= 0 || resLength > 10485760)
                return null;
            byte[] responseBytes = new byte[resLength];
            entrada.readFully(responseBytes);
            String respuestaJson = new String(responseBytes, java.nio.charset.StandardCharsets.UTF_8);

            return mapper.readTree(respuestaJson);

        } catch (IOException e) {
            Platform.runLater(() -> {
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Error de Conexión");
                errorAlert.setHeaderText("No se pudo conectar con el Servidor Central desde el Simulador");
                errorAlert.setContentText(
                        "Asegúrate de que el servidor esté encendido en el puerto 5000.");
                errorAlert.show();
            });
            return null;
        }
    }

    public void enviarSolicitudAsync(ObjectNode request, Consumer<JsonNode> callback) {
        new Thread(() -> {
            JsonNode res = enviarSolicitud(request);
            callback.accept(res);
        }).start();
    }

    public void enviarAccion(String accion, Consumer<ObjectNode> datosBuilder, Runnable onSuccess, Consumer<String> onError) {
        new Thread(() -> {
            try {
                ObjectNode request = mapper.createObjectNode();
                request.put("accion", accion);
                request.put("token", "GOD_MODE");
                ObjectNode datos = request.putObject("datos");
                datosBuilder.accept(datos);

                JsonNode respuesta = enviarSolicitud(request);

                Platform.runLater(() -> {
                    if (respuesta != null && respuesta.get("status").asInt() == 200) {
                        onSuccess.run();
                    } else if (respuesta != null) {
                        onError.accept(respuesta.get("mensaje").asText());
                    } else {
                        onError.accept("Sin respuesta del servidor");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> onError.accept(e.getMessage()));
            }
        }).start();
    }

    public ObjectMapper getMapper() {
        return mapper;
    }
}

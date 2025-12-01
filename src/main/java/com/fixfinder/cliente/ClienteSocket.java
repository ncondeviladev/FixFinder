package com.fixfinder.cliente;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * Gestiona la conexión Socket desde el lado del Cliente (Escritorio).
 */
public class ClienteSocket {

    private Socket socket;
    private DataInputStream entrada;
    private DataOutputStream salida;
    private boolean conectado = false;
    private Consumer<String> onMensajeRecibido; // Callback para actualizar UI
    private final ObjectMapper mapper = new ObjectMapper();

    public void conectar(String host, int puerto) throws IOException {
        this.socket = new Socket(host, puerto);
        this.entrada = new DataInputStream(socket.getInputStream());
        this.salida = new DataOutputStream(socket.getOutputStream());
        this.conectado = true;

        // Iniciar hilo de escucha (para recibir mensajes asíncronos del servidor)
        new Thread(this::escucharServidor).start();
    }

    public void enviar(String accion, ObjectNode datos) throws IOException {
        if (!conectado)
            throw new IOException("No conectado al servidor");

        ObjectNode mensaje = mapper.createObjectNode();
        mensaje.put("accion", accion);
        if (datos != null) {
            mensaje.set("datos", datos);
        }

        salida.writeUTF(mapper.writeValueAsString(mensaje));
        salida.flush();
    }

    private void escucharServidor() {
        try {
            while (conectado) {
                String json = entrada.readUTF();
                if (onMensajeRecibido != null) {
                    // Ejecutar en el hilo de JavaFX si es necesario, pero aquí solo pasamos el
                    // string
                    onMensajeRecibido.accept(json);
                }
            }
        } catch (IOException e) {
            conectado = false;
            System.out.println("Desconectado del servidor: " + e.getMessage());
        }
    }

    public void desconectar() {
        conectado = false;
        try {
            if (socket != null)
                socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setOnMensajeRecibido(Consumer<String> callback) {
        this.onMensajeRecibido = callback;
    }

    public boolean isConectado() {
        return conectado;
    }

    public ObjectMapper getMapper() {
        return mapper;
    }
}

package com.fixfinder.servicios;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.fixfinder.utilidades.ServiceException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.Semaphore;

/**
 * Hilo dedicado a atender a UN solo cliente.
 * Lee JSONs, ejecuta acciones en los DAOs y responde.
 * Implementa Runnable para ser ejecutado en un Thread.
 */
public class GestorCliente implements Runnable {

    private final Socket socket;
    private final Semaphore semaforo;
    private DataInputStream entrada;
    private DataOutputStream salida;

    // DAOs necesarios

    public GestorCliente(Socket socket, Semaphore semaforo) {
        this.socket = socket;
        this.semaforo = semaforo;

    }

    @Override
    public void run() {
        try {
            entrada = new DataInputStream(socket.getInputStream());
            salida = new DataOutputStream(socket.getOutputStream());

            // Bucle principal de comunicación con este cliente
            while (!socket.isClosed()) {
                try {
                    // 1. Leer mensaje (JSON en formato String UTF)
                    // readUTF es bloqueante, espera hasta recibir datos
                    String jsonRecibido = entrada.readUTF();
                    System.out.println("📩 Recibido: " + jsonRecibido);

                    // 2. Procesar el comando
                    String respuestaJson = procesarComando(jsonRecibido);

                    // 3. Enviar respuesta
                    salida.writeUTF(respuestaJson);
                    salida.flush();

                } catch (IOException e) {
                    // Cliente se desconectó abruptamente o error de red
                    throw new ServiceException("Error de comunicación con cliente", e);
                }
            }

        } catch (ServiceException e) {
            System.out.println("ℹ️ Fin de sesión: " + socket.getInetAddress() + " (" + e.getMessage() + ")");
        } catch (Exception e) {
            System.err.println("🔥 Error inesperado en hilo cliente: " + e.getMessage());
        } finally {
            cerrarRecursos();
            // IMPORTANTE: Liberar el permiso del semáforo al terminar
            semaforo.release();
            System.out.println("🔓 Conexión liberada. Huecos disponibles: " + semaforo.availablePermits());
        }
    }

    private String procesarComando(String json) {
        try {
            // Parsear el JSON recibido
            JsonNode rootNode = ServidorCentral.jsonMapper.readTree(json);
            String accion = rootNode.get("accion").asText();

            ObjectNode respuesta = ServidorCentral.jsonMapper.createObjectNode();

            switch (accion) {
                case "LOGIN":
                    // Lógica de login (ejemplo simplificado)
                    if (rootNode.has("datos") && rootNode.get("datos").has("email")) {
                        String email = rootNode.get("datos").get("email").asText();
                        respuesta.put("status", 200);
                        respuesta.put("mensaje", "Login OK (Simulado) para " + email);
                    } else {
                        respuesta.put("status", 400);
                        respuesta.put("mensaje", "Datos incompletos para LOGIN");
                    }
                    break;

                case "PING":
                    respuesta.put("status", 200);
                    respuesta.put("mensaje", "PONG");
                    break;

                default:
                    respuesta.put("status", 400);
                    respuesta.put("mensaje", "Acción no reconocida: " + accion);
            }

            return ServidorCentral.jsonMapper.writeValueAsString(respuesta);

        } catch (Exception e) {
            return "{\"status\": 500, \"mensaje\": \"Error interno procesando JSON\"}";
        }
    }

    private void cerrarRecursos() {
        try {
            if (socket != null && !socket.isClosed())
                socket.close();
            if (entrada != null)
                entrada.close();
            if (salida != null)
                salida.close();
        } catch (IOException e) {
            // Ignoramos errores al cerrar
        }
    }
}

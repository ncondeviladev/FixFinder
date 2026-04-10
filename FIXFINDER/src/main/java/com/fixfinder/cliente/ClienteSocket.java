package com.fixfinder.cliente;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

/**
 * Gestiona la conexión Socket desde el lado del Cliente (Escritorio).
 * Implementa el patrón "Lector Avaro" para evitar el Síndrome del Embudo TCP.
 */
public class ClienteSocket {

    private Socket socket;
    private DataInputStream entrada;
    private DataOutputStream salida;
    private boolean conectado = false;
    private Consumer<String> onMensajeRecibido;
    private final ObjectMapper mapper = new ObjectMapper();

    // COLA DE MENSAJES: El hilo de red deposita aquí los JSONs lo más rápido posible.
    private final LinkedBlockingQueue<String> colaMensajes = new LinkedBlockingQueue<>();
    private Thread hiloLector;
    private Thread hiloProcesador;

    public void conectar(String host, int puerto) throws IOException {
        System.out.println("🔌 [SOCKET-DEBUG] Intentando conectar a " + host + ":" + puerto);
        this.socket = new Socket(host, puerto);
        this.entrada = new DataInputStream(socket.getInputStream());
        this.salida = new DataOutputStream(socket.getOutputStream());
        this.conectado = true;

        // 1. Iniciamos el hilo PROCESADOR (el que saca de la cola y avisa a la UI)
        iniciarHiloProcesador();

        // 2. Iniciamos el hilo LECTOR AVARO (el que solo lee de la red y llena la cola)
        this.hiloLector = new Thread(this::escucharServidor, "Hilo-Lector-Avaro");
        this.hiloLector.setDaemon(true);
        this.hiloLector.start();
        
        System.out.println("🚀 [SOCKET-DEBUG] Hilos de comunicación (Lector + Procesador) operativos.");
    }

    public synchronized void enviar(String accion, ObjectNode datos, String token) throws IOException {
        if (!conectado)
            throw new IOException("No conectado al servidor");

        ObjectNode mensaje = mapper.createObjectNode();
        mensaje.put("accion", accion);

        if (token != null) {
            mensaje.put("token", token);
        }

        if (datos != null) {
            mensaje.set("datos", datos);
        }

        String json = mapper.writeValueAsString(mensaje);
        byte[] bytes = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        
        // Bloqueamos la salida para asegurar que un mensaje no se mezcle con otro (Safe Thread)
        salida.writeInt(bytes.length);
        salida.write(bytes);
        salida.flush();
    }

    /**
     * HILO LECTOR AVARO: 
     * Su única misión es vaciar el buffer del Sistema Operativo lo más rápido posible.
     * No parsea, no actualiza UI, solo lee y encola.
     */
    private void escucharServidor() {
        try {
            while (conectado && !socket.isClosed()) {
                // Leemos la longitud del mensaje (4 bytes)
                int length = entrada.readInt();
                
                if (length <= 0 || length > 10485760) { // Límite 10MB
                    System.err.println("❌ [SOCKET-ERROR] Recibida longitud inválida: " + length);
                    continue;
                }

                // Leemos el payload completo
                byte[] bytes = new byte[length];
                entrada.readFully(bytes);
                String json = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);

                // ENCOLADO INMEDIATO
                colaMensajes.offer(json);
                
                int limit = Math.min(json.length(), 50);
                System.out.println("📥 [SOCKET-AVARO] Mensaje encolado (" + length + " bytes). Inicio: " + json.substring(0, limit) + "...");
            }
        } catch (IOException e) {
            if (conectado) {
                System.err.println("🔌 [SOCKET-INFO] Conexión cerrada por el servidor o error de red: " + e.getMessage());
                conectado = false;
            }
        }
    }

    /**
     * HILO PROCESADOR:
     * Consume la cola interna y notifica al suscriptor (normalmente el Controller de JFX).
     * Así, si el procesamiento es lento, la red no se detiene.
     */
    private void iniciarHiloProcesador() {
        this.hiloProcesador = new Thread(() -> {
            try {
                while (conectado) {
                    String json = colaMensajes.take(); // Bloqueante si la cola está vacía
                    if (onMensajeRecibido != null) {
                        onMensajeRecibido.accept(json);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Hilo-Procesador-Mensajes");
        this.hiloProcesador.setDaemon(true);
        this.hiloProcesador.start();
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

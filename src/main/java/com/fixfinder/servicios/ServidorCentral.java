package com.fixfinder.servicios;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Semaphore;

/**
 * Servidor Central de Sockets.
 * Escucha conexiones en el puerto 5000 y delega cada cliente a un hilo
 * independiente.
 * 
 * Implementa control de concurrencia mediante Semáforos (Requisito PSP).
 */
public class ServidorCentral {

    private static final int PUERTO = 5000;
    private static final int MAX_CONEXIONES = 10; // Limitamos a 10 clientes simultáneos

    private boolean ejecutando = true;
    private final Semaphore semaforo; // Control de acceso concurrente

    // Jackson ObjectMapper para parsear JSON (Singleton para toda la app)
    public static final ObjectMapper jsonMapper = new ObjectMapper();

    public ServidorCentral() {
        // Inicializamos el semáforo con los permisos máximos
        this.semaforo = new Semaphore(MAX_CONEXIONES);
    }

    public void iniciar() {
        System.out.println("🚀 Iniciando Servidor FIXFINDER en puerto " + PUERTO + "...");
        System.out.println("ℹ️ Máximo de conexiones simultáneas: " + MAX_CONEXIONES);

        try (ServerSocket serverSocket = new ServerSocket(PUERTO)) {
            System.out.println("✅ Servidor esperando conexiones...");

            while (ejecutando) {
                // 1. Esperar a que llegue un cliente (se bloquea aquí hasta que alguien se
                // conecta)
                // Socket es como el "cable" virtual que nos une a ese cliente específico.
                Socket socketCliente = serverSocket.accept();

                // 2. Intentar adquirir permiso del semáforo (El "Portero" de la discoteca)
                if (semaforo.tryAcquire()) {
                    System.out.println("🔌 Nuevo cliente conectado: " + socketCliente.getInetAddress());
                    System.out.println("📊 Conexiones activas: " + (MAX_CONEXIONES - semaforo.availablePermits()) + "/"
                            + MAX_CONEXIONES);

                    // 3. Crear el Runnable (GestorCliente)
                    // Le pasamos el socket para que hable con el cliente y el semáforo para que
                    // avise al salir.
                    GestorCliente gestor = new GestorCliente(socketCliente, semaforo);

                    // 4. Lanzar el Hilo manualmente (Requisito académico)
                    // Creamos un nuevo hilo de ejecución para que el servidor pueda volver a
                    // escuchar
                    // mientras este hilo atiende al cliente.
                    Thread hiloCliente = new Thread(gestor);
                    hiloCliente.start();
                } else {
                    System.err
                            .println("⛔ Servidor saturado. Rechazando conexión de: " + socketCliente.getInetAddress());
                    socketCliente.close();
                }
            }

        } catch (IOException e) {
            System.err.println("🔥 Error crítico en el servidor: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        new ServidorCentral().iniciar();
    }
}

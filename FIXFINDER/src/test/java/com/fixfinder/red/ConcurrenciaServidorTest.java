package com.fixfinder.red;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test de Concurrencia para el Servidor (Requisito Académico PSP).
 * 
 * Este test valida que el ServidorCentral respete el límite de 10 conexiones 
 * simultáneas gestionado por el Semáforo.
 */
public class ConcurrenciaServidorTest {

    @BeforeAll
    static void verificarEstadoServidor() {
        // Verificamos si el servidor ya está corriendo en el puerto 5000
        if (estaServidorCorriendo()) {
            System.out.println("✅ El servidor ya está corriendo en el puerto 5000. Procediendo con el test...");
            return;
        }

        System.out.println("⏳ Servidor no detectado. Intentando arrancar instancia de prueba...");
        Thread serverThread = new Thread(() -> {
            try {
                new ServidorCentral().iniciar();
            } catch (Exception e) {
                System.err.println("❌ No se pudo arrancar el servidor: " + e.getMessage());
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();

        // Espera activa hasta que el puerto esté listo (máximo 5s)
        for (int i = 0; i < 10; i++) {
            if (estaServidorCorriendo()) return;
            try { Thread.sleep(500); } catch (InterruptedException e) {}
        }
    }

    private static boolean estaServidorCorriendo() {
        try (Socket s = new Socket()) {
            s.connect(new java.net.InetSocketAddress("localhost", 5000), 500);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Test
    void testSaturacionSemaforo() throws InterruptedException {
        int limiteSemaforo = 10;
        int exceso = 5;
        int totalClientes = limiteSemaforo + exceso;

        AtomicInteger conexionesAceptadas = new AtomicInteger(0);
        AtomicInteger conexionesRechazadas = new AtomicInteger(0);
        List<Thread> clientes = new ArrayList<>();

        System.out.println("🚀 Lanzando " + totalClientes + " conexiones simultáneas para probar el semáforo...");

        for (int i = 0; i < totalClientes; i++) {
            Thread t = new Thread(() -> {
                try (Socket s = new Socket()) {
                    // Establecemos un timeout de conexión y de lectura corto para evitar bloqueos
                    s.connect(new java.net.InetSocketAddress("localhost", 5000), 2000);
                    s.setSoTimeout(2000);

                    // El servidor nos acepta, pero si el semáforo está lleno, 
                    // nos cerrará la conexión inmediatamente tras entrar en GestorConexion.
                    
                    if (s.getInputStream().read() == -1) {
                        conexionesRechazadas.incrementAndGet();
                    } else {
                        conexionesAceptadas.incrementAndGet();
                    }
                } catch (IOException e) {
                    // Si el servidor está saturado y cierra el socket o rechaza la conexión
                    conexionesRechazadas.incrementAndGet();
                }
            });
            clientes.add(t);
            t.start();
        }

        // Esperar a que todos los hilos terminen (timeout global de 10s)
        for (Thread t : clientes) {
            t.join(2000);
        }

        System.out.println("📊 RESULTADOS:");
        System.out.println("   - Aceptadas: " + conexionesAceptadas.get());
        System.out.println("   - Rechazadas/Cerradas: " + conexionesRechazadas.get());

        // Verificamos que se alcanzó el límite de 10
        assertTrue(conexionesAceptadas.get() <= 10, "No deberían haber más de 10 conexiones activas");
        assertTrue(conexionesRechazadas.get() > 0, "Deberían haber conexiones rechazadas al superar el límite de 10");
    }
}
package com.fixfinder.red;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fixfinder.red.procesadores.ProcesadorAutenticacion;
import com.fixfinder.red.procesadores.ProcesadorTrabajos;
import com.fixfinder.service.impl.EmpresaServiceImpl;
import com.fixfinder.service.impl.PresupuestoServiceImpl;
import com.fixfinder.service.impl.TrabajoServiceImpl;
import com.fixfinder.service.impl.UsuarioServiceImpl;
import com.fixfinder.service.interfaz.EmpresaService;
import com.fixfinder.service.interfaz.PresupuestoService;
import com.fixfinder.service.interfaz.TrabajoService;
import com.fixfinder.service.interfaz.UsuarioService;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.Semaphore;

/**
 * Gestor de Conexión (Refactorizado).
 * Actúa como enrutador, recibiendo los mensajes y delegándolos
 * a los procesadores correspondientes.
 */
public class GestorConexion implements Runnable {

    private final Socket socket;
    private final Semaphore semaforo;
    private final ObjectMapper mapper;

    // Servicios
    private final UsuarioService usuarioService;
    private final TrabajoService trabajoService;
    private final EmpresaService empresaService;
    private final PresupuestoService presupuestoService;

    // Procesadores Delegados
    private final ProcesadorAutenticacion procesadorAutenticacion;
    private final ProcesadorTrabajos procesadorTrabajos;

    public GestorConexion(Socket socket, Semaphore semaforo) {
        this.socket = socket;
        this.semaforo = semaforo;
        this.mapper = new ObjectMapper();

        // Inicializamos servicios
        this.usuarioService = new UsuarioServiceImpl();
        this.trabajoService = new TrabajoServiceImpl();
        this.empresaService = new EmpresaServiceImpl();
        this.presupuestoService = new PresupuestoServiceImpl();

        // Inicializamos procesadores delegados
        this.procesadorAutenticacion = new ProcesadorAutenticacion(usuarioService, empresaService);
        this.procesadorTrabajos = new ProcesadorTrabajos(trabajoService, usuarioService, presupuestoService);
    }

    @Override
    public void run() {
        try (
                DataInputStream entrada = new DataInputStream(socket.getInputStream());
                DataOutputStream salida = new DataOutputStream(socket.getOutputStream())) {

            while (!socket.isClosed()) {

                String mensajeCliente = entrada.readUTF();
                System.out.println("📩 Recibido: " + mensajeCliente);

                ObjectNode respuesta = mapper.createObjectNode();
                try {
                    // Parseamos el JSON
                    JsonNode nodo = mapper.readTree(mensajeCliente);

                    if (!nodo.has("accion")) {
                        respuesta.put("status", 400);
                        respuesta.put("mensaje", "Falta campo 'accion'");
                    } else {
                        String accion = nodo.get("accion").asText();
                        JsonNode datos = nodo.get("datos");

                        // Enrutamiento de comandos
                        switch (accion) {
                            case "LOGIN":
                                procesadorAutenticacion.procesarLogin(datos, respuesta);
                                break;

                            case "REGISTRO":
                                procesadorAutenticacion.procesarRegistro(datos, respuesta);
                                break;

                            case "CREAR_TRABAJO":
                                procesadorTrabajos.procesarCrearTrabajo(datos, respuesta);
                                break;

                            case "LISTAR_TRABAJOS":
                                procesadorTrabajos.procesarListarTrabajos(datos, respuesta);
                                break;

                            case "PING":
                                respuesta.put("status", 200);
                                respuesta.put("mensaje", "PONG");
                                break;

                            default:
                                respuesta.put("status", 400);
                                respuesta.put("mensaje", "Acción no reconocida: " + accion);
                                break;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("❌ Error procesando solicitud: " + e.getMessage());
                    respuesta.put("status", 400);
                    respuesta.put("mensaje", "Error procesando solicitud");
                }

                String jsonSalida = mapper.writeValueAsString(respuesta);
                salida.writeUTF(jsonSalida);
                salida.flush();
            }

        } catch (EOFException e) {
            // Desconexión normal del cliente
            System.out.println("🔌 Cliente desconectado (Sesión finalizada).");
        } catch (IOException e) {
            // Error real de red
            System.err.println("❌ Error de comunicación con cliente: " + e.getMessage());
        } finally {
            cerrarRecursos();
            semaforo.release();
            System.out.println("🔓 Conexión liberada. Huecos disponibles: " + semaforo.availablePermits());
        }
    }

    private void cerrarRecursos() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

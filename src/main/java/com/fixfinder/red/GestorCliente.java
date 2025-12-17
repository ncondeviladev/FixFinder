package com.fixfinder.red;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fixfinder.modelos.Usuario;
import com.fixfinder.modelos.Usuario;
// import com.fixfinder.modelos.enums.Rol; // Eliminated unused import
import com.fixfinder.service.interfaz.UsuarioService;
import com.fixfinder.service.impl.UsuarioServiceImpl;
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
    private final UsuarioService usuarioService;

    public GestorCliente(Socket socket, Semaphore semaforo) {
        this.socket = socket;
        this.semaforo = semaforo;
        this.usuarioService = new UsuarioServiceImpl();
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
                    // Comprobamos que el json tenga los datos necesarios para el login
                    if (rootNode.has("datos") && rootNode.get("datos").has("email")
                            && rootNode.get("datos").has("password")) {
                        String email = rootNode.get("datos").get("email").asText();
                        String password = rootNode.get("datos").get("password").asText();

                        try {
                            // Delegamos la lógica al servicio
                            Usuario usuario = usuarioService.login(email, password);

                            // Login exitoso
                            respuesta.put("status", 200);
                            respuesta.put("mensaje", "Login correcto");

                            // Devolvemos los datos del usuario al cliente (sin el password)
                            ObjectNode datosUsuario = respuesta.putObject("datos");
                            datosUsuario.put("id", usuario.getId());
                            datosUsuario.put("email", usuario.getEmail());
                            datosUsuario.put("rol", usuario.getRol().name());
                            // Solo enviamos fecha de registro si existe
                            if (usuario.getFechaRegistro() != null) {
                                datosUsuario.put("fechaRegistro", usuario.getFechaRegistro().toString());
                            }

                            if (usuario instanceof com.fixfinder.modelos.Operario) {
                                datosUsuario.put("idEmpresa",
                                        ((com.fixfinder.modelos.Operario) usuario).getIdEmpresa());
                            }

                        } catch (ServiceException e) {
                            // Controlamos errores de negocio (usuario no existe, pass incorrecta)
                            // O errores técnicos envueltos
                            if (e.getMessage().equals("Contraseña incorrecta")
                                    || e.getMessage().equals("Usuario no encontrado")) {
                                respuesta.put("status", 401);
                                respuesta.put("mensaje", "Credenciales incorrectas");
                            } else {
                                System.err.println("Error en login: " + e.getMessage());
                                respuesta.put("status", 500);
                                respuesta.put("mensaje", "Error interno del servidor");
                            }
                        }
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

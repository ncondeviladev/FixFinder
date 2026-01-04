
package com.fixfinder.red;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fixfinder.modelos.Cliente;
import com.fixfinder.modelos.Empresa;
import com.fixfinder.modelos.Operario;
import com.fixfinder.modelos.Usuario;
import com.fixfinder.modelos.enums.CategoriaServicio;
import com.fixfinder.modelos.enums.Rol;
import com.fixfinder.modelos.enums.Rol;
import com.fixfinder.modelos.Usuario;
import com.fixfinder.service.interfaz.TrabajoService;
import com.fixfinder.service.interfaz.EmpresaService;
// import com.fixfinder.modelos.enums.Rol; // Eliminated unused import
import com.fixfinder.service.interfaz.UsuarioService;
import com.fixfinder.service.impl.EmpresaServiceImpl;
import com.fixfinder.service.impl.TrabajoServiceImpl;
import com.fixfinder.service.impl.UsuarioServiceImpl;
import com.fixfinder.utilidades.ServiceException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

/**
 * Hilo dedicado a atender a UN solo cliente.
 * Lee JSONs, ejecuta acciones en los DAOs y responde.
 * Implementa Runnable para ser ejecutado en un Thread.
 */
public class GestorConexion implements Runnable {

    private final Socket socket;
    private final Semaphore semaforo;
    private DataInputStream entrada;
    private DataOutputStream salida;
    private final UsuarioService usuarioService;
    private final TrabajoService trabajoService;
    private final EmpresaService empresaService;

    public GestorConexion(Socket socket, Semaphore semaforo) {
        this.socket = socket;
        this.semaforo = semaforo;
        this.usuarioService = new UsuarioServiceImpl();
        this.trabajoService = new TrabajoServiceImpl();
        this.empresaService = new EmpresaServiceImpl();
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

            // Validar que existe el campo acción
            if (!rootNode.has("accion")) {
                return "{\"status\": 400, \"mensaje\": \"Falta campo 'accion'\"}";
            }

            String accion = rootNode.get("accion").asText();
            // Preparamos la respuesta
            ObjectNode respuesta = ServidorCentral.jsonMapper.createObjectNode();
            JsonNode datos = rootNode.get("datos"); // Extraemos los datos una vez

            switch (accion) {
                case "LOGIN":
                    // Comprobamos que el json tenga los datos necesarios para el login
                    if (datos != null && datos.has("email") && datos.has("password")) {
                        String email = datos.get("email").asText();
                        String password = datos.get("password").asText();

                        try {
                            // Delegamos la lógica al servicio
                            Usuario usuario = usuarioService.login(email, password);

                            // Login exitoso
                            respuesta.put("status", 200);
                            respuesta.put("mensaje", "Login correcto");

                            // Devolvemos los datos del usuario al cliente (sin el password)
                            ObjectNode datosUsuario = respuesta.putObject("datos");
                            datosUsuario.put("id", usuario.getId());
                            datosUsuario.put("nombreCompleto", usuario.getNombreCompleto());
                            datosUsuario.put("email", usuario.getEmail());
                            datosUsuario.put("rol", usuario.getRol().name());
                            // Solo enviamos fecha de registro si existe
                            if (usuario.getFechaRegistro() != null) {
                                datosUsuario.put("fechaRegistro", usuario.getFechaRegistro().toString());
                            }

                            if (usuario instanceof Operario) {
                                datosUsuario.put("idEmpresa", ((Operario) usuario).getIdEmpresa());
                            }

                        } catch (ServiceException e) {
                            // Controlamos errores de negocio
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

                case "REGISTRO":

                    String tipoRegistro = "CLIENTE";
                    if (datos != null && datos.has("tipo")) {
                        tipoRegistro = datos.get("tipo").asText().toUpperCase();
                    }
                    // Registro de Empresa
                    if (tipoRegistro.equals("EMPRESA")) {

                        if (datos != null) {
                            try {
                                // Creamos la empresa
                                Empresa nuevaEmpresa = new Empresa();
                                nuevaEmpresa.setNombre(
                                        datos.has("nombreEmpresa") ? datos.get("nombreEmpresa").asText() : "");
                                nuevaEmpresa.setCif(datos.has("cif") ? datos.get("cif").asText() : "");
                                nuevaEmpresa.setEmailContacto(
                                        datos.has("emailEmpresa") ? datos.get("emailEmpresa").asText() : "");
                                nuevaEmpresa.setTelefono(datos.has("telefono") ? datos.get("telefono").asText() : "");
                                nuevaEmpresa
                                        .setDireccion(datos.has("direccion") ? datos.get("direccion").asText() : "");
                                nuevaEmpresa.setUrlFoto(datos.has("urlFoto") ? datos.get("urlFoto").asText() : "");
                                nuevaEmpresa.setEspecialidades(new ArrayList<>()); // Lista vacia para evitar
                                                                                   // nullpointer

                                empresaService.registrarEmpresa(nuevaEmpresa);
                                // Obtenemos el idEmpresa para crear al Operario Gerente
                                int idEmpresa = nuevaEmpresa.getId();
                                Operario gerente = new Operario();
                                gerente.setNombreCompleto(
                                        datos.has("nombreGerente") ? datos.get("nombreGerente").asText() : "");
                                gerente.setEmail(datos.has("emailGerente") ? datos.get("emailGerente").asText() : "");
                                gerente.setPasswordHash(
                                        datos.has("password") ? datos.get("password").asText()
                                                : (datos.has("passwordGerente") ? datos.get("passwordGerente").asText()
                                                        : ""));
                                gerente.setDni(datos.has("dniGerente") ? datos.get("dniGerente").asText() : "");
                                gerente.setTelefono(
                                        datos.has("telefonoGerente") ? datos.get("telefonoGerente").asText() : "");

                                gerente.setRol(Rol.GERENTE);
                                gerente.setIdEmpresa(idEmpresa);
                                gerente.setEstaActivo(true);
                                gerente.setEspecialidad(CategoriaServicio.ELECTRICIDAD); // Por defecto para evitar
                                                                                         // nullpointer
                                // Guardamos nuevo usuario operario gerente con la empresa asignada
                                usuarioService.registrarUsuario(gerente);

                                respuesta.put("status", 200);
                                respuesta.put("mensaje", "Empresa y gerente registrado correctamente");

                            } catch (Exception e) {
                                String errorMsg = e.getMessage()
                                        + (e.getCause() != null ? " -> " + e.getCause().getMessage() : "");
                                System.err.println("Error registro empresa: " + errorMsg);
                                respuesta.put("status", 500);
                                respuesta.put("mensaje", "Error al registrar empresa y gerente");
                            }
                        } else {
                            respuesta.put("status", 400);
                            respuesta.put("mensaje", "Datos incompletos para REGISTRO");
                        }
                        // Registro de Operario
                    } else if (tipoRegistro.equals("OPERARIO")) {

                        if (datos != null) {
                            try {
                                Operario nuevoOperario = new Operario();
                                nuevoOperario.setNombreCompleto(
                                        datos.has("nombreOperario") ? datos.get("nombreOperario").asText() : "");
                                nuevoOperario.setEmail(
                                        datos.has("emailOperario") ? datos.get("emailOperario").asText() : "");
                                nuevoOperario.setPasswordHash(
                                        datos.has("passwordOperario") ? datos.get("passwordOperario").asText() : "");
                                nuevoOperario.setDni(datos.has("dniOperario") ? datos.get("dniOperario").asText() : "");
                                nuevoOperario.setTelefono(
                                        datos.has("telefonoOperario") ? datos.get("telefonoOperario").asText() : "");

                                // Lógica para ID Empresa y Especialidad
                                if (datos.has("idEmpresa")) {
                                    // Parseo robusto de string a int
                                    try {
                                        nuevoOperario.setIdEmpresa(Integer.parseInt(datos.get("idEmpresa").asText()));
                                    } catch (NumberFormatException e) {
                                        throw new ServiceException("El ID de empresa debe ser numérico");
                                    }
                                } else {
                                    throw new ServiceException("Falta idEmpresa para crear operario");
                                }

                                String especialidadStr = datos.has("especialidad") ? datos.get("especialidad").asText()
                                        : "ELECTRICIDAD";
                                try {
                                    nuevoOperario
                                            .setEspecialidad(CategoriaServicio.valueOf(especialidadStr.toUpperCase()));
                                } catch (IllegalArgumentException e) {
                                    nuevoOperario.setEspecialidad(CategoriaServicio.ELECTRICIDAD);
                                }

                                nuevoOperario.setRol(Rol.OPERARIO);
                                nuevoOperario.setEstaActivo(true);
                                // Guardamos nuevo usuario operario con la empresa asignada
                                usuarioService.registrarUsuario(nuevoOperario);
                                respuesta.put("status", 200);
                                respuesta.put("mensaje", "Operario registrado correctamente");
                            } catch (Exception e) {
                                String errorMsg = e.getMessage()
                                        + (e.getCause() != null ? " -> " + e.getCause().getMessage() : "");
                                respuesta.put("status", 500);
                                respuesta.put("mensaje", "Error registro operario: " + errorMsg);
                            }
                        }
                    } else if (tipoRegistro.equals("CLIENTE")) {

                        if (datos != null) {
                            try {

                                Cliente nuevoCliente = new Cliente();
                                nuevoCliente.setNombreCompleto(
                                        datos.has("nombre") ? datos.get("nombre").asText() : "Sin Nombre");
                                nuevoCliente.setEmail(datos.has("email") ? datos.get("email").asText() : "");
                                nuevoCliente
                                        .setPasswordHash(datos.has("password") ? datos.get("password").asText() : "");
                                nuevoCliente.setDni(datos.has("dni") ? datos.get("dni").asText() : "");
                                if (datos.has("telefono"))
                                    nuevoCliente.setTelefono(datos.get("telefono").asText());
                                if (datos.has("direccion"))
                                    nuevoCliente.setDireccion(datos.get("direccion").asText());

                                nuevoCliente.setRol(Rol.CLIENTE);
                                usuarioService.registrarUsuario(nuevoCliente);

                                respuesta.put("status", 201);
                                respuesta.put("mensaje", "Cliente registrado OK. ID: " + nuevoCliente.getId());
                            } catch (Exception e) {
                                String errorMsg = e.getMessage()
                                        + (e.getCause() != null ? " -> " + e.getCause().getMessage() : "");
                                respuesta.put("status", 500);
                                respuesta.put("mensaje", "Error registro cliente: " + errorMsg);
                            }
                        } else {
                            respuesta.put("status", 400);
                            respuesta.put("mensaje", "Faltan datos registro");
                        }
                    }
                    break;

                case "CREAR_TRABAJO":
                    if (datos != null) {
                        try {
                            // 1. Validaciones básicas
                            if (!datos.has("idCliente"))
                                throw new ServiceException("Falta ID Cliente");
                            if (!datos.has("descripcion"))
                                throw new ServiceException("Falta descripción");

                            // 2. Extraer datos
                            int idCliente = datos.get("idCliente").asInt();
                            String descripcion = datos.get("descripcion").asText();
                            String direccion = datos.has("direccion") ? datos.get("direccion").asText() : "";
                            int urgencia = datos.has("urgencia") ? datos.get("urgencia").asInt() : 1;

                            // 3. Convertir Categoría
                            CategoriaServicio categoria = CategoriaServicio.OTROS;
                            if (datos.has("categoria")) {
                                try {
                                    categoria = CategoriaServicio
                                            .valueOf(datos.get("categoria").asText().toUpperCase());
                                } catch (IllegalArgumentException e) {
                                    categoria = CategoriaServicio.OTROS;
                                }
                            }

                            // 4. Llamar al servicio
                            // Nota: TrabajoServiceImpl ya se encarga de asignar fecha y estado inicial
                            // (PENDIENTE)
                            var nuevoTrabajo = trabajoService.solicitarReparacion(idCliente, categoria, descripcion,
                                    direccion, urgencia);

                            // 5. Respuesta
                            respuesta.put("status", 201);
                            respuesta.put("mensaje", "Trabajo creado correctamente");
                            ObjectNode datosTrabajo = respuesta.putObject("datos");
                            datosTrabajo.put("id", nuevoTrabajo.getId());

                        } catch (ServiceException e) {
                            respuesta.put("status", 400);
                            respuesta.put("mensaje", "Error validación: " + e.getMessage());
                        } catch (Exception e) {
                            respuesta.put("status", 500);
                            respuesta.put("mensaje", "Error servidor: " + e.getMessage());
                            e.printStackTrace();
                        }
                    } else {
                        respuesta.put("status", 400);
                        respuesta.put("mensaje", "Faltan datos para CREAR_TRABAJO");
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

        } catch (

        Exception e) {
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

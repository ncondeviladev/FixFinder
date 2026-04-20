package com.fixfinder.red;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fixfinder.data.ConexionDB;
import com.fixfinder.red.procesadores.ProcesadorAutenticacion;
import com.fixfinder.red.procesadores.ProcesadorFacturas;
import com.fixfinder.red.procesadores.ProcesadorPresupuestos;
import com.fixfinder.red.procesadores.ProcesadorTrabajos;
import com.fixfinder.service.impl.EmpresaServiceImpl;
import com.fixfinder.service.impl.FacturaServiceImpl;
import com.fixfinder.service.impl.PresupuestoServiceImpl;
import com.fixfinder.service.impl.TrabajoServiceImpl;
import com.fixfinder.service.impl.UsuarioServiceImpl;
import com.fixfinder.service.interfaz.EmpresaService;
import com.fixfinder.service.interfaz.FacturaService;
import com.fixfinder.service.interfaz.PresupuestoService;
import com.fixfinder.service.interfaz.TrabajoService;
import com.fixfinder.service.interfaz.UsuarioService;
import com.fixfinder.service.interfaz.OperarioService;
import com.fixfinder.service.impl.OperarioServiceImpl;
import com.fixfinder.red.procesadores.ProcesadorUsuarios;
import com.fixfinder.red.procesadores.ProcesadorEmpresa;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.Semaphore;

public class GestorConexion implements Runnable {

    private final Socket socket;
    private final Semaphore semaforo;
    private final ObjectMapper mapper;

    // Servicios
    private final UsuarioService usuarioService;
    private final TrabajoService trabajoService;
    private final EmpresaService empresaService;
    private final PresupuestoService presupuestoService;
    private final OperarioService operarioService;
    private final FacturaService facturaService;

    // Procesadores Delegados
    private final ProcesadorAutenticacion procesadorAutenticacion;
    private final ProcesadorTrabajos procesadorTrabajos;
    private final ProcesadorUsuarios procesadorUsuarios;
    private final ProcesadorEmpresa procesadorEmpresa;
    private final ProcesadorPresupuestos procesadorPresupuestos;
    private final ProcesadorFacturas procesadorFacturas;

    public GestorConexion(Socket socket, Semaphore semaforo) {
        this.socket = socket;
        this.semaforo = semaforo;
        this.mapper = new ObjectMapper();

        // Inicializamos servicios
        this.usuarioService = new UsuarioServiceImpl();
        this.trabajoService = new TrabajoServiceImpl();
        this.empresaService = new EmpresaServiceImpl();
        this.presupuestoService = new PresupuestoServiceImpl();
        this.operarioService = new OperarioServiceImpl();
        this.facturaService = new FacturaServiceImpl();

        // Inicializamos procesadores delegados
        this.procesadorAutenticacion = new ProcesadorAutenticacion(usuarioService, empresaService);
        this.procesadorTrabajos = new ProcesadorTrabajos(trabajoService, usuarioService, presupuestoService,
                facturaService);
        this.procesadorUsuarios = new ProcesadorUsuarios(operarioService);
        this.procesadorEmpresa = new ProcesadorEmpresa(empresaService);
        this.procesadorPresupuestos = new ProcesadorPresupuestos(presupuestoService);
        this.procesadorFacturas = new ProcesadorFacturas(facturaService);
    }

    @Override
    public void run() {
        try (
                DataInputStream entrada = new DataInputStream(socket.getInputStream());
                DataOutputStream salida = new DataOutputStream(socket.getOutputStream())) {

            while (!socket.isClosed()) {
                System.err.println("📡 [SOCKET-WAIT] Esperando nueva cabecera (4 bytes)...");
                int length = entrada.readInt();
                System.err.println("📡 [SOCKET-IN] Recibidos " + length + " bytes.");

                if (length <= 0 || length > 1024 * 1024) {
                    System.err.println("⚠️ [SOCKET-WARN] Longitud inválida recibida: " + length);
                    continue;
                }

                byte[] bytes = new byte[length];
                entrada.readFully(bytes);
                System.err.println("📡 [SOCKET-IN] Payload leído completo.");
                String mensajeCliente = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                System.out.println("📩 Recibido: " + mensajeCliente);

                ObjectNode respuesta = mapper.createObjectNode();
                try {
                    // Parseamos el JSON
                    JsonNode nodo = mapper.readTree(mensajeCliente);

                    if (!nodo.has("accion")) {
                        respuesta.put("status", 400);
                        respuesta.put("mensaje", "Falta campo 'accion'");
                    } else {
                        // REBOTAR ID DE PETICIÓN (Si existe) para el sistema de tickets
                        if (nodo.has("id_peticion")) {
                            respuesta.set("id_peticion", nodo.get("id_peticion"));
                        }

                        String accion = nodo.get("accion").asText();
                        respuesta.put("accion", accion);
                        JsonNode datos = nodo.get("datos");

                        // Enrutamiento de comandos
                        switch (accion) {
                            case "LOGIN":
                                procesadorAutenticacion.procesarLogin(datos, respuesta);
                                break;

                            case "REGISTRO":
                                procesadorAutenticacion.procesarRegistro(datos, respuesta);
                                break;

                            case "PING":
                                respuesta.put("status", 200);
                                respuesta.put("mensaje", "PONG");
                                break;

                            default:
                                // VALIDACIÓN DE TOKEN para el resto de acciones
                                String tokenMsg = nodo.has("token") ? nodo.get("token").asText() : null;
                                if (SessionManager.esTokenValido(tokenMsg)) {
                                    System.err.println("🔥 [GESTOR-DEBUG] Acción: " + accion + " | Usuario: "
                                            + SessionManager.obtenerUsuario(tokenMsg));
                                    switch (accion) {
                                        case "CREAR_TRABAJO":
                                            procesadorTrabajos.procesarCrearTrabajo(datos, respuesta);
                                            break;

                                        case "LISTAR_TRABAJOS":
                                            procesadorTrabajos.procesarListarTrabajos(datos, respuesta);
                                            break;

                                        case "ASIGNAR_OPERARIO":
                                            procesadorTrabajos.procesarAsignarOperario(datos, respuesta);
                                            break;

                                        case "FINALIZAR_TRABAJO":
                                            procesadorTrabajos.procesarFinalizarTrabajo(datos, respuesta);
                                            break;

                                        case "CANCELAR_TRABAJO":
                                            procesadorTrabajos.procesarCancelarTrabajo(datos, respuesta);
                                            break;

                                        case "MODIFICAR_TRABAJO":
                                            procesadorTrabajos.procesarModificarTrabajo(datos, respuesta);
                                            break;

                                        case "VALORAR_TRABAJO":
                                            procesadorTrabajos.procesarValorarTrabajo(datos, respuesta);
                                            break;

                                        case "GET_OPERARIOS":
                                            procesadorUsuarios.procesarListarOperarios(datos, respuesta);
                                            break;

                                        case "MODIFICAR_OPERARIO":
                                            procesadorUsuarios.procesarModificarOperario(datos, respuesta);
                                            break;

                                        case "LISTAR_EMPRESAS":
                                            procesadorEmpresa.procesarListarEmpresas(datos, respuesta);
                                            break;

                                        case "GET_EMPRESA":
                                            procesadorEmpresa.procesarObtenerEmpresa(datos, respuesta);
                                            break;

                                        case "MODIFICAR_EMPRESA":
                                            procesadorEmpresa.procesarModificarEmpresa(datos, respuesta);
                                            break;

                                        case "ACTUALIZAR_FOTO_PERFIL":
                                            procesadorUsuarios.procesarActualizarFotoPerfil(datos, respuesta);
                                            break;

                                        case "MODIFICAR_USUARIO":
                                            procesadorUsuarios.procesarModificarUsuario(datos, respuesta);
                                            break;

                                        case "CREAR_PRESUPUESTO":
                                            procesadorPresupuestos.procesarCrearPresupuesto(datos, respuesta);
                                            break;

                                        case "LISTAR_PRESUPUESTOS":
                                            procesadorPresupuestos.procesarListarPresupuestos(datos, respuesta);
                                            break;

                                        case "ACEPTAR_PRESUPUESTO":
                                            procesadorPresupuestos.procesarAceptarPresupuesto(datos, respuesta);
                                            break;

                                        case "RECHAZAR_PRESUPUESTO":
                                            procesadorPresupuestos.procesarRechazarPresupuesto(datos, respuesta);
                                            break;

                                        case "GENERAR_FACTURA":
                                            procesadorFacturas.procesarGenerarFactura(datos, respuesta);
                                            break;

                                        case "PAGAR_FACTURA":
                                            procesadorFacturas.procesarPagarFactura(datos, respuesta);
                                            break;

                                        default:
                                            respuesta.put("status", 400);
                                            respuesta.put("mensaje", "Acción no reconocida: " + accion);
                                            break;
                                    }
                                } else {
                                    respuesta.put("status", 401);
                                    respuesta.put("mensaje", "Sesión no válida o expirada. Por favor, identifícate.");
                                }
                                break;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("❌ Error procesando solicitud: " + e.getMessage());
                    e.printStackTrace();
                    respuesta.put("status", 400);
                    respuesta.put("mensaje", "Error procesando solicitud");
                } finally {
                    ConexionDB.cerrarConexion();
                }

                String jsonSalida = mapper.writeValueAsString(respuesta);
                byte[] bytesSalida = jsonSalida.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                int limit = Math.min(jsonSalida.length(), 250);
                String act = respuesta.has("accion") ? respuesta.get("accion").asText() : "N/A";
                System.out.println("📤 Enviando respuesta a " + act + ": " + jsonSalida.substring(0, limit)
                        + (jsonSalida.length() > limit ? "..." : ""));
                salida.writeInt(bytesSalida.length);
                salida.write(bytesSalida);
                salida.flush();
            }

        } catch (EOFException e) {
            // Desconexión normal del cliente
            System.err.println("🔌 [SOCKET] Cliente desconectado (EOF alcanzado).");
        } catch (IOException e) {
            // Error real de red
            System.err.println("❌ [SOCKET] Error de comunicación: " + e.getMessage());
            e.printStackTrace();
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

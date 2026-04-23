package com.fixfinder.red;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fixfinder.modelos.Operario;
import com.fixfinder.modelos.Usuario;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sistema central de notificaciones en tiempo real (Push).
 * Gestiona el registro de conexiones activas y permite el envío de mensajes
 * no solicitados a clientes específicos, roles o de forma global (filtrado).
 */
public class Broadcaster {

    private static final Broadcaster instancia = new Broadcaster();
    // ConcurrentHashMap para evitar colisiones entre hilos
    private final Set<GestorConexion> conexionesControlladas = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final ObjectMapper mapper = new ObjectMapper();

    private Broadcaster() {
    }

    public static Broadcaster getInstancia() {
        return instancia;
    }

    /**
     * Registra una nueva conexión en el sistema.
     * 
     * @param conexion El gestor de la conexión socket.
     */
    public void registrarConexion(GestorConexion conexion) {
        conexionesControlladas.add(conexion);
        System.out.println("🔌 [SISTEMA] Nueva conexión establecida.");
    }

    /**
     * Elimina una conexión del sistema (al cerrarse el socket).
     * 
     * @param conexion La conexión a eliminar.
     */
    public void desregistrarConexion(GestorConexion conexion) {
        conexionesControlladas.remove(conexion);
        System.out.println("🔌 [SISTEMA] Conexión cerrada.");
    }

    /**
     * Envía una notificación a TODOS los usuarios conectados.
     */
    public void difundirGlobal(String mensaje) {
        ObjectNode payload = crearPayloadBase("SISTEMA");
        ((ObjectNode) payload.get("datos")).put("mensaje", mensaje);
        enviarATodos(payload);
    }

    /**
     * Notifica un cambio en un trabajo a los interesados.
     * @param subtipo NUEVO, ASIGNACION, FINALIZADO, etc.
     * @param idTrabajo ID del trabajo afectado.
     * @param idCliente ID del cliente dueño (para notificarle).
     * @param idEmpresa ID de la empresa involucrada (para notificar a sus gerentes).
     * @param info Mensaje adicional.
     */
    public void difundirEventoTrabajo(String subtipo, int idTrabajo, int idCliente, int idEmpresa, String info) {
        ObjectNode payload = crearPayloadBase("TRABAJO");
        ObjectNode datos = (ObjectNode) payload.get("datos");

        datos.put("subtipo", subtipo);
        datos.put("idTrabajo", idTrabajo);
        datos.put("idCliente", idCliente);
        datos.put("idEmpresa", idEmpresa);
        if (info != null) datos.put("info", info);
        System.out.println("📢 [BROADCAST] Evento TRABAJO (" + subtipo + ") difundido.");

        
        for (GestorConexion con : conexionesControlladas) {
            Usuario u = con.getUsuario();
            if (u == null) continue;

            boolean esClienteInteresado = (idCliente > 0 && u.getId() == idCliente);
            // Sincronización Global para Gerentes: Todos necesitan ver los cambios de estado en tiempo real
            boolean esGerente = "GERENTE".equals(u.getRol().name());

            if (esClienteInteresado || esGerente) {
                con.enviarPush(payload);
            }
        }
    }

    /**
     * Notifica un cambio en un presupuesto a los interesados.
     */
    public void difundirEventoPresupuesto(String subtipo, int idTrabajo, int idCliente, int idEmpresa, String info) {
        ObjectNode payload = crearPayloadBase("PRESUPUESTO");
        ObjectNode datos = (ObjectNode) payload.get("datos");

        datos.put("subtipo", subtipo);
        datos.put("idTrabajo", idTrabajo);
        datos.put("idCliente", idCliente);
        datos.put("idEmpresa", idEmpresa);
        if (info != null) datos.put("info", info);
        System.out.println("📢 [BROADCAST] Evento PRESUPUESTO (" + subtipo + ") difundido.");


        for (GestorConexion con : conexionesControlladas) {
            Usuario u = con.getUsuario();
            if (u == null) continue;

            boolean esClienteInteresado = (idCliente > 0 && u.getId() == idCliente);
            boolean esGerente = "GERENTE".equals(u.getRol().name());

            if (esClienteInteresado || esGerente) {
                con.enviarPush(payload);
            }
        }
    }

    private ObjectNode crearPayloadBase(String categoria) {
        ObjectNode root = mapper.createObjectNode();
        root.put("accion", "BROADCAST");

        ObjectNode datos = root.putObject("datos");
        datos.put("categoria", categoria);
        datos.put("timestamp", System.currentTimeMillis());

        return root; // Devuelve el root, pero las subclases añadirán a root.get("datos")
    }

    private void enviarATodos(ObjectNode payload) {
        for (GestorConexion con : conexionesControlladas) {
            con.enviarPush(payload);
        }
    }

    private void enviarAUsuario(int idUsuario, ObjectNode payload) {
        for (GestorConexion con : conexionesControlladas) {
            Usuario u = con.getUsuario();
            if (u != null && u.getId() == idUsuario) {
                con.enviarPush(payload);
            }
        }
    }

    /**
     * Notifica una nueva valoración de un trabajo.
     */
    public void difundirValoracion(int idTrabajo, int idEmpresa, String info) {
        ObjectNode payload = crearPayloadBase("VALORACION");
        ObjectNode datos = (ObjectNode) payload.get("datos");
        datos.put("idTrabajo", idTrabajo);
        datos.put("idEmpresa", idEmpresa);
        datos.put("info", info);
        
        enviarAEmpresa(idEmpresa, payload);
    }

    /**
     * Notifica cambios en los datos de una empresa.
     */
    public void difundirEventoEmpresa(String subtipo, int idEmpresa, String info) {
        ObjectNode payload = crearPayloadBase("EMPRESA");
        ObjectNode datos = (ObjectNode) payload.get("datos");
        datos.put("subtipo", subtipo);
        datos.put("idEmpresa", idEmpresa);
        datos.put("info", info);
        
        enviarAEmpresa(idEmpresa, payload);
    }

    /**
     * Notifica cambios en la gestión de operarios solo a su propia empresa.
     */
    public void difundirEventoOperario(String subtipo, int idOperario, int idEmpresa, String info) {
        ObjectNode payload = crearPayloadBase("OPERARIO");
        ObjectNode datos = (ObjectNode) payload.get("datos");
        datos.put("subtipo", subtipo);
        datos.put("idOperario", idOperario);
        if (info != null) datos.put("info", info);

        enviarAEmpresa(idEmpresa, payload);
    }

    /**
     * Envía una notificación push a todos los miembros de una empresa específica.
     */
    private void enviarAEmpresa(int idEmpresa, ObjectNode payload) {
        if (idEmpresa <= 0) return;
        for (GestorConexion con : conexionesControlladas) {
            Usuario u = con.getUsuario();
            if (u instanceof Operario && ((Operario) u).getIdEmpresa() == idEmpresa) {
                con.enviarPush(payload);
            }
        }
    }

    /**
     * Notifica cambios en el perfil de un usuario (Nombre, Foto, etc).
     */
    public void difundirEventoUsuario(String subtipo, int idUsuario, String info, String urlFoto, String nombre) {
        ObjectNode payload = crearPayloadBase("USUARIO");
        ObjectNode datos = (ObjectNode) payload.get("datos");
        datos.put("subtipo", subtipo);
        datos.put("idUsuario", idUsuario);
        datos.put("info", info);
        if (urlFoto != null)
            try {
                datos.put("url_foto", urlFoto);
            } catch (Exception ignored) {
            }
        if (nombre != null)
            try {
                datos.put("nombre", nombre);
            } catch (Exception ignored) {
            }
        enviarATodos(payload);
    }

    /**
     * Notifica a todos los gerentes registrados.
     */
    public void difundirAGerentes(ObjectNode payload) {
        for (GestorConexion con : conexionesControlladas) {
            Usuario u = con.getUsuario();
            if (u != null && "GERENTE".equals(u.getRol().name())) {
                con.enviarPush(payload);
            }
        }
    }
}

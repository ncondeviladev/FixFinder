package com.fixfinder.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Servicio central de notificaciones en tiempo real.
 * Reemplaza al antiguo Broadcaster de Sockets manuales.
 * Utiliza STOMP sobre WebSockets para el envío de mensajes push.
 */
@Service
public class NotificationService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Notifica eventos relacionados con trabajos.
     */
    public void difundirEventoTrabajo(String subtipo, int idTrabajo, int idCliente, int idEmpresa, String info) {
        Map<String, Object> payload = crearPayload("TRABAJO", subtipo, info);
        payload.put("idTrabajo", idTrabajo);
        payload.put("idCliente", idCliente);
        payload.put("idEmpresa", idEmpresa);

        // Notificar a temas generales y específicos
        messagingTemplate.convertAndSend("/topic/trabajos", payload);
        messagingTemplate.convertAndSend("/topic/gerentes", payload);
        
        if (idCliente > 0) {
            messagingTemplate.convertAndSendToUser(String.valueOf(idCliente), "/queue/notificaciones", payload);
        }
    }

    /**
     * Notifica eventos relacionados con presupuestos.
     */
    public void difundirEventoPresupuesto(String subtipo, int idTrabajo, int idCliente, int idEmpresa, String info) {
        Map<String, Object> payload = crearPayload("PRESUPUESTO", subtipo, info);
        payload.put("idTrabajo", idTrabajo);
        payload.put("idCliente", idCliente);
        payload.put("idEmpresa", idEmpresa);

        messagingTemplate.convertAndSend("/topic/presupuestos", payload);
        messagingTemplate.convertAndSend("/topic/gerentes", payload);

        if (idCliente > 0) {
            messagingTemplate.convertAndSendToUser(String.valueOf(idCliente), "/queue/notificaciones", payload);
        }
    }

    /**
     * Notifica eventos relacionados con usuarios.
     */
    public void difundirEventoUsuario(String subtipo, int idUsuario, String nombre, String urlFoto, String info, String email, String telefono, String direccion) {
        Map<String, Object> payload = crearPayload("USUARIO", subtipo, info);
        payload.put("idUsuario", idUsuario);
        if (nombre != null) payload.put("nombre", nombre);
        if (urlFoto != null) payload.put("url_foto", urlFoto);
        if (email != null) payload.put("email", email);
        if (telefono != null) payload.put("telefono", telefono);
        if (direccion != null) payload.put("direccion", direccion);

        // A diferencia del anterior broadcast que iba a /topic/usuarios general,
        // enviaremos un mensaje dirigido al usuario para seguridad y evitar SPAM.
        if (idUsuario > 0) {
            messagingTemplate.convertAndSendToUser(String.valueOf(idUsuario), "/queue/notificaciones", payload);
        }
    }

    /**
     * Notifica eventos relacionados con empresas.
     */
    public void difundirEventoEmpresa(String subtipo, int idEmpresa, String info) {
        Map<String, Object> payload = crearPayload("EMPRESA", subtipo, info);
        payload.put("idEmpresa", idEmpresa);

        // Notificar solo a los suscritos al canal de la empresa
        messagingTemplate.convertAndSend("/topic/empresa/" + idEmpresa, payload);
    }

    /**
     * Notifica eventos relacionados con operarios.
     */
    public void difundirEventoOperario(String subtipo, int idOperario, int idEmpresa, String info) {
        Map<String, Object> payload = crearPayload("OPERARIO", subtipo, info);
        payload.put("idOperario", idOperario);
        payload.put("idEmpresa", idEmpresa);

        messagingTemplate.convertAndSend("/topic/empresa/" + idEmpresa, payload);
    }

    private Map<String, Object> crearPayload(String categoria, String subtipo, String info) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("accion", "BROADCAST");
        payload.put("categoria", categoria);
        payload.put("subtipo", subtipo);
        payload.put("info", info != null ? info : "");
        payload.put("timestamp", System.currentTimeMillis());
        return payload;
    }
}

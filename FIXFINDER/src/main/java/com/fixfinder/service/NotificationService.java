package com.fixfinder.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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

    private void ejecutarTrasCommit(Runnable accion) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    accion.run();
                }
            });
        } else {
            accion.run();
        }
    }

    /**
     * Notifica eventos relacionados con trabajos.
     */
    public void difundirEventoTrabajo(String subtipo, int idTrabajo, int idCliente, int idOperario, int idEmpresa, String info) {
        ejecutarTrasCommit(() -> {
            Map<String, Object> payload = crearPayload("TRABAJO", subtipo, info);
            @SuppressWarnings("unchecked")
            Map<String, Object> datos = (Map<String, Object>) payload.get("datos");
            datos.put("idTrabajo", idTrabajo);
            datos.put("idCliente", idCliente);
            datos.put("idOperario", idOperario);
            datos.put("idEmpresa", idEmpresa);

            // Solo notificar al canal global /topic/trabajos si es una nueva incidencia (PENDIENTE)
            // para que las empresas y operarios interesados puedan presupuestarla.
            if (idEmpresa <= 0 || "NUEVO".equals(subtipo)) {
                messagingTemplate.convertAndSend("/topic/trabajos", payload);
                messagingTemplate.convertAndSend("/topic/gerentes", payload);
            } else {
                // Si el trabajo ya está asignado o aceptado por una empresa, notificar de forma aislada
                messagingTemplate.convertAndSend("/topic/empresa/" + idEmpresa, payload);
            }
            
            if (idCliente > 0) {
                messagingTemplate.convertAndSend("/topic/usuario/" + idCliente, payload);
            }
            
            if (idOperario > 0) {
                messagingTemplate.convertAndSend("/topic/usuario/" + idOperario, payload);
            }
        });
    }

    /**
     * Notifica eventos relacionados con presupuestos.
     */
    public void difundirEventoPresupuesto(String subtipo, int idTrabajo, int idCliente, int idEmpresa, String info) {
        ejecutarTrasCommit(() -> {
            Map<String, Object> payload = crearPayload("PRESUPUESTO", subtipo, info);
            @SuppressWarnings("unchecked")
            Map<String, Object> datos = (Map<String, Object>) payload.get("datos");
            datos.put("idTrabajo", idTrabajo);
            datos.put("idCliente", idCliente);
            datos.put("idEmpresa", idEmpresa);

            // Un presupuesto pertenece en exclusiva a la empresa que lo envía y al cliente del trabajo
            if (idEmpresa > 0) {
                messagingTemplate.convertAndSend("/topic/empresa/" + idEmpresa, payload);
            }

            if (idCliente > 0) {
                messagingTemplate.convertAndSend("/topic/usuario/" + idCliente, payload);
            }

            // Si el presupuesto es ACEPTADO o RECHAZADO, notificar globalmente para actualizar todos los dashboards
            if ("ACEPTADO".equals(subtipo) || "RECHAZADO".equals(subtipo)) {
                messagingTemplate.convertAndSend("/topic/trabajos", payload);
                messagingTemplate.convertAndSend("/topic/gerentes", payload);
            }
        });
    }

    /**
     * Notifica eventos relacionados con usuarios.
     */
    public void difundirEventoUsuario(String subtipo, int idUsuario, String nombre, String urlFoto, String info, String email, String telefono, String direccion) {
        ejecutarTrasCommit(() -> {
            Map<String, Object> payload = crearPayload("USUARIO", subtipo, info);
            @SuppressWarnings("unchecked")
            Map<String, Object> datos = (Map<String, Object>) payload.get("datos");
            datos.put("idUsuario", idUsuario);
            if (nombre != null) datos.put("nombre", nombre);
            if (urlFoto != null) datos.put("url_foto", urlFoto);
            if (email != null) datos.put("email", email);
            if (telefono != null) datos.put("telefono", telefono);
            if (direccion != null) datos.put("direccion", direccion);

            // Notificamos a los canales generales para el Dashboard
            messagingTemplate.convertAndSend("/topic/usuarios", payload);
            messagingTemplate.convertAndSend("/topic/gerentes", payload);
            messagingTemplate.convertAndSend("/topic/trabajos", payload);

            // A diferencia del anterior broadcast que iba a /topic/usuarios general,
            // enviaremos un mensaje dirigido al usuario para seguridad y evitar SPAM.
            if (idUsuario > 0) {
                messagingTemplate.convertAndSend("/topic/usuario/" + idUsuario, payload);
            }
        });
    }

    /**
     * Notifica eventos relacionados con empresas.
     */
    public void difundirEventoEmpresa(String subtipo, int idEmpresa, String info) {
        ejecutarTrasCommit(() -> {
            Map<String, Object> payload = crearPayload("EMPRESA", subtipo, info);
            @SuppressWarnings("unchecked")
            Map<String, Object> datos = (Map<String, Object>) payload.get("datos");
            datos.put("idEmpresa", idEmpresa);

            // Notificar solo a los suscritos al canal de la empresa
            messagingTemplate.convertAndSend("/topic/empresa/" + idEmpresa, payload);
        });
    }

    /**
     * Notifica eventos relacionados con operarios.
     */
    public void difundirEventoOperario(String subtipo, int idOperario, int idEmpresa, String info) {
        ejecutarTrasCommit(() -> {
            Map<String, Object> payload = crearPayload("OPERARIO", subtipo, info);
            @SuppressWarnings("unchecked")
            Map<String, Object> datos = (Map<String, Object>) payload.get("datos");
            datos.put("idOperario", idOperario);
            datos.put("idEmpresa", idEmpresa);

            messagingTemplate.convertAndSend("/topic/empresa/" + idEmpresa, payload);
        });
    }

    private Map<String, Object> crearPayload(String categoria, String subtipo, String info) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("accion", "BROADCAST");
        payload.put("status", 200);
        
        Map<String, Object> datos = new HashMap<>();
        datos.put("categoria", categoria);
        datos.put("subtipo", subtipo);
        datos.put("info", info != null ? info : "");
        datos.put("timestamp", System.currentTimeMillis());
        
        payload.put("datos", datos);
        return payload;
    }
}

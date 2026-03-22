package com.fixfinder.controladores;

import com.fasterxml.jackson.databind.JsonNode;
import com.fixfinder.cliente.RespuestaServidor;
import com.fixfinder.utilidades.SessionManager;
import javafx.application.Platform;
import java.io.IOException;

/**
 * Clase encargada de procesar las respuestas del servidor para el DashboardController.
 * Desacopla la lógica de interpretación de mensajes JSON del controlador de la vista,
 * siguiendo el principio de responsabilidad única.
 */
public class ManejadorRespuestaDashboard {

    private final DashboardController controller;

    public ManejadorRespuestaDashboard(DashboardController controller) {
        this.controller = controller;
    }

    /**
     * Método principal que recibe el JSON del servidor y delega la actualización de la UI.
     */
    public void procesar(String json) {
        try {
            // Interpretamos la estructura genérica de la respuesta
            RespuestaServidor respuesta = controller.getServicioCliente().interpretarRespuesta(json);
            int status = respuesta.getStatus();
            String mensaje = respuesta.getMensaje();
            JsonNode datos = respuesta.getDatos();

            if (respuesta.esExito()) {
                // Si la respuesta incluye ID y Rol, es un Login exitoso
                if (datos != null && datos.has("id") && datos.has("rol")) {
                    procesarLogin(datos);
                } 
                // Si es un listado de trabajos
                else if (datos != null && datos.isArray() && mensaje.contains("Listado")) {
                    controller.actualizarTabla(datos);
                } 
                // Si es la lista de operarios para el combo de asignación
                else if (datos != null && datos.isArray() && mensaje.contains("operarios")) {
                    controller.actualizarListaOperarios(datos);
                } 
                // Feedback para acciones de creación o modificación
                else if (mensaje.contains("TRABAJO") || mensaje.contains("Presupuesto") || mensaje.contains("asignado")) {
                    controller.onRefrescarTrabajosClick();
                    controller.limpiarCamposTrabajo();
                }
            } else {
                // Gestionamos los casos de error reportados por el servidor
                procesarError(status, mensaje);
            }
        } catch (Exception e) {
            controller.log("Error crítico procesando respuesta: " + e.getMessage());
        }
    }

    /**
     * Gestiona el almacenamiento de la sesión y la actualización visual tras un login.
     */
    private void procesarLogin(JsonNode datos) {
        int id = datos.get("id").asInt();
        String nombre = datos.has("nombreCompleto") ? datos.get("nombreCompleto").asText() : "Usuario";
        String rol = datos.get("rol").asText();
        Integer idEmp = datos.has("idEmpresa") ? datos.get("idEmpresa").asInt() : null;

        // Guardamos en el Singleton de sesión
        SessionManager.getInstance().setSession(id, nombre, rol, idEmp);

        // Actualizamos la UI en el hilo de JavaFX
        Platform.runLater(() -> {
            controller.actualizarUILogin(nombre, rol, id);
            // Si es gerente, pedimos la lista de sus operarios para tenerla en caché
            if (SessionManager.getInstance().isGerente() && idEmp != null) {
                try {
                    controller.getServicioCliente().solicitarListaOperarios(idEmp);
                } catch (IOException ignored) {}
            }
        });
        controller.log("Sesión iniciada correctamente: " + nombre);
    }

    /**
     * Muestra mensajes de error amigables en la interfaz según el código de estado.
     */
    private void procesarError(int status, String mensaje) {
        Platform.runLater(() -> {
            if (status == 401 || mensaje.toLowerCase().contains("login")) {
                controller.mostrarErrorLogin("Credenciales incorrectas o sesión expirada");
            } else if (SessionManager.getInstance().isLogged()) {
                controller.mostrarErrorTrabajo(mensaje);
            }
            controller.log("Servidor reporta error (" + status + "): " + mensaje);
        });
    }
}

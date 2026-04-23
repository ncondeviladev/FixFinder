package com.fixfinder.red.procesadores.trabajos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fixfinder.data.DataRepositoryImpl;
import com.fixfinder.data.interfaces.FotoTrabajoDAO;
import com.fixfinder.modelos.Trabajo;
import com.fixfinder.modelos.componentes.FotoTrabajo;
import com.fixfinder.modelos.enums.CategoriaServicio;
import com.fixfinder.red.Broadcaster;
import com.fixfinder.service.interfaz.FacturaService;
import com.fixfinder.service.interfaz.TrabajoService;
import com.fixfinder.utilidades.ServiceException;
import com.fixfinder.data.interfaces.TrabajoDAO;

import java.util.ArrayList;
import java.util.List;

/**
 * Especialista en operaciones que modifican el estado o la existencia de un trabajo.
 * Maneja creación, asignación, finalización, cancelación y valoración.
 */
public class ManejadorCicloVidaTrabajo {

    private final TrabajoService trabajoService;
    private final FacturaService facturaService;
    private final FotoTrabajoDAO fotoTrabajoDAO;

    public ManejadorCicloVidaTrabajo(TrabajoService trabajoService, FacturaService facturaService) {
        this.trabajoService = trabajoService;
        this.facturaService = facturaService;
        this.fotoTrabajoDAO = new DataRepositoryImpl().getFotoTrabajoDAO();
    }

    public void procesarCrearTrabajo(JsonNode datos, ObjectNode respuesta) {
        try {
            validarRequeridos(datos, "idCliente", "descripcion");

            int idCliente = datos.get("idCliente").asInt();
            String titulo = datos.path("titulo").asText("");
            String descripcion = datos.get("descripcion").asText();
            String direccion = datos.path("direccion").asText("");
            int urgencia = datos.path("urgencia").asInt(1);

            if (urgencia == 3) descripcion = "[URGENTE!!!] " + descripcion;
            else if (urgencia == 2) descripcion = "[PRIORIDAD] " + descripcion;

            CategoriaServicio cat = parseCategoria(datos.path("categoria").asText("OTROS"));

            Trabajo nuevo = trabajoService.solicitarReparacion(idCliente, titulo, cat, descripcion, direccion, 1);
            System.out.println("🛠️ [TRABAJO] Nueva incidencia '" + titulo + "' registrada.");
            procesarFotos(datos.path("urls_fotos"), nuevo.getId());

            respuesta.put("status", 201);
            respuesta.put("mensaje", "Trabajo creado correctamente");
            respuesta.putObject("datos").put("id", nuevo.getId());

            // NUEVO TRABAJO: Se notifica a todos los gerentes para que puedan presupuestar
            Broadcaster.getInstancia().difundirEventoTrabajo("NUEVO", nuevo.getId(), idCliente, -1, "Nueva incidencia registrada");

        } catch (ServiceException e) {
            error(respuesta, 400, e.getMessage());
        } catch (Exception e) {
            error(respuesta, 500, "Error interno: " + e.getMessage());
        }
    }

    public void procesarAsignarOperario(JsonNode datos, ObjectNode respuesta) {
        try {
            validarRequeridos(datos, "idTrabajo", "idOperario");
            int idT = datos.get("idTrabajo").asInt();
            int idO = datos.get("idOperario").asInt();

            trabajoService.asignarOperario(idT, idO > 0 ? idO : null);
            System.out.println("💼 [TRABAJO] Cambio de asignación en Incidencia #" + idT);

            respuesta.put("status", 200);
            respuesta.put("mensaje", idO > 0 ? "Operario asignado" : "Operario desasignado");
            
            // Obtenemos los interesados para el broadcast dirigido
            Trabajo t = trabajoService.obtenerPorId(idT);
            int idCliente = t.getCliente() != null ? t.getCliente().getId() : 0;
            int idEmpresa = t.getOperarioAsignado() != null ? t.getOperarioAsignado().getIdEmpresa() : 0;
            
            Broadcaster.getInstancia().difundirEventoTrabajo("ASIGNACION", idT, idCliente, idEmpresa, "Operario asignado");
        } catch (Exception e) {
            error(respuesta, 400, "Error asignación: " + e.getMessage());
        }
    }

    public void procesarFinalizarTrabajo(JsonNode datos, ObjectNode respuesta) {
        try {
            validarRequeridos(datos, "idTrabajo");
            int idT = datos.get("idTrabajo").asInt();
            String informe = datos.path("informe").asText("Finalizado correctamente.");

            trabajoService.finalizarTrabajo(idT, informe);
            procesarFotos(datos.path("fotos"), idT);
            facturaService.generarFactura(idT);

            respuesta.put("status", 200);
            respuesta.put("mensaje", "Trabajo finalizado y factura generada.");
            
            Trabajo t = trabajoService.obtenerPorId(idT);
            int idCliente = t.getCliente() != null ? t.getCliente().getId() : 0;
            int idEmpresa = t.getOperarioAsignado() != null ? t.getOperarioAsignado().getIdEmpresa() : 0;

            Broadcaster.getInstancia().difundirEventoTrabajo("FINALIZADO", idT, idCliente, idEmpresa, "Trabajo finalizado");
        } catch (Exception e) {
            error(respuesta, 500, "Error al finalizar: " + e.getMessage());
        }
    }

    public void procesarCancelarTrabajo(JsonNode datos, ObjectNode respuesta) {
        try {
            int idT = datos.get("idTrabajo").asInt();
            Trabajo t = trabajoService.obtenerPorId(idT);
            int idCliente = t.getCliente() != null ? t.getCliente().getId() : 0;
            int idEmpresa = t.getOperarioAsignado() != null ? t.getOperarioAsignado().getIdEmpresa() : 0;

            trabajoService.cancelarTrabajo(idT, datos.path("motivo").asText("Cancelado"));
            respuesta.put("status", 200);
            Broadcaster.getInstancia().difundirEventoTrabajo("CANCELACION", idT, idCliente, idEmpresa, "Incidencia cancelada");
        } catch (Exception e) {
            error(respuesta, 400, e.getMessage());
        }
    }

    public void procesarModificarTrabajo(JsonNode datos, ObjectNode respuesta) {
        try {
            validarRequeridos(datos, "idTrabajo");
            int idT = datos.get("idTrabajo").asInt();

            trabajoService.modificarTrabajo(idT,
                    datos.path("titulo").asText(null),
                    datos.path("descripcion").asText(null),
                    datos.path("direccion").asText(null),
                    parseCategoria(datos.path("categoria").asText(null)),
                    datos.path("urgencia").asInt(1));

            if (datos.has("urls_fotos")) {
                procesarFotos(datos.get("urls_fotos"), idT);
            }

            Trabajo t = trabajoService.obtenerPorId(idT);
            int idCliente = t.getCliente() != null ? t.getCliente().getId() : 0;
            int idEmpresa = t.getOperarioAsignado() != null ? t.getOperarioAsignado().getIdEmpresa() : 0;

            respuesta.put("status", 200);
            Broadcaster.getInstancia().difundirEventoTrabajo("MODIFICACION", idT, idCliente, idEmpresa, "Datos actualizados");
        } catch (Exception e) {
            error(respuesta, 400, e.getMessage());
        }
    }

    public void procesarValorarTrabajo(JsonNode datos, ObjectNode respuesta) {
        try {
            int idT = datos.get("idTrabajo").asInt();
            trabajoService.valorarTrabajo(idT,
                    datos.get("valoracion").asInt(),
                    datos.path("comentarioCliente").asText(""));
            respuesta.put("status", 200);

            // BROADCAST: Notificar nueva valoración
            TrabajoDAO trabajoDAO = new DataRepositoryImpl().getTrabajoDAO();
            Trabajo t = trabajoDAO.obtenerPorId(idT);
            if (t != null && t.getOperarioAsignado() != null) {
                int idEmp = t.getOperarioAsignado().getIdEmpresa();
                Broadcaster.getInstancia().difundirValoracion(idT, idEmp, "Nueva valoración recibida");
            }
        } catch (Exception e) {
            error(respuesta, 400, e.getMessage());
        }
    }

    // --- Helpers de soporte ---

    private void validarRequeridos(JsonNode datos, String... campos) throws ServiceException {
        for (String c : campos) {
            if (!datos.has(c) || datos.get(c).isNull())
                throw new ServiceException("Falta campo obligatorio: " + c);
        }
    }

    private CategoriaServicio parseCategoria(String value) {
        if (value == null) return null;
        try {
            return CategoriaServicio.valueOf(value.toUpperCase());
        } catch (Exception e) {
            return CategoriaServicio.OTROS;
        }
    }

    private void procesarFotos(JsonNode array, int idT) {
        if (array.isArray()) {
            // Obtenemos las fotos que ya tiene el trabajo para no duplicarlas
            List<FotoTrabajo> actuales = fotoTrabajoDAO.obtenerPorTrabajo(idT);
            List<String> urlsActuales = new ArrayList<>();
            if (actuales != null) {
                for (FotoTrabajo ft : actuales) urlsActuales.add(ft.getUrl());
            }

            // Recorremos las nuevas URLs y las insertamos si no existen
            for (JsonNode node : array) {
                String url = node.asText();
                if (!url.isBlank() && !urlsActuales.contains(url)) {
                    fotoTrabajoDAO.insertar(new FotoTrabajo(0, idT, url));
                }
            }
        }
    }

    private void error(ObjectNode resp, int status, String msg) {
        resp.put("status", status);
        resp.put("mensaje", msg);
    }
}

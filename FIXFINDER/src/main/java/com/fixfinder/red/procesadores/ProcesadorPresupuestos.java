package com.fixfinder.red.procesadores;

import com.fixfinder.red.Broadcaster;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixfinder.modelos.Empresa;
import com.fixfinder.modelos.Presupuesto;
import com.fixfinder.modelos.Trabajo;
import com.fixfinder.service.interfaz.PresupuestoService;
import com.fixfinder.utilidades.ServiceException;
import com.fixfinder.data.DataRepositoryImpl;
import com.fixfinder.data.interfaces.TrabajoDAO;

import com.fixfinder.modelos.enums.EstadoPresupuesto;
import java.util.List;

public class ProcesadorPresupuestos {

    private final PresupuestoService presupuestoService;
    private final ObjectMapper mapper;

    public ProcesadorPresupuestos(PresupuestoService presupuestoService) {
        this.presupuestoService = presupuestoService;
        this.mapper = new ObjectMapper();
    }

    public void procesarCrearPresupuesto(JsonNode datos, ObjectNode respuesta) {
        if (datos == null) {
            respuesta.put("status", 400);
            return;
        }
        try {
            int idTrabajo = datos.get("idTrabajo").asInt();
            int idEmpresa = datos.get("idEmpresa").asInt();
            double monto = datos.get("monto").asDouble();
            String notas = datos.path("notas").asText("");


            Presupuesto p = new Presupuesto();
            Trabajo t = new Trabajo();
            t.setId(idTrabajo);
            p.setTrabajo(t);

            Empresa emp = new Empresa();
            emp.setId(idEmpresa);
            p.setEmpresa(emp);

            p.setMonto(monto);
            p.setNotas(notas);
            p.setEstado(EstadoPresupuesto.PENDIENTE);

            presupuestoService.crearPresupuesto(p);
            System.out.println("💰 [PRESUPUESTO] Nueva oferta registrada de " + monto + "€ para Incidencia #" + idTrabajo);

            respuesta.put("status", 201);
            respuesta.put("mensaje", "Presupuesto enviado correctamente");
            respuesta.set("datos", presupuestoToJson(p));

            // BROADCAST: Notificar cambio de estado (PRESUPUESTADO) al cliente y a los gerentes
            TrabajoDAO trabajoDAO = new DataRepositoryImpl().getTrabajoDAO();
            int idCliente = trabajoDAO.obtenerPorId(idTrabajo).getCliente().getId();
            Broadcaster.getInstancia().difundirEventoPresupuesto("TRABAJO_PRESUPUESTADO", idTrabajo, idCliente, idEmpresa, "Nueva oferta de presupuesto registrada");

        } catch (ServiceException e) {
            System.out.println("[DEBUG] ServiceException: " + e.getMessage());
            respuesta.put("status", 400);
            respuesta.put("mensaje", e.getMessage());
        } catch (Exception e) {
            System.out.println("[DEBUG] EXCEPTION CRITICA en Crear Presupuesto: " + e.getMessage());
            e.printStackTrace();
            respuesta.put("status", 500);
            respuesta.put("mensaje", "Error interno al crear presupuesto");
            e.printStackTrace();
        }
    }

    public void procesarListarPresupuestos(JsonNode datos, ObjectNode respuesta) {
        try {
            int idTrabajo = datos.get("idTrabajo").asInt();
            List<Presupuesto> lista = presupuestoService.listarPorTrabajo(idTrabajo);

            respuesta.put("status", 200);
            respuesta.put("mensaje", "Listado de presupuestos obtenido");

            ArrayNode array = mapper.createArrayNode();
            for (Presupuesto p : lista) {
                array.add(presupuestoToJson(p));
            }
            respuesta.set("datos", array);

        } catch (ServiceException e) {
            respuesta.put("status", 400);
            respuesta.put("mensaje", e.getMessage());
        } catch (Exception e) {
            respuesta.put("status", 500);
            respuesta.put("mensaje", "Error interno al listar presupuestos");
            e.printStackTrace();
        }
    }

    public void procesarAceptarPresupuesto(JsonNode datos, ObjectNode respuesta) {
        try {
            int idPresupuesto = datos.get("idPresupuesto").asInt();
            presupuestoService.aceptarPresupuesto(idPresupuesto);
            System.out.println("✅ [PRESUPUESTO] Oferta aceptada para la Incidencia.");

            respuesta.put("status", 200);
            respuesta.put("mensaje", "Presupuesto aceptado correctamente.");

            // BROADCAST: Notificar aceptación
            Presupuesto p = presupuestoService.obtenerPorId(idPresupuesto);
            int idT = p.getTrabajo().getId();
            int idCliente = p.getTrabajo().getCliente() != null ? p.getTrabajo().getCliente().getId() : 0;
            int idEmp = p.getEmpresa() != null ? p.getEmpresa().getId() : 0;

            Broadcaster.getInstancia().difundirEventoPresupuesto("PRESUPUESTO_ACEPTADO", idT, idCliente, idEmp, "Presupuesto aceptado");

        } catch (ServiceException e) {
            respuesta.put("status", 400);
            respuesta.put("mensaje", e.getMessage());
        } catch (Exception e) {
            respuesta.put("status", 500);
            respuesta.put("mensaje", "Error interno al aceptar presupuesto");
            e.printStackTrace();
        }
    }

    public void procesarRechazarPresupuesto(JsonNode datos, ObjectNode respuesta) {
        try {
            int idPresupuesto = datos.get("idPresupuesto").asInt();
            presupuestoService.rechazarPresupuesto(idPresupuesto);

            respuesta.put("status", 200);
            respuesta.put("mensaje", "Presupuesto rechazado correctamente.");

            // BROADCAST: Notificar rechazo (permite refrescar dashboards en tiempo real)
            Presupuesto p = presupuestoService.obtenerPorId(idPresupuesto);
            int idT = p.getTrabajo().getId();
            int idCliente = p.getTrabajo().getCliente() != null ? p.getTrabajo().getCliente().getId() : 0;
            int idEmp = p.getEmpresa() != null ? p.getEmpresa().getId() : 0;

            Broadcaster.getInstancia().difundirEventoPresupuesto("PRESUPUESTO_RECHAZADO", idT, idCliente, idEmp, "Presupuesto rechazado por el cliente");

        } catch (ServiceException e) {
            respuesta.put("status", 400);
            respuesta.put("mensaje", e.getMessage());
        } catch (Exception e) {
            respuesta.put("status", 500);
            respuesta.put("mensaje", "Error interno al rechazar presupuesto");
            e.printStackTrace();
        }
    }

    private ObjectNode presupuestoToJson(Presupuesto p) {
        ObjectNode n = mapper.createObjectNode();
        n.put("id", p.getId());
        n.put("monto", p.getMonto());
        n.put("estado", p.getEstado() != null ? p.getEstado().toString() : "PENDIENTE");
        n.put("fechaEnvio", p.getFechaEnvio() != null ? p.getFechaEnvio().toString() : null);
        n.put("notas", p.getNotas());

        if (p.getEmpresa() != null) {
            ObjectNode emp = n.putObject("empresa");
            emp.put("id", p.getEmpresa().getId());
            emp.put("nombre", p.getEmpresa().getNombre());
            emp.put("email", p.getEmpresa().getEmailContacto());
            emp.put("telefono", p.getEmpresa().getTelefono());
            emp.put("direccion", p.getEmpresa().getDireccion());
            emp.put("cif", p.getEmpresa().getCif());
            emp.put("url_foto", p.getEmpresa().getUrlFoto());
        }
        return n;
    }
}

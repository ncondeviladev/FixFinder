package com.fixfinder.red.procesadores;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixfinder.modelos.Empresa;
import com.fixfinder.modelos.Presupuesto;
import com.fixfinder.modelos.Trabajo;
import com.fixfinder.service.interfaz.PresupuestoService;
import com.fixfinder.utilidades.ServiceException;

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
        System.err.println("🚨 [DEBUG-CRITICO] Entrando en ProcesadorPresupuestos.procesarCrearPresupuesto");
        if (datos == null) {
            System.err.println("🚨 [DEBUG-CRITICO] Error: Datos son NULL");
            respuesta.put("status", 400);
            return;
        }
        System.err.println("🚨 [DEBUG-CRITICO] Payload: " + datos.toString());
        try {
            int idTrabajo = datos.get("idTrabajo").asInt();
            int idEmpresa = datos.get("idEmpresa").asInt();
            double monto = datos.get("monto").asDouble();
            String notas = datos.path("notas").asText("");

            System.out.println("📩 [DEBUG-BUDGET] Recibida oferta: Trabajo=" + idTrabajo + ", Empresa=" + idEmpresa + ", Monto=" + monto);

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

            respuesta.put("status", 201);
            respuesta.put("mensaje", "Presupuesto enviado correctamente");
            System.out.println("✅ [DEBUG-BUDGET] Presupuesto procesado y guardado en DB.");
            respuesta.set("datos", presupuestoToJson(p));

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

            respuesta.put("status", 200);
            respuesta.put("mensaje", "Presupuesto aceptado correctamente.");

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
        }
        return n;
    }
}

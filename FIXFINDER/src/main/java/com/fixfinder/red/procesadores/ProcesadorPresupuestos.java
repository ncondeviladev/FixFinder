package com.fixfinder.red.procesadores;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixfinder.data.DataRepository;
import com.fixfinder.data.DataRepositoryImpl;
import com.fixfinder.modelos.Empresa;
import com.fixfinder.modelos.Presupuesto;
import com.fixfinder.modelos.Trabajo;
import com.fixfinder.service.interfaz.PresupuestoService;
import com.fixfinder.utilidades.ServiceException;

import java.util.List;

public class ProcesadorPresupuestos {

    private final PresupuestoService presupuestoService;
    private final ObjectMapper mapper;

    public ProcesadorPresupuestos(PresupuestoService presupuestoService) {
        this.presupuestoService = presupuestoService;
        this.mapper = new ObjectMapper();
    }

    public void procesarCrearPresupuesto(JsonNode datos, ObjectNode respuesta) {
        System.out.println("[DEBUG] INICIO procesarCrearPresupuesto");
        if (datos == null) {
            System.out.println("[DEBUG] Datos es NULL");
            respuesta.put("status", 400);
            return;
        }
        System.out.println("[DEBUG] Datos: " + datos.toString());
        try {
            int idTrabajo = datos.get("idTrabajo").asInt();
            int idEmpresa = datos.get("idEmpresa").asInt();
            double monto = datos.get("monto").asDouble();

            Presupuesto p = new Presupuesto();
            Trabajo t = new Trabajo();
            t.setId(idTrabajo);
            p.setTrabajo(t);

            Empresa e = new Empresa();
            e.setId(idEmpresa);
            p.setEmpresa(e);

            p.setMonto(monto);

            // Actualizar la descripción del trabajo como "Hoja Informativa" compartida
            if (datos.has("nuevaDescripcion")) {
                String desc = datos.get("nuevaDescripcion").asText();
                try {
                    // Acceder al repositorio de trabajos directamente o vía servicio
                    DataRepository repository = new DataRepositoryImpl();
                    Trabajo trabajoOriginal = repository.getTrabajoDAO().obtenerPorId(idTrabajo);
                    if (trabajoOriginal != null) {
                        trabajoOriginal.setDescripcion(desc);
                        repository.getTrabajoDAO().actualizar(trabajoOriginal);
                        System.out.println("[DEBUG] Hoja informativa de trabajo #" + idTrabajo + " actualizada.");
                    }
                } catch (Exception e_repo) {
                    System.err.println("Error actualizando descripción del trabajo: " + e_repo.getMessage());
                }
            }

            System.out.println("[DEBUG] Llamando a presupuestoService.crearPresupuesto...");
            presupuestoService.crearPresupuesto(p);
            System.out.println("[DEBUG] Retorno de presupuestoService.crearPresupuesto OK");

            respuesta.put("status", 200);
            respuesta.put("mensaje", "Presupuesto enviado correctamente por " + monto + "€");
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

    private ObjectNode presupuestoToJson(Presupuesto p) {
        ObjectNode n = mapper.createObjectNode();
        n.put("id", p.getId());
        n.put("monto", p.getMonto());
        n.put("estado", p.getEstado() != null ? p.getEstado().toString() : "PENDIENTE");
        n.put("fechaEnvio", p.getFechaEnvio() != null ? p.getFechaEnvio().toString() : null);
        // notas eliminadas del JSON de salida del presupuesto

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

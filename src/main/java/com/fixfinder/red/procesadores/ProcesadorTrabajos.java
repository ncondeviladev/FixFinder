package com.fixfinder.red.procesadores;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fixfinder.modelos.Trabajo;
import com.fixfinder.modelos.enums.CategoriaServicio;
import com.fixfinder.service.interfaz.TrabajoService;
import com.fixfinder.utilidades.ServiceException;

public class ProcesadorTrabajos {

    private final TrabajoService trabajoService;

    public ProcesadorTrabajos(TrabajoService trabajoService) {
        this.trabajoService = trabajoService;
    }

    public void procesarCrearTrabajo(JsonNode datos, ObjectNode respuesta) {
        if (datos != null) {
            try {
                // Validaciones básicas
                if (!datos.has("idCliente"))
                    throw new ServiceException("Falta ID Cliente");
                if (!datos.has("descripcion"))
                    throw new ServiceException("Falta descripción");

                // Extraer datos del JSON
                int idCliente = datos.get("idCliente").asInt();
                String titulo = datos.has("titulo") ? datos.get("titulo").asText() : "";
                String descOriginal = datos.get("descripcion").asText();
                String direccion = datos.has("direccion") ? datos.get("direccion").asText() : "";

                // Recuperamos la urgencia (1=Normal, 2=Prioridad, 3=Urgente)
                int urgencia = datos.has("urgencia") ? datos.get("urgencia").asInt() : 1;

                // Modificar visualmente la descripción si es urgente
                String descripcionFinal = descOriginal;
                if (urgencia == 3)
                    descripcionFinal = "[URGENTE!!!] " + descOriginal;
                else if (urgencia == 2)
                    descripcionFinal = "[PRIORIDAD] " + descOriginal;

                // Convertir la categoría que llega como texto al Enum
                CategoriaServicio categoria = CategoriaServicio.OTROS;
                if (datos.has("categoria") && !datos.get("categoria").isNull()) {
                    try {
                        categoria = CategoriaServicio.valueOf(datos.get("categoria").asText().toUpperCase());
                    } catch (IllegalArgumentException e) {
                        // Si mandan algo raro, lo metemos a OTROS
                        categoria = CategoriaServicio.OTROS;
                    }
                }

                // Llamar al servicio para crear el trabajo PENDIENTE
                // Pasamos '1' como urgencia al servicio porque el modelo no la soporta, solo
                // como informacion en la UI
                Trabajo nuevoTrabajo = trabajoService.solicitarReparacion(idCliente,
                        titulo, categoria, descripcionFinal, direccion, 1);

                System.out.println("[TRABAJO-CREADO] ID: " + nuevoTrabajo.getId() + " - "
                        + titulo + " (" + descripcionFinal + ") [Cliente ID: " + idCliente + "]");

                // Devolver respuesta OK
                respuesta.put("status", 201);
                respuesta.put("mensaje", "Trabajo creado correctamente");
                ObjectNode datosTrabajo = respuesta.putObject("datos");
                datosTrabajo.put("id", nuevoTrabajo.getId());

            } catch (ServiceException e) {
                System.out.println("[ERROR-CREAR-TRABAJO-VALIDACION] " + e.getMessage());
                respuesta.put("status", 400);
                respuesta.put("mensaje", "Error validación: " + e.getMessage());
            } catch (Exception e) {
                System.err.println("[ERROR-CREAR-TRABAJO-SERVER] " + e.getMessage());
                e.printStackTrace();
                respuesta.put("status", 500);
                respuesta.put("mensaje", "Error servidor: " + e.getMessage());
            }
        } else {
            respuesta.put("status", 400);
            respuesta.put("mensaje", "Faltan datos para CREAR_TRABAJO");
        }
    }
}

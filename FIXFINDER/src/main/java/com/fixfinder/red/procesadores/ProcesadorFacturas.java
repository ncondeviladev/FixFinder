package com.fixfinder.red.procesadores;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fixfinder.modelos.Factura;
import com.fixfinder.service.interfaz.FacturaService;
import com.fixfinder.utilidades.ServiceException;

public class ProcesadorFacturas {

    private final FacturaService facturaService;
    private final ObjectMapper mapper;

    public ProcesadorFacturas(FacturaService facturaService) {
        this.facturaService = facturaService;
        this.mapper = new ObjectMapper();
    }

    public void procesarGenerarFactura(JsonNode datos, ObjectNode respuesta) {
        try {
            int idTrabajo = datos.get("idTrabajo").asInt();
            Factura factura = facturaService.generarFactura(idTrabajo);

            respuesta.put("status", 200);
            respuesta.put("mensaje", "Factura generada correctamente.");
            respuesta.set("datos", facturaToJson(factura));

        } catch (ServiceException e) {
            respuesta.put("status", 400);
            respuesta.put("mensaje", e.getMessage());
        } catch (Exception e) {
            respuesta.put("status", 500);
            respuesta.put("mensaje", "Error interno al generar factura");
            e.printStackTrace();
        }
    }

    private JsonNode facturaToJson(Factura f) {
        ObjectNode node = mapper.createObjectNode();
        node.put("id", f.getId());
        node.put("numeroFactura", f.getNumeroFactura());
        node.put("baseImponible", f.getBaseImponible());
        node.put("iva", f.getIva());
        node.put("total", f.getTotal());
        node.put("fechaEmision", f.getFechaEmision() != null ? f.getFechaEmision().toString() : null);
        node.put("rutaPdf", f.getRutaPdf());
        node.put("pagada", f.isPagada());

        if (f.getTrabajo() != null) {
            ObjectNode tNode = mapper.createObjectNode();
            tNode.put("id", f.getTrabajo().getId());
            tNode.put("titulo", f.getTrabajo().getTitulo());
            node.set("trabajo", tNode);
        }
        return node;
    }

    public void procesarPagarFactura(JsonNode datos, ObjectNode respuesta) {
        try {
            int idFactura;
            if (datos.has("idFactura")) {
                idFactura = datos.get("idFactura").asInt();
            } else if (datos.has("idTrabajo")) {
                int idTrabajo = datos.get("idTrabajo").asInt();
                Factura f = facturaService.obtenerPorTrabajo(idTrabajo);
                if (f == null) {
                    throw new ServiceException("No se encontró factura para el trabajo ID " + idTrabajo);
                }
                idFactura = f.getId();
            } else {
                throw new ServiceException("Falta idFactura o idTrabajo para procesar el pago.");
            }

            facturaService.marcarComoPagada(idFactura);

            respuesta.put("status", 200);
            respuesta.put("mensaje", "Factura " + idFactura + " pagada correctamente.");

        } catch (ServiceException e) {
            respuesta.put("status", 400);
            respuesta.put("mensaje", e.getMessage());
        } catch (Exception e) {
            respuesta.put("status", 500);
            respuesta.put("mensaje", "Error interno al pagar factura");
            e.printStackTrace();
        }
    }
}

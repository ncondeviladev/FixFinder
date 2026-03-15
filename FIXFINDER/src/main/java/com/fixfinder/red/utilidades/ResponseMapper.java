package com.fixfinder.red.utilidades;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fixfinder.modelos.Operario;
import com.fixfinder.modelos.Presupuesto;
import com.fixfinder.modelos.Trabajo;
import com.fixfinder.modelos.Usuario;
import com.fixfinder.modelos.componentes.FotoTrabajo;

/**
 * Utilidad para transformar modelos de dominio en estructuras JSON enriquecidas
 * preparadas para la red. Centraliza la lógica de DTOs y mapeo.
 */
public class ResponseMapper {

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Transforma una lista de Trabajos en una lista de mapas enriquecidos con datos
     * de contactos y presupuestos.
     */
    public List<Map<String, Object>> mapearListaTrabajos(List<Trabajo> lista) {
        List<Map<String, Object>> resultado = new ArrayList<>();
        for (Trabajo t : lista) {
            resultado.add(mapearTrabajoEnriquecido(t));
        }
        return resultado;
    }

    /**
     * Mapea un único trabajo con todos sus detalles relacionados.
     */
    public Map<String, Object> mapearTrabajoEnriquecido(Trabajo t) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", t.getId());
        map.put("titulo", t.getTitulo() != null ? t.getTitulo() : "Sin título");
        map.put("descripcion", t.getDescripcion());
        map.put("categoria", t.getCategoria().toString());
        map.put("direccion", t.getDireccion() != null ? t.getDireccion() : "");
        map.put("estado", t.getEstado().toString());
        map.put("fecha", t.getFechaCreacion() != null ? t.getFechaCreacion().toString() : "");
        map.put("valoracion", t.getValoracion());
        map.put("comentarioCliente", t.getComentarioCliente());
        map.put("fechaFinalizacion", t.getFechaFinalizacion() != null ? t.getFechaFinalizacion().toString() : null);

        // IDs básicos
        map.put("idCliente", t.getCliente() != null ? t.getCliente().getId() : null);
        map.put("idOperario", t.getOperarioAsignado() != null ? t.getOperarioAsignado().getId() : null);

        // Fotos
        List<String> urlsFotos = new ArrayList<>();
        if (t.getFotos() != null) {
            for (FotoTrabajo f : t.getFotos()) {
                urlsFotos.add(f.getUrl());
            }
        }
        map.put("urls_fotos", urlsFotos);

        // Contactos
        map.put("cliente", mapearUsuario(t.getCliente()));
        map.put("operarioAsignado", mapearUsuario(t.getOperarioAsignado()));

        // Ubicación
        if (t.getUbicacion() != null) {
            Map<String, Double> loc = new HashMap<>();
            loc.put("lat", t.getUbicacion().getLatitud());
            loc.put("lon", t.getUbicacion().getLongitud());
            map.put("ubicacion", loc);
        }

        return map;
    }

    /**
     * Mapea un usuario a un nodo JSON profesional.
     */
    public ObjectNode mapearUsuario(Usuario u) {
        if (u == null)
            return null;
        ObjectNode node = mapper.createObjectNode();
        node.put("id", u.getId());
        node.put("nombre", u.getNombreCompleto());
        node.put("telefono", u.getTelefono());
        node.put("email", u.getEmail());
        node.put("foto", u.getUrlFoto());
        node.put("direccion", u.getDireccion());

        if (u instanceof Operario) {
            node.put("especialidad", ((Operario) u).getEspecialidad().toString());
            node.put("idEmpresa", ((Operario) u).getIdEmpresa());
        }
        return node;
    }

    /**
     * Mapea un presupuesto a un nodo JSON profesional.
     */
    public ObjectNode mapearPresupuesto(Presupuesto p) {
        if (p == null)
            return null;
        ObjectNode node = mapper.createObjectNode();
        node.put("id", p.getId());
        node.put("estado", p.getEstado().toString());
        node.put("precioTotal", p.getMonto());

        if (p.getEmpresa() != null) {
            ObjectNode emp = node.putObject("empresa");
            emp.put("id", p.getEmpresa().getId());
            emp.put("nombre", p.getEmpresa().getNombre());
            emp.put("telefono", p.getEmpresa().getTelefono());
        }
        return node;
    }
}

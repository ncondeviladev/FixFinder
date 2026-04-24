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
 * Utilidad para transformar modelos de dominio en estructuras JSON enriquecidas.
 */
public class ResponseMapper {

    private final ObjectMapper mapper = new ObjectMapper();

    public List<Map<String, Object>> mapearListaTrabajos(List<Trabajo> lista) {
        List<Map<String, Object>> resultado = new ArrayList<>();
        for (Trabajo t : lista) {
            resultado.add(mapearTrabajoEnriquecido(t));
        }
        return resultado;
    }

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

        map.put("idCliente", t.getCliente() != null ? t.getCliente().getId() : null);
        map.put("idOperario", t.getOperarioAsignado() != null ? t.getOperarioAsignado().getId() : null);

        List<String> urlsFotos = new ArrayList<>();
        if (t.getFotos() != null) {
            for (FotoTrabajo f : t.getFotos()) {
                urlsFotos.add(f.getUrl());
            }
        }
        map.put("urls_fotos", urlsFotos);

        map.put("cliente", mapearUsuario(t.getCliente()));
        map.put("operarioAsignado", mapearUsuario(t.getOperarioAsignado()));

        if (t.getUbicacion() != null) {
            Map<String, Double> loc = new HashMap<>();
            loc.put("lat", t.getUbicacion().getLatitud());
            loc.put("lon", t.getUbicacion().getLongitud());
            map.put("ubicacion", loc);
        }

        return map;
    }

    public ObjectNode mapearUsuario(Usuario u) {
        if (u == null) return null;
        ObjectNode node = mapper.createObjectNode();
        node.put("id", u.getId());
        node.put("nombre", u.getNombreCompleto());
        node.put("telefono", u.getTelefono());
        node.put("email", u.getEmail());
        node.put("url_foto", u.getUrlFoto());
        node.put("direccion", u.getDireccion());

        if (u instanceof Operario) {
            node.put("especialidad", ((Operario) u).getEspecialidad().toString());
            node.put("idEmpresa", ((Operario) u).getIdEmpresa());
        }
        return node;
    }

    /**
     * Mapea un presupuesto con Filtro de Privacidad Contextual.
     * El dueño de la incidencia y la propia empresa emisora tienen visibilidad total.
     */
    public ObjectNode mapearPresupuesto(Presupuesto p, int idUsuarioConsulta, int idEmpresaConsulta, int idClientePropietario) {
        if (p == null) return null;
        ObjectNode node = mapper.createObjectNode();
        node.put("id", p.getId());
        node.put("estado", p.getEstado().toString());

        // Visibilidad total si:
        // 1. Es un presupuesto emitido por la empresa que consulta.
        // 2. El usuario que consulta es el Cliente dueño de la incidencia.
        // 3. El presupuesto ya ha sido aceptado (se vuelve público para los involucrados).
        boolean esPropio = (p.getEmpresa() != null && p.getEmpresa().getId() == idEmpresaConsulta);
        boolean esDuenioIncidencia = (idUsuarioConsulta == idClientePropietario);
        boolean esAceptado = "ACEPTADO".equalsIgnoreCase(p.getEstado().toString());

        if (esAceptado || esPropio || esDuenioIncidencia) {
            node.put("monto", p.getMonto());
            node.put("notas", p.getNotas());
        } else {
            // Máscara de privacidad para la competencia
            node.put("monto", 0.0);
            node.put("notas", "--- Contenido Privado (Oferta de competencia) ---");
        }

        if (p.getEmpresa() != null) {
            ObjectNode emp = node.putObject("empresa");
            emp.put("id", p.getEmpresa().getId());
            emp.put("nombre", p.getEmpresa().getNombre() != null ? p.getEmpresa().getNombre() : "Empresa");
            emp.put("telefono", p.getEmpresa().getTelefono() != null ? p.getEmpresa().getTelefono() : "");
            emp.put("cif", p.getEmpresa().getCif() != null ? p.getEmpresa().getCif() : "---");
            emp.put("email", p.getEmpresa().getEmailContacto() != null ? p.getEmpresa().getEmailContacto() : "");
            emp.put("direccion", p.getEmpresa().getDireccion() != null ? p.getEmpresa().getDireccion() : "");
            emp.put("url_foto", p.getEmpresa().getUrlFoto());
        }
        return node;
    }
}

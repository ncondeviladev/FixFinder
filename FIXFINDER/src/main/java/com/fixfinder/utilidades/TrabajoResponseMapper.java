package com.fixfinder.utilidades;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fixfinder.modelos.Operario;
import com.fixfinder.modelos.Presupuesto;
import com.fixfinder.modelos.Trabajo;
import com.fixfinder.modelos.Usuario;
import com.fixfinder.modelos.componentes.FotoTrabajo;
import com.fixfinder.service.interfaz.PresupuestoService;

/**
 * Utilidad para transformar modelos de dominio Trabajo en estructuras JSON enriquecidas,
 * garantizando compatibilidad total con la App Flutter y el Dashboard JavaFX.
 */
@Component
public class TrabajoResponseMapper {

    private final ObjectMapper mapper = new ObjectMapper();
    
    @Autowired(required = false)
    private PresupuestoService presupuestoService;

    public List<Map<String, Object>> mapearListaTrabajos(List<Trabajo> lista) {
        return mapearListaTrabajos(lista, -1, -1);
    }

    public List<Map<String, Object>> mapearListaTrabajos(List<Trabajo> lista, int idUsuarioConsulta, int idEmpresaConsulta) {
        List<Map<String, Object>> resultado = new ArrayList<>();
        if (lista != null) {
            for (Trabajo t : lista) {
                resultado.add(mapearTrabajoEnriquecido(t, idUsuarioConsulta, idEmpresaConsulta));
            }
        }
        return resultado;
    }

    public Map<String, Object> mapearTrabajoEnriquecido(Trabajo t) {
        return mapearTrabajoEnriquecido(t, -1, -1);
    }

    public Map<String, Object> mapearTrabajoEnriquecido(Trabajo t, int idUsuarioConsulta, int idEmpresaConsulta) {
        if (t == null) return null;
        Map<String, Object> map = new HashMap<>();
        map.put("id", t.getId());
        map.put("titulo", t.getTitulo() != null ? t.getTitulo() : "Sin título");
        map.put("descripcion", t.getDescripcion());
        map.put("categoria", t.getCategoria() != null ? t.getCategoria().toString() : "OTROS");
        map.put("direccion", t.getDireccion() != null ? t.getDireccion() : "");
        map.put("estado", t.getEstado() != null ? t.getEstado().toString() : "PENDIENTE");
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

        // Enriquecer presupuestos con máscara de privacidad contextual
        enriquecerPresupuestos(t, map, idUsuarioConsulta, idEmpresaConsulta);

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
            node.put("especialidad", ((Operario) u).getEspecialidad() != null ? ((Operario) u).getEspecialidad().toString() : "");
            node.put("idEmpresa", ((Operario) u).getIdEmpresa());
        }
        return node;
    }

    public ObjectNode mapearPresupuesto(Presupuesto p, int idUsuarioConsulta, int idEmpresaConsulta, int idClientePropietario) {
        if (p == null) return null;
        ObjectNode node = mapper.createObjectNode();
        node.put("id", p.getId());
        node.put("estado", p.getEstado() != null ? p.getEstado().toString() : "PENDIENTE");

        boolean esPropio = (p.getEmpresa() != null && p.getEmpresa().getId() == idEmpresaConsulta);
        boolean esDuenioIncidencia = (idUsuarioConsulta == idClientePropietario);
        boolean esAceptado = "ACEPTADO".equalsIgnoreCase(p.getEstado() != null ? p.getEstado().toString() : "");

        if (esAceptado || esPropio || esDuenioIncidencia) {
            node.put("monto", p.getMonto());
            node.put("notas", p.getNotas());
        } else {
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

    private void enriquecerPresupuestos(Trabajo t, Map<String, Object> jobMap, int idUsuarioConsulta, int idEmpresaConsulta) {
        try {
            if (presupuestoService == null) return;
            List<Presupuesto> listap = presupuestoService.listarPorTrabajo(t.getId());
            Object estadoObj = jobMap.get("estado");
            String estadoReal = (estadoObj != null) ? estadoObj.toString() : "PENDIENTE";

            if ("PRESUPUESTADO".equalsIgnoreCase(estadoReal) && idEmpresaConsulta != -1) {
                boolean haOfertado = false;
                if (listap != null) {
                    for (Presupuesto p : listap) {
                        if (p.getEmpresa() != null && p.getEmpresa().getId() == idEmpresaConsulta) {
                            haOfertado = true;
                            break;
                        }
                    }
                }
                if (!haOfertado) {
                    jobMap.put("estado", "PENDIENTE");
                }
            } 

            int idClientePropietario = t.getCliente() != null ? t.getCliente().getId() : -1;

            if (listap != null && !listap.isEmpty()) {
                List<ObjectNode> nodosPresus = new ArrayList<>();
                Presupuesto aceptado = null;

                for (Presupuesto p : listap) {
                    nodosPresus.add(mapearPresupuesto(p, idUsuarioConsulta, idEmpresaConsulta, idClientePropietario));
                    
                    String estP = p.getEstado() != null ? p.getEstado().toString() : "";
                    if ("ACEPTADO".equalsIgnoreCase(estP)) {
                        aceptado = p;
                    }
                }

                jobMap.put("presupuestos", nodosPresus);
                if (aceptado != null) {
                    jobMap.put("presupuesto", mapearPresupuesto(aceptado, idUsuarioConsulta, idEmpresaConsulta, idClientePropietario));
                    jobMap.put("tienePresupuestoAceptado", true);
                } else {
                    jobMap.put("presupuesto", mapearPresupuesto(listap.get(listap.size() - 1), idUsuarioConsulta, idEmpresaConsulta, idClientePropietario));
                    jobMap.put("tienePresupuestoAceptado", false);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

package com.fixfinder.red.procesadores.trabajos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fixfinder.modelos.Operario;
import com.fixfinder.modelos.Presupuesto;
import com.fixfinder.modelos.Trabajo;
import com.fixfinder.modelos.Usuario;
import com.fixfinder.modelos.enums.EstadoPresupuesto;
import com.fixfinder.modelos.enums.EstadoTrabajo;
import com.fixfinder.red.utilidades.ResponseMapper;
import com.fixfinder.service.interfaz.PresupuestoService;
import com.fixfinder.service.interfaz.TrabajoService;
import com.fixfinder.service.interfaz.UsuarioService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Especialista en operaciones de lectura y filtrado de trabajos.
 * Extraído de ProcesadorTrabajos para mejorar la mantenibilidad.
 */
public class ManejadorLecturaTrabajo {

    private final TrabajoService trabajoService;
    private final UsuarioService usuarioService;
    private final PresupuestoService presupuestoService;
    private final ResponseMapper mapper = new ResponseMapper();
    private final ObjectMapper jsonMapper = new ObjectMapper();

    public ManejadorLecturaTrabajo(TrabajoService trabajoService, UsuarioService usuarioService, PresupuestoService presupuestoService) {
        this.trabajoService = trabajoService;
        this.usuarioService = usuarioService;
        this.presupuestoService = presupuestoService;
    }

    public void procesarListarTrabajos(JsonNode datos, ObjectNode respuesta) {
        if (datos == null || !datos.has("idUsuario") || !datos.has("rol")) {
            respuesta.put("status", 400);
            respuesta.put("mensaje", "Faltan parámetros idUsuario/rol");
            return;
        }

        try {
            int idUsuario = datos.get("idUsuario").asInt();
            String rol = datos.get("rol").asText().toUpperCase();
            List<Trabajo> lista;

            switch (rol) {
                case "CLIENTE":
                    lista = trabajoService.historialCliente(idUsuario);
                    break;
                case "OPERARIO":
                    lista = trabajoService.historialOperario(idUsuario);
                    break;
                case "GERENTE":
                    lista = filtrarParaGerente(idUsuario);
                    break;
                case "ADMIN":
                    lista = trabajoService.listarTodos();
                    break;
                default:
                    lista = Collections.emptyList();
            }

            int idEmpresaConsulta = -1;
            if ("GERENTE".equals(rol) || "OPERARIO".equals(rol)) {
                try {
                    Usuario u = usuarioService.obtenerPorId(idUsuario);
                    if (u instanceof Operario) {
                        idEmpresaConsulta = ((Operario) u).getIdEmpresa();
                    }
                } catch (Exception e) {
                    System.err.println("⚠️ [LISTAR] Error cargando perfil de empresa para usuario " + idUsuario);
                }
            }

            if (idEmpresaConsulta == -1 && datos.has("idEmpresa")) {
                idEmpresaConsulta = datos.get("idEmpresa").asInt();
            }

            List<Map<String, Object>> jobsData = new ArrayList<>();
            for (Trabajo t : lista) {
                Map<String, Object> jobMap = mapper.mapearTrabajoEnriquecido(t);
                enriquecerPresupuestos(t.getId(), jobMap, idUsuario, idEmpresaConsulta, t.getCliente() != null ? t.getCliente().getId() : -1);
                jobsData.add(jobMap);
            }

            respuesta.put("status", 200);
            respuesta.put("mensaje", "Listado obtenido");
            respuesta.set("datos", jsonMapper.valueToTree(jobsData));

        } catch (Exception e) {
            e.printStackTrace();
            respuesta.put("status", 500);
            respuesta.put("mensaje", "Error listando trabajos: " + e.getMessage());
        }
    }

    private List<Trabajo> filtrarParaGerente(int idGerente) {
        try {
            Usuario u = usuarioService.obtenerPorId(idGerente);
            int idEmpresa = (u instanceof Operario) ? ((Operario) u).getIdEmpresa() : -1;

            List<Trabajo> todos = trabajoService.listarTodos();
            List<Trabajo> visibles = new ArrayList<>();

            for (Trabajo t : todos) {
                if (t.getEstado() == EstadoTrabajo.PENDIENTE || t.getEstado() == EstadoTrabajo.PRESUPUESTADO) {
                    visibles.add(t);
                    continue;
                }

                try {
                    List<Presupuesto> presus = presupuestoService.listarPorTrabajo(t.getId());
                    if (presus != null) {
                        boolean esMiGanador = presus.stream().anyMatch(p -> 
                            p.getEmpresa() != null && 
                            p.getEmpresa().getId() == idEmpresa && 
                            p.getEstado() == EstadoPresupuesto.ACEPTADO
                        );
                        
                        if (esMiGanador) {
                            visibles.add(t);
                            continue;
                        }
                    }
                } catch (Exception ignored) {}

                if (t.getOperarioAsignado() != null && t.getOperarioAsignado().getIdEmpresa() == idEmpresa) {
                    visibles.add(t);
                }
            }
            return visibles;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private void enriquecerPresupuestos(int idTrabajo, Map<String, Object> jobMap, int idUsuarioConsulta, int idEmpresaConsulta, int idClientePropietario) {
        try {
            List<Presupuesto> listap = presupuestoService.listarPorTrabajo(idTrabajo);
            Object estadoObj = jobMap.get("estado");
            String estadoReal = (estadoObj != null) ? estadoObj.toString() : "NULL";

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

            if (listap != null && !listap.isEmpty()) {
                List<ObjectNode> nodosPresus = new ArrayList<>();
                Presupuesto aceptado = null;

                for (Presupuesto p : listap) {
                    nodosPresus.add(mapper.mapearPresupuesto(p, idUsuarioConsulta, idEmpresaConsulta, idClientePropietario));
                    
                    String estP = p.getEstado() != null ? p.getEstado().toString() : "";
                    if ("ACEPTADO".equalsIgnoreCase(estP)) {
                        aceptado = p;
                    }
                }

                jobMap.put("presupuestos", nodosPresus);
                if (aceptado != null) {
                    jobMap.put("presupuesto", mapper.mapearPresupuesto(aceptado, idUsuarioConsulta, idEmpresaConsulta, idClientePropietario));
                    jobMap.put("tienePresupuestoAceptado", true);
                } else {
                    jobMap.put("presupuesto", mapper.mapearPresupuesto(listap.get(listap.size() - 1), idUsuarioConsulta, idEmpresaConsulta, idClientePropietario));
                    jobMap.put("tienePresupuestoAceptado", false);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

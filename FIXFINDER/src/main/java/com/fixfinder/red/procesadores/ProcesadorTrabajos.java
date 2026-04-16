package com.fixfinder.red.procesadores;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fixfinder.modelos.Operario;
import com.fixfinder.modelos.Presupuesto;
import com.fixfinder.modelos.Trabajo;
import com.fixfinder.modelos.Usuario;
import com.fixfinder.modelos.enums.CategoriaServicio;
import com.fixfinder.modelos.enums.EstadoTrabajo;
import com.fixfinder.modelos.enums.EstadoPresupuesto;
import com.fixfinder.service.interfaz.FacturaService;
import com.fixfinder.service.interfaz.PresupuestoService;
import com.fixfinder.service.interfaz.TrabajoService;
import com.fixfinder.service.interfaz.UsuarioService;
import com.fixfinder.data.DataRepositoryImpl;
import com.fixfinder.data.interfaces.FotoTrabajoDAO;
import com.fixfinder.modelos.componentes.FotoTrabajo;
import com.fixfinder.utilidades.ServiceException;
import com.fixfinder.red.utilidades.ResponseMapper;

/**
 * Procesador de peticiones relacionadas con Trabajos (Incidencias).
 * Maneja la lógica de entrada/salida de red y delega el negocio a los
 * servicios.
 */
public class ProcesadorTrabajos {

    private final TrabajoService trabajoService;
    private final UsuarioService usuarioService;
    private final PresupuestoService presupuestoService;
    private final FacturaService facturaService;
    private final FotoTrabajoDAO fotoTrabajoDAO;
    private final ResponseMapper mapper = new ResponseMapper();
    private final ObjectMapper jsonMapper = new ObjectMapper();

    public ProcesadorTrabajos(TrabajoService trabajoService, UsuarioService usuarioService,
            PresupuestoService presupuestoService, FacturaService facturaService) {
        this.trabajoService = trabajoService;
        this.usuarioService = usuarioService;
        this.presupuestoService = presupuestoService;
        this.facturaService = facturaService;
        this.fotoTrabajoDAO = new DataRepositoryImpl().getFotoTrabajoDAO();
    }

    /**
     * Crea un nuevo trabajo a partir de los datos recibidos.
     */
    public void procesarCrearTrabajo(JsonNode datos, ObjectNode respuesta) {
        if (datos == null) {
            error(respuesta, 400, "Faltan datos");
            return;
        }

        try {
            validarRequeridos(datos, "idCliente", "descripcion");

            int idCliente = datos.get("idCliente").asInt();
            String titulo = datos.path("titulo").asText("");
            String descripcion = datos.get("descripcion").asText();
            String direccion = datos.path("direccion").asText("");
            int urgencia = datos.path("urgencia").asInt(1);

            // Prefijo decorativo por urgencia
            if (urgencia == 3)
                descripcion = "[URGENTE!!!] " + descripcion;
            else if (urgencia == 2)
                descripcion = "[PRIORIDAD] " + descripcion;

            CategoriaServicio cat = parseCategoria(datos.path("categoria").asText("OTROS"));

            Trabajo nuevo = trabajoService.solicitarReparacion(idCliente, titulo, cat, descripcion, direccion, 1);

            // Vincular fotos iniciales
            procesarFotosIniciales(datos.path("urls_fotos"), nuevo.getId());

            respuesta.put("status", 201);
            respuesta.put("mensaje", "Trabajo creado correctamente");
            respuesta.putObject("datos").put("id", nuevo.getId());

        } catch (ServiceException e) {
            error(respuesta, 400, e.getMessage());
        } catch (Exception e) {
            error(respuesta, 500, "Error interno: " + e.getMessage());
        }
    }

    /**
     * Lista trabajos filtrados por el rol y permisos del usuario.
     */
    public void procesarListarTrabajos(JsonNode datos, ObjectNode respuesta) {
        if (datos == null || !datos.has("idUsuario") || !datos.has("rol")) {
            error(respuesta, 400, "Faltan parámetros idUsuario/rol");
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
            if ("GERENTE".equals(rol)) {
                Usuario u = usuarioService.obtenerPorId(idUsuario);
                idEmpresaConsulta = (u instanceof Operario) ? ((Operario) u).getIdEmpresa() : -1;
            }

            // Mapeo profesional y enriquecimiento mediante ResponseMapper
            List<Map<String, Object>> jobsData = new ArrayList<>();
            for (Trabajo t : lista) {
                Map<String, Object> jobMap = mapper.mapearTrabajoEnriquecido(t);

                // Enriquecimiento dinámico de lista de presupuestos (con filtro de privacidad)
                enriquecerPresupuestos(t.getId(), jobMap, idEmpresaConsulta);

                jobsData.add(jobMap);
            }

            System.out.println(
                    "[DEBUG-LIST] Enviando " + jobsData.size() + " trabajos a " + rol + " (ID: " + idUsuario + ")");
            respuesta.put("status", 200);
            respuesta.put("mensaje", "Listado obtenido");
            respuesta.set("datos", jsonMapper.valueToTree(jobsData));

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("[ERROR-LIST] " + e.getMessage());
            error(respuesta, 500, "Error listando trabajos: " + e.getMessage());
        }
    }

    private List<Trabajo> filtrarParaGerente(int idGerente) {
        try {
            Usuario u = usuarioService.obtenerPorId(idGerente);
            int idEmpresa = (u instanceof Operario) ? ((Operario) u).getIdEmpresa() : -1;

            List<Trabajo> todos = trabajoService.listarTodos();
            List<Trabajo> visibles = new ArrayList<>();

            for (Trabajo t : todos) {
                // 1. Visibles para todos si están en fase de subasta activa
                if (t.getEstado() == EstadoTrabajo.PENDIENTE || t.getEstado() == EstadoTrabajo.PRESUPUESTADO) {
                    visibles.add(t);
                    continue;
                }

                // 2. Si el trabajo ya está en curso (ACEPTADO o superior), 
                // solo lo ve la empresa que ganó la subasta.
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

                // 3. Fallback de seguridad por asignación directa de operario
                if (t.getOperarioAsignado() != null && t.getOperarioAsignado().getIdEmpresa() == idEmpresa) {
                    visibles.add(t);
                }
            }
            return visibles;
        } catch (Exception e) {
            System.err.println("[ERROR-GERENTE] Error filtrando para gerente ID " + idGerente + ": " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    private void enriquecerPresupuestos(int idTrabajo, Map<String, Object> jobMap, int idEmpresaConsulta) {
        try {
            List<Presupuesto> listap = presupuestoService.listarPorTrabajo(idTrabajo);
            if (listap != null && !listap.isEmpty()) {
                List<ObjectNode> nodosPresus = new ArrayList<>();
                Presupuesto aceptado = null;

                for (Presupuesto p : listap) {
                    nodosPresus.add(mapper.mapearPresupuesto(p, idEmpresaConsulta));
                    if ("ACEPTADO".equalsIgnoreCase(p.getEstado().toString())) {
                        aceptado = p;
                    }
                }

                // Lista completa para el modo "Subasta"
                jobMap.put("presupuestos", nodosPresus);

                // Mantener el campo singular para compatibilidad o para mostrar el ganador
                if (aceptado != null) {
                    jobMap.put("presupuesto", mapper.mapearPresupuesto(aceptado, idEmpresaConsulta));
                    jobMap.put("tienePresupuestoAceptado", true);
                } else {
                    // Si no hay aceptado, mandamos el último enviado como "actual"
                    jobMap.put("presupuesto", mapper.mapearPresupuesto(listap.get(listap.size() - 1), idEmpresaConsulta));
                    jobMap.put("tienePresupuestoAceptado", false);
                }
            }
        } catch (Exception ignored) {
        }
    }

    public void procesarAsignarOperario(JsonNode datos, ObjectNode respuesta) {
        try {
            validarRequeridos(datos, "idTrabajo", "idOperario");
            int idTrabajo = datos.get("idTrabajo").asInt();
            int idOperario = datos.get("idOperario").asInt();

            trabajoService.asignarOperario(idTrabajo, idOperario > 0 ? idOperario : null);

            respuesta.put("status", 200);
            respuesta.put("mensaje", idOperario > 0 ? "Operario asignado" : "Operario desasignado");
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

            // Fotos finales
            procesarFotosFinales(datos.path("fotos"), idT);

            facturaService.generarFactura(idT);

            respuesta.put("status", 200);
            respuesta.put("mensaje", "Trabajo finalizado y factura generada.");
        } catch (Exception e) {
            error(respuesta, 500, "Error al finalizar: " + e.getMessage());
        }
    }

    public void procesarCancelarTrabajo(JsonNode datos, ObjectNode respuesta) {
        try {
            validarRequeridos(datos, "idTrabajo");
            trabajoService.cancelarTrabajo(datos.get("idTrabajo").asInt(), datos.path("motivo").asText("Cancelado"));
            respuesta.put("status", 200);
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

            // Procesar fotos nuevas si vienen en la petición (Requisito añadir fotos al
            // editar)
            if (datos.has("urls_fotos")) {
                procesarFotosIniciales(datos.get("urls_fotos"), idT);
            }

            respuesta.put("status", 200);
            respuesta.put("mensaje", "Incidencia modificada correctamente");
        } catch (Exception e) {
            error(respuesta, 400, e.getMessage());
        }
    }

    public void procesarValorarTrabajo(JsonNode datos, ObjectNode respuesta) {
        try {
            validarRequeridos(datos, "idTrabajo", "valoracion");
            trabajoService.valorarTrabajo(datos.get("idTrabajo").asInt(),
                    datos.get("valoracion").asInt(),
                    datos.path("comentarioCliente").asText(""));
            respuesta.put("status", 200);
        } catch (Exception e) {
            error(respuesta, 400, e.getMessage());
        }
    }

    // --- Helpers ---

    private void validarRequeridos(JsonNode datos, String... campos) throws ServiceException {
        for (String c : campos) {
            if (!datos.has(c) || datos.get(c).isNull())
                throw new ServiceException("Falta campo obligatorio: " + c);
        }
    }

    private CategoriaServicio parseCategoria(String value) {
        if (value == null)
            return null;
        try {
            return CategoriaServicio.valueOf(value.toUpperCase());
        } catch (Exception e) {
            return CategoriaServicio.OTROS;
        }
    }

    private void procesarFotosIniciales(JsonNode array, int idT) {
        if (array.isArray()) {
            List<FotoTrabajo> actuales = fotoTrabajoDAO.obtenerPorTrabajo(idT);
            List<String> urlsActuales = new ArrayList<>();
            if (actuales != null) {
                for (FotoTrabajo ft : actuales)
                    urlsActuales.add(ft.getUrl());
            }
            for (JsonNode node : array) {
                String url = node.asText();
                if (!url.isEmpty() && !urlsActuales.contains(url)) {
                    fotoTrabajoDAO.insertar(new FotoTrabajo(0, idT, url));
                }
            }
        }
    }

    private void procesarFotosFinales(JsonNode array, int idT) {
        if (array.isArray()) {
            List<FotoTrabajo> actuales = fotoTrabajoDAO.obtenerPorTrabajo(idT);
            List<String> urlsActuales = new ArrayList<>();
            if (actuales != null) {
                for (FotoTrabajo ft : actuales)
                    urlsActuales.add(ft.getUrl());
            }
            for (JsonNode f : array) {
                String url = f.asText();
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

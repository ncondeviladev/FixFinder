package com.fixfinder.red.procesadores;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
import com.fixfinder.modelos.enums.EstadoTrabajo;
import com.fixfinder.service.interfaz.PresupuestoService;
import com.fixfinder.service.interfaz.TrabajoService;
import com.fixfinder.service.interfaz.UsuarioService;
import com.fixfinder.utilidades.ServiceException;

public class ProcesadorTrabajos {

    private final TrabajoService trabajoService;
    private final UsuarioService usuarioService;
    private final PresupuestoService presupuestoService;

    public ProcesadorTrabajos(TrabajoService trabajoService, UsuarioService usuarioService,
            PresupuestoService presupuestoService) {
        this.trabajoService = trabajoService;
        this.usuarioService = usuarioService;
        this.presupuestoService = presupuestoService;
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

    // Muestra la lista de trabajos filtrando por tipo de usuario
    public void procesarListarTrabajos(JsonNode datos, ObjectNode respuesta) {
        if (datos != null && datos.has("idUsuario") && datos.has("rol")) {
            try {
                int idUsuario = datos.get("idUsuario").asInt();
                String rol = datos.get("rol").asText();

                System.out.println("[DEBUG-SERVER] Solicitud listar trabajos. ID: " + idUsuario + ", Rol: " + rol);

                List<Trabajo> lista;

                if ("CLIENTE".equalsIgnoreCase(rol)) {
                    // Cliente ve SOLO sus solicitudes
                    lista = trabajoService.historialCliente(idUsuario);
                } else if ("OPERARIO".equalsIgnoreCase(rol)) {
                    // Operario ve SOLO sus asignaciones
                    lista = trabajoService.historialOperario(idUsuario);
                } else if ("GERENTE".equalsIgnoreCase(rol)) {
                    // Gerente: Filtrado avanzado
                    System.out.println("[DEBUG-SERVER] Rol GERENTE detectado. Aplicando filtro de empresa...");

                    // Onbtenemos el usuario completo de la bd con los datos propocionados
                    Usuario u = usuarioService.obtenerPorId(idUsuario);
                    // Obtenemos el id de la empresa del operario
                    int idEmpresa = -1;
                    if (u instanceof Operario) {
                        idEmpresa = ((Operario) u).getIdEmpresa();
                    }

                    // Obtenemos todos los trabajos
                    List<Trabajo> todos = trabajoService.listarTodos();
                    lista = new ArrayList<>();

                    for (Trabajo t : todos) {
                        // A) Es PENDIENTE (Mercado) -> SÍ
                        if (t.getEstado() == EstadoTrabajo.PENDIENTE) {
                            lista.add(t);
                            continue;
                        }

                        // B) Tiene Operario de mi Empresa -> SÍ
                        if (t.getOperarioAsignado() != null
                                && t.getOperarioAsignado().getIdEmpresa() == idEmpresa) {
                            lista.add(t);
                            continue;
                        }

                        // C) Tiene Presupuesto ACEPTADO de mi Empresa (aunque no tenga operario) -> SÍ
                        // NOTA: Esto añade N consultas, pero es necesario por modelo de datos actual ya
                        // que el trabajo no tiene idEmpresa y el presupuesto sí, asi no modificamos la
                        // estructura ni capa de datos una vez es funcional
                        if (idEmpresa != -1) {
                            Presupuesto p = presupuestoService
                                    .obtenerPorTrabajo(t.getId());
                            if (p != null && p.getEmpresa() != null
                                    && p.getEmpresa().getId() == idEmpresa) {
                                // Si hay presupuesto aceptado de mi empresa, es mi trabajo
                                lista.add(t);
                                continue;
                            }
                        }
                    }
                    System.out.println("[DEBUG-SERVER] Trabajos tras filtro Gerente: " + lista.size());

                } else {
                    lista = Collections.emptyList();
                }

                // Lista de DTOs enriquecidos (Mapas)
                List<Map<String, Object>> listaEnriquecida = new ArrayList<>();

                for (Trabajo t : lista) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", t.getId());
                    map.put("titulo", t.getTitulo() != null ? t.getTitulo() : "Sin título");
                    map.put("descripcion", t.getDescripcion());
                    map.put("categoria", t.getCategoria().toString()); // AÑADIDO: Categoria para el frontend
                    map.put("estado", t.getEstado().toString());
                    map.put("fecha", t.getFechaCreacion() != null ? t.getFechaCreacion().toString() : "");

                    Integer idCliente = t.getCliente() != null ? t.getCliente().getId() : null;
                    Integer idOperario = t.getOperarioAsignado() != null ? t.getOperarioAsignado().getId() : null;

                    map.put("idCliente", idCliente);
                    map.put("idOperario", idOperario);

                    // --- ENRIQUECIMIENTO DE DATOS ---
                    if (!"CLIENTE".equalsIgnoreCase(rol)) {
                        try {
                            // Nombre Cliente
                            if (t.getCliente() != null) {
                                String nombre = t.getCliente().getNombreCompleto();
                                // Si el DAO no trajo el nombre (solo ID), lo buscamos
                                if (nombre == null && idCliente != null) {
                                    Usuario cli = usuarioService.obtenerPorId(idCliente);
                                    nombre = (cli != null) ? cli.getNombreCompleto() : "Desconocido";
                                }
                                map.put("nombreCliente", nombre != null ? nombre : "Desconocido");
                                map.put("direccionCliente", t.getCliente().getDireccion()); // Asumimos que viene o es
                                                                                            // null
                            }

                            // Nombre Operario
                            if (t.getOperarioAsignado() != null) {
                                String nombre = t.getOperarioAsignado().getNombreCompleto();
                                if (nombre == null && idOperario != null) {
                                    Usuario op = usuarioService.obtenerPorId(idOperario);
                                    nombre = (op != null) ? op.getNombreCompleto() : "Desconocido";
                                }
                                map.put("nombreOperario", nombre != null ? nombre : "Desconocido");
                            } else {
                                map.put("nombreOperario", "Sin asignar");
                            }
                        } catch (Exception e) {
                            // Si falla, seguimos
                        }
                    }

                    listaEnriquecida.add(map);
                }

                ObjectMapper mapper = new ObjectMapper();
                JsonNode listaJson = mapper.valueToTree(listaEnriquecida);

                respuesta.put("status", 200);
                respuesta.put("mensaje", "Listado obtenido correctamente");
                respuesta.set("datos", listaJson);

                System.out.println("[LISTAR-TRABAJOS] Usuario " + idUsuario + " (" + rol + ") -> "
                        + listaEnriquecida.size() + " trabajos encontrados.");

            } catch (ServiceException e) {
                System.err.println("[ERROR-LISTAR] " + e.getMessage());
                respuesta.put("status", 500);
                respuesta.put("mensaje", "Error obteniendo lista: " + e.getMessage());
            } catch (Exception e) {
                System.err.println("[ERROR-LISTAR-SERVER] " + e.getMessage());
                e.printStackTrace();
                respuesta.put("status", 500);
                respuesta.put("mensaje", "Error interno servidor: " + e.getMessage());
            }
        } else {
            respuesta.put("status", 400);
            respuesta.put("mensaje", "Faltan datos (idUsuario, rol) para listar");
        }
    }
}

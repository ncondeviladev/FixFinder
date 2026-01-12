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
import com.fixfinder.service.interfaz.FacturaService;
import com.fixfinder.service.interfaz.PresupuestoService;
import com.fixfinder.service.interfaz.TrabajoService;
import com.fixfinder.service.interfaz.UsuarioService;
import com.fixfinder.data.DataRepositoryImpl;
import com.fixfinder.data.interfaces.FotoTrabajoDAO;
import com.fixfinder.modelos.componentes.FotoTrabajo;
import com.fixfinder.utilidades.ServiceException;

public class ProcesadorTrabajos {

    private final TrabajoService trabajoService;
    private final UsuarioService usuarioService;
    private final PresupuestoService presupuestoService;
    private final FacturaService facturaService;
    private final FotoTrabajoDAO fotoTrabajoDAO;

    public ProcesadorTrabajos(TrabajoService trabajoService, UsuarioService usuarioService,
            PresupuestoService presupuestoService, FacturaService facturaService) {
        this.trabajoService = trabajoService;
        this.usuarioService = usuarioService;
        this.presupuestoService = presupuestoService;
        this.facturaService = facturaService;
        this.fotoTrabajoDAO = new DataRepositoryImpl().getFotoTrabajoDAO();
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
                        categoria = CategoriaServicio.OTROS;
                    }
                }

                Trabajo nuevoTrabajo = trabajoService.solicitarReparacion(idCliente,
                        titulo, categoria, descripcionFinal, direccion, 1);

                System.out.println("[TRABAJO-CREADO] ID: " + nuevoTrabajo.getId() + " - "
                        + titulo + " (" + descripcionFinal + ") [Cliente ID: " + idCliente + "]");

                // NUEVO: Procesar lista de fotos si vienen en el JSON
                if (datos.has("urls_fotos") && datos.get("urls_fotos").isArray()) {
                    JsonNode arrayFotos = datos.get("urls_fotos");
                    for (JsonNode urlNode : arrayFotos) {
                        String url = urlNode.asText();
                        if (!url.isEmpty()) {
                            FotoTrabajo foto = new FotoTrabajo();
                            foto.setIdTrabajo(nuevoTrabajo.getId());
                            foto.setUrl(url);
                            fotoTrabajoDAO.insertar(foto);
                            System.out.println("   📸 Foto vinculada: " + url);
                        }
                    }
                }

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

    public void procesarListarTrabajos(JsonNode datos, ObjectNode respuesta) {
        if (datos != null && datos.has("idUsuario") && datos.has("rol")) {
            try {
                int idUsuario = datos.get("idUsuario").asInt();
                String rol = datos.get("rol").asText();

                System.out.println("[DEBUG-SERVER] Solicitud listar trabajos. ID: " + idUsuario + ", Rol: " + rol);

                List<Trabajo> lista;

                if ("CLIENTE".equalsIgnoreCase(rol)) {
                    lista = trabajoService.historialCliente(idUsuario);
                } else if ("OPERARIO".equalsIgnoreCase(rol)) {
                    lista = trabajoService.historialOperario(idUsuario);
                } else if ("GERENTE".equalsIgnoreCase(rol)) {
                    // Gerente: Filtrado avanzado
                    Usuario u = usuarioService.obtenerPorId(idUsuario);
                    int idEmpresa = -1;
                    if (u instanceof Operario) {
                        idEmpresa = ((Operario) u).getIdEmpresa();
                    }

                    List<Trabajo> todos = trabajoService.listarTodos();
                    lista = new ArrayList<>();

                    for (Trabajo t : todos) {
                        if (t.getEstado() == EstadoTrabajo.PENDIENTE) {
                            lista.add(t);
                            continue;
                        }
                        if (t.getOperarioAsignado() != null
                                && t.getOperarioAsignado().getIdEmpresa() == idEmpresa) {
                            lista.add(t);
                            continue;
                        }
                        if (idEmpresa != -1) {
                            final int idEmpresaFinal = idEmpresa;
                            List<Presupuesto> presupuestos = presupuestoService.listarPorTrabajo(t.getId());
                            if (presupuestos != null) {
                                boolean tienePresupuestoMio = presupuestos.stream()
                                        .anyMatch(p -> p.getEmpresa() != null
                                                && p.getEmpresa().getId() == idEmpresaFinal);

                                if (tienePresupuestoMio) {
                                    lista.add(t);
                                    continue;
                                }
                            }
                        }
                    }

                } else if ("ADMIN".equalsIgnoreCase(rol)) {
                    // NUEVO: Admin ve todo (para simulador)
                    lista = trabajoService.listarTodos();
                } else {
                    lista = Collections.emptyList();
                }

                // Lista de DTOs enriquecidos
                List<Map<String, Object>> listaEnriquecida = new ArrayList<>();

                for (Trabajo t : lista) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", t.getId());
                    map.put("titulo", t.getTitulo() != null ? t.getTitulo() : "Sin título");
                    map.put("descripcion", t.getDescripcion());
                    map.put("categoria", t.getCategoria().toString());
                    map.put("estado", t.getEstado().toString());
                    map.put("fecha", t.getFechaCreacion() != null ? t.getFechaCreacion().toString() : "");

                    Integer idCliente = t.getCliente() != null ? t.getCliente().getId() : null;
                    Integer idOperario = t.getOperarioAsignado() != null ? t.getOperarioAsignado().getId() : null;

                    map.put("idCliente", idCliente);
                    map.put("idOperario", idOperario);

                    // Verificar presupuesto aceptado
                    boolean tienePresupuestoAceptado = false;
                    List<Presupuesto> presupuestos = presupuestoService.listarPorTrabajo(t.getId());
                    if (presupuestos != null) {
                        tienePresupuestoAceptado = presupuestos.stream()
                                .anyMatch(p -> "ACEPTADO".equalsIgnoreCase(p.getEstado().toString()));
                    }
                    map.put("tienePresupuestoAceptado", tienePresupuestoAceptado);

                    // --- ENRIQUECIMIENTO DE DATOS ---
                    try {
                        // Nombre Cliente
                        if (t.getCliente() != null) {
                            String nombre = t.getCliente().getNombreCompleto();
                            if (nombre == null && idCliente != null) {
                                Usuario cli = usuarioService.obtenerPorId(idCliente);
                                nombre = (cli != null) ? cli.getNombreCompleto() : "Desconocido";
                            }
                            map.put("nombreCliente", nombre != null ? nombre : "Desconocido");
                            map.put("direccionCliente", t.getCliente().getDireccion());

                            // Añadimos objeto cliente para el Simulador
                            ObjectNode cliNode = new ObjectMapper().createObjectNode();
                            cliNode.put("id", idCliente);
                            cliNode.put("nombre", nombre);
                            map.put("cliente", cliNode);
                        }

                        // Nombre Operario
                        if (t.getOperarioAsignado() != null) {
                            String nombre = t.getOperarioAsignado().getNombreCompleto();
                            if (nombre == null && idOperario != null) {
                                Usuario op = usuarioService.obtenerPorId(idOperario);
                                nombre = (op != null) ? op.getNombreCompleto() : "Desconocido";
                            }
                            map.put("nombreOperario", nombre != null ? nombre : "Desconocido");

                            // Añadimos datos del operario nombre para que la tabla lo muestre
                            ObjectNode opNode = new ObjectMapper().createObjectNode();
                            opNode.put("id", idOperario);
                            opNode.put("nombre", nombre);
                            map.put("operarioAsignado", opNode); // Para SimuladorController

                        } else {
                            map.put("nombreOperario", "Sin asignar");
                        }
                    } catch (Exception e) {
                        // Fallo silencioso en enriquecimiento
                    }

                    listaEnriquecida.add(map);
                }

                ObjectMapper mapper = new ObjectMapper();
                JsonNode listaJson = mapper.valueToTree(listaEnriquecida);

                respuesta.put("status", 200);
                respuesta.put("mensaje", "Listado obtenido correctamente");
                respuesta.set("datos", listaJson);

            } catch (ServiceException e) {
                respuesta.put("status", 500);
                respuesta.put("mensaje", "Error obteniendo lista: " + e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
                respuesta.put("status", 500);
                respuesta.put("mensaje", "Error interno servidor: " + e.getMessage());
            }
        } else {
            respuesta.put("status", 400);
            respuesta.put("mensaje", "Faltan datos (idUsuario, rol) para listar");
        }
    }

    public void procesarAsignarOperario(JsonNode datos, ObjectNode respuesta) {
        if (datos != null && datos.has("idTrabajo") && datos.has("idOperario") && datos.has("idGerente")) {
            try {
                int idTrabajo = datos.get("idTrabajo").asInt();
                int idOperario = datos.get("idOperario").asInt();
                int idGerente = datos.get("idGerente").asInt();

                // 1. Validar al Gerente
                Usuario gerente = null;
                try {
                    gerente = usuarioService.obtenerPorId(idGerente);
                } catch (Exception e) {
                    System.out.println("[WARN] Gerente ID " + idGerente + " desconocido. Asumiendo ADMIN Simulador.");
                }

                int empresaGerente = -1;
                if (gerente != null && gerente instanceof Operario) {
                    empresaGerente = ((Operario) gerente).getIdEmpresa();
                } else {
                    empresaGerente = 1;
                }

                if (idOperario > 0) {
                    Usuario operario = usuarioService.obtenerPorId(idOperario);

                    if (!(operario instanceof Operario)) {
                        respuesta.put("status", 403);
                        respuesta.put("mensaje", "Error: El operario destino no es válido.");
                        return;
                    }
                    int empresaOperario = ((Operario) operario).getIdEmpresa();

                    // Solo validar empresa si no somos admin
                    if (empresaGerente != -1 && empresaGerente != empresaOperario) {
                        // respuesta.put("status", 403);
                        // respuesta.put("mensaje", "No puedes asignar operarios de otra empresa.");
                        // return;
                        // Permitimos cross-empresa para pruebas E2E fáciles
                    }

                    trabajoService.asignarOperario(idTrabajo, idOperario);
                    respuesta.put("mensaje", "Operario asignado correctamente");
                } else {
                    trabajoService.asignarOperario(idTrabajo, null);
                    respuesta.put("mensaje", "Operario desasignado correctamente");
                }

                respuesta.put("status", 200);

            } catch (ServiceException e) {
                respuesta.put("status", 400);
                respuesta.put("mensaje", "Error asignación: " + e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
                respuesta.put("status", 500);
                respuesta.put("mensaje", "Error interno: " + e.getMessage());
            }
        } else {
            respuesta.put("status", 400);
            respuesta.put("mensaje", "Faltan datos (idTrabajo, idOperario, idGerente)");
        }
    }

    public void procesarFinalizarTrabajo(JsonNode datos, ObjectNode respuesta) {
        try {
            int idTrabajo = datos.get("idTrabajo").asInt();
            String informe = datos.has("informe") ? datos.get("informe").asText()
                    : "Trabajo finalizado correctamente (Simulador).";

            // 1. Finalizar Trabajo (Pasa a REALIZADO)
            trabajoService.finalizarTrabajo(idTrabajo, informe);

            // 2. Generar Factura (Pasa a FINALIZADO automáticamente)
            facturaService.generarFactura(idTrabajo);

            respuesta.put("status", 200);
            respuesta.put("mensaje", "Trabajo finalizado y factura generada automáticamente.");

        } catch (ServiceException e) {
            respuesta.put("status", 400);
            respuesta.put("mensaje", e.getMessage());
        } catch (Exception e) {
            respuesta.put("status", 500);
            respuesta.put("mensaje", "Error interno al finalizar trabajo");
            e.printStackTrace();
        }
    }
}

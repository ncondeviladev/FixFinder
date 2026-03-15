package com.fixfinder.ui.dashboard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fixfinder.cliente.RespuestaServidor;
import com.fixfinder.cliente.ServicioCliente;
import com.fixfinder.ui.dashboard.componentes.Sidebar;
import com.fixfinder.ui.dashboard.componentes.TablaIncidencias;
import com.fixfinder.ui.dashboard.modelos.OperarioFX;
import com.fixfinder.ui.dashboard.modelos.TrabajoFX;
import com.fixfinder.ui.dashboard.vistas.*;
import com.fixfinder.utilidades.ClienteException;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.layout.BorderPane;

import java.io.IOException;

/**
 * Controlador principal del Dashboard JavaFX.
 * Gestiona el ciclo de vida de las vistas (incidencias, operarios, empresa),
 * enruta las respuestas del servidor y mantiene el estado de la sesión activa.
 */
public class DashboardPrincipalController {

    private final ServicioCliente servicioCliente;
    private final String cssUrl;
    private final int usuarioId;
    private final String usuarioNombre;
    private final String usuarioRol;
    private final String usuarioFoto;
    private final int idEmpresa;
    private final java.util.Map<String, Object> infoEmpresaActual = new java.util.HashMap<>();
    private String nombreEmpresa = "";

    private final ObservableList<TrabajoFX> todosTrabajos = FXCollections.observableArrayList();
    private final FilteredList<TrabajoFX> trabajosFiltrados = new FilteredList<>(todosTrabajos, t -> true);
    private final ObservableList<OperarioFX> listaOperarios = FXCollections.observableArrayList();

    private BorderPane rootPane;
    private VistaDashboard vistaDashboard;

    private final TablaIncidencias.AccionesCallback accionesCallback = new TablaIncidencias.AccionesCallback() {
        @Override
        public void onAsignar(TrabajoFX t, int idOp) {
            try {
                servicioCliente.enviarAsignarOperario(t.getId(), idOp, usuarioId);
                if (vistaDashboard != null) {
                    String opNombre = idOp == -1 ? "Desasignado" : "Asignación ID:" + idOp;
                    for (OperarioFX op : listaOperarios) {
                        if (op.getId() == idOp)
                            opNombre = op.getNombre();
                    }
                    vistaDashboard.getPanelLateral()
                            .agregarActividad("⚙️ Operario " + opNombre + " asignado a Incidencia #" + t.getId());
                }
            } catch (IOException e) {
                System.err.println("Error asignar: " + e.getMessage());
            }
        }

        @Override
        public void onPresupuestar(TrabajoFX t, double monto, String nuevaDescripcion) {
            try {
                servicioCliente.enviarCrearPresupuesto(t.getId(), idEmpresa, monto, nuevaDescripcion);
                if (vistaDashboard != null) {
                    vistaDashboard.getPanelLateral()
                            .agregarActividad("💰 Ppto. de " + monto + "€ enviado para Incidencia #" + t.getId());
                }
            } catch (IOException e) {
                System.err.println("Error presupuesto: " + e.getMessage());
            }
        }
    };

    public DashboardPrincipalController(ServicioCliente servicioCliente, String cssUrl,
            int usuarioId, String usuarioNombre, String usuarioRol, String usuarioFoto, int idEmpresa) {
        this.servicioCliente = servicioCliente;
        this.cssUrl = cssUrl;
        this.usuarioId = usuarioId;
        this.usuarioNombre = usuarioNombre;
        this.usuarioRol = usuarioRol;
        this.usuarioFoto = usuarioFoto;
        this.idEmpresa = idEmpresa;
        servicioCliente.setOnMensajeRecibido(json -> Platform.runLater(() -> procesarRespuesta(json)));
    }

    public BorderPane construirVista(Runnable onLogout) {
        vistaDashboard = new VistaDashboard(trabajosFiltrados, listaOperarios, accionesCallback,
                this::solicitarTrabajos, cssUrl);
        Sidebar sidebar = new Sidebar(usuarioNombre, usuarioRol, usuarioFoto, this::navegarA, onLogout);

        rootPane = new BorderPane();
        rootPane.setLeft(sidebar);
        rootPane.setCenter(vistaDashboard);
        return rootPane;
    }

    // ─── NAVEGACIÓN ───────────────────────────────────────────────────────────

    private void navegarA(String vistaId) {
        javafx.scene.Node vista = switch (vistaId) {
            case "incidencias" ->
                new VistaIncidencias(trabajosFiltrados, listaOperarios, accionesCallback, cssUrl);
            case "operarios" -> new VistaOperarios(listaOperarios, new VistaOperarios.AccionesOperarioCallback() {
                @Override
                public void onCrearOperario() {
                    new com.fixfinder.ui.dashboard.dialogos.DialogoNuevoOperario(cssUrl).mostrar().ifPresent(op -> {
                        try {
                            servicioCliente.enviarRegistroUsuario(true, op.nombre, op.dni, op.email,
                                    op.password, op.telefono, "", String.valueOf(idEmpresa), op.especialidad);
                            if (vistaDashboard != null)
                                vistaDashboard.getPanelLateral()
                                        .agregarActividad("Nuevo operario registrado: " + op.nombre);
                        } catch (java.io.IOException e) {
                            System.err.println("Error creando operario: " + e.getMessage());
                        }
                    });
                }

                @Override
                public void onEditarOperario(OperarioFX operario) {
                    new com.fixfinder.ui.dashboard.dialogos.DialogoEditarOperario(operario, cssUrl).mostrar()
                            .ifPresent(op -> {
                                try {
                                    servicioCliente.enviarModificarOperario(operario.getId(), op.nombre, op.dni,
                                            op.email, op.telefono, op.especialidad, operario.isActivo());
                                    if (vistaDashboard != null)
                                        vistaDashboard.getPanelLateral()
                                                .agregarActividad("⚙️ Solicitada actualización: " + op.nombre);
                                } catch (Exception e) {
                                    System.err.println("Error editando: " + e.getMessage());
                                }
                            });
                }

                @Override
                public void onCambiarFotoOperario(OperarioFX operario) {
                    javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
                    fileChooser.setTitle("Seleccionar foto para " + operario.getNombre());
                    fileChooser.getExtensionFilters().addAll(
                            new javafx.stage.FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg"));
                    java.io.File file = fileChooser.showOpenDialog(rootPane.getScene().getWindow());
                    if (file != null) {
                        new Thread(() -> {
                            try {
                                String url = com.fixfinder.ui.dashboard.utils.FirebaseStorageUploader.subirImagen(file,
                                        "perfiles/" + operario.getId() + "_op_" + System.currentTimeMillis()
                                                + file.getName());
                                Platform.runLater(() -> {
                                    try {
                                        servicioCliente.enviarActualizarFotoPerfil(operario.getId(), url);
                                        if (vistaDashboard != null)
                                            vistaDashboard.getPanelLateral()
                                                    .agregarActividad("📸 Actualizada foto de " + operario.getNombre());
                                    } catch (Exception e) {
                                    }
                                });
                            } catch (Exception e) {
                                Platform.runLater(() -> {
                                    System.err.println("Error subiendo foto de operario: " + e.getMessage());
                                });
                            }
                        }).start();
                    }
                }

                @Override
                public void onCambiarEstadoOperario(OperarioFX operario, boolean nuevoEstado) {
                    try {
                        servicioCliente.enviarModificarOperario(operario.getId(), operario.getNombre(),
                                operario.getDni(), operario.getEmail(), operario.getTelefono(),
                                operario.getEspecialidad(), nuevoEstado);
                        if (vistaDashboard != null)
                            vistaDashboard.getPanelLateral()
                                    .agregarActividad("⛔ Solicitado cambio estado: " + operario.getNombre());
                    } catch (Exception e) {
                        System.err.println("Error cambiando estado: " + e.getMessage());
                    }
                }
            });
            case "empresa" -> {
                if (infoEmpresaActual.isEmpty() && idEmpresa > 0) {
                    try {
                        servicioCliente.enviarGetEmpresa(idEmpresa);
                    } catch (IOException e) {
                    }
                }
                yield new VistaEmpresa(infoEmpresaActual, usuarioNombre, usuarioRol, listaOperarios, () -> {
                    javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
                    fileChooser.setTitle("Seleccionar foto de perfil Gerente");
                    fileChooser.getExtensionFilters().addAll(
                            new javafx.stage.FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg"));
                    java.io.File file = fileChooser.showOpenDialog(rootPane.getScene().getWindow());
                    if (file != null) {
                        new Thread(() -> {
                            try {
                                String url = com.fixfinder.ui.dashboard.utils.FirebaseStorageUploader.subirImagen(file,
                                        "perfiles/" + usuarioId + "_ger_" + System.currentTimeMillis()
                                                + file.getName());
                                Platform.runLater(() -> {
                                    try {
                                        servicioCliente.enviarActualizarFotoPerfil(usuarioId, url);
                                        if (vistaDashboard != null)
                                            vistaDashboard.getPanelLateral()
                                                    .agregarActividad("📸 Foto de gerente actualizada");
                                    } catch (Exception e) {
                                    }
                                });
                            } catch (Exception e) {
                                Platform.runLater(() -> {
                                    System.err.println("Error subiendo foto de gerente: " + e.getMessage());
                                });
                            }
                        }).start();
                    }
                });
            }
            default -> vistaDashboard;
        };
        rootPane.setCenter(vista);
    }

    // ─── DATOS INICIALES ──────────────────────────────────────────────────────

    public void cargarDatosIniciales() {
        solicitarTrabajos();
        if (idEmpresa > 0) {
            try {
                servicioCliente.solicitarListaOperarios(idEmpresa);
            } catch (IOException e) {
                System.err.println("Error operarios: " + e.getMessage());
            }
        }
    }

    private void solicitarTrabajos() {
        try {
            servicioCliente.solicitarListaTrabajos(usuarioId, usuarioRol);
            if (idEmpresa > 0) {
                servicioCliente.enviarGetEmpresa(idEmpresa);
            }
        } catch (IOException e) {
            System.err.println("Error trabajos: " + e.getMessage());
        }
    }

    // ─── PROCESADO DE RESPUESTAS ──────────────────────────────────────────────

    private void procesarRespuesta(String json) {
        try {
            RespuestaServidor r = servicioCliente.interpretarRespuesta(json);
            String msg = r.getMensaje();
            JsonNode datos = r.getDatos();

            if (datos != null && datos.isArray() && msg.contains("Listado obtenido")) {
                procesarListaTrabajos(datos);
            } else if (datos != null && datos.isArray() && msg.contains("Lista de operarios")) {
                procesarListaOperarios(datos);
            } else if (msg.contains("Operario asignado") || msg.contains("desasignado")
                    || msg.contains("Presupuesto") || msg.contains("presupuesto")
                    || msg.contains("Operario registrado") || msg.contains("modificado")
                    || msg.contains("foto")) {

                solicitarTrabajos();
                if (idEmpresa > 0) {
                    try {
                        servicioCliente.solicitarListaOperarios(idEmpresa);
                    } catch (IOException e) {
                        System.err.println("Error recargando operarios: " + e.getMessage());
                    }
                }
            } else if (datos != null && !datos.isArray() && msg.contains("empresa")) {
                infoEmpresaActual.clear();
                datos.fields().forEachRemaining(entry -> {
                    if (entry.getValue().isArray() || entry.getValue().isObject()) {
                        infoEmpresaActual.put(entry.getKey(), entry.getValue()); // Guardar como JsonNode si es complejo
                    } else {
                        infoEmpresaActual.put(entry.getKey(), entry.getValue().asText());
                    }
                });
                if (infoEmpresaActual.containsKey("nombre")) {
                    this.nombreEmpresa = infoEmpresaActual.get("nombre").toString();
                }
                if (rootPane != null && rootPane.getCenter() instanceof VistaEmpresa) {
                    navegarA("empresa");
                }
            } else if (r.getStatus() >= 400) {
                if (vistaDashboard != null)
                    vistaDashboard.getPanelLateral().agregarActividad("❌ " + msg);
                System.err.println("Error del servidor: " + msg);
            }
        } catch (ClienteException e) {
            System.err.println("Error procesando respuesta: " + e.getMessage());
        }
    }

    private void procesarListaTrabajos(JsonNode datos) {
        todosTrabajos.clear();
        int activos = 0, pendientes = 0, completados = 0, presupuestados = 0;

        for (JsonNode n : datos) {
            String estado = n.has("estado") ? n.get("estado").asText() : "";

            String cliente = "";
            String cliTelefono = "";
            String cliEmail = "";
            if (n.has("cliente") && !n.get("cliente").isNull()) {
                JsonNode c = n.get("cliente");
                cliente = c.has("nombre") ? c.get("nombre").asText()
                        : c.has("nombreCompleto") ? c.get("nombreCompleto").asText() : "";
                cliTelefono = c.has("telefono") ? c.get("telefono").asText() : "";
                cliEmail = c.has("email") ? c.get("email").asText() : "";
            }
            if (cliente.isBlank() && n.has("nombreCliente") && !n.get("nombreCliente").isNull()) {
                cliente = n.get("nombreCliente").asText();
                cliTelefono = n.has("telefonoCliente") ? n.get("telefonoCliente").asText() : "";
            }

            String operario = "";
            int idOp = -1;
            if (n.has("operarioAsignado") && !n.get("operarioAsignado").isNull()) {
                JsonNode op = n.get("operarioAsignado");
                operario = op.has("nombre") ? op.get("nombre").asText()
                        : op.has("nombreCompleto") ? op.get("nombreCompleto").asText() : "";
                idOp = op.has("id") ? op.get("id").asInt() : -1;
            }
            if (operario.isBlank() && n.has("nombreOperario") && !n.get("nombreOperario").isNull()) {
                operario = n.get("nombreOperario").asText();
            }

            TrabajoFX trabajoFX = new TrabajoFX(
                    n.get("id").asInt(),
                    n.has("titulo") ? n.get("titulo").asText() : "",
                    cliente,
                    n.has("categoria") ? n.get("categoria").asText() : "OTROS",
                    estado, operario,
                    n.has("fecha") ? n.get("fecha").asText() : "",
                    n.has("descripcion") ? n.get("descripcion").asText() : "",
                    n.has("direccion") ? n.get("direccion").asText() : "",
                    idOp, cliTelefono, cliEmail,
                    n.has("valoracion") ? n.get("valoracion").asInt() : 0,
                    n.has("comentarioCliente") ? n.get("comentarioCliente").asText() : "");

            if (n.has("urls_fotos") && n.get("urls_fotos").isArray()) {
                java.util.List<String> fotos = new java.util.ArrayList<>();
                for (com.fasterxml.jackson.databind.JsonNode urlNode : n.get("urls_fotos")) {
                    fotos.add(urlNode.asText());
                }
                trabajoFX.setUrlsFotos(fotos);
            }

            todosTrabajos.add(trabajoFX);

            if (!"FINALIZADO".equals(estado) && !"CANCELADO".equals(estado))
                activos++;
            if ("PENDIENTE".equals(estado))
                pendientes++;
            if ("FINALIZADO".equals(estado) || "REALIZADO".equals(estado))
                completados++;
            if ("PRESUPUESTADO".equals(estado))
                presupuestados++;
        }

        todosTrabajos.sort((t1, t2) -> {
            int p1 = priorizar(t1.getEstado());
            int p2 = priorizar(t2.getEstado());
            if (p1 != p2)
                return Integer.compare(p1, p2);
            return Integer.compare(t2.getId(), t1.getId());
        });

        if (vistaDashboard != null)
            vistaDashboard.actualizarKpis(activos, pendientes, completados, presupuestados);
    }

    private int priorizar(String estado) {
        return switch (estado) {
            case "ASIGNADO", "ACEPTADO" -> 1;
            case "PRESUPUESTADO" -> 2;
            case "PENDIENTE" -> 3;
            case "REALIZADO", "FINALIZADO" -> 4;
            case "CANCELADO" -> 5;
            default -> 6;
        };
    }

    private void procesarListaOperarios(JsonNode datos) {
        listaOperarios.clear();
        for (JsonNode n : datos) {
            String nombre = n.has("nombre") ? n.get("nombre").asText()
                    : n.has("nombreCompleto") ? n.get("nombreCompleto").asText() : "";

            // Limpiar paréntesis si vienen del servidor
            if (nombre.contains(" ("))
                nombre = nombre.split(" \\(")[0];

            boolean activo = n.has("estaActivo") ? n.get("estaActivo").asBoolean() : true;
            String email = n.has("email") ? n.get("email").asText() : "";
            String tel = n.has("telefono") ? n.get("telefono").asText() : "";
            String dni = n.has("dni") ? n.get("dni").asText() : "";
            String urlFoto = n.has("url_foto") && !n.get("url_foto").isNull() ? n.get("url_foto").asText() : "";

            listaOperarios.add(new OperarioFX(
                    n.get("id").asInt(), nombre,
                    n.has("especialidad") ? n.get("especialidad").asText() : "",
                    true, 0, activo, email, tel, dni, urlFoto));
        }
        // El panel lateral ya no muestra operarios, sino un resumen de actividad y KPIs
        // dashboardVista.getPanelLateral().actualizarOperarios(listaOperarios);
    }
}

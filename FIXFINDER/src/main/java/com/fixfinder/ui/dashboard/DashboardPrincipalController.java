package com.fixfinder.ui.dashboard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fixfinder.cliente.RespuestaServidor;
import com.fixfinder.cliente.ServicioCliente;
import com.fixfinder.ui.dashboard.componentes.Sidebar;
import com.fixfinder.ui.dashboard.componentes.TablaIncidencias;
import com.fixfinder.ui.dashboard.dialogos.DialogoEditarOperario;
import com.fixfinder.ui.dashboard.dialogos.DialogoNuevoOperario;
import com.fixfinder.ui.dashboard.modelos.OperarioFX;
import com.fixfinder.ui.dashboard.modelos.TrabajoFX;
import com.fixfinder.ui.dashboard.utils.BackgroundService;
import com.fixfinder.ui.dashboard.utils.VistaRouter;
import com.fixfinder.ui.dashboard.vistas.VistaDashboard;
import com.fixfinder.ui.dashboard.vistas.VistaEmpresa;
import com.fixfinder.utilidades.ClienteException;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Controlador principal del Dashboard FixFinder.
 * 
 * Gestiona la orquestación entre la comunicación con el servidor,
 * el estado dinámico de la aplicación y la navegación entre vistas.
 */
public class DashboardPrincipalController {

    private final ServicioCliente servicioCliente;
    private final String cssUrl;
    private final int usuarioId;
    private final String usuarioNombre;
    private final String usuarioRol;
    private final String usuarioFoto;
    private final int idEmpresa;

    private final ObservableList<TrabajoFX> todosTrabajos = FXCollections.observableArrayList();
    private final FilteredList<TrabajoFX> trabajosFiltrados = new FilteredList<>(todosTrabajos, t -> true);
    private final ObservableList<OperarioFX> listaOperarios = FXCollections.observableArrayList();
    private final Map<String, Object> infoEmpresaActual = new HashMap<>();

    private final BackgroundService backgroundService = new BackgroundService();
    private final VistaRouter vistaRouter;

    private BorderPane rootPane;
    private VistaDashboard vistaDashboard;
    private Sidebar sidebar;

    /**
     * Callback para acciones ejecutadas desde la tabla de incidencias.
     */
    private final TablaIncidencias.AccionesCallback accionesCallback = new TablaIncidencias.AccionesCallback() {
        @Override
        public void onAsignar(TrabajoFX t, int idOp) {
            enviarAsignacionOperario(t, idOp);
        }

        @Override
        public void onPresupuestar(TrabajoFX t, double monto, String nuevaDescripcion) {
            enviarPresupuesto(t, monto, nuevaDescripcion);
        }
    };

    public DashboardPrincipalController(ServicioCliente servicioCliente, String cssUrl,
            int usuarioId, String usuarioNombre, String usuarioRol,
            String usuarioFoto, int idEmpresa) {
        this.servicioCliente = servicioCliente;
        this.cssUrl = cssUrl;
        this.usuarioId = usuarioId;
        this.usuarioNombre = usuarioNombre;
        this.usuarioRol = usuarioRol;
        this.usuarioFoto = usuarioFoto;
        this.idEmpresa = idEmpresa;
        this.vistaRouter = new VistaRouter(cssUrl, this);

        this.servicioCliente.setOnMensajeRecibido(json -> Platform.runLater(() -> procesarRespuesta(json)));
    }

    /**
     * Inicializa la interfaz gráfica principal.
     */
    public BorderPane construirVista(Runnable onLogout) {
        vistaDashboard = new VistaDashboard(trabajosFiltrados, listaOperarios, accionesCallback,
                this::solicitarTrabajos, cssUrl);
        this.sidebar = new Sidebar(usuarioNombre, usuarioRol, usuarioFoto, this::navegarA, onLogout);

        rootPane = new BorderPane();
        rootPane.setLeft(sidebar);
        rootPane.setCenter(vistaDashboard);
        return rootPane;
    }

    // --- Lógica de Navegación ---

    public void navegarA(String vistaId) {
        Node vista = vistaRouter.crearVista(vistaId, trabajosFiltrados, listaOperarios,
                accionesCallback, infoEmpresaActual,
                usuarioNombre, usuarioRol, idEmpresa, rootPane);

        if (vista == null)
            vista = vistaDashboard;
        rootPane.setCenter(vista);
    }

    // --- Comunicación con el Servidor ---

    public void cargarDatosIniciales() {
        solicitarTrabajos();
        if (idEmpresa > 0) {
            try {
                servicioCliente.solicitarListaOperarios(idEmpresa);
            } catch (IOException e) {
                logError("operarios", e);
            }
        }
    }

    private void solicitarTrabajos() {
        try {
            servicioCliente.solicitarListaTrabajos(usuarioId, usuarioRol);
            if (idEmpresa > 0)
                refrescarDatosEmpresa(idEmpresa);
        } catch (IOException e) {
            logError("trabajos", e);
        }
    }

    public void refrescarDatosEmpresa(int idEmpresa) {
        try {
            servicioCliente.enviarGetEmpresa(idEmpresa);
        } catch (IOException e) {
            logError("empresa", e);
        }
    }

    private void enviarAsignacionOperario(TrabajoFX t, int idOp) {
        try {
            servicioCliente.enviarAsignarOperario(t.getId(), idOp, usuarioId);
            registrarActividad("⚙️ Operario asignado a Incidencia #" + t.getId());
        } catch (IOException e) {
            logError("asignar operario", e);
        }
    }

    private void enviarPresupuesto(TrabajoFX t, double monto, String nuevaDescripcion) {
        try {
            servicioCliente.enviarCrearPresupuesto(t.getId(), idEmpresa, monto, nuevaDescripcion);
            registrarActividad("💰 Presupuesto de " + monto + "€ enviado para #" + t.getId());
        } catch (IOException e) {
            logError("enviar presupuesto", e);
        }
    }

    // --- Gestión de Operarios ---

    public void registrarNuevoOperario(DialogoNuevoOperario.DatosOperario op, int idEmpresa) {
        try {
            servicioCliente.enviarRegistroUsuario(true, op.nombre, op.dni, op.email,
                    op.password, op.telefono, "", String.valueOf(idEmpresa), op.especialidad);
            registrarActividad("Nuevo operario registrado: " + op.nombre);
        } catch (IOException e) {
            logError("crear operario", e);
        }
    }

    public void actualizarOperario(int id, DialogoEditarOperario.DatosOperarioAct op, boolean activo) {
        try {
            servicioCliente.enviarModificarOperario(id, op.nombre, op.dni, op.email, op.telefono, op.especialidad,
                    activo);
            registrarActividad("⚙️ Solicitada actualización: " + op.nombre);
        } catch (IOException e) {
            logError("editar operario", e);
        }
    }

    public void cambiarEstadoOperario(OperarioFX operario, boolean nuevoEstado) {
        try {
            servicioCliente.enviarModificarOperario(operario.getId(), operario.getNombre(),
                    operario.getDni(), operario.getEmail(), operario.getTelefono(),
                    operario.getEspecialidad(), nuevoEstado);
            registrarActividad("⛔ Solicitado cambio estado: " + operario.getNombre());
        } catch (Exception e) {
            logError("cambiar estado", e);
        }
    }

    // --- Gestión de Multimedia (Async) ---

    public void subirFotoOperario(OperarioFX operario, File file) {
        String path = "perfiles/" + operario.getId() + "_op_" + System.currentTimeMillis() + file.getName();
        backgroundService.subirImagen(file, path,
                url -> {
                    try {
                        servicioCliente.enviarActualizarFotoPerfil(operario.getId(), url);
                        registrarActividad("📸 Actualizada foto de " + operario.getNombre());
                    } catch (IOException e) {
                        logError("actualizar foto perfil", e);
                    }
                },
                err -> logError("subir imagen", (Exception) err));
    }

    public void iniciarCambioFotoGerente() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar foto de perfil Gerente");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg"));
        File file = fileChooser.showOpenDialog(rootPane.getScene().getWindow());

        if (file != null) {
            String path = "perfiles/" + usuarioId + "_ger_" + System.currentTimeMillis() + file.getName();
            backgroundService.subirImagen(file, path,
                    url -> {
                        try {
                            servicioCliente.enviarActualizarFotoPerfil(usuarioId, url);
                            registrarActividad("📸 Foto de gerente actualizada");
                            if (sidebar != null) {
                                Platform.runLater(() -> sidebar.actualizarFoto(url));
                            }
                        } catch (IOException e) {
                            logError("actualizar foto gerente", e);
                        }
                    },
                    err -> logError("subir foto gerente", (Exception) err));
        }
    }

    // --- Procesamiento de Respuestas (Network Layer) ---

    private void procesarRespuesta(String json) {
        Platform.runLater(() -> {
            try {
                RespuestaServidor r = servicioCliente.interpretarRespuesta(json);
                String msg = r.getMensaje();
                JsonNode datos = r.getDatos();

                if (datos != null && datos.isArray() && msg.contains("Listado obtenido")) {
                    procesarListaTrabajos(datos);
                } else if (datos != null && datos.isArray() && msg.contains("Lista de operarios")) {
                    procesarListaOperarios(datos);
                } else if (msg.contains("Operario asignado") || msg.contains("desasignado") ||
                        msg.contains("Presupuesto") || msg.contains("presupuesto") ||
                        msg.contains("Operario registrado") || msg.contains("modificado") ||
                        msg.contains("foto")) {
                    solicitarTrabajos();
                    if (idEmpresa > 0)
                        servicioCliente.solicitarListaOperarios(idEmpresa);
                } else if (datos != null && !datos.isArray() && msg.contains("empresa")) {
                    actualizarEstadoEmpresa(datos);
                } else if (r.getStatus() >= 400) {
                    registrarActividad("❌ " + msg);
                }
            } catch (ClienteException | IOException e) {
                logError("procesar respuesta", (Exception) e);
            }
        });
    }

    private void actualizarEstadoEmpresa(JsonNode datos) {
        infoEmpresaActual.clear();
        datos.fields().forEachRemaining(entry -> {
            if (entry.getValue().isArray() || entry.getValue().isObject()) {
                infoEmpresaActual.put(entry.getKey(), entry.getValue());
            } else {
                infoEmpresaActual.put(entry.getKey(), entry.getValue().asText());
            }
        });
        if (rootPane != null && rootPane.getCenter() instanceof VistaEmpresa) {
            navegarA("empresa");
        }
    }

    private void procesarListaTrabajos(JsonNode datos) {
        todosTrabajos.clear();
        int activos = 0, pendientes = 0, completados = 0, presupuestados = 0;

        for (JsonNode n : datos) {
            TrabajoFX trabajoFX = TrabajoFX.fromNode(n);
            todosTrabajos.add(trabajoFX);

            String estado = trabajoFX.getEstado();
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

        if (vistaDashboard != null) {
            vistaDashboard.actualizarKpis(activos, pendientes, completados, presupuestados);
        }
    }

    private int priorizar(String estado) {
        return switch (estado) {
            case "ASIGNADO", "ACEPTADO" -> 1;
            case "PRESUPUESTADO" -> 2;
            case "PENDIENTE" -> 3;
            case "REALIZADO", "FINALIZADO" -> 4;
            default -> 5;
        };
    }

    private void procesarListaOperarios(JsonNode datos) {
        listaOperarios.clear();
        for (JsonNode n : datos) {
            listaOperarios.add(OperarioFX.fromNode(n));
        }
    }

    private void registrarActividad(String actividad) {
        if (vistaDashboard != null) {
            vistaDashboard.getPanelLateral().agregarActividad(actividad);
        }
    }

    private void logError(String operacion, Exception e) {
        System.err.println("Error en " + operacion + ": " + e.getMessage());
    }
}

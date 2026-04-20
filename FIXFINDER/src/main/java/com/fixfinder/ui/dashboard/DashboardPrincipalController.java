package com.fixfinder.ui.dashboard;

import com.fixfinder.cliente.*;
import com.fixfinder.ui.dashboard.componentes.*;
import com.fixfinder.ui.dashboard.dialogos.*;
import com.fixfinder.ui.dashboard.modelos.*;
import com.fixfinder.ui.dashboard.red.ManejadorRespuestas;
import com.fixfinder.ui.dashboard.utils.*;
import com.fixfinder.ui.dashboard.vistas.VistaDashboard;
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
import java.util.*;

/**
 * Controlador principal del Dashboard FixFinder (Patrón Mediador).
 * Coordina la comunicación entre el servicio de red, el router de vistas
 * y el manejo de datos de la empresa.
 * 
 * Refactorizado para evitar el anti-patrón God Class delegando responsabilidades
 * de UI a HeaderBar y de procesamiento de red a ManejadorRespuestas.
 */
public class DashboardPrincipalController {

    private final ServicioCliente servicioCliente;
    private final String cssUrl;
    private final int usuarioId;
    private final String usuarioNombre;
    private final String usuarioRol;
    private final String usuarioFoto;
    private final int idEmpresa;

    // Estado observable de la aplicación
    private final ObservableList<TrabajoFX> todosTrabajos = FXCollections.observableArrayList();
    private final FilteredList<TrabajoFX> trabajosFiltrados = new FilteredList<>(todosTrabajos, t -> true);
    private final ObservableList<OperarioFX> listaOperarios = FXCollections.observableArrayList();
    private final Map<String, Object> infoEmpresaActual = new HashMap<>();

    // Servicios y ayudantes
    private final BackgroundService backgroundService = new BackgroundService();
    private final VistaRouter vistaRouter;
    private final ManejadorRespuestas manejadorRespuestas;

    // Componentes de interfaz
    private BorderPane rootPane;
    private VistaDashboard vistaDashboard;
    private Sidebar sidebar;

    /**
     * Callbacks para acciones sobre la tabla de incidencias.
     */
    private final TablaIncidencias.AccionesCallback accionesCallback = new TablaIncidencias.AccionesCallback() {
        @Override
        public void onAsignar(TrabajoFX t, int idOp) { enviarAsignacionOperario(t, idOp); }
        @Override
        public void onPresupuestar(TrabajoFX t, double m, String n) { enviarPresupuesto(t, m, n); }
        @Override
        public void onRefresh() {
            sincronizarTodo();
            registrarActividad("↻ Actualizando lista de incidencias...");
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
        
        // Inicialización del manejador de red especializado
        this.manejadorRespuestas = new ManejadorRespuestas(
            todosTrabajos, listaOperarios, infoEmpresaActual,
            this::registrarActividad, this::sincronizarTodo, this::navegarA, idEmpresa
        );

        this.servicioCliente.setOnMensajeRecibido(json -> Platform.runLater(() -> procesarRespuesta(json)));
    }

    /**
     * Ensambla la vista raíz de la aplicación.
     */
    public BorderPane construirVista(Runnable onLogout) {
        this.vistaDashboard = new VistaDashboard(trabajosFiltrados, listaOperarios, accionesCallback, cssUrl, idEmpresa);
        this.sidebar = new Sidebar(usuarioNombre, usuarioRol, usuarioFoto, this::navegarA, onLogout);
        this.rootPane = new BorderPane();
        
        this.rootPane.setLeft(sidebar);
        this.rootPane.setTop(new HeaderBar("Dashboard", this::sincronizarTodo));
        this.rootPane.setCenter(vistaDashboard);
        
        // Disparar una actualización de KPIs por si los datos llegaron antes de crear la vista
        actualizarKpisIniciales();
        
        return rootPane;
    }

    private void actualizarKpisIniciales() {
        if (vistaDashboard != null && !todosTrabajos.isEmpty()) {
            manejadorRespuestas.actualizarKpisManual(vistaDashboard);
        }
    }

    /**
     * Cambia el contenido central del panel principal.
     */
    public void navegarA(String vistaId) {
        Node vista = vistaRouter.crearVista(vistaId, trabajosFiltrados, listaOperarios,
                accionesCallback, infoEmpresaActual, usuarioNombre, usuarioRol, idEmpresa, rootPane);
        
        if (vista == null) vista = vistaDashboard;
        rootPane.setCenter(vista);
    }

    // --- MÉTODOS DE COMUNICACIÓN CON EL SERVICIO ---

    /**
     * Punto de entrada único para la sincronización de datos de red.
     * Refresca Incidencias, Perfil de Empresa y Lista de Operarios.
     */
    public void sincronizarTodo() {
        try {
            // Refresco total de datos
            servicioCliente.solicitarListaTrabajos(usuarioId, usuarioRol);
            if (idEmpresa > 0) {
                servicioCliente.enviarGetEmpresa(idEmpresa);
                servicioCliente.solicitarListaOperarios(idEmpresa);
            }
        } catch (IOException e) { logError("sincronización total", e); }
    }

    private void enviarAsignacionOperario(TrabajoFX t, int idOp) {
        try {
            servicioCliente.enviarAsignarOperario(t.getId(), idOp, usuarioId);
            registrarActividad("⚙️ Operario asignado a Incidencia #" + t.getId());
        } catch (IOException e) { logError("asignación", e); }
    }

    private void enviarPresupuesto(TrabajoFX t, double monto, String desc) {
        try {
            servicioCliente.enviarCrearPresupuesto(t.getId(), idEmpresa, monto, desc);
            registrarActividad("💰 Presupuesto de " + monto + "€ enviado para #" + t.getId());
        } catch (IOException e) { logError("presupuesto", e); }
    }

    // --- GESTIÓN DE OPERARIOS ---

    public void registrarNuevoOperario(DialogoGestionOperario.ResultadoOperario op, int idEmp) {
        try {
            servicioCliente.enviarRegistroUsuario(true, op.nombre(), op.dni(), op.email(),
                    op.password(), op.telefono(), "", String.valueOf(idEmp), op.especialidad());
            registrarActividad("Nuevo operario registrado: " + op.nombre());
        } catch (IOException e) { logError("registro op", e); }
    }

    public void actualizarOperario(int id, DialogoGestionOperario.ResultadoOperario op, boolean activo) {
        try {
            servicioCliente.enviarModificarOperario(id, op.nombre(), op.dni(), op.email(), op.telefono(), op.especialidad(), activo);
            registrarActividad("⚙️ Solicitada actualización: " + op.nombre());
        } catch (IOException e) { logError("edición op", e); }
    }

    public void cambiarEstadoOperario(OperarioFX operario, boolean nuevoEstado) {
        try {
            servicioCliente.enviarModificarOperario(operario.getId(), operario.getNombre(),
                    operario.getDni(), operario.getEmail(), operario.getTelefono(),
                    operario.getEspecialidad(), nuevoEstado);
            registrarActividad("⛔ Cambio de estado: " + operario.getNombre());
        } catch (Exception e) { logError("estado op", e); }
    }

    public void subirFotoOperario(OperarioFX operario, File file) {
        String path = "perfiles/" + operario.getId() + "_op_" + System.currentTimeMillis() + file.getName();
        backgroundService.subirImagen(file, path,
                url -> {
                    try {
                        servicioCliente.enviarActualizarFotoPerfil(operario.getId(), url);
                        registrarActividad("📸 Actualizada foto de " + operario.getNombre());
                    } catch (IOException e) { logError("foto op", e); }
                },
                err -> logError("subida imagen", (Exception) err));
    }

    public void iniciarCambioFotoGerente() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Seleccionar foto Perfil");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(rootPane.getScene().getWindow());

        if (file != null) {
            String path = "perfiles/" + usuarioId + "_ger_" + System.currentTimeMillis() + file.getName();
            backgroundService.subirImagen(file, path,
                    url -> {
                        try {
                            servicioCliente.enviarActualizarFotoPerfil(usuarioId, url);
                            registrarActividad("📸 Foto de gerente actualizada");
                            if (sidebar != null) Platform.runLater(() -> sidebar.actualizarFoto(url));
                        } catch (IOException e) { logError("foto gerente", e); }
                    },
                    err -> logError("subida gerente", (Exception) err));
        }
    }

    public void iniciarCambioLogoEmpresa() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Seleccionar Logo Corporativo");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(rootPane.getScene().getWindow());

        if (file != null) {
            String path = "logos/" + idEmpresa + "_logo_" + System.currentTimeMillis() + file.getName();
            backgroundService.subirImagen(file, path,
                    url -> {
                        try {
                            // Usamos el comando atómico enviando la nueva URL y el resto de info actual
                            servicioCliente.enviarModificarEmpresa(
                                idEmpresa,
                                (String) infoEmpresaActual.getOrDefault("nombre", ""),
                                (String) infoEmpresaActual.getOrDefault("cif", ""),
                                (String) infoEmpresaActual.getOrDefault("email", ""),
                                (String) infoEmpresaActual.getOrDefault("telefono", ""),
                                (String) infoEmpresaActual.getOrDefault("direccion", ""),
                                url
                            );
                            registrarActividad("🏢 Logo corporativo actualizado");
                        } catch (IOException e) { logError("logo empresa", e); }
                    },
                    err -> logError("subida logo", (Exception) err));
        }
    }

    public void abrirDialogoGestionEmpresa() {
        DialogoGestionEmpresa diag = new DialogoGestionEmpresa(infoEmpresaActual, cssUrl);
        diag.mostrar().ifPresent(res -> {
            try {
                servicioCliente.enviarModificarEmpresa(
                    idEmpresa,
                    res.nombre(),
                    res.cif(),
                    res.email(),
                    res.telefono(),
                    res.direccion(),
                    (String) infoEmpresaActual.getOrDefault("url_foto", "")
                );
                registrarActividad("⚙️ Solicitada actualización de datos de empresa");
            } catch (IOException e) { logError("edición empresa", e); }
        });
    }

    // --- PROCESAMIENTO DE RESPUESTAS (Delegado) ---

    private void procesarRespuesta(String json) {
        try {
            RespuestaServidor r = servicioCliente.interpretarRespuesta(json);
            if (r != null) {
                manejadorRespuestas.procesar(r.getAccion(), r.getMensaje(), r.getDatos(), r.getStatus(), vistaDashboard, rootPane);
            }
        } catch (ClienteException e) {
            logError("interpretación red", e);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void registrarActividad(String actividad) {
        if (vistaDashboard != null) {
            vistaDashboard.getPanelLateral().agregarActividad(actividad);
        }
    }

    private void logError(String op, Exception e) {
        System.err.println("❌ [" + op + "] Error: " + e.getMessage());
    }
}

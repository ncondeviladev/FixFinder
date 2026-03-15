package com.fixfinder.ui.dashboard.utils;

import com.fixfinder.ui.dashboard.DashboardPrincipalController;
import com.fixfinder.ui.dashboard.componentes.TablaIncidencias;
import com.fixfinder.ui.dashboard.dialogos.DialogoEditarOperario;
import com.fixfinder.ui.dashboard.dialogos.DialogoNuevoOperario;
import com.fixfinder.ui.dashboard.modelos.OperarioFX;
import com.fixfinder.ui.dashboard.modelos.TrabajoFX;
import com.fixfinder.ui.dashboard.vistas.*;
import javafx.collections.transformation.FilteredList;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.stage.FileChooser;
import java.io.File;
import java.util.Map;

/**
 * Gestiona la creación y conmutación de vistas dentro del Dashboard.
 * Desacopla la lógica de navegación del controlador principal.
 */
public class VistaRouter {

    private final String cssUrl;
    private final DashboardPrincipalController controller;

    public VistaRouter(String cssUrl, DashboardPrincipalController controller) {
        this.cssUrl = cssUrl;
        this.controller = controller;
    }

    public Node crearVista(String vistaId,
            FilteredList<TrabajoFX> trabajos,
            ObservableList<OperarioFX> operarios,
            TablaIncidencias.AccionesCallback accionesIncidencias,
            Map<String, Object> infoEmpresa,
            String usuarioNombre,
            String usuarioRol,
            int idEmpresa,
            Node rootNode) {

        return switch (vistaId) {
            case "incidencias" -> new VistaIncidencias(trabajos, operarios, accionesIncidencias, cssUrl);
            case "operarios" -> crearVistaOperarios(operarios, idEmpresa, rootNode);
            case "empresa" -> crearVistaEmpresa(infoEmpresa, usuarioNombre, usuarioRol, operarios, idEmpresa);
            default -> null; // El controlador manejará el fallback a VistaDashboard
        };
    }

    private Node crearVistaOperarios(ObservableList<OperarioFX> operarios, int idEmpresa, Node rootNode) {
        return new VistaOperarios(operarios, new VistaOperarios.AccionesOperarioCallback() {
            @Override
            public void onCrearOperario() {
                new DialogoNuevoOperario(cssUrl).mostrar().ifPresent(op -> {
                    controller.registrarNuevoOperario(op, idEmpresa);
                });
            }

            @Override
            public void onEditarOperario(OperarioFX operario) {
                new DialogoEditarOperario(operario, cssUrl).mostrar().ifPresent(op -> {
                    controller.actualizarOperario(operario.getId(), op, operario.isActivo());
                });
            }

            @Override
            public void onCambiarFotoOperario(OperarioFX operario) {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Seleccionar foto para " + operario.getNombre());
                fileChooser.getExtensionFilters()
                        .add(new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg"));
                File file = fileChooser.showOpenDialog(rootNode.getScene().getWindow());
                if (file != null) {
                    controller.subirFotoOperario(operario, file);
                }
            }

            @Override
            public void onCambiarEstadoOperario(OperarioFX operario, boolean nuevoEstado) {
                controller.cambiarEstadoOperario(operario, nuevoEstado);
            }
        });
    }

    private Node crearVistaEmpresa(Map<String, Object> infoEmpresa, String usuarioNombre, String usuarioRol,
            ObservableList<OperarioFX> operarios, int idEmpresa) {
        if (infoEmpresa.isEmpty() && idEmpresa > 0) {
            controller.refrescarDatosEmpresa(idEmpresa);
        }
        return new VistaEmpresa(infoEmpresa, usuarioNombre, usuarioRol, operarios, () -> {
            // Lógica de cambio de foto de gerente delegada al controlador
            controller.iniciarCambioFotoGerente();
        });
    }
}

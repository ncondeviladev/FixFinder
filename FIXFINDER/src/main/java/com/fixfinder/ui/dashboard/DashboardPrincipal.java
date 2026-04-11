package com.fixfinder.ui.dashboard;

import com.fixfinder.cliente.ServicioCliente;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 * Ventana principal del Dashboard de escritorio (JavaFX).
 * Orquesta la creación de la escena, aplica el CSS y arranca la carga de datos
 * iniciales.
 */
public class DashboardPrincipal {

    private static final String CSS_URL = DashboardPrincipal.class
            .getResource("/com/fixfinder/ui/dashboard/dashboard-principal.css")
            .toExternalForm();

    private final Stage stage;
    private final DashboardPrincipalController controller;

    public DashboardPrincipal(ServicioCliente servicioCliente,
            int usuarioId, String usuarioNombre,
            String usuarioRol, String usuarioFoto, int idEmpresa) {
        this.stage = new Stage();
        this.controller = new DashboardPrincipalController(
                servicioCliente, CSS_URL,
                usuarioId, usuarioNombre, usuarioRol, usuarioFoto, idEmpresa);
    }

    public void mostrar() {
        BorderPane root = controller.construirVista(this::cerrar);

        Scene scene = new Scene(root, 1080, 600);
        scene.getStylesheets().add(CSS_URL);

        stage.setTitle("FixFinder — Panel de Control");
        try {
            stage.getIcons()
                    .add(new Image(getClass().getResourceAsStream("/com/fixfinder/ui/dashboard/imagenes/logo.png")));
        } catch (Exception ignored) {
        }

        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.show();

        controller.sincronizarTodo();
    }

    private void cerrar() {
        stage.close();
    }

    public Stage getStage() {
        return stage;
    }
}

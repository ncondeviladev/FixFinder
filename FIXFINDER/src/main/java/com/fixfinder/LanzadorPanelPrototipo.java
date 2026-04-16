package com.fixfinder;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Panel de pruebas del Dashboard antiguo (Simulador de control).
 * Conservado para pruebas de desarrollo independientes.
 */
public class LanzadorPanelPrototipo {

    private static Scene scene;

    public static class FXApp extends Application {
        @Override
        public void start(Stage stage) throws IOException {
            // Cargar el FXML desde resources
            FXMLLoader fxmlLoader = new FXMLLoader(LanzadorPanelPrototipo.class.getResource("/com/fixfinder/vistas/dashboard.fxml"));
            Parent root = fxmlLoader.load();

            scene = new Scene(root, 800, 600);
            stage.setTitle("FixFinder - Test Panel de Control");
            stage.setScene(scene);
            stage.show();
        }
    }

    public static void main(String[] args) {
        Application.launch(FXApp.class, args);
    }
}

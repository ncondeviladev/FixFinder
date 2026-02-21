package com.fixfinder;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Punto de entrada para la aplicación de Escritorio JavaFX.
 */
public class AppEscritorio extends Application {

    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
        // Cargar el FXML desde resources
        FXMLLoader fxmlLoader = new FXMLLoader(AppEscritorio.class.getResource("/com/fixfinder/vistas/dashboard.fxml"));
        Parent root = fxmlLoader.load();

        scene = new Scene(root, 800, 600);
        stage.setTitle("FixFinder - Panel de Control");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}

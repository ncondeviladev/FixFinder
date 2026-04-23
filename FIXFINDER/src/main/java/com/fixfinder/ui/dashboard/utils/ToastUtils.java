package com.fixfinder.ui.dashboard.utils;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/**
 * Utilidad para mostrar notificaciones tipo "Toast" en el Dashboard.
 */
public class ToastUtils {

    public static void showToast(Stage owner, String message, String type) {
        Stage toastStage = new Stage();
        toastStage.initOwner(owner);
        toastStage.initStyle(StageStyle.TRANSPARENT);

        Label lbl = new Label(message);
        lbl.setTextFill(Color.WHITE);
        lbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10 20 10 20;");

        HBox container = new HBox(lbl);
        container.setAlignment(Pos.CENTER);
        
        container.setStyle("-fx-background-color: #000000BB; " + // Negro 75%
                           "-fx-background-radius: 12; " +
                           "-fx-border-color: #FF9800; " + // Naranja FixFinder
                           "-fx-border-radius: 12; " +
                           "-fx-border-width: 2;");

        StackPane root = new StackPane(container);
        root.setStyle("-fx-background-color: transparent;");
        root.setPadding(new Insets(10));

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        toastStage.setScene(scene);

        // Posicionamiento CENTRADO SUPERIOR de la VENTANA PADRE
        double toastWidth = 360; 
        double x = owner.getX() + (owner.getWidth() - toastWidth) / 2;
        double y = owner.getY() + 40; // Margen superior
        
        toastStage.setX(x);
        toastStage.setY(y);

        // Animaciones
        toastStage.show();
        
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), root);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        PauseTransition delay = new PauseTransition(Duration.seconds(4));
        delay.setOnFinished(e -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(500), root);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(e2 -> toastStage.close());
            fadeOut.play();
        });
        delay.play();
    }
}

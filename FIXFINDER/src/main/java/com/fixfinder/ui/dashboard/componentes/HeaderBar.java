package com.fixfinder.ui.dashboard.componentes;

import javafx.animation.RotateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

/**
 * Componente que representa la barra superior institucional del Dashboard.
 * Gestiona la identidad visual y el botón de actualización.
 */
public class HeaderBar extends HBox {

    private final Runnable alRefrescar;

    /**
     * Constructor de la barra superior.
     * @param titulo Texto principal a mostrar (ej: "FixFinder Dashboard").
     * @param alRefrescar Acción a ejecutar al pulsar el botón de actualización.
     */
    public HeaderBar(String titulo, Runnable alRefrescar) {
        this.alRefrescar = alRefrescar;
        
        // Configuración básica del contenedor (HBox)
        setSpacing(12);
        setAlignment(Pos.CENTER_LEFT);
        getStyleClass().add("barra-cabecera");
        setMinHeight(64);
        setPrefHeight(64);

        // Logo e Identidad Visual (Official Branding)
        HBox branding = new HBox(10);
        branding.setAlignment(Pos.CENTER_LEFT);
        
        StackPane logoContainer = new StackPane();
        logoContainer.setMinSize(36, 36);
        logoContainer.setMaxSize(36, 36);

        try {
            Image logoImg = new Image(getClass().getResourceAsStream("/com/fixfinder/ui/dashboard/imagenes/logo.png"));
            ImageView logoView = new ImageView(logoImg);
            logoView.setFitWidth(36);
            logoView.setFitHeight(36);
            logoView.setPreserveRatio(true);
            
            // Recorte circular premium
            Circle clip = new Circle(18, 18, 18);
            logoView.setClip(clip);
            
            logoContainer.getChildren().add(logoView);
        } catch (Exception e) {
            // Fallback elegante si la imagen no carga
            logoContainer.getStyleClass().add("caja-logo-cabecera");
            Label letra = new Label("F");
            letra.getStyleClass().add("letra-logo-cabecera");
            logoContainer.getChildren().add(letra);
        }
        
        Label lblLogoText = new Label("FixFinder");
        lblLogoText.getStyleClass().add("nombre-logo-cabecera");
        
        branding.getChildren().addAll(logoContainer, lblLogoText);

        // Separador visual entre logo y título de sección
        Separator sep = new Separator(javafx.geometry.Orientation.VERTICAL);
        sep.setPrefHeight(20);
        sep.setMaxHeight(20);
        sep.setPadding(new Insets(0, 5, 0, 5));

        // Título de la sección actual (ej: "Dashboard", "Incidencias")
        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add("titulo-cabecera");
        lblTitulo.setOpacity(0.6);

        // Espaciador flexible para empujar el botón a la derecha
        Region espaciador = new Region();
        HBox.setHgrow(espaciador, Priority.ALWAYS);

        // Botón de actualización (↻)
        Button btnActualizar = new Button("↻ Actualizar");
        btnActualizar.getStyleClass().add("btn-cabecera");
        
        btnActualizar.setOnAction(e -> {
            // Animación de rotación simple para feedback visual
            RotateTransition rt = new RotateTransition(Duration.millis(500), btnActualizar);
            rt.setByAngle(360);
            rt.setCycleCount(1);
            rt.play();
            
            this.alRefrescar.run();
        });

        // Ensamblado de componentes
        getChildren().addAll(branding, sep, lblTitulo, espaciador, btnActualizar);
    }
}

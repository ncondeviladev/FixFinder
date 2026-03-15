package com.fixfinder.ui.dashboard.componentes;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Barra lateral de navegación del Dashboard JavaFX.
 * Muestra el logo, los botones de sección y el panel del usuario activo con
 * logout.
 */
public class Sidebar extends VBox {

    private final List<Button> navButtons = new ArrayList<>();
    private String vistaActual = "dashboard";

    public Sidebar(String usuarioNombre, String usuarioRol,
            Consumer<String> onNavegar, Runnable onLogout) {
        getStyleClass().add("sidebar");
        getChildren().addAll(
                construirLogo(),
                construirNav(onNavegar),
                construirUsuario(usuarioNombre, usuarioRol, null, onLogout));
    }

    public Sidebar(String usuarioNombre, String usuarioRol, String urlFoto,
            Consumer<String> onNavegar, Runnable onLogout) {
        getStyleClass().add("sidebar");
        getChildren().addAll(
                construirLogo(),
                construirNav(onNavegar),
                construirUsuario(usuarioNombre, usuarioRol, urlFoto, onLogout));
    }

    public void marcarActivo(String vistaId) {
        this.vistaActual = vistaId;
    }

    private HBox construirLogo() {
        HBox logoBox = new HBox(12);
        logoBox.getStyleClass().add("sidebar-logo-box");

        StackPane icono = new StackPane();
        icono.getStyleClass().add("sidebar-logo-icon");
        icono.setMinSize(36, 36);
        icono.setMaxSize(36, 36);
        Label letra = new Label("F");
        letra.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px;");
        icono.getChildren().add(letra);

        Label texto = new Label("FixFinder");
        texto.getStyleClass().add("sidebar-logo-text");

        logoBox.getChildren().addAll(icono, texto);
        return logoBox;
    }

    private VBox construirNav(Consumer<String> onNavegar) {
        VBox nav = new VBox(4);
        nav.setPadding(new Insets(12, 8, 12, 8));
        VBox.setVgrow(nav, Priority.ALWAYS);

        String[][] items = {
                { "dashboard", "⊞", "Dashboard" },
                { "incidencias", "≡", "Incidencias" },
                { "operarios", "●", "Operarios" },
                { "empresa", "◉", "Empresa" }
        };

        for (String[] item : items) {
            Button btn = new Button(item[1] + "  " + item[2]);
            btn.getStyleClass().add("nav-item");
            btn.setMaxWidth(Double.MAX_VALUE);
            if ("dashboard".equals(item[0]))
                btn.getStyleClass().add("active");

            String vistaId = item[0];
            btn.setOnAction(e -> {
                if (vistaId.equals(vistaActual))
                    return;
                vistaActual = vistaId;
                navButtons.forEach(b -> b.getStyleClass().remove("active"));
                btn.getStyleClass().add("active");
                onNavegar.accept(vistaId);
            });

            navButtons.add(btn);
            nav.getChildren().add(btn);
        }
        return nav;
    }

    private VBox construirUsuario(String nombre, String rol, String urlFoto, Runnable onLogout) {
        VBox box = new VBox(6);
        box.getStyleClass().add("sidebar-user-box");

        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        StackPane avatar = new StackPane();
        avatar.getStyleClass().add("user-avatar");
        avatar.setMinSize(36, 36);
        avatar.setMaxSize(36, 36);

        if (urlFoto != null && (urlFoto.startsWith("http") || urlFoto.startsWith("https"))) {
            try {
                javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(
                        new javafx.scene.image.Image(urlFoto, 36, 36, true, true, true));
                javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(18, 18, 18);
                iv.setClip(clip);
                avatar.getChildren().add(iv);
            } catch (Exception e) {
                Label ini = new Label(iniciales(nombre));
                ini.setStyle("-fx-text-fill: #F97316; -fx-font-weight: bold; -fx-font-size: 13px;");
                avatar.getChildren().add(ini);
            }
        } else {
            Label ini = new Label(iniciales(nombre));
            ini.setStyle("-fx-text-fill: #F97316; -fx-font-weight: bold; -fx-font-size: 13px;");
            avatar.getChildren().add(ini);
        }

        VBox info = new VBox(3);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label lblNombre = new Label(nombre);
        lblNombre.getStyleClass().add("sidebar-user-name");
        lblNombre.setMaxWidth(120);
        Label lblRol = new Label(rol);
        lblRol.getStyleClass().add("sidebar-user-role");
        info.getChildren().addAll(lblNombre, lblRol);

        Button btnLogout = new Button("↪");
        btnLogout.getStyleClass().add("btn-logout");
        btnLogout.setTooltip(new Tooltip("Cerrar sesión"));
        btnLogout.setOnAction(e -> onLogout.run());

        row.getChildren().addAll(avatar, info, btnLogout);
        box.getChildren().add(row);
        return box;
    }

    private String iniciales(String nombre) {
        if (nombre == null || nombre.isBlank())
            return "??";
        String[] p = nombre.trim().split("\\s+");
        return p.length >= 2
                ? ("" + p[0].charAt(0) + p[1].charAt(0)).toUpperCase()
                : nombre.substring(0, Math.min(2, nombre.length())).toUpperCase();
    }
}

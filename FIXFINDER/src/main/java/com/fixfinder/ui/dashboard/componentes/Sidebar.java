package com.fixfinder.ui.dashboard.componentes;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Barra lateral de navegación del Dashboard JavaFX.
 * Muestra el logo, los botones de sección y el panel del usuario activo con logout.
 */
public class Sidebar extends VBox {

    private final List<Button> navButtons = new ArrayList<>();
    private String vistaActual = "dashboard";
    private StackPane avatarPane;
    private String nombreUsuario;

    /**
     * Constructor principal.
     *
     * @param usuarioNombre Nombre a mostrar en el perfil.
     * @param usuarioRol    Rol del usuario activo.
     * @param urlFoto       URL opcional de la foto de perfil.
     * @param onNavegar     Acción a ejecutar al pulsar un botón de navegación.
     * @param onLogout      Acción a ejecutar al cerrar sesión.
     */
    public Sidebar(String usuarioNombre, String usuarioRol, String urlFoto,
            Consumer<String> onNavegar, Runnable onLogout) {
        this.nombreUsuario = usuarioNombre;
        getStyleClass().add("sidebar");
        getChildren().addAll(
                construirLogo(),
                construirNav(onNavegar),
                construirUsuario(usuarioNombre, usuarioRol, urlFoto, onLogout));
    }

    /** Sobrecarga sin foto de perfil. */
    public Sidebar(String usuarioNombre, String usuarioRol,
            Consumer<String> onNavegar, Runnable onLogout) {
        this(usuarioNombre, usuarioRol, null, onNavegar, onLogout);
    }

    public void marcarActivo(String vistaId) {
        this.vistaActual = vistaId;
    }

    /** Actualiza la foto del avatar en el sidebar sin reconstruir la vista. */
    public void actualizarFoto(String url) {
        if (avatarPane != null) {
            avatarPane.getChildren().clear();
            cargarFotoEnPane(avatarPane, url, nombreUsuario);
        }
    }

    // ─── Construcción de secciones ────────────────────────────────────────────

    private HBox construirLogo() {
        HBox logoBox = new HBox(12);
        logoBox.getStyleClass().add("sidebar-logo-box");
        logoBox.setAlignment(Pos.CENTER_LEFT);

        ImageView logoIcon = new ImageView();
        try {
            Image img = new Image(getClass().getResourceAsStream(
                    "/com/fixfinder/ui/dashboard/imagenes/logo.png"));
            logoIcon.setImage(img);
            logoIcon.setFitWidth(32);
            logoIcon.setFitHeight(32);
            logoIcon.setPreserveRatio(true);
            logoIcon.setClip(new Circle(16, 16, 16));
        } catch (Exception e) {
            StackPane icono = new StackPane();
            icono.getStyleClass().add("sidebar-logo-icon");
            icono.setMinSize(32, 32);
            Label letra = new Label("F");
            letra.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px;");
            icono.getChildren().add(letra);
            logoBox.getChildren().add(icono);
        }

        if (logoIcon.getImage() != null) {
            logoBox.getChildren().add(logoIcon);
        }

        Label texto = new Label("FixFinder");
        texto.getStyleClass().add("sidebar-logo-text");
        logoBox.getChildren().add(texto);
        return logoBox;
    }

    private VBox construirNav(Consumer<String> onNavegar) {
        VBox nav = new VBox(4);
        nav.setPadding(new Insets(12, 8, 12, 8));
        VBox.setVgrow(nav, Priority.ALWAYS);

        String[][] items = {
                { "dashboard",  "⊞", "Dashboard"  },
                { "incidencias","≡", "Incidencias" },
                { "operarios",  "●", "Operarios"   },
                { "empresa",    "◉", "Empresa"     }
        };

        for (String[] item : items) {
            Button btn = new Button(item[1] + "  " + item[2]);
            btn.getStyleClass().add("nav-item");
            btn.setMaxWidth(Double.MAX_VALUE);
            if ("dashboard".equals(item[0])) btn.getStyleClass().add("active");

            String vistaId = item[0];
            btn.setOnAction(e -> {
                if (vistaId.equals(vistaActual)) return;
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

        this.avatarPane = new StackPane();
        avatarPane.getStyleClass().add("user-avatar");
        avatarPane.setMinSize(36, 36);
        avatarPane.setMaxSize(36, 36);
        cargarFotoEnPane(avatarPane, urlFoto, nombre);

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

        row.getChildren().addAll(avatarPane, info, btnLogout);
        box.getChildren().add(row);
        return box;
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void cargarFotoEnPane(StackPane pane, String url, String nombre) {
        if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
            try {
                Image img = new Image(url, 36, 36, true, true, false);
                ImageView iv = new ImageView(img);
                iv.setClip(new Circle(18, 18, 18));
                pane.getChildren().add(iv);
            } catch (Exception e) {
                mostrarAvatarTexto(pane, nombre);
            }
        } else {
            mostrarAvatarTexto(pane, nombre);
        }
    }

    private void mostrarAvatarTexto(StackPane avatar, String nombre) {
        Label ini = new Label(iniciales(nombre));
        ini.setStyle("-fx-text-fill: #F97316; -fx-font-weight: bold; -fx-font-size: 13px;");
        avatar.getChildren().add(ini);
    }

    private String iniciales(String nombre) {
        if (nombre == null || nombre.isBlank()) return "??";
        String[] p = nombre.trim().split("\\s+");
        return p.length >= 2
                ? ("" + p[0].charAt(0) + p[1].charAt(0)).toUpperCase()
                : nombre.substring(0, Math.min(1, nombre.length())).toUpperCase();
    }
}

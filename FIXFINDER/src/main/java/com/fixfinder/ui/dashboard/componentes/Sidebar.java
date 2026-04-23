package com.fixfinder.ui.dashboard.componentes;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;

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
    private StackPane avatarPane;
    private Label lblNombreUsuario; // Referencia para actualización en vivo
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
        getStyleClass().add("barra-lateral");
        getChildren().addAll(
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

    /** Actualiza el nombre a mostrar en el perfil del sidebar. */
    public void actualizarNombre(String nuevo) {
        if (lblNombreUsuario != null) {
            Platform.runLater(() -> {
                this.nombreUsuario = nuevo;
                lblNombreUsuario.setText(nuevo);
                // Si la foto es de iniciales, también refrescarla
                if (avatarPane != null && avatarPane.getChildren().size() == 1 && avatarPane.getChildren().get(0) instanceof Label) {
                    actualizarFoto(null); // Refrescará las iniciales con el nuevo nombre
                }
            });
        }
    }

    /** Actualiza la foto del avatar en el sidebar sin reconstruir la vista. */
public void actualizarFoto(String url) {
        if (avatarPane != null) {
            avatarPane.getChildren().clear();
            cargarFotoEnPane(avatarPane, url, nombreUsuario);
        }
    }

    // ─── Construcción de secciones ────────────────────────────────────────────

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
            btn.getStyleClass().add("item-navegacion");
            btn.setMaxWidth(Double.MAX_VALUE);
            if ("dashboard".equals(item[0]))
                btn.getStyleClass().add("activo");

            String vistaId = item[0];
            btn.setOnAction(e -> {
                if (vistaId.equals(vistaActual))
                    return;
                vistaActual = vistaId;
                navButtons.forEach(b -> b.getStyleClass().remove("activo"));
                btn.getStyleClass().add("activo");
                onNavegar.accept(vistaId);
            });

            navButtons.add(btn);
            nav.getChildren().add(btn);
        }
        return nav;
    }

    private VBox construirUsuario(String nombre, String rol, String urlFoto, Runnable onLogout) {
        VBox box = new VBox(6);
        box.getStyleClass().add("caja-usuario-lateral");

        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        this.avatarPane = new StackPane();
        avatarPane.getStyleClass().add("avatar-usuario");
        avatarPane.setMinSize(36, 36);
        avatarPane.setMaxSize(36, 36);
        cargarFotoEnPane(avatarPane, urlFoto, nombre);

        VBox info = new VBox(3);
        HBox.setHgrow(info, Priority.ALWAYS);
        this.lblNombreUsuario = new Label(nombre);
        lblNombreUsuario.getStyleClass().add("nombre-usuario-lateral");
        lblNombreUsuario.setMaxWidth(120);
        Label lblRol = new Label(rol);
        lblRol.getStyleClass().add("rol-usuario-lateral");
        info.getChildren().addAll(lblNombreUsuario, lblRol);

        Button btnLogout = new Button("↪");
        btnLogout.getStyleClass().add("btn-cerrar-sesion");
        btnLogout.setTooltip(new Tooltip("Cerrar sesión"));
        btnLogout.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Cerrar Sesión");
            alert.setHeaderText("¿Estás seguro de que deseas salir?");
            alert.setContentText("Cualquier cambio no guardado podría perderse.");

            // Personalizamos los botones a Sí/No para que sea más claro
            ButtonType btnSi = new ButtonType("Sí");
            ButtonType btnNo = new ButtonType("No", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(btnSi, btnNo);

            alert.showAndWait().ifPresent(response -> {
                if (response == btnSi) {
                    onLogout.run();
                }
            });
        });

        row.getChildren().addAll(avatarPane, info, btnLogout);
        box.getChildren().add(row);
        return box;
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void cargarFotoEnPane(StackPane pane, String url, String nombre) {
        if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
            try {
                // Carga asíncrona de alta calidad
                Image img = new Image(url, 72, 72, true, true, true);
                Circle circulo = new Circle(18);
                circulo.setSmooth(true);
                circulo.setFill(javafx.scene.paint.Color.web("#1A1D27")); // Fondo neutro mientras carga

                img.progressProperty().addListener((obs, old, progress) -> {
                    if (progress.doubleValue() == 1.0) {
                        Platform.runLater(() -> {
                            circulo.setFill(new ImagePattern(img));
                        });
                    }
                });

                if (img.getProgress() == 1.0) {
                    circulo.setFill(new ImagePattern(img));
                }

                pane.getChildren().add(circulo);
            } catch (Exception e) {
                mostrarAvatarTexto(pane, nombre);
            }
        } else {
            mostrarAvatarTexto(pane, nombre);
        }
    }

    private void mostrarAvatarTexto(StackPane avatar, String nombre) {
        Label ini = new Label(iniciales(nombre));
        ini.getStyleClass().add("iniciales-avatar");
        avatar.getChildren().add(ini);
    }

    private String iniciales(String nombre) {
        if (nombre == null || nombre.isBlank())
            return "??";
        String[] p = nombre.trim().split("\\s+");
        return p.length >= 2
                ? ("" + p[0].charAt(0) + p[1].charAt(0)).toUpperCase()
                : nombre.substring(0, Math.min(1, nombre.length())).toUpperCase();
    }
}

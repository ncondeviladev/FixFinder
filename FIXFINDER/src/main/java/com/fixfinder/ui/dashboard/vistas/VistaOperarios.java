package com.fixfinder.ui.dashboard.vistas;

import com.fixfinder.ui.dashboard.modelos.OperarioFX;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;

/**
 * Vista de la sección "Operarios" del Dashboard.
 * Muestra la tabla del equipo técnico con acciones de crear, editar, cambiar foto y estado.
 */
public class VistaOperarios extends VBox {

    public interface AccionesOperarioCallback {
        void onCrearOperario();
        void onEditarOperario(OperarioFX operario);
        void onCambiarFotoOperario(OperarioFX operario);
        void onCambiarEstadoOperario(OperarioFX operario, boolean nuevoEstado);
    }

    private final TableView<OperarioFX> tabla;

    public VistaOperarios(ObservableList<OperarioFX> operarios, AccionesOperarioCallback callback) {
        getStyleClass().add("area-contenido");
        VBox.setVgrow(this, Priority.ALWAYS);

        tabla = construirTabla(operarios, callback);

        VBox card = new VBox();
        card.getStyleClass().add("tarjeta-tabla");
        VBox.setVgrow(card, Priority.ALWAYS);

        HBox cardHeader = new HBox(12);
        cardHeader.getStyleClass().add("cabecera-tarjeta");
        cardHeader.setAlignment(Pos.CENTER_LEFT);

        Label titulo = new Label("Equipo Técnico");
        titulo.getStyleClass().add("titulo-tarjeta");
        HBox.setHgrow(titulo, Priority.ALWAYS);

        Button btnNuevo = new Button("＋ Nuevo Operario");
        btnNuevo.getStyleClass().add("btn-primario");
        btnNuevo.setOnAction(e -> callback.onCrearOperario());

        cardHeader.getChildren().addAll(titulo, btnNuevo);

        VBox.setVgrow(tabla, Priority.ALWAYS);
        card.getChildren().addAll(cardHeader, tabla);

        VBox body = new VBox();
        body.setPadding(new Insets(20, 24, 24, 24));
        VBox.setVgrow(body, Priority.ALWAYS);
        body.getChildren().add(card);
        VBox.setVgrow(card, Priority.ALWAYS);

        getChildren().add(body);
    }

    @SuppressWarnings("unchecked")
    private TableView<OperarioFX> construirTabla(ObservableList<OperarioFX> lista, AccionesOperarioCallback callback) {
        TableView<OperarioFX> t = new TableView<>(lista);
        t.getStyleClass().add("table-view");
        t.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        t.setPlaceholder(new Label("No hay operarios registrados"));
        VBox.setVgrow(t, Priority.ALWAYS);

        TableColumn<OperarioFX, String> colNombre = new TableColumn<>("Nombre");
        colNombre.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNombre()));
        colNombre.setMinWidth(180);
        colNombre.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setGraphic(null); return; }
                HBox box = new HBox(10);
                box.setAlignment(Pos.CENTER_LEFT);
                OperarioFX op = getTableView().getItems().get(getIndex());
                StackPane av = miniAvatar(v, op.getUrlFoto(), 32);
                Label lbl = new Label(v);
                if (!op.isActivo()) {
                    lbl.getStyleClass().add("texto-tabla-inactivo");
                    av.setOpacity(0.4);
                } else {
                    lbl.getStyleClass().add("texto-tabla-activo");
                    av.setOpacity(1.0);
                }
                box.getChildren().addAll(av, lbl);
                setGraphic(box);
                setText(null);
            }
        });

        TableColumn<OperarioFX, String> colEsp = new TableColumn<>("Especialidad");
        colEsp.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEspecialidad()));
        colEsp.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setGraphic(null); return; }
                Label l = new Label(v.isBlank() ? "General" : v.charAt(0) + v.substring(1).toLowerCase());
                l.getStyleClass().add("texto-tenue");
                setGraphic(l);
                setText(null);
            }
        });

        TableColumn<OperarioFX, String> colEstado = new TableColumn<>("Estado");
        colEstado.setMaxWidth(130);
        colEstado.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isDisponible() ? "Disponible" : "Ocupado"));
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setGraphic(null); return; }
                HBox box = new HBox(6);
                box.setAlignment(Pos.CENTER_LEFT);
                OperarioFX op = getTableView().getItems().get(getIndex());
                if (!op.isActivo()) {
                    Circle dot = new Circle(5, Color.web("#64748B"));
                    Label lbl = new Label("De Baja");
                    lbl.getStyleClass().add("etiqueta-estado-inactivo");
                    box.getChildren().addAll(dot, lbl);
                } else {
                    Circle dot = new Circle(5);
                    dot.setFill("Disponible".equals(v) ? Color.web("#22C55E") : Color.web("#F59E0B"));
                    Label lbl = new Label(v);
                    lbl.getStyleClass().add("Disponible".equals(v) ? "estado-disponible" : "estado-ocupado");
                    box.getChildren().addAll(dot, lbl);
                }
                setGraphic(box);
                setText(null);
            }
        });

        TableColumn<OperarioFX, Number> colCarga = new TableColumn<>("Trabajos activos");
        colCarga.setMaxWidth(140);
        colCarga.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getCargaTrabajo()));

        TableColumn<OperarioFX, Void> colAcciones = new TableColumn<>("Acciones");
        colAcciones.setMinWidth(150);
        colAcciones.setMaxWidth(160);
        colAcciones.setSortable(false);
        colAcciones.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) { setGraphic(null); return; }
                OperarioFX op = getTableView().getItems().get(getIndex());

                Button btnEditar = actionBtn("✏️", "Editar operario");
                btnEditar.setOnAction(e -> callback.onEditarOperario(op));

                Button btnEstado = actionBtn(op.isActivo() ? "⛔" : "✅", op.isActivo() ? "Dar de baja" : "Dar de alta");
                btnEstado.setOnAction(e -> callback.onCambiarEstadoOperario(op, !op.isActivo()));

                Button btnFoto = actionBtn("📸", "Cambiar foto");
                btnFoto.setOnAction(e -> callback.onCambiarFotoOperario(op));

                HBox box = new HBox(14);
                box.setAlignment(Pos.CENTER_LEFT);
                box.setPadding(new Insets(0, 0, 0, 15));
                box.getChildren().addAll(btnEditar, btnFoto, btnEstado);
                setGraphic(box);
            }
        });

        t.getColumns().addAll(colAcciones, colNombre, colEsp, colEstado, colCarga);
        return t;
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Button actionBtn(String txt, String tooltip) {
        Button b = new Button(txt);
        b.getStyleClass().add("btn-accion-tabla");
        Tooltip.install(b, new Tooltip(tooltip));
        return b;
    }

    private StackPane miniAvatar(String nombre, String urlFoto, int size) {
        StackPane av = new StackPane();
        av.setMinSize(size, size);
        av.setMaxSize(size, size);
        av.getStyleClass().add("contenedor-mini-avatar");
        av.setStyle("-fx-background-radius: " + (size / 2) + ";");

        if (urlFoto != null && (urlFoto.startsWith("http://") || urlFoto.startsWith("https://"))) {
            try {
                // Carga asíncrona de 2 niveles de calidad
                Image img = new Image(urlFoto, size * 2, size * 2, true, true, true);
                Circle circuloFoto = new Circle(size / 2.0);
                circuloFoto.setSmooth(true);
                circuloFoto.setFill(Color.TRANSPARENT);

                img.progressProperty().addListener((obs, old, progress) -> {
                    if (progress.doubleValue() == 1.0) {
                        Platform.runLater(() -> {
                            if (!img.isError()) {
                                circuloFoto.setFill(new ImagePattern(img));
                            } else {
                                mostrarIniciales(av, nombre, size);
                            }
                        });
                    }
                });

                if (img.getProgress() == 1.0) {
                    if (!img.isError()) {
                        circuloFoto.setFill(new ImagePattern(img));
                    } else {
                        mostrarIniciales(av, nombre, size);
                    }
                }

                av.getChildren().add(circuloFoto);
                return av;
            } catch (Exception ignored) {
            }
        }

        mostrarIniciales(av, nombre, size);
        return av;
    }

    private void mostrarIniciales(StackPane av, String nombre, int size) {
        String[] p = nombre.trim().split("\\s+");
        String ini = p.length >= 2 ? ("" + p[0].charAt(0) + p[1].charAt(0)).toUpperCase()
                : nombre.substring(0, Math.min(2, nombre.length())).toUpperCase();
        Label l = new Label(ini);
        l.getStyleClass().add("iniciales-avatar");
        l.setStyle("-fx-font-size: " + (size / 3) + "px;");
        av.getChildren().add(l);
    }
}

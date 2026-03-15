package com.fixfinder.ui.dashboard.componentes;

import com.fixfinder.ui.dashboard.modelos.TrabajoFX;
import com.fixfinder.ui.dashboard.dialogos.DialogoAsignarOperario;
import com.fixfinder.ui.dashboard.dialogos.DialogoDetalleIncidencia;
import com.fixfinder.ui.dashboard.dialogos.DialogoFichaCliente;
import com.fixfinder.ui.dashboard.modelos.OperarioFX;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Optional;

/**
 * Componente VBox reutilizable que renderiza la tabla de incidencias del
 * Dashboard.
 * Gestiona las columnas, celdas personalizadas, filtrado por texto y tabs de
 * estado,
 * y expone callbacks para las acciones de asignar operario y crear presupuesto.
 */
public class TablaIncidencias extends VBox {

    public interface AccionesCallback {
        void onAsignar(TrabajoFX trabajo, int idOperario);

        void onPresupuestar(TrabajoFX trabajo, double monto, String notas);
    }

    private final FilteredList<TrabajoFX> trabajosFiltrados;
    private final ObservableList<OperarioFX> operarios;
    private final AccionesCallback callback;
    private final String cssUrl;
    private String filtroActivo = "todas";

    public TablaIncidencias(FilteredList<TrabajoFX> trabajosFiltrados,
            ObservableList<OperarioFX> operarios,
            AccionesCallback callback,
            String cssUrl) {
        this.trabajosFiltrados = trabajosFiltrados;
        this.operarios = operarios;
        this.callback = callback;
        this.cssUrl = cssUrl;

        getStyleClass().add("table-card");
        VBox.setVgrow(this, Priority.ALWAYS);
        construir();
    }

    private void construir() {
        VBox header = new VBox(10);
        header.getStyleClass().add("card-header");

        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label titulo = new Label("Incidencias");
        titulo.getStyleClass().add("card-title");
        HBox.setHgrow(titulo, Priority.ALWAYS);

        TextField busqueda = new TextField();
        busqueda.setPromptText("⌕  Buscar...");
        busqueda.getStyleClass().add("search-field");
        busqueda.textProperty().addListener((obs, old, val) -> aplicarFiltro(val));
        topRow.getChildren().addAll(titulo, busqueda);

        HBox tabs = construirTabs(busqueda);
        header.getChildren().addAll(topRow, tabs);

        TableView<TrabajoFX> tabla = construirTabla();
        VBox.setVgrow(tabla, Priority.ALWAYS);
        getChildren().addAll(header, tabla);
    }

    private HBox construirTabs(TextField busqueda) {
        HBox tabs = new HBox(4);
        tabs.getStyleClass().add("filter-tabs");
        tabs.setAlignment(Pos.CENTER_LEFT);

        ToggleGroup grupo = new ToggleGroup();
        String[][] opciones = { { "todas", "Todas" }, { "pendientes", "Pendientes" },
                { "proceso", "En proceso" }, { "finalizadas", "Finalizadas" } };

        for (String[] op : opciones) {
            ToggleButton btn = new ToggleButton(op[1]);
            btn.getStyleClass().add("filter-tab");
            btn.setToggleGroup(grupo);
            if ("todas".equals(op[0])) {
                btn.setSelected(true);
                btn.getStyleClass().add("selected");
            }
            String id = op[0];
            btn.selectedProperty().addListener((obs, old, sel) -> {
                if (sel) {
                    btn.getStyleClass().add("selected");
                    filtroActivo = id;
                    aplicarFiltro(busqueda.getText());
                } else
                    btn.getStyleClass().remove("selected");
            });
            tabs.getChildren().add(btn);
        }
        return tabs;
    }

    @SuppressWarnings("unchecked")
    private TableView<TrabajoFX> construirTabla() {
        TableView<TrabajoFX> tabla = new TableView<>(trabajosFiltrados);
        tabla.getStyleClass().add("table-view");
        tabla.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        tabla.setPlaceholder(new Label("No hay incidencias para mostrar"));
        VBox.setVgrow(tabla, Priority.ALWAYS);

        tabla.getColumns().addAll(
                colAcciones(), colId(), colTitulo(), colCliente(), colCategoria(),
                colEstado(), colOperario(), colFecha());
        return tabla;
    }

    private TableColumn<TrabajoFX, Number> colId() {
        TableColumn<TrabajoFX, Number> col = new TableColumn<>("ID");
        col.setCellValueFactory(c -> c.getValue().idProperty());
        col.setMaxWidth(50);
        col.setMinWidth(40);
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Number v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) {
                    setGraphic(null);
                    return;
                }
                Label l = new Label(String.valueOf(v.intValue()));
                l.getStyleClass().add("cell-id");
                setGraphic(l);
                setText(null);
            }
        });
        return col;
    }

    private TableColumn<TrabajoFX, String> colTitulo() {
        TableColumn<TrabajoFX, String> col = new TableColumn<>("Título");
        col.setCellValueFactory(c -> c.getValue().tituloProperty());
        col.setMinWidth(150);
        return col;
    }

    private TableColumn<TrabajoFX, String> colCliente() {
        TableColumn<TrabajoFX, String> col = new TableColumn<>("Cliente");
        col.setCellValueFactory(c -> c.getValue().clienteProperty());
        col.setMinWidth(110);
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) {
                    setGraphic(null);
                    return;
                }
                Hyperlink link = new Hyperlink(v);
                link.setStyle("-fx-text-fill: #38BDF8; -fx-underline: false; -fx-padding: 0;");
                link.setOnAction(e -> {
                    TrabajoFX t = getTableView().getItems().get(getIndex());
                    new DialogoFichaCliente(cssUrl).mostrar(t);
                });
                link.setOnMouseEntered(
                        e -> link.setStyle("-fx-text-fill: #7DD3FC; -fx-underline: true; -fx-padding: 0;"));
                link.setOnMouseExited(
                        e -> link.setStyle("-fx-text-fill: #38BDF8; -fx-underline: false; -fx-padding: 0;"));
                setGraphic(link);
                setText(null);
            }
        });
        return col;
    }

    private TableColumn<TrabajoFX, String> colCategoria() {
        TableColumn<TrabajoFX, String> col = new TableColumn<>("Categoría");
        col.setCellValueFactory(c -> c.getValue().categoriaProperty());
        col.setMinWidth(130);
        col.setMaxWidth(150);
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) {
                    setGraphic(null);
                    return;
                }
                HBox box = new HBox(5);
                box.setAlignment(Pos.CENTER_LEFT);
                box.getChildren().addAll(iconoLabel(iconoCategoria(v)),
                        textoMuted(v.charAt(0) + v.substring(1).toLowerCase()));
                setGraphic(box);
                setText(null);
            }
        });
        return col;
    }

    private TableColumn<TrabajoFX, String> colEstado() {
        TableColumn<TrabajoFX, String> col = new TableColumn<>("Estado");
        col.setCellValueFactory(c -> c.getValue().estadoProperty());
        col.setMaxWidth(160);
        col.setMinWidth(135);
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) {
                    setGraphic(null);
                    return;
                }
                Label badge = new Label(v);
                badge.getStyleClass().addAll("badge", "badge-" + v.toLowerCase());
                setGraphic(badge);
                setText(null);
            }
        });
        return col;
    }

    private TableColumn<TrabajoFX, String> colOperario() {
        TableColumn<TrabajoFX, String> col = new TableColumn<>("Operario");
        col.setCellValueFactory(c -> c.getValue().operarioProperty());
        col.setMinWidth(110);
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) {
                    setGraphic(null);
                    return;
                }
                if ("Sin asignar".equals(v)) {
                    Label l = new Label(v);
                    l.setStyle("-fx-text-fill: #94A3B8; -fx-font-style: italic; -fx-font-size: 12px;");
                    setGraphic(l);
                } else {
                    HBox box = new HBox(8);
                    box.setAlignment(Pos.CENTER_LEFT);
                    box.getChildren().addAll(miniAvatar(v), crearLabel(v, "#F8FAFC", 12.5));
                    setGraphic(box);
                }
                setText(null);
            }
        });
        return col;
    }

    private TableColumn<TrabajoFX, String> colFecha() {
        TableColumn<TrabajoFX, String> col = new TableColumn<>("Fecha");
        col.setCellValueFactory(c -> c.getValue().fechaProperty());
        col.setMaxWidth(105);
        return col;
    }

    private TableColumn<TrabajoFX, Void> colAcciones() {
        TableColumn<TrabajoFX, Void> col = new TableColumn<>("Acciones");
        col.setMaxWidth(80);
        col.setMinWidth(80);
        col.setSortable(false);
        col.setCellFactory(c -> new TableCell<>() {
            private final Button btnVer = actionBtn("👁", "Ver detalle / Gestionar");
            private final Button btnAsignar = actionBtn("👤", "Asignar operario");
            {
                btnVer.setOnAction(e -> mostrarDetalle(getTableView().getItems().get(getIndex())));
                btnAsignar.setOnAction(e -> abrirAsignar(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                TrabajoFX t = getTableView().getItems().get(getIndex());
                String estado = t.getEstado();
                btnAsignar.setDisable(!"ACEPTADO".equals(estado) && !"ASIGNADO".equals(estado));

                HBox box = new HBox(5);
                box.setAlignment(Pos.CENTER);
                box.getChildren().addAll(btnVer, btnAsignar);
                setGraphic(box);
            }
        });
        return col;
    }

    private void aplicarFiltro(String texto) {
        String q = texto == null ? "" : texto.toLowerCase().trim();
        trabajosFiltrados.setPredicate(t -> {
            boolean b = q.isEmpty() || t.getTitulo().toLowerCase().contains(q)
                    || t.getCliente().toLowerCase().contains(q)
                    || String.valueOf(t.getId()).contains(q);
            boolean f = switch (filtroActivo) {
                case "pendientes" -> "PENDIENTE".equals(t.getEstado());
                case "proceso" -> List.of("ASIGNADO", "PRESUPUESTADO", "ACEPTADO").contains(t.getEstado());
                case "finalizadas" -> List.of("FINALIZADO", "REALIZADO").contains(t.getEstado());
                default -> true;
            };
            return b && f;
        });
    }

    private void mostrarDetalle(TrabajoFX t) {
        boolean esPendiente = "PENDIENTE".equals(t.getEstado());
        Optional<DialogoDetalleIncidencia.Resultado> res = new DialogoDetalleIncidencia(
                t, cssUrl, esPendiente).mostrar();

        if (esPendiente) {
            res.ifPresent(datos -> callback.onPresupuestar(t, datos.monto(), datos.notas()));
        }
    }

    private void abrirAsignar(TrabajoFX t) {
        if (operarios.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "No hay operarios disponibles.").show();
            return;
        }
        Optional<Integer> res = new DialogoAsignarOperario(t, List.copyOf(operarios), cssUrl).mostrar();
        res.ifPresent(idOp -> callback.onAsignar(t, idOp));
    }

    // ─── Utils ────────────────────────────────────────────────────────────────

    private StackPane miniAvatar(String nombre) {
        StackPane av = new StackPane();
        av.setMinSize(24, 24);
        av.setMaxSize(24, 24);
        av.setStyle("-fx-background-color: rgba(249,115,22,0.2); -fx-background-radius: 12;");
        String[] p = nombre.trim().split("\\s+");
        String ini = p.length >= 2 ? ("" + p[0].charAt(0) + p[1].charAt(0)).toUpperCase()
                : nombre.substring(0, Math.min(2, nombre.length())).toUpperCase();
        Label l = new Label(ini);
        l.setStyle("-fx-text-fill: #F97316; -fx-font-size: 9px; -fx-font-weight: bold;");
        av.getChildren().add(l);
        return av;
    }

    private Button actionBtn(String texto, String tooltip) {
        Button btn = new Button(texto);
        btn.getStyleClass().add("action-btn");
        btn.setTooltip(new Tooltip(tooltip));
        return btn;
    }

    private Label iconoLabel(String texto) {
        Label l = new Label(texto);
        l.setStyle("-fx-text-fill: #94A3B8;");
        return l;
    }

    private Label textoMuted(String texto) {
        return crearLabel(texto, "#94A3B8", 12);
    }

    private Label crearLabel(String texto, String color, double size) {
        Label l = new Label(texto);
        l.setStyle("-fx-text-fill: " + color + "; -fx-font-size: " + size + "px;");
        return l;
    }

    private String iconoCategoria(String cat) {
        if (cat == null)
            return "🔧";
        return switch (cat.toUpperCase()) {
            case "ELECTRICIDAD" -> "⚡";
            case "FONTANERIA" -> "💧";
            case "CLIMATIZACION" -> "❄";
            case "PINTURA" -> "🖌";
            case "ALBANILERIA" -> "🧱";
            default -> "🔧";
        };
    }
}

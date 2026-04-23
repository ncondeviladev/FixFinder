package com.fixfinder.ui.dashboard.componentes;

import com.fixfinder.ui.dashboard.componentes.tabla.CabeceraTabla;
import com.fixfinder.ui.dashboard.componentes.tabla.celdas.*;
import com.fixfinder.ui.dashboard.modelos.OperarioFX;
import com.fixfinder.ui.dashboard.modelos.TrabajoFX;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Componente principal que orquesta la visualización de la tabla de incidencias.
 */
public class TablaIncidencias extends VBox {

    public interface AccionesCallback {
        void onAsignar(TrabajoFX trabajo, int idOperario);
        void onPresupuestar(TrabajoFX trabajo, double monto, String notas);
        void onRefresh();
    }

    private final FilteredList<TrabajoFX> trabajosFiltrados;
    private final ObservableList<OperarioFX> operarios;
    private final AccionesCallback callback;
    private final String cssUrl;
    private final int idEmpresa;

    public TablaIncidencias(FilteredList<TrabajoFX> trabajosFiltrados,
            ObservableList<OperarioFX> operarios,
            AccionesCallback callback,
            String cssUrl,
            int idEmpresa) {
        this.trabajosFiltrados = trabajosFiltrados;
        this.operarios = operarios;
        this.callback = callback;
        this.cssUrl = cssUrl;
        this.idEmpresa = idEmpresa;

        getStyleClass().add("tarjeta-tabla");
        VBox.setVgrow(this, Priority.ALWAYS);

        inicializarComponentes();
    }

    private void inicializarComponentes() {
        CabeceraTabla cabecera = new CabeceraTabla(this::procesarCambioFiltro);
        TableView<TrabajoFX> tabla = configurarTabla();
        getChildren().addAll(cabecera, tabla);
    }

    @SuppressWarnings("unchecked")
    private TableView<TrabajoFX> configurarTabla() {
        TableView<TrabajoFX> tabla = new TableView<>(trabajosFiltrados);
        tabla.getStyleClass().add("table-view");
        tabla.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        tabla.setPlaceholder(new Label("No hay incidencias para mostrar"));
        VBox.setVgrow(tabla, Priority.ALWAYS);

        tabla.getColumns().addAll(
                crearColumnaAcciones(),
                crearColumnaTitulo(),
                crearColumnaCliente(),
                crearColumnaCategoria(),
                crearColumnaEstado(),
                crearColumnaOperario(),
                crearColumnaFecha());

        return tabla;
    }

    private TableColumn<TrabajoFX, Void> crearColumnaAcciones() {
        TableColumn<TrabajoFX, Void> col = new TableColumn<>("Acciones");
        col.setMinWidth(80);
        col.setMaxWidth(80);
        col.setSortable(false);
        col.setCellFactory(c -> new CeldaAcciones(callback, operarios, cssUrl, idEmpresa));
        return col;
    }


    private TableColumn<TrabajoFX, String> crearColumnaTitulo() {
        TableColumn<TrabajoFX, String> col = new TableColumn<>("Título");
        col.setCellValueFactory(c -> c.getValue().tituloProperty());
        col.setMinWidth(150);
        return col;
    }

    private TableColumn<TrabajoFX, String> crearColumnaCliente() {
        TableColumn<TrabajoFX, String> col = new TableColumn<>("Cliente");
        col.setCellValueFactory(c -> c.getValue().clienteProperty());
        col.setMinWidth(110);
        col.setCellFactory(c -> new CeldaCliente(cssUrl));
        return col;
    }

    private TableColumn<TrabajoFX, String> crearColumnaCategoria() {
        TableColumn<TrabajoFX, String> col = new TableColumn<>("Categoría");
        col.setCellValueFactory(c -> c.getValue().categoriaProperty());
        col.setMinWidth(130);
        col.setCellFactory(c -> new CeldaCategoria());
        return col;
    }

    private TableColumn<TrabajoFX, String> crearColumnaEstado() {
        TableColumn<TrabajoFX, String> col = new TableColumn<>("Estado");
        col.setCellValueFactory(c -> c.getValue().estadoProperty());
        col.setMinWidth(165);
        col.setCellFactory(c -> new CeldaEstadoBadge(idEmpresa));
        return col;
    }

    private TableColumn<TrabajoFX, String> crearColumnaOperario() {
        TableColumn<TrabajoFX, String> col = new TableColumn<>("Operario");
        col.setCellValueFactory(c -> c.getValue().operarioProperty());
        col.setMinWidth(110);
        col.setCellFactory(c -> new CeldaOperario());
        return col;
    }

    private TableColumn<TrabajoFX, String> crearColumnaFecha() {
        TableColumn<TrabajoFX, String> col = new TableColumn<>("Fecha");
        col.setCellValueFactory(c -> c.getValue().fechaProperty());
        col.setMinWidth(130);
        col.setMaxWidth(150);
        col.setCellFactory(c -> new CeldaFecha());
        return col;
    }

    private void procesarCambioFiltro(String textoBusqueda, String filtroTab) {
        String query = textoBusqueda == null ? "" : textoBusqueda.toLowerCase().trim();
        trabajosFiltrados.setPredicate(trabajo -> {
            boolean coincideTexto = query.isEmpty()
                    || trabajo.getTitulo().toLowerCase().contains(query)
                    || trabajo.getCliente().toLowerCase().contains(query)
                    || String.valueOf(trabajo.getId()).contains(query);

            boolean coincideTab = switch (filtroTab) {
                case "pendientes" -> "PENDIENTE".equals(trabajo.getEstado());
                case "proceso" -> List.of("ASIGNADO", "PRESUPUESTADO", "ACEPTADO").contains(trabajo.getEstado());
                case "finalizadas" -> List.of("FINALIZADO", "REALIZADO").contains(trabajo.getEstado());
                default -> true;
            };
            return coincideTexto && coincideTab;
        });
    }
}

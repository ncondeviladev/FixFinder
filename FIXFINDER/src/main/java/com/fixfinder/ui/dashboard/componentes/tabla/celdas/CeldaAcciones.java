package com.fixfinder.ui.dashboard.componentes.tabla.celdas;

import com.fixfinder.ui.dashboard.componentes.TablaIncidencias;
import com.fixfinder.ui.dashboard.dialogos.DialogoAsignarOperario;
import com.fixfinder.ui.dashboard.dialogos.DialogoDetalleIncidencia;
import com.fixfinder.ui.dashboard.modelos.OperarioFX;
import com.fixfinder.ui.dashboard.modelos.TrabajoFX;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;

import java.util.List;
import java.util.Optional;

/**
 * Celda compleja que contiene los botones de acción (Ver y Asignar) para cada incidencia.
 * Gestiona el estado de los botones (habilitado/deshabilitado) según el estado del trabajo
 * y lanza los diálogos correspondientes.
 */
public class CeldaAcciones extends TableCell<TrabajoFX, Void> {

    private final Button btnVer;
    private final Button btnAsignar;
    private final HBox contenedor;
    
    private final TablaIncidencias.AccionesCallback callback;
    private final ObservableList<OperarioFX> operarios;
    private final String cssUrl;

    public CeldaAcciones(TablaIncidencias.AccionesCallback callback, 
                        ObservableList<OperarioFX> operarios, 
                        String cssUrl) {
        this.callback = callback;
        this.operarios = operarios;
        this.cssUrl = cssUrl;

        this.btnVer = crearBotonAccion("👁", "Ver detalle / Gestionar");
        this.btnAsignar = crearBotonAccion("👤", "Asignar operario");
        
        this.btnVer.setOnAction(e -> manejarVerDetalle());
        this.btnAsignar.setOnAction(e -> manejarAsignarOperario());

        this.contenedor = new HBox(5);
        this.contenedor.setAlignment(Pos.CENTER);
        this.contenedor.getChildren().addAll(btnVer, btnAsignar);
    }

    @Override
    protected void updateItem(Void item, boolean vacio) {
        super.updateItem(item, vacio);

        if (vacio) {
            setGraphic(null);
            return;
        }

        TrabajoFX trabajo = getTableView().getItems().get(getIndex());
        String estado = trabajo.getEstado();

        // Lógica de negocio: solo se puede asignar si está ACEPTADO o ASIGNADO (para re-asignar)
        btnAsignar.setDisable(!"ACEPTADO".equals(estado) && !"ASIGNADO".equals(estado));

        setGraphic(contenedor);
    }

    private void manejarVerDetalle() {
        TrabajoFX trabajo = getTableView().getItems().get(getIndex());
        boolean esPendiente = "PENDIENTE".equals(trabajo.getEstado());
        
        Optional<DialogoDetalleIncidencia.Resultado> resultado = 
            new DialogoDetalleIncidencia(trabajo, cssUrl, esPendiente).mostrar();

        if (esPendiente) {
            resultado.ifPresent(datos -> 
                callback.onPresupuestar(trabajo, datos.monto(), datos.notas()));
        }
    }

    private void manejarAsignarOperario() {
        TrabajoFX trabajo = getTableView().getItems().get(getIndex());
        
        if (operarios.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "No hay operarios disponibles.").show();
            return;
        }
        
        Optional<Integer> resultado = 
            new DialogoAsignarOperario(trabajo, List.copyOf(operarios), cssUrl).mostrar();
            
        resultado.ifPresent(idOp -> callback.onAsignar(trabajo, idOp));
    }

    private Button crearBotonAccion(String texto, String tooltipTxt) {
        Button btn = new Button(texto);
        btn.getStyleClass().add("action-btn");
        btn.setTooltip(new Tooltip(tooltipTxt));
        return btn;
    }
}

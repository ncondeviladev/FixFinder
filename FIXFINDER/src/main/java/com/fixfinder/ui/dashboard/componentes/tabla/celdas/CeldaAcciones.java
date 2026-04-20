package com.fixfinder.ui.dashboard.componentes.tabla.celdas;

import com.fixfinder.ui.dashboard.componentes.TablaIncidencias;
import com.fixfinder.ui.dashboard.dialogos.DialogoGestionIncidencia;
import com.fixfinder.ui.dashboard.modelos.OperarioFX;
import com.fixfinder.ui.dashboard.modelos.TrabajoFX;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import java.util.Optional;

/**
 * Celda compleja que contiene los botones de acción para cada incidencia.
 * Ahora unificada para usar el DialogoGestionIncidencia modular.
 */
public class CeldaAcciones extends TableCell<TrabajoFX, Void> {

    private final Button btnVer;
    private final HBox contenedor;

    private final TablaIncidencias.AccionesCallback callback;
    private final ObservableList<OperarioFX> operarios;
    private final String cssUrl;
    private final int idEmpresaLogueada;

    public CeldaAcciones(TablaIncidencias.AccionesCallback callback,
            ObservableList<OperarioFX> operarios,
            String cssUrl,
            int idEmpresa) {
        this.callback = callback;
        this.operarios = operarios;
        this.cssUrl = cssUrl;
        this.idEmpresaLogueada = idEmpresa;

        this.btnVer = crearBotonAccion("👁", "Ver detalle / Gestionar");

        this.btnVer.setOnAction(e -> abrirGestionIncidencia());

        this.contenedor = new HBox(5);
        this.contenedor.setAlignment(Pos.CENTER);
        this.contenedor.getChildren().addAll(btnVer);
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

        if ("PENDIENTE".equals(estado) || "PRESUPUESTADO".equals(estado)) {
            if (trabajo.fueRechazado(idEmpresaLogueada)) {
                btnVer.setText("🔄");
                btnVer.setTooltip(new Tooltip("Re-pujar / Nueva oferta (Rechazada)"));
            } else if (trabajo.haPresupuestado(idEmpresaLogueada)) {
                btnVer.setText("✅");
                btnVer.setTooltip(new Tooltip("Oferta enviada (En espera)"));
            } else {
                btnVer.setText("💰");
                btnVer.setTooltip(new Tooltip("Presentar Presupuesto"));
            }
        } else {
            btnVer.setText("👁");
            btnVer.setTooltip(new Tooltip("Ver ficha gestión"));
        }

        setGraphic(contenedor);
    }

    private void abrirGestionIncidencia() {
        TrabajoFX trabajo = getTableView().getItems().get(getIndex());
        String estado = trabajo.getEstado();
        
        // Es presupuestable si está en fase de puja y: No he pujado O fue rechazado
        boolean esPresupuestable = ("PENDIENTE".equals(estado) || "PRESUPUESTADO".equals(estado))
                && (!trabajo.haPresupuestado(idEmpresaLogueada) || trabajo.fueRechazado(idEmpresaLogueada));

        Optional<DialogoGestionIncidencia.Resultado> resultado = new DialogoGestionIncidencia(
                trabajo, cssUrl, idEmpresaLogueada, operarios, esPresupuestable).mostrar();

        resultado.ifPresent(datos -> {
            if (datos.idOperario() != null) {
                callback.onAsignar(trabajo, datos.idOperario());
            } else if (datos.monto() != null) {
                callback.onPresupuestar(trabajo, datos.monto(), datos.notas());
            }
        });
    }

    private Button crearBotonAccion(String texto, String tooltipTxt) {
        Button btn = new Button(texto);
        btn.getStyleClass().add("btn-accion");
        btn.setTooltip(new Tooltip(tooltipTxt));
        return btn;
    }
}

package com.fixfinder.ui.dashboard.componentes.tabla.celdas;

import com.fixfinder.ui.dashboard.modelos.TrabajoFX;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;

/**
 * Celda personalizada que renderiza el estado de un trabajo dentro de un "Badge".
 * Muestra el indicador » si la empresa ya ha presupuestado.
 */
public class CeldaEstadoBadge extends TableCell<TrabajoFX, String> {

    private final int idEmpresaLogueada;

    public CeldaEstadoBadge(int idEmpresaLogueada) {
        this.idEmpresaLogueada = idEmpresaLogueada;
    }

    @Override
    protected void updateItem(String estado, boolean vacio) {
        super.updateItem(estado, vacio);

        if (vacio || estado == null) {
            setGraphic(null);
            return;
        }

        TrabajoFX trabajo = getTableView().getItems().get(getIndex());
        String textoEstado = estado;
        String extraClass = "insignia-" + estado.toLowerCase();

        // 1. Prioridad: Si mi presupuesto fue RECHAZADO
        if (trabajo.fueRechazado(idEmpresaLogueada)) {
            textoEstado = "✘ RECHAZADO";
            extraClass = "insignia-cancelado"; // Usar el estilo rojo
        } 
        // 2. Si tengo un presupuesto ACTIVO (Pendiente/Aceptado)
        else if (trabajo.haPresupuestado(idEmpresaLogueada)) {
            textoEstado = estado;
        }

        Label badge = new Label(textoEstado);
        badge.getStyleClass().addAll("insignia", extraClass);

        setGraphic(badge);
        setText(null);
    }
}

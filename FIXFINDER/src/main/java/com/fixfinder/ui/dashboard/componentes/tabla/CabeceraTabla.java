package com.fixfinder.ui.dashboard.componentes.tabla;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.function.BiConsumer;

/**
 * Componente que representa la parte superior de la tabla de incidencias.
 * Incluye el título, el buscador global y las pestañas de filtrado rápido.
 */
public class CabeceraTabla extends VBox {

    private final TextField txtBusqueda;
    private String filtroActivo = "todas";
    private final BiConsumer<String, String> alCambiarFiltro;

    /**
     * @param alCambiarFiltro Callback que recibe (textoBusqueda, idFiltroTab).
     */
    public CabeceraTabla(BiConsumer<String, String> alCambiarFiltro) {
        super(10);
        this.alCambiarFiltro = alCambiarFiltro;
        this.txtBusqueda = new TextField();
        
        getStyleClass().add("cabecera-tarjeta");
        construir();
    }

    private void construir() {
        // Fila 1: Título y Buscador
        HBox filaSuperior = new HBox(12);
        filaSuperior.setAlignment(Pos.CENTER_LEFT);
        
        Label lblTitulo = new Label("Incidencias");
        lblTitulo.getStyleClass().add("titulo-tarjeta");
        HBox.setHgrow(lblTitulo, Priority.ALWAYS);

        txtBusqueda.setPromptText("⌕  Buscar por ID, Cliente o Título...");
        txtBusqueda.getStyleClass().add("campo-busqueda");
        txtBusqueda.textProperty().addListener((obs, viejo, nuevo) -> 
            alCambiarFiltro.accept(nuevo, filtroActivo));
        
        filaSuperior.getChildren().addAll(lblTitulo, txtBusqueda);

        // Fila 2: Pestañas de filtrado (Tabs)
        HBox filaTabs = construirTabs();
        
        getChildren().addAll(filaSuperior, filaTabs);
    }

    private HBox construirTabs() {
        HBox tabs = new HBox(4);
        tabs.getStyleClass().add("pestanas-filtro");
        tabs.setAlignment(Pos.CENTER_LEFT);

        ToggleGroup grupo = new ToggleGroup();
        String[][] configuracion = { 
            { "todas", "Todas" }, 
            { "pendientes", "Pendientes" },
            { "proceso", "En proceso" }, 
            { "finalizadas", "Finalizadas" } 
        };

        for (String[] op : configuracion) {
            ToggleButton btn = new ToggleButton(op[1]);
            btn.getStyleClass().add("pestana-filtro");
            btn.setToggleGroup(grupo);
            
            if ("todas".equals(op[0])) {
                btn.setSelected(true);
                btn.getStyleClass().add("seleccionada");
            }
            
            String idFiltro = op[0];
            btn.selectedProperty().addListener((obs, eraSeleccionado, esSeleccionadoAhora) -> {
                if (esSeleccionadoAhora) {
                    btn.getStyleClass().add("seleccionada");
                    this.filtroActivo = idFiltro;
                    alCambiarFiltro.accept(txtBusqueda.getText(), idFiltro);
                } else {
                    btn.getStyleClass().remove("seleccionada");
                }
            });
            
            tabs.getChildren().add(btn);
        }
        return tabs;
    }
    
    public String getTextoBusqueda() {
        return txtBusqueda.getText();
    }
}

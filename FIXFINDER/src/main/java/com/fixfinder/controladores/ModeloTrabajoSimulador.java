package com.fixfinder.controladores;

import com.fasterxml.jackson.databind.JsonNode;
import javafx.beans.property.SimpleStringProperty;

/**
 * Modelo de datos para representar un trabajo dentro de la tabla del Simulador.
 * Utiliza propiedades de JavaFX para permitir el binding automático con la UI.
 */
public class ModeloTrabajoSimulador {
    private final int id;
    private final SimpleStringProperty titulo;
    private final SimpleStringProperty cliente;
    private final SimpleStringProperty estado;
    private final SimpleStringProperty operario;
    private final boolean tienePresupuestoAceptado;

    public ModeloTrabajoSimulador(JsonNode nodo) {
        this.id = nodo.get("id").asInt();
        this.titulo = new SimpleStringProperty(nodo.get("titulo").asText());
        this.estado = new SimpleStringProperty(nodo.get("estado").asText());
        this.tienePresupuestoAceptado = nodo.has("tienePresupuestoAceptado")
                && nodo.get("tienePresupuestoAceptado").asBoolean();

        if (nodo.has("cliente") && !nodo.get("cliente").isNull()) {
            JsonNode c = nodo.get("cliente");
            this.cliente = new SimpleStringProperty(
                    c.has("nombre") ? c.get("nombre").asText() : "ID: " + c.get("id").asInt());
        } else {
            this.cliente = new SimpleStringProperty("Desconocido");
        }

        if (nodo.has("operarioAsignado") && !nodo.get("operarioAsignado").isNull()) {
            JsonNode o = nodo.get("operarioAsignado");
            this.operario = new SimpleStringProperty(
                    o.has("nombre") ? o.get("nombre").asText() : "ID: " + o.get("id").asInt());
        } else {
            this.operario = new SimpleStringProperty("Sin asignar");
        }
    }

    public int getId() { return id; }
    public String getTitulo() { return titulo.get(); }
    public String getCliente() { return cliente.get(); }
    public String getEstado() { return estado.get(); }
    public String getOperario() { return operario.get(); }
    public boolean isTienePresupuestoAceptado() { return tienePresupuestoAceptado; }
}

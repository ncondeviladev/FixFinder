package com.fixfinder.ui.dashboard.modelos;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Modelo de datos reactivo (JavaFX Properties) para representar un Trabajo en
 * la UI del Dashboard.
 * Permite el binding directo con TableView y otros controles JavaFX.
 * Mantiene la lista de URLs de las incidencias subidas desde Firebase.
 */
public class TrabajoFX {

    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty titulo = new SimpleStringProperty();
    private final StringProperty cliente = new SimpleStringProperty();
    private final StringProperty categoria = new SimpleStringProperty();
    private final StringProperty estado = new SimpleStringProperty();
    private final StringProperty operario = new SimpleStringProperty();
    private final StringProperty fecha = new SimpleStringProperty();
    private final StringProperty descripcion = new SimpleStringProperty();
    private final StringProperty direccion = new SimpleStringProperty();
    private final IntegerProperty idOperario = new SimpleIntegerProperty(-1);

    private final StringProperty clienteTelefono = new SimpleStringProperty("");
    private final StringProperty clienteEmail = new SimpleStringProperty("");
    private final IntegerProperty valoracion = new SimpleIntegerProperty(0);
    private final StringProperty comentarioCliente = new SimpleStringProperty("");

    private final ObservableList<String> urlsFotos = FXCollections.observableArrayList();

    public TrabajoFX(int id, String titulo, String cliente, String categoria,
            String estado, String operario, String fecha,
            String descripcion, String direccion, int idOperario) {
        this.id.set(id);
        this.titulo.set(titulo);
        this.cliente.set(cliente);
        this.categoria.set(categoria != null ? categoria : "OTROS");
        this.estado.set(estado);
        this.operario.set(operario != null && !operario.isBlank() ? operario : "Sin asignar");
        this.fecha.set(fecha);
        this.descripcion.set(descripcion != null ? descripcion : "");
        this.direccion.set(direccion != null ? direccion : "");
        this.idOperario.set(idOperario);
    }

    public TrabajoFX(int id, String titulo, String cliente, String categoria,
            String estado, String operario, String fecha,
            String descripcion, String direccion, int idOperario,
            String clienteTelefono, String clienteEmail, int valoracion, String comentarioCliente) {
        this(id, titulo, cliente, categoria, estado, operario, fecha, descripcion, direccion, idOperario);
        this.clienteTelefono.set(clienteTelefono != null ? clienteTelefono : "");
        this.clienteEmail.set(clienteEmail != null ? clienteEmail : "");
        this.valoracion.set(valoracion);
        this.comentarioCliente.set(comentarioCliente != null ? comentarioCliente : "");
    }

    public int getId() {
        return id.get();
    }

    public IntegerProperty idProperty() {
        return id;
    }

    public String getTitulo() {
        return titulo.get();
    }

    public StringProperty tituloProperty() {
        return titulo;
    }

    public String getCliente() {
        return cliente.get();
    }

    public StringProperty clienteProperty() {
        return cliente;
    }

    public String getCategoria() {
        return categoria.get();
    }

    public StringProperty categoriaProperty() {
        return categoria;
    }

    public String getEstado() {
        return estado.get();
    }

    public StringProperty estadoProperty() {
        return estado;
    }

    public String getOperario() {
        return operario.get();
    }

    public StringProperty operarioProperty() {
        return operario;
    }

    public String getFecha() {
        return fecha.get();
    }

    public StringProperty fechaProperty() {
        return fecha;
    }

    public String getDescripcion() {
        return descripcion.get();
    }

    public String getDireccion() {
        return direccion.get();
    }

    public int getIdOperario() {
        return idOperario.get();
    }

    public String getClienteTelefono() {
        return clienteTelefono.get();
    }

    public String getClienteEmail() {
        return clienteEmail.get();
    }

    public int getValoracion() {
        return valoracion.get();
    }

    public IntegerProperty valoracionProperty() {
        return valoracion;
    }

    public String getComentarioCliente() {
        return comentarioCliente.get();
    }

    public StringProperty comentarioClienteProperty() {
        return comentarioCliente;
    }

    public ObservableList<String> getUrlsFotos() {
        return urlsFotos;
    }

    public void setUrlsFotos(java.util.List<String> fotos) {
        this.urlsFotos.setAll(fotos);
    }
}

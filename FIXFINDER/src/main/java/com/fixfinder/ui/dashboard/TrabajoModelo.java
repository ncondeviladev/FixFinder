package com.fixfinder.ui.dashboard;

/**
 * Modelo para la representación visual de un Trabajo en la tabla del Dashboard.
 */
public class TrabajoModelo {
    private final int id;
    private final String titulo;
    private final String nombreCliente;
    private final String nombreOperario;
    private final String estado;
    private final String fecha;
    private final String descripcion;
    private final String direccion;
    private final String categoria;
    private final String urlFotoCliente;

    public TrabajoModelo(int id, String titulo, String nombreCliente, String nombreOperario, String estado,
            String fecha, String descripcion, String direccion, String categoria, String urlFotoCliente) {
        this.id = id;
        this.titulo = titulo;
        this.nombreCliente = nombreCliente;
        this.nombreOperario = nombreOperario;
        this.estado = estado;
        this.fecha = fecha;
        this.descripcion = descripcion;
        this.direccion = direccion;
        this.categoria = categoria;
        this.urlFotoCliente = urlFotoCliente;
    }

    public int getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getNombreCliente() { return nombreCliente; }
    public String getNombreOperario() { return nombreOperario; }
    public String getEstado() { return estado; }
    public String getFecha() { return fecha; }
    public String getDescripcion() { return descripcion; }
    public String getDireccion() { return direccion; }
    public String getCategoria() { return categoria; }
    public String getUrlFotoCliente() { return urlFotoCliente; }
}

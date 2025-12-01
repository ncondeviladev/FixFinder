package com.fixfinder.modelos;

import com.fixfinder.modelos.enums.EstadoTrabajo;
import java.time.LocalDateTime;

/**
 * Representa una solicitud de servicio o trabajo.
 *
 * Es la entidad central del sistema que vincula:
 * - Un {@link Usuario} (Cliente).
 * - Un {@link Operario} (Técnico asignado).
 * - Una {@link Categoria} de servicio.
 * - Una {@link Ubicacion} y dirección.
 */
public class Trabajo {
    private int id;
    private Usuario cliente;
    private Operario operarioAsignado;
    private Categoria categoria;
    private String titulo;
    private String descripcion;
    private Ubicacion ubicacion;
    private String direccion;
    private EstadoTrabajo estado;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaFinalizacion;

    public Trabajo() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Usuario getCliente() {
        return cliente;
    }

    public void setCliente(Usuario cliente) {
        this.cliente = cliente;
    }

    public Operario getOperarioAsignado() {
        return operarioAsignado;
    }

    public void setOperarioAsignado(Operario operarioAsignado) {
        this.operarioAsignado = operarioAsignado;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Ubicacion getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(Ubicacion ubicacion) {
        this.ubicacion = ubicacion;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public EstadoTrabajo getEstado() {
        return estado;
    }

    public void setEstado(EstadoTrabajo estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaFinalizacion() {
        return fechaFinalizacion;
    }

    public void setFechaFinalizacion(LocalDateTime fechaFinalizacion) {
        this.fechaFinalizacion = fechaFinalizacion;
    }
}

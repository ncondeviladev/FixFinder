package com.fixfinder.modelos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fixfinder.modelos.componentes.FotoTrabajo;
import com.fixfinder.modelos.componentes.Ubicacion;
import com.fixfinder.modelos.enums.CategoriaServicio;
import com.fixfinder.modelos.enums.EstadoTrabajo;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa una solicitud de servicio o trabajo.
 *
 * Es la entidad central del sistema que vincula:
 * - Un {@link Usuario} (Cliente).
 * - Un {@link Operario} (Técnico asignado).
 * - Una {@link CategoriaServicio} de servicio.
 * - Una {@link Ubicacion} y dirección.
 */
@Entity
@Table(name = "trabajo")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Trabajo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente")
    private Usuario cliente;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_operario_asignado")
    private Operario operarioAsignado;
    
    @Enumerated(EnumType.STRING)
    private CategoriaServicio categoria;
    
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;
    
    @Embedded
    private Ubicacion ubicacion;
    
    private String direccion;
    
    @Enumerated(EnumType.STRING)
    private EstadoTrabajo estado;
    
    @Column(name = "fecha_creacion", insertable = false, updatable = false)
    private LocalDateTime fechaCreacion;
    
    @Column(name = "fecha_finalizacion")
    private LocalDateTime fechaFinalizacion;

    // Componentes nuevos
    private int valoracion; // 0-5
    
    @Column(name = "comentario_cliente", columnDefinition = "TEXT")
    private String comentarioCliente;
    
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_trabajo")
    private List<FotoTrabajo> fotos = new ArrayList<>();

    public Trabajo() {
    }

    public int getValoracion() {
        return valoracion;
    }

    public void setValoracion(int valoracion) {
        this.valoracion = valoracion;
    }

    public String getComentarioCliente() {
        return comentarioCliente;
    }

    public void setComentarioCliente(String comentarioCliente) {
        this.comentarioCliente = comentarioCliente;
    }

    public List<FotoTrabajo> getFotos() {
        return fotos;
    }

    public void setFotos(List<FotoTrabajo> fotos) {
        this.fotos = fotos;
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

    public CategoriaServicio getCategoria() {
        return categoria;
    }

    public void setCategoria(CategoriaServicio categoria) {
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

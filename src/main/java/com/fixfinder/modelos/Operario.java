package com.fixfinder.modelos;

import com.fixfinder.modelos.enums.CategoriaServicio;
import com.fixfinder.modelos.enums.Rol;

import java.time.LocalDateTime;

/**
 * Representa a un técnico u operario que realiza los trabajos.
 *
 * Extiende de {@link Usuario} e incluye datos laborales.
 */
public class Operario extends Usuario {

    // Vinculación laboral
    private int idEmpresa;

    // Datos profesionales específicos
    private CategoriaServicio especialidad;
    private boolean estaActivo; // Representa estado DISPONIBLE/OCUPADO
    private double latitud;
    private double longitud;
    private LocalDateTime ultimaActualizacion;

    public Operario() {
        super();
    }

    public Operario(int id, String email, String passwordHash, Rol rol, String nombreCompleto, String dni,
            int idEmpresa, CategoriaServicio especialidad, boolean estaActivo) {
        super(id, email, passwordHash, rol, nombreCompleto, dni);
        this.idEmpresa = idEmpresa;
        this.especialidad = especialidad;
        this.estaActivo = estaActivo;
    }

    public int getIdEmpresa() {
        return idEmpresa;
    }

    public void setIdEmpresa(int idEmpresa) {
        this.idEmpresa = idEmpresa;
    }

    public CategoriaServicio getEspecialidad() {
        return especialidad;
    }

    public void setEspecialidad(CategoriaServicio especialidad) {
        this.especialidad = especialidad;
    }

    public boolean isEstaActivo() {
        return estaActivo;
    }

    public void setEstaActivo(boolean estaActivo) {
        this.estaActivo = estaActivo;
    }

    public double getLatitud() {
        return latitud;
    }

    public void setLatitud(double latitud) {
        this.latitud = latitud;
    }

    public double getLongitud() {
        return longitud;
    }

    public void setLongitud(double longitud) {
        this.longitud = longitud;
    }

    public LocalDateTime getUltimaActualizacion() {
        return ultimaActualizacion;
    }

    public void setUltimaActualizacion(LocalDateTime ultimaActualizacion) {
        this.ultimaActualizacion = ultimaActualizacion;
    }
}

package com.fixfinder.modelos;

import java.time.LocalDateTime;

/**
 * Representa a un técnico u operario que realiza los trabajos.
 *
 * Extiende de {@link Usuario} añadiendo información específica profesional
 * y de geolocalización para el seguimiento en tiempo real.
 */
public class Operario extends Usuario {
    private String dni;
    private String especialidad;
    private boolean estaActivo; // Representa estado DISPONIBLE/OCUPADO simplificado o mapeado
    private double latitud;
    private double longitud;
    private LocalDateTime ultimaActualizacion;

    public Operario() {
        super();
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public String getEspecialidad() {
        return especialidad;
    }

    public void setEspecialidad(String especialidad) {
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

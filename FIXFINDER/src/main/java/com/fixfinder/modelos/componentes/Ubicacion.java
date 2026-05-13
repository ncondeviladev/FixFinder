package com.fixfinder.modelos.componentes;

import jakarta.persistence.Embeddable;

/**
 * Clase auxiliar para manejar coordenadas geográficas (Latitud/Longitud).
 */
@Embeddable
public class Ubicacion {
    private double latitud;
    private double longitud;

    public Ubicacion() {
    }

    public Ubicacion(double latitud, double longitud) {
        this.latitud = latitud;
        this.longitud = longitud;
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

    @Override
    public String toString() {
        return "Ubicacion{" +
                "latitud=" + latitud +
                ", longitud=" + longitud +
                '}';
    }
}

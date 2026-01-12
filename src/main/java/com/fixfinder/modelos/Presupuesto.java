package com.fixfinder.modelos;

import com.fixfinder.modelos.enums.EstadoPresupuesto;
import java.time.LocalDateTime;

/**
 * Representa una oferta económica previa a la realización del trabajo.
 */
public class Presupuesto {
    private int id;
    private Trabajo trabajo;
    private Empresa empresa;
    private double monto;
    private LocalDateTime fechaEnvio;
    private EstadoPresupuesto estado;

    public Presupuesto() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Trabajo getTrabajo() {
        return trabajo;
    }

    public void setTrabajo(Trabajo trabajo) {
        this.trabajo = trabajo;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public double getMonto() {
        return monto;
    }

    public void setMonto(double monto) {
        this.monto = monto;
    }

    public LocalDateTime getFechaEnvio() {
        return fechaEnvio;
    }

    public void setFechaEnvio(LocalDateTime fechaEnvio) {
        this.fechaEnvio = fechaEnvio;
    }

    public EstadoPresupuesto getEstado() {
        return estado;
    }

    public void setEstado(EstadoPresupuesto estado) {
        this.estado = estado;
    }

    // Campo informativo (No persistido en BD actual, pero útil para el cliente)
    private String notas = "El precio es orientativo y puede estar sujeto a modificaciones por material o reparación.";

    public String getNotas() {
        return notas;
    }

    public void setNotas(String notas) {
        this.notas = notas;
    }
}

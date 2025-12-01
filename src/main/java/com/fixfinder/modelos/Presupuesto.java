package com.fixfinder.modelos;

import java.util.Date;

/**
 * Representa una oferta económica previa a la realización del trabajo.
 *
 * (Opcional en el flujo básico, pero útil para ampliaciones).
 */
public class Presupuesto {
    private int id;
    private Trabajo trabajo;
    private Empresa empresa;
    private double monto;
    private Date fechaEnvio;

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

    public Date getFechaEnvio() {
        return fechaEnvio;
    }

    public void setFechaEnvio(Date fechaEnvio) {
        this.fechaEnvio = fechaEnvio;
    }
}

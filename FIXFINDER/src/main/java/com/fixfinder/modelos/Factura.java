package com.fixfinder.modelos;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Representa el documento final de cobro asociado a un trabajo finalizado.
 *
 * Contiene los cálculos económicos (Base, IVA, Total) y la referencia
 * al archivo PDF generado físicamente en el servidor.
 */
@Entity
@Table(name = "factura")
public class Factura {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_trabajo")
    private Trabajo trabajo;
    
    @Column(name = "numero_factura", unique = true)
    private String numeroFactura;
    @Column(name = "base_imponible")
    private double baseImponible;
    private double iva;
    private double total;
    
    @Column(name = "fecha_emision", insertable = false, updatable = false)
    private LocalDateTime fechaEmision;
    
    @Column(name = "ruta_pdf")
    private String rutaPdf;
    
    private boolean pagada;

    public Factura() {
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

    public String getNumeroFactura() {
        return numeroFactura;
    }

    public void setNumeroFactura(String numeroFactura) {
        this.numeroFactura = numeroFactura;
    }

    public double getBaseImponible() {
        return baseImponible;
    }

    public void setBaseImponible(double baseImponible) {
        this.baseImponible = baseImponible;
    }

    public double getIva() {
        return iva;
    }

    public void setIva(double iva) {
        this.iva = iva;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public LocalDateTime getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(LocalDateTime fechaEmision) {
        this.fechaEmision = fechaEmision;
    }

    public String getRutaPdf() {
        return rutaPdf;
    }

    public void setRutaPdf(String rutaPdf) {
        this.rutaPdf = rutaPdf;
    }

    public boolean isPagada() {
        return pagada;
    }

    public void setPagada(boolean pagada) {
        this.pagada = pagada;
    }
}

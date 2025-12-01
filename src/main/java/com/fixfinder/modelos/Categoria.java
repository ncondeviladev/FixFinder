package com.fixfinder.modelos;

/**
 * Define el tipo de servicio (Fontanería, Electricidad, etc.).
 *
 * Incluye información visual (icono) y base para presupuestos (precio hora).
 */
public class Categoria {
    private int id;
    private String nombre;
    private String iconoUrl;
    private String descripcion;
    private double precioHoraBase;

    public Categoria() {
    }

    public Categoria(int id, String nombre, String iconoUrl) {
        this.id = id;
        this.nombre = nombre;
        this.iconoUrl = iconoUrl;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getIconoUrl() {
        return iconoUrl;
    }

    public void setIconoUrl(String iconoUrl) {
        this.iconoUrl = iconoUrl;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public double getPrecioHoraBase() {
        return precioHoraBase;
    }

    public void setPrecioHoraBase(double precioHoraBase) {
        this.precioHoraBase = precioHoraBase;
    }
}

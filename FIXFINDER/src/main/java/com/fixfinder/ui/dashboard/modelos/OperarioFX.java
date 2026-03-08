package com.fixfinder.ui.dashboard.modelos;

/**
 * Modelo POJO para representar un Operario en la UI del Dashboard.
 * Contiene todos los campos necesarios para mostrar el estado del operario en
 * la tabla.
 */
public class OperarioFX {

    private final int id;
    private final String nombre;
    private final String especialidad;
    private final boolean disponible;
    private final int cargaTrabajo;
    private final boolean activo;
    private final String email;
    private final String telefono;
    private final String dni;

    public OperarioFX(int id, String nombre, String especialidad,
            boolean disponible, int cargaTrabajo, boolean activo, String email, String telefono, String dni) {
        this.id = id;
        this.nombre = nombre;
        this.especialidad = especialidad != null ? especialidad : "";
        this.disponible = disponible;
        this.cargaTrabajo = cargaTrabajo;
        this.activo = activo;
        this.email = email;
        this.telefono = telefono;
        this.dni = dni;
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getEspecialidad() {
        return especialidad;
    }

    public boolean isDisponible() {
        return disponible;
    }

    public int getCargaTrabajo() {
        return cargaTrabajo;
    }

    public boolean isActivo() {
        return activo;
    }

    public String getEmail() {
        return email;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getDni() {
        return dni;
    }

    public String getIniciales() {
        String[] partes = nombre.trim().split("\\s+");
        if (partes.length >= 2) {
            return ("" + partes[0].charAt(0) + partes[1].charAt(0)).toUpperCase();
        }
        return nombre.substring(0, Math.min(2, nombre.length())).toUpperCase();
    }

    @Override
    public String toString() {
        return nombre + " (" + especialidad + ")";
    }
}

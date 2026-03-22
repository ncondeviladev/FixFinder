package com.fixfinder.ui.dashboard;

/**
 * Modelo para representar un Operario en los listados y combos del Dashboard.
 */
public class OperarioModelo {
    private final int id;
    private final String nombre;
    private final String especialidad;

    public OperarioModelo(int id, String nombre, String especialidad) {
        this.id = id;
        this.nombre = nombre;
        this.especialidad = especialidad;
    }

    public int getId() { return id; }
    public String getNombre() { return nombre; }
    public String getEspecialidad() { return especialidad; }

    @Override
    public String toString() {
        return nombre + " (" + especialidad + ")";
    }
}

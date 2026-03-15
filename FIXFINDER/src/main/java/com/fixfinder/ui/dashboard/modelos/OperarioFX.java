package com.fixfinder.ui.dashboard.modelos;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Modelo POJO para representar un Operario en la UI del Dashboard.
 * Contiene todos los campos necesarios para mostrar el estado del operario en
 * la tabla.
 */
public class OperarioFX {

    /**
     * Crea una instancia de OperarioFX a partir de un nodo JSON del servidor.
     * Centraliza la lógica de mapeo para evitar duplicidad en los controladores.
     */
    public static OperarioFX fromNode(JsonNode n) {
        String nombre = n.has("nombre") ? n.get("nombre").asText()
                : n.has("nombreCompleto") ? n.get("nombreCompleto").asText() : "";

        // Limpiar paréntesis si vienen del servidor (ej. "(Gerente)")
        if (nombre.contains(" ("))
            nombre = nombre.split(" \\(")[0];

        boolean activo = n.has("estaActivo") ? n.get("estaActivo").asBoolean() : true;
        String email = n.has("email") ? n.get("email").asText() : "";
        String tel = n.has("telefono") ? n.get("telefono").asText() : "";
        String dni = n.has("dni") ? n.get("dni").asText() : "";
        String urlFoto = n.has("url_foto") && !n.get("url_foto").isNull() ? n.get("url_foto").asText() : "";

        return new OperarioFX(
                n.get("id").asInt(), nombre,
                n.has("especialidad") ? n.get("especialidad").asText() : "",
                true, 0, activo, email, tel, dni, urlFoto);
    }

    private final int id;
    private final String nombre;
    private final String especialidad;
    private final boolean disponible;
    private final int cargaTrabajo;
    private final boolean activo;
    private final String email;
    private final String telefono;
    private final String dni;
    private final String urlFoto;

    public OperarioFX(int id, String nombre, String especialidad,
            boolean disponible, int cargaTrabajo, boolean activo, String email, String telefono, String dni,
            String urlFoto) {
        this.id = id;
        this.nombre = nombre;
        this.especialidad = especialidad != null ? especialidad : "";
        this.disponible = disponible;
        this.cargaTrabajo = cargaTrabajo;
        this.activo = activo;
        this.email = email;
        this.telefono = telefono;
        this.dni = dni;
        this.urlFoto = urlFoto;
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

    public String getUrlFoto() {
        return urlFoto;
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

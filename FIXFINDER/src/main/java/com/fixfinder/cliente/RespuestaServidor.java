package com.fixfinder.cliente;

import com.fasterxml.jackson.databind.JsonNode;

public class RespuestaServidor {
    private int status;
    private String mensaje;
    private JsonNode datos;
    private String token; // Token de sesión (opcional)
    private String accion; // Nombre de la acción que disparó la respuesta

    public RespuestaServidor(int status, String mensaje, JsonNode datos) {
        this(status, mensaje, datos, null, null);
    }

    public RespuestaServidor(int status, String mensaje, JsonNode datos, String token) {
        this(status, mensaje, datos, token, null);
    }

    public RespuestaServidor(int status, String mensaje, JsonNode datos, String token, String accion) {
        this.status = status;
        this.mensaje = mensaje;
        this.datos = datos;
        this.token = token;
        this.accion = accion;
    }

    public String getAccion() {
        return accion;
    }

    public String getToken() {
        return token;
    }

    public int getStatus() {
        return status;
    }

    public String getMensaje() {
        return mensaje;
    }

    public JsonNode getDatos() {
        return datos;
    }

    public boolean esExito() {
        return status >= 200 && status < 300;
    }
}

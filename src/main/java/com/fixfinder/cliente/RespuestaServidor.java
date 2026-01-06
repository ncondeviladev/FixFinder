package com.fixfinder.cliente;

import com.fasterxml.jackson.databind.JsonNode;

public class RespuestaServidor {
    private int status;
    private String mensaje;
    private JsonNode datos;

    public RespuestaServidor(int status, String mensaje, JsonNode datos) {
        this.status = status;
        this.mensaje = mensaje;
        this.datos = datos;
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

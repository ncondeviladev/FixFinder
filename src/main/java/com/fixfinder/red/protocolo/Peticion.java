package com.fixfinder.red.protocolo;

import java.io.Serializable;

/**
 * Representa una solicitud enviada desde el cliente al servidor.
 * Implementa Serializable para poder enviarse por Sockets via
 * ObjectOutputStream.
 */
public class Peticion implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Tipo de acción que el cliente quiere realizar.
     */
    private TipoAccion accion;

    /**
     * Objeto genérico con los datos necesarios (ej: un objeto Usuario para
     * registro,
     * o Trabajo para crear incidencia).
     */
    private Object cuerpo;

    /**
     * Token o ID de sesión (opcional para auth básica).
     */
    private String token;

    public Peticion(TipoAccion accion, Object cuerpo) {
        this.accion = accion;
        this.cuerpo = cuerpo;
    }

    public Peticion(TipoAccion accion, Object cuerpo, String token) {
        this(accion, cuerpo);
        this.token = token;
    }

    public TipoAccion getAccion() {
        return accion;
    }

    public Object getCuerpo() {
        return cuerpo;
    }

    public String getToken() {
        return token;
    }
}

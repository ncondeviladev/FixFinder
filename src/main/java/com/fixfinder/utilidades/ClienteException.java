package com.fixfinder.utilidades;

/**
 * Excepción para errores ocurridos en el cliente o dashboard.
 * Encapsula errores de parsing JSON, problemas de UI o errores de lógica
 * cliente.
 */
public class ClienteException extends RuntimeException {

    public ClienteException(String message) {
        super(message);
    }

    public ClienteException(String message, Throwable cause) {
        super(message, cause);
    }
}

package com.fixfinder.utilidades;

import java.sql.SQLException;

/**
 * Excepción personalizada para encapsular errores de acceso a datos.
 *
 * Permite desacoplar la capa lógica de la tecnología de persistencia (SQL).
 * Si mañana cambiamos MySQL por ficheros, la capa lógica seguirá recibiendo
 * esta misma excepción.
 */
public class DataAccessException extends Exception {

    public DataAccessException(String mensaje) {
        super(mensaje);
    }

    public DataAccessException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }

    /**
     * Constructor específico para envolver SQLExceptions.
     */
    public DataAccessException(SQLException e) {
        super("Error de base de datos: " + e.getMessage(), e);
    }
}

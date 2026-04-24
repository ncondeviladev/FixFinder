package com.fixfinder.data.interfaces;

import com.fixfinder.modelos.Cliente;

public interface ClienteDAO extends BaseDAO<Cliente> {
    /**
     * Elimina un cliente por su email. Útil para limpieza de tests.
     */
    void eliminarPorEmail(String email) throws com.fixfinder.utilidades.DataAccessException;
}

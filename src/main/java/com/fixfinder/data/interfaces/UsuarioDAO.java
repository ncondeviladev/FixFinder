package com.fixfinder.data.interfaces;

import com.fixfinder.modelos.Usuario;
import com.fixfinder.utilidades.DataAccessException;

public interface UsuarioDAO extends BaseDAO<Usuario> {
    Usuario obtenerPorEmail(String email) throws DataAccessException;
}

package com.fixfinder.service.impl;

import com.fixfinder.data.UsuarioDAO;
import com.fixfinder.modelos.Usuario;
import com.fixfinder.utilidades.DataAccessException;
import com.fixfinder.utilidades.GestorPassword;
import com.fixfinder.utilidades.ServiceException;

/**
 * Implementación del Servicio de Lógica de Negocio para Usuarios.
 */
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioDAO usuarioDAO;

    public UsuarioServiceImpl() {
        this.usuarioDAO = new UsuarioDAO();
    }

    @Override
    public Usuario login(String email, String password) throws ServiceException {
        try {
            Usuario usuario = usuarioDAO.obtenerPorEmail(email);

            if (usuario == null) {
                throw new ServiceException("Usuario no encontrado");
            }

            if (!GestorPassword.verificarPassword(password, usuario.getPasswordHash())) {
                throw new ServiceException("Contraseña incorrecta");
            }

            return usuario;

        } catch (DataAccessException e) {
            throw new ServiceException("Error de base de datos durante el login", e);
        }
    }
}

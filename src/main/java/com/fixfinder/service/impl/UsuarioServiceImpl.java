package com.fixfinder.service.impl;

import com.fixfinder.data.DataRepository;
import com.fixfinder.data.DataRepositoryImpl;
import com.fixfinder.data.interfaces.UsuarioDAO;
import com.fixfinder.modelos.Usuario;
import com.fixfinder.service.interfaz.UsuarioService;
import com.fixfinder.utilidades.DataAccessException;
import com.fixfinder.utilidades.GestorPassword;
import com.fixfinder.utilidades.ServiceException;

/**
 * Implementación del Servicio de Lógica de Negocio para Usuarios.
 */
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioDAO usuarioDAO;

    public UsuarioServiceImpl() {
        DataRepository repository = new DataRepositoryImpl();
        this.usuarioDAO = repository.getUsuarioDAO();
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

    @Override
    public boolean validarPassword(String password, String confirmacion) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void cambiarPassword(Integer idUsuario, String oldPassword, String newPassword) throws ServiceException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void eliminarUsuario(Integer idUsuario) throws ServiceException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void registrarUsuario(Usuario usuario) throws ServiceException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void modificarUsuario(Usuario usuario) throws ServiceException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Usuario obtenerPorId(Integer idUsuario) throws ServiceException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}

package com.fixfinder.service.impl;

import com.fixfinder.data.DataRepository;
import com.fixfinder.data.DataRepositoryImpl;
import com.fixfinder.data.interfaces.UsuarioDAO;
import com.fixfinder.modelos.Usuario;
import com.fixfinder.service.interfaz.UsuarioService;
import com.fixfinder.utilidades.DataAccessException;
import com.fixfinder.utilidades.GestorPassword;
import com.fixfinder.utilidades.ServiceException;
import java.time.LocalDateTime;

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
                // Por seguridad, mensaje genérico pero útil para desarrollo/debug
                throw new ServiceException("Credenciales inválidas.");
            }

            if (!GestorPassword.verificarPassword(password, usuario.getPasswordHash())) {
                throw new ServiceException("Credenciales inválidas.");
            }

            return usuario;

        } catch (DataAccessException e) {
            throw new ServiceException("Error de base de datos durante el login", e);
        }
    }

    @Override
    public void registrarUsuario(Usuario usuario) throws ServiceException {
        try {
            if (usuario == null)
                throw new ServiceException("El usuario no puede ser nulo.");

            if (usuario.getEmail() == null || usuario.getEmail().isEmpty())
                throw new ServiceException("El email es obligatorio.");

            // Comprobar si ya existe el email
            Usuario existente = usuarioDAO.obtenerPorEmail(usuario.getEmail());
            if (existente != null) {
                throw new ServiceException("Ya existe un usuario registrado con ese email.");
            }

            if (usuario.getPasswordHash() != null && !usuario.getPasswordHash().startsWith("$2a$")
                    && !usuario.getPasswordHash().contains(":")) {
                usuario.setPasswordHash(GestorPassword.hashearPassword(usuario.getPasswordHash()));
            }

            usuario.setFechaRegistro(LocalDateTime.now());
            usuarioDAO.insertar(usuario);

        } catch (DataAccessException e) {
            throw new ServiceException("Error al registrar usuario.", e);
        }
    }

    @Override
    public void modificarUsuario(Usuario usuario) throws ServiceException {
        try {
            if (usuario == null || usuario.getId() == 0)
                throw new ServiceException("Usuario inválido para modificar.");

            // Validar que el nuevo email no pertenezca a OTRO usuario
            Usuario usuarioConMismoEmail = usuarioDAO.obtenerPorEmail(usuario.getEmail());
            if (usuarioConMismoEmail != null && usuarioConMismoEmail.getId() != usuario.getId()) {
                throw new ServiceException("El email ya está en uso por otro usuario.");
            }

            Usuario original = usuarioDAO.obtenerPorId(usuario.getId());
            if (original != null) {
                usuario.setPasswordHash(original.getPasswordHash());
                usuario.setFechaRegistro(original.getFechaRegistro());
            }

            usuarioDAO.actualizar(usuario);

        } catch (DataAccessException e) {
            throw new ServiceException("Error al modificar usuario.", e);
        }
    }

    @Override
    public void eliminarUsuario(Integer idUsuario) throws ServiceException {
        try {
            usuarioDAO.eliminar(idUsuario);
        } catch (DataAccessException e) {
            throw new ServiceException("Error al eliminar usuario.", e);
        }
    }

    @Override
    public Usuario obtenerPorId(Integer idUsuario) throws ServiceException {
        try {
            Usuario u = usuarioDAO.obtenerPorId(idUsuario);
            if (u == null)
                throw new ServiceException("Usuario no encontrado.");
            return u;
        } catch (DataAccessException e) {
            throw new ServiceException("Error al obtener usuario.", e);
        }
    }

    @Override
    public boolean validarPassword(String passwordInput, String passwordHash) {
        if (passwordInput == null || passwordHash == null)
            return false;
        return GestorPassword.verificarPassword(passwordInput, passwordHash);
    }

    @Override
    public void cambiarPassword(Integer idUsuario, String oldPassword, String newPassword) throws ServiceException {
        try {
            Usuario usuario = usuarioDAO.obtenerPorId(idUsuario);
            if (usuario == null)
                throw new ServiceException("Usuario no encontrado.");

            if (!GestorPassword.verificarPassword(oldPassword, usuario.getPasswordHash())) {
                throw new ServiceException("La contraseña actual es incorrecta.");
            }

            if (newPassword == null || newPassword.length() < 6) {
                throw new ServiceException("La nueva contraseña debe tener al menos 6 caracteres.");
            }

            usuario.setPasswordHash(GestorPassword.hashearPassword(newPassword));
            usuarioDAO.actualizar(usuario);

        } catch (DataAccessException e) {
            throw new ServiceException("Error al cambiar la contraseña.", e);
        }
    }
}

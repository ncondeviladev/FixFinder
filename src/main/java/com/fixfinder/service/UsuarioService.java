package com.fixfinder.service;

import com.fixfinder.modelos.Usuario;
import com.fixfinder.utilidades.ServiceException;

/**
 * Interfaz del Servicio de Usuarios.
 * Gestiona autenticación, registro y datos de perfil.
 */
public interface UsuarioService {

    /**
     * Autentica a un usuario verificando su email y contraseña.
     *
     * @param email    Email del usuario.
     * @param password Contraseña en texto plano.
     * @return El objeto Usuario si la autenticación es correcta.
     * @throws ServiceException Si las credenciales son inválidas o hay error.
     */
    Usuario login(String email, String password) throws ServiceException;

    /**
     * Registra un nuevo usuario en el sistema.
     *
     * @param usuario Objeto usuario con los datos a registrar.
     * @throws ServiceException Si el email ya existe o datos inválidos.
     */
    void registrarUsuario(Usuario usuario) throws ServiceException;

    /**
     * Actualiza los datos personales de un usuario.
     *
     * @param usuario Usuario con los datos modificados.
     * @throws ServiceException Si ocurre un error al guardar.
     */
    void modificarUsuario(Usuario usuario) throws ServiceException;

    /**
     * Da de baja un usuario del sistema (puede ser baja lógica).
     *
     * @param idUsuario ID del usuario a eliminar.
     * @throws ServiceException Si el usuario tiene dependencias activas.
     */
    void eliminarUsuario(Integer idUsuario) throws ServiceException;

    /**
     * Obtiene un usuario por su ID.
     *
     * @param id ID del usuario.
     * @return El usuario encontrado.
     * @throws ServiceException Si no existe.
     */
    Usuario obtenerPorId(Integer id) throws ServiceException;

    /**
     * Verifica si una contraseña coincide con el hash almacenado.
     * Útil para re-autenticación antes de acciones críticas.
     *
     * @param passwordInput Contraseña en texto plano.
     * @param passwordHash  Hash almacenado.
     * @return true si coinciden.
     */
    boolean validarPassword(String passwordInput, String passwordHash);

    /**
     * Cambia la contraseña de un usuario.
     *
     * @param idUsuario   ID del usuario.
     * @param oldPassword Contraseña actual para verificación.
     * @param newPassword Nueva contraseña.
     * @throws ServiceException Si la contraseña actual es incorrecta.
     */
    void cambiarPassword(Integer idUsuario, String oldPassword, String newPassword) throws ServiceException;
}

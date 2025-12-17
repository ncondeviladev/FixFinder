package com.fixfinder.service.impl;

import com.fixfinder.data.DataRepository;
import com.fixfinder.data.DataRepositoryImpl;
import com.fixfinder.data.interfaces.ClienteDAO;
import com.fixfinder.data.interfaces.OperarioDAO;
import com.fixfinder.data.interfaces.UsuarioDAO;
import com.fixfinder.modelos.Usuario;
import com.fixfinder.modelos.enums.Rol;
import com.fixfinder.service.interfaz.UsuarioService;
import com.fixfinder.utilidades.DataAccessException;
import com.fixfinder.utilidades.GestorPassword;
import com.fixfinder.utilidades.ServiceException;
import java.time.LocalDateTime;

/**
 * Implementación del Servicio para Usuarios.
 * Ahora maneja lógica polimórfica para cargar Operarios y Clientes.
 */
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioDAO usuarioDAO;
    private final OperarioDAO operarioDAO;
    private final ClienteDAO clienteDAO;

    public UsuarioServiceImpl() {
        DataRepository repository = new DataRepositoryImpl();
        this.usuarioDAO = repository.getUsuarioDAO();
        this.operarioDAO = repository.getOperarioDAO();
        this.clienteDAO = repository.getClienteDAO();
    }

    @Override
    public Usuario login(String email, String password) throws ServiceException {
        try {
            // 1. Obtener usuario base (tabla usuario)
            Usuario usuario = usuarioDAO.obtenerPorEmail(email);

            if (usuario == null) {
                throw new ServiceException("Credenciales inválidas.");
            }

            if (!GestorPassword.verificarPassword(password, usuario.getPasswordHash())) {
                throw new ServiceException("Credenciales inválidas.");
            }

            // 2. Comprobar Rol y cargar datos extendidos si procede
            Rol rol = usuario.getRol();

            if (rol == Rol.OPERARIO) {
                Usuario operario = operarioDAO.obtenerPorId(usuario.getId());
                if (operario != null)
                    return operario;
                // Si falla la integridad (no existe registro en operario), devolvemos usuario
                // base o error logueado
            } else if (rol == Rol.CLIENTE) {
                Usuario cliente = clienteDAO.obtenerPorId(usuario.getId());
                if (cliente != null)
                    return cliente;
            }

            // Para ADMIN, GERENTE o fallo de integridad, devolvemos el usuario base
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

            Usuario existente = usuarioDAO.obtenerPorEmail(usuario.getEmail());
            if (existente != null) {
                throw new ServiceException("Ya existe un usuario registrado con ese email.");
            }

            validarDatosUsuario(usuario);

            if (usuario.getPasswordHash() != null && !usuario.getPasswordHash().startsWith("$2a$")
                    && !usuario.getPasswordHash().contains(":")) {
                usuario.setPasswordHash(GestorPassword.hashearPassword(usuario.getPasswordHash()));
            }

            usuario.setFechaRegistro(LocalDateTime.now());

            // Delegar inserción al DAO específico según el tipo de instancia (Polimorfismo)
            if (usuario instanceof com.fixfinder.modelos.Operario) {
                operarioDAO.insertar((com.fixfinder.modelos.Operario) usuario);
            } else if (usuario instanceof com.fixfinder.modelos.Cliente) {
                clienteDAO.insertar((com.fixfinder.modelos.Cliente) usuario);
            } else {
                usuarioDAO.insertar(usuario);
            }

        } catch (DataAccessException e) {
            throw new ServiceException("Error al registrar usuario.", e);
        }
    }

    @Override
    public void modificarUsuario(Usuario usuario) throws ServiceException {
        try {
            if (usuario == null || usuario.getId() == 0)
                throw new ServiceException("Usuario inválido para modificar.");

            Usuario usuarioConMismoEmail = usuarioDAO.obtenerPorEmail(usuario.getEmail());
            if (usuarioConMismoEmail != null && usuarioConMismoEmail.getId() != usuario.getId()) {
                throw new ServiceException("El email ya está en uso por otro usuario.");
            }

            validarDatosUsuario(usuario);

            Usuario original = usuarioDAO.obtenerPorId(usuario.getId());
            if (original != null) {
                usuario.setPasswordHash(original.getPasswordHash());
                usuario.setFechaRegistro(original.getFechaRegistro());
            }

            // Polimorfismo en actualización
            if (usuario instanceof com.fixfinder.modelos.Operario) {
                operarioDAO.actualizar((com.fixfinder.modelos.Operario) usuario);
            } else if (usuario instanceof com.fixfinder.modelos.Cliente) {
                clienteDAO.actualizar((com.fixfinder.modelos.Cliente) usuario);
            } else {
                usuarioDAO.actualizar(usuario);
            }

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

            // Cargar datos extendidos si procede (igual que en Login)
            if (u.getRol() == Rol.OPERARIO) {
                Usuario op = operarioDAO.obtenerPorId(idUsuario);
                if (op != null)
                    return op;
            } else if (u.getRol() == Rol.CLIENTE) {
                Usuario cli = clienteDAO.obtenerPorId(idUsuario);
                if (cli != null)
                    return cli;
            }

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
            Usuario usuario = obterPorIdInternal(idUsuario); // Helper para no repetir logica de carga

            if (!GestorPassword.verificarPassword(oldPassword, usuario.getPasswordHash())) {
                throw new ServiceException("La contraseña actual es incorrecta.");
            }

            if (newPassword == null || newPassword.length() < 6) {
                throw new ServiceException("La nueva contraseña debe tener al menos 6 caracteres.");
            }

            usuario.setPasswordHash(GestorPassword.hashearPassword(newPassword));

            // Usar update adecuado
            modificarUsuario(usuario);

        } catch (DataAccessException | ServiceException e) {
            throw new ServiceException("Error al cambiar la contraseña.", e);
        }
    }

    // Helper privado para evitar recargar lógica compleja
    private Usuario obterPorIdInternal(int id) throws DataAccessException, ServiceException {
        Usuario u = usuarioDAO.obtenerPorId(id);
        if (u == null)
            throw new ServiceException("Usuario no encontrado.");
        if (u.getRol() == Rol.OPERARIO) {
            Usuario op = operarioDAO.obtenerPorId(id);
            return (op != null) ? op : u;
        } else if (u.getRol() == Rol.CLIENTE) {
            Usuario cli = clienteDAO.obtenerPorId(id);
            return (cli != null) ? cli : u;
        }
        return u;
    }

    private void validarDatosUsuario(Usuario usuario) throws ServiceException {
        if (usuario.getEmail() == null || !usuario.getEmail().matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            throw new ServiceException("El formato del email no es válido.");
        }

        if (usuario.getTelefono() != null && !usuario.getTelefono().isEmpty()) {
            if (!usuario.getTelefono().matches("^\\d{9}$")) {
                throw new ServiceException("El teléfono debe contener exactamente 9 dígitos numéricos.");
            }
        }

        if (usuario.getUrlFoto() != null && usuario.getUrlFoto().length() > 255) {
            throw new ServiceException("La URL de la foto excede el límite de 255 caracteres.");
        }

        if (usuario.getDireccion() != null && usuario.getDireccion().trim().isEmpty()) {
            throw new ServiceException("La dirección no puede estar formada solo por espacios.");
        }
    }
}

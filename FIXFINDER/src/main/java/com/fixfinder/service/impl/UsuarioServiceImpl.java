package com.fixfinder.service.impl;

import com.fixfinder.modelos.Cliente;
import com.fixfinder.modelos.Operario;
import com.fixfinder.modelos.Usuario;
import com.fixfinder.modelos.enums.Rol;
import com.fixfinder.repository.ClienteRepository;
import com.fixfinder.repository.OperarioRepository;
import com.fixfinder.repository.UsuarioRepository;
import com.fixfinder.service.NotificationService;
import com.fixfinder.service.interfaz.UsuarioService;
import com.fixfinder.utilidades.GestorPassword;
import com.fixfinder.utilidades.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Implementación del servicio de gestión de usuarios (clientes y operarios).
 * Maneja la autenticación, registro y actualización del perfil y contraseña.
 */
@Service
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final OperarioRepository operarioRepository;
    private final ClienteRepository clienteRepository;
    private final NotificationService notificationService;

    @Autowired
    public UsuarioServiceImpl(UsuarioRepository usuarioRepository, OperarioRepository operarioRepository, 
                             ClienteRepository clienteRepository, NotificationService notificationService) {
        this.usuarioRepository = usuarioRepository;
        this.operarioRepository = operarioRepository;
        this.clienteRepository = clienteRepository;
        this.notificationService = notificationService;
    }

    @Override
    public Usuario login(String email, String password) throws ServiceException {
        Usuario usuario = usuarioRepository.findByEmail(email);
        if (usuario == null) {
            throw new ServiceException("No se encuentra al usuario.");
        }
        if (!GestorPassword.verificarPassword(password, usuario.getPasswordHash())) {
            throw new ServiceException("Contraseña incorrecta.");
        }
        return obtenerDatosExtendidos(usuario);
    }

    @Override
    @Transactional
    public void registrarUsuario(Usuario usuario) throws ServiceException {
        if (usuario == null) throw new ServiceException("El usuario no puede ser nulo.");
        if (usuario.getEmail() == null || usuario.getEmail().isEmpty()) throw new ServiceException("El email es obligatorio.");
        
        Usuario existente = usuarioRepository.findByEmail(usuario.getEmail());
        if (existente != null) throw new ServiceException("Ya existe un usuario registrado con ese email.");
        
        validarDatosUsuario(usuario);
        
        if (usuario.getPasswordHash() != null && !usuario.getPasswordHash().startsWith("$2a$") && !usuario.getPasswordHash().contains(":")) {
            usuario.setPasswordHash(GestorPassword.hashearPassword(usuario.getPasswordHash()));
        }
        usuario.setFechaRegistro(LocalDateTime.now());
        
        usuarioRepository.save(usuario); 
    }

    @Override
    @Transactional
    public void modificarUsuario(Usuario usuario) throws ServiceException {
        if (usuario == null || usuario.getId() == 0) throw new ServiceException("Usuario inválido para modificar.");
        
        Usuario usuarioConMismoEmail = usuarioRepository.findByEmail(usuario.getEmail());
        if (usuarioConMismoEmail != null && usuarioConMismoEmail.getId() != usuario.getId()) {
            throw new ServiceException("El email ya está en uso por otro usuario.");
        }
        
        validarDatosUsuario(usuario);
        
        Usuario original = usuarioRepository.findById(usuario.getId()).orElse(null);
        if (original != null) {
            usuario.setPasswordHash(original.getPasswordHash());
            usuario.setFechaRegistro(original.getFechaRegistro());
        }
        
        usuarioRepository.save(usuario);
        
        // Notificar cambio de perfil
        notificationService.difundirEventoUsuario("PERFIL_MODIFICADO", usuario.getId(), 
            usuario.getNombreCompleto(), usuario.getUrlFoto(), "Perfil actualizado", 
            usuario.getEmail(), usuario.getTelefono(), usuario.getDireccion());
    }

    @Override
    @Transactional
    public void eliminarUsuario(Integer idUsuario) throws ServiceException {
        usuarioRepository.deleteById(idUsuario);
    }

    @Override
    public Usuario obtenerPorId(Integer idUsuario) throws ServiceException {
        Usuario u = usuarioRepository.findById(idUsuario).orElse(null);
        if (u == null) throw new ServiceException("Usuario no encontrado.");
        return obtenerDatosExtendidos(u);
    }

    @Override
    public Usuario obtenerPorEmail(String email) throws ServiceException {
        Usuario u = usuarioRepository.findByEmail(email);
        if (u == null) throw new ServiceException("Usuario no encontrado.");
        return obtenerDatosExtendidos(u);
    }

    private Usuario obtenerDatosExtendidos(Usuario u) {
        if (u instanceof Operario || u instanceof Cliente) return u;

        if (u.getRol() == Rol.OPERARIO || u.getRol() == Rol.GERENTE) {
            return operarioRepository.findById(u.getId()).map(o -> (Usuario) o).orElse(u);
        } else if (u.getRol() == Rol.CLIENTE) {
            return clienteRepository.findById(u.getId()).map(c -> (Usuario) c).orElse(u);
        }
        return u;
    }

    @Override
    public boolean validarPassword(String passwordInput, String passwordHash) {
        if (passwordInput == null || passwordHash == null) return false;
        return GestorPassword.verificarPassword(passwordInput, passwordHash);
    }

    @Override
    @Transactional
    public void cambiarPassword(Integer idUsuario, String oldPassword, String newPassword) throws ServiceException {
        Usuario usuario = usuarioRepository.findById(idUsuario).orElse(null);
        if (usuario == null) throw new ServiceException("Usuario no encontrado.");

        if (!GestorPassword.verificarPassword(oldPassword, usuario.getPasswordHash())) {
            throw new ServiceException("La contraseña actual es incorrecta.");
        }

        if (newPassword == null || newPassword.length() < 6) {
            throw new ServiceException("La nueva contraseña debe tener al menos 6 caracteres.");
        }

        usuario.setPasswordHash(GestorPassword.hashearPassword(newPassword));
        usuarioRepository.save(usuario);
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

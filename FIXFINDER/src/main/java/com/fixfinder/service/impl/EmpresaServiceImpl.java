package com.fixfinder.service.impl;

import com.fixfinder.modelos.Empresa;
import com.fixfinder.modelos.Operario;
import com.fixfinder.modelos.enums.Rol;
import com.fixfinder.repository.EmpresaRepository;
import com.fixfinder.repository.UsuarioRepository;
import com.fixfinder.service.interfaz.EmpresaService;
import com.fixfinder.utilidades.GestorPassword;
import com.fixfinder.utilidades.ServiceException;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Implementación del servicio de gestión de empresas: registro, modificación, baja y consultas. */
@Service
public class EmpresaServiceImpl implements EmpresaService {

    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;

    @Autowired
    public EmpresaServiceImpl(EmpresaRepository empresaRepository, UsuarioRepository usuarioRepository) {
        this.empresaRepository = empresaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /** Devuelve la lista completa de empresas registradas en el sistema. */
    @Override
    public List<Empresa> listarTodas() throws ServiceException {
        return empresaRepository.findAll();
    }

    /** Obtiene los datos de una empresa por su ID. Lanza excepción si no existe. */
    @Override
    public Empresa obtenerPorId(Integer id) throws ServiceException {
        return empresaRepository.findById(id)
                .orElseThrow(() -> new ServiceException("Empresa no encontrada con ID: " + id));
    }

    /** Obtiene los datos de una empresa para mostrar en el panel de estadísticas del Dashboard. */
    @Override
    public Empresa obtenerEstadisticas(Integer idEmpresa) throws ServiceException {
        if (idEmpresa == null) throw new ServiceException("El ID de empresa es obligatorio.");
        Empresa empresa = empresaRepository.findById(idEmpresa).orElse(null);
        if (empresa == null) throw new ServiceException("Empresa no encontrada.");
        return empresa;
    }

    /**
     * Da de baja permanente a una empresa eliminando su registro de la BD.
     * Esta acción es irreversible.
     */
    @Override
    @Transactional
    public void bajaEmpresa(Integer idEmpresa) throws ServiceException {
        if (idEmpresa == null) throw new ServiceException("El ID de empresa es obligatorio para dar de baja.");
        if (!empresaRepository.existsById(idEmpresa)) {
            throw new ServiceException("No existe empresa con el ID proporcionado.");
        }
        empresaRepository.deleteById(idEmpresa);
    }

    /**
     * Registra una nueva empresa y su gerente en una única transacción atómica.
     * Si falla alguno de los dos pasos, ninguno se persiste.
     */
    @Override
    @Transactional
    public void registrarEmpresaConGerente(Map<String, Object> datos) throws ServiceException {
        // 1. Crear Empresa
        Empresa empresa = new Empresa();
        empresa.setNombre((String) datos.get("nombreEmpresa"));
        empresa.setCif((String) datos.get("cif"));
        empresa.setEmailContacto((String) datos.get("emailEmpresa"));
        empresa.setTelefono((String) datos.get("telefonoEmpresa"));
        empresa.setDireccion((String) datos.get("direccion"));
        
        validarDatosEmpresa(empresa);
        empresa = empresaRepository.save(empresa);

        // 2. Crear Gerente (es un Operario con rol GERENTE)
        Operario gerente = new Operario();
        gerente.setNombreCompleto((String) datos.get("nombreGerente"));
        gerente.setEmail((String) datos.get("emailGerente"));
        gerente.setDni((String) datos.get("dniGerente"));
        gerente.setTelefono((String) datos.get("telefonoGerente"));
        gerente.setRol(Rol.GERENTE);
        gerente.setIdEmpresa(empresa.getId());
        gerente.setEstaActivo(true);
        
        String pass = (String) datos.get("passwordGerente");
        if (pass == null || pass.isEmpty()) throw new ServiceException("La contraseña del gerente es obligatoria.");
        gerente.setPasswordHash(GestorPassword.hashearPassword(pass));

        if (usuarioRepository.existsByEmail(gerente.getEmail())) {
            throw new ServiceException("El email del gerente ya está registrado.");
        }

        usuarioRepository.save(gerente);
    }

    /** Registra una empresa a partir de un objeto ya construido. */
    @Override
    @Transactional
    public void registrarEmpresa(Empresa empresa) throws ServiceException {
        validarDatosEmpresa(empresa);
        empresaRepository.save(empresa);
    }

    /** Actualiza los datos de una empresa existente (nombre, CIF, email de contacto, logo...). */
    @Override
    @Transactional
    public void modificarEmpresa(Empresa empresa) throws ServiceException {
        if (empresa.getId() == 0) throw new ServiceException("El ID de empresa es necesario para modificar.");
        validarDatosEmpresa(empresa);

        if (!empresaRepository.existsById(empresa.getId())) {
            throw new ServiceException("La empresa a modificar no existe.");
        }

        empresaRepository.save(empresa);
    }

    /**
     * Valida los campos obligatorios de una empresa antes de persistirla.
     * Comprueba nombre, CIF, formato de email y longitud de URL de foto.
     */
    private void validarDatosEmpresa(Empresa empresa) throws ServiceException {
        if (empresa == null) throw new ServiceException("El objeto empresa no puede ser nulo.");
        if (empresa.getNombre() == null || empresa.getNombre().trim().isEmpty()) {
            throw new ServiceException("El nombre de la empresa es obligatorio.");
        }
        if (empresa.getCif() == null || empresa.getCif().trim().isEmpty()) {
            throw new ServiceException("El CIF es obligatorio.");
        }
        if (empresa.getEmailContacto() != null && !empresa.getEmailContacto().isEmpty()) {
            if (!empresa.getEmailContacto().matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
                throw new ServiceException("El formato del email de contacto no es válido.");
            }
        }
        if (empresa.getUrlFoto() != null && empresa.getUrlFoto().length() > 255) {
            throw new ServiceException("La URL de la foto excede los 255 caracteres.");
        }
    }
}

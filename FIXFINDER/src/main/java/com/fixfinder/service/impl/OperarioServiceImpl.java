package com.fixfinder.service.impl;

import com.fixfinder.modelos.Operario;
import com.fixfinder.repository.OperarioRepository;
import com.fixfinder.service.NotificationService;
import com.fixfinder.service.interfaz.OperarioService;
import com.fixfinder.utilidades.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;


/** Implementación del servicio de gestión de operarios: alta, baja, modificación y notificaciones en tiempo real. */
@Service
public class OperarioServiceImpl implements OperarioService {

    private final OperarioRepository operarioRepository;
    private final NotificationService notificationService;

    @Autowired
    public OperarioServiceImpl(OperarioRepository operarioRepository, NotificationService notificationService) {
        this.operarioRepository = operarioRepository;
        this.notificationService = notificationService;
    }

    /**
     * Lista los operarios disponibles (activos) de una empresa.
     * Si {@code idEmpresa} es nulo, devuelve todos los operarios activos del sistema.
     */
    @Override
    public List<Operario> listarDisponibles(Integer idEmpresa) throws ServiceException {
        List<Operario> todos = operarioRepository.findAll();
        return todos.stream()
                .filter(op -> idEmpresa == null || op.getIdEmpresa() == idEmpresa.intValue())
                .filter(Operario::isEstaActivo)
                .collect(Collectors.toList());
    }

    /**
     * Busca operarios por especialidad (ej. FONTANERIA, ELECTRICIDAD).
     * La búsqueda es insensible a mayúsculas/minúsculas.
     */
    @Override
    public List<Operario> buscarPorEspecialidad(String especialidad) throws ServiceException {
        if (especialidad == null || especialidad.isEmpty()) {
            throw new ServiceException("La especialidad es obligatoria para la búsqueda.");
        }
        List<Operario> todos = operarioRepository.findAll();
        return todos.stream()
                .filter(op -> op.getEspecialidad() != null && op.getEspecialidad().name().equalsIgnoreCase(especialidad))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void actualizarOperarioParcial(int idOperario, java.util.Map<String, Object> datos) throws ServiceException {
        Operario existente = operarioRepository.findById(idOperario).orElse(null);
        if (existente == null) {
            throw new ServiceException("El operario a modificar no existe.");
        }

        if (datos.containsKey("nombreCompleto")) {
            existente.setNombreCompleto((String) datos.get("nombreCompleto"));
        }
        if (datos.containsKey("dni")) {
            existente.setDni((String) datos.get("dni"));
        }
        if (datos.containsKey("email")) {
            existente.setEmail((String) datos.get("email"));
        }
        if (datos.containsKey("telefono")) {
            existente.setTelefono((String) datos.get("telefono"));
        }
        if (datos.containsKey("especialidad") && datos.get("especialidad") != null) {
            existente.setEspecialidad(com.fixfinder.modelos.enums.CategoriaServicio.valueOf((String) datos.get("especialidad")));
        }
        if (datos.containsKey("estaActivo")) {
            existente.setEstaActivo((Boolean) datos.get("estaActivo"));
        }
        if (datos.containsKey("isEstaActivo")) {
            existente.setEstaActivo((Boolean) datos.get("isEstaActivo"));
        }

        validarDatosOperario(existente);
        operarioRepository.save(existente);

        notificationService.difundirEventoOperario("MODIFICACION", existente.getId(), existente.getIdEmpresa(), "Datos de operario actualizados");
        notificationService.difundirEventoUsuario("DATOS", existente.getId(), 
            existente.getNombreCompleto(), existente.getUrlFoto(), "Tu empresa ha actualizado tu perfil", 
            existente.getEmail(), existente.getTelefono(), "");
    }

    /**
     * Actualiza los datos del operario y notifica al gerente y al propio operario
     * para sincronizar su app en tiempo real sin necesidad de reloguear.
     */
    @Override
    @Transactional
    public void modificarOperario(Operario operario) throws ServiceException {
        if (operario == null || operario.getId() == 0) {
            throw new ServiceException("Operario inválido para modificar.");
        }

        Operario existente = operarioRepository.findById(operario.getId()).orElse(null);
        if (existente == null) {
            throw new ServiceException("El operario a modificar no existe.");
        }

        // Safe Merge
        if (operario.getNombreCompleto() != null && !operario.getNombreCompleto().trim().isEmpty()) {
            existente.setNombreCompleto(operario.getNombreCompleto());
        }
        if (operario.getDni() != null && !operario.getDni().trim().isEmpty()) {
            existente.setDni(operario.getDni());
        }
        if (operario.getEmail() != null && !operario.getEmail().trim().isEmpty()) {
            existente.setEmail(operario.getEmail());
        }
        if (operario.getTelefono() != null && !operario.getTelefono().trim().isEmpty()) {
            existente.setTelefono(operario.getTelefono());
        }
        if (operario.getEspecialidad() != null) {
            existente.setEspecialidad(operario.getEspecialidad());
        }
        
        // El Dashboard JavaFX envía isEstaActivo = false explicitamente en bajas
        existente.setEstaActivo(operario.isEstaActivo());

        validarDatosOperario(existente);

        operarioRepository.save(existente);
        
        notificationService.difundirEventoOperario("MODIFICACION", operario.getId(), operario.getIdEmpresa(), "Datos de operario actualizados");
        notificationService.difundirEventoUsuario("DATOS", operario.getId(), 
            operario.getNombreCompleto(), operario.getUrlFoto(), "Tu empresa ha actualizado tu perfil", 
            operario.getEmail(), operario.getTelefono(), "");
    }

    /** Lista todos los operarios (activos e inactivos) pertenecientes a una empresa. */
    @Override
    public List<Operario> listarPorEmpresa(Integer idEmpresa) throws ServiceException {
        if (idEmpresa == null) throw new ServiceException("El ID de empresa es obligatorio.");
        return operarioRepository.findByIdEmpresa(idEmpresa);
    }

    /** Da de alta un nuevo operario en el sistema (con validación de datos previa). */
    @Override
    @Transactional
    public void altaOperario(Operario operario) throws ServiceException {
        if (operario == null) throw new ServiceException("El operario no puede ser nulo.");
        validarDatosOperario(operario);
        operarioRepository.save(operario);
    }

    /**
     * Cambia el estado activo/inactivo del operario y notifica en tiempo real.
     * @param disponible true para activar, false para dar de baja.
     */
    @Override
    @Transactional
    public void establecerDisponibilidad(Integer idOperario, boolean disponible) throws ServiceException {
        if (idOperario == null) throw new ServiceException("ID de operario obligatorio.");
        Operario op = operarioRepository.findById(idOperario).orElse(null);
        if (op == null) throw new ServiceException("Operario no encontrado.");

        op.setEstaActivo(disponible);
        operarioRepository.save(op);
        
        notificationService.difundirEventoOperario("MODIFICACION", idOperario, op.getIdEmpresa(), "Estado de operario actualizado");
        notificationService.difundirEventoUsuario("DATOS", idOperario, 
            op.getNombreCompleto(), op.getUrlFoto(), "Tu empresa ha modificado tu estado", 
            op.getEmail(), op.getTelefono(), "");
    }

    /** Valida nombre, email, DNI (mín. 8 caracteres) y teléfono (9 dígitos) del operario. */
    private void validarDatosOperario(Operario op) throws ServiceException {
        if (op.getNombreCompleto() == null || op.getNombreCompleto().isEmpty()) {
            throw new ServiceException("El nombre del operario es obligatorio.");
        }
        if (op.getEmail() == null || !op.getEmail().matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            throw new ServiceException("El email del operario no es válido.");
        }
        if (op.getDni() == null || op.getDni().length() < 8) {
            throw new ServiceException("El DNI del operario no es válido.");
        }
        if (op.getTelefono() != null && !op.getTelefono().matches("^\\d{9}$")) {
            throw new ServiceException("El teléfono debe tener 9 dígitos.");
        }
    }

    /** Obtiene un operario por su ID de usuario. Devuelve null si no existe (sin lanzar excepción). */
    @Override
    public Operario obtenerPorId(int idUsuario) throws ServiceException {
        return operarioRepository.findById(idUsuario).orElse(null);
    }
}

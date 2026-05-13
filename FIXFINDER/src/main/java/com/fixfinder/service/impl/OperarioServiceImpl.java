package com.fixfinder.service.impl;

import com.fixfinder.modelos.Operario;
import com.fixfinder.repository.OperarioRepository;
import com.fixfinder.service.interfaz.OperarioService;
import com.fixfinder.utilidades.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class OperarioServiceImpl implements OperarioService {

    private final OperarioRepository operarioRepository;

    @Autowired
    public OperarioServiceImpl(OperarioRepository operarioRepository) {
        this.operarioRepository = operarioRepository;
    }

    @Override
    public List<Operario> listarDisponibles(Integer idEmpresa) throws ServiceException {
        List<Operario> todos = operarioRepository.findAll();
        return todos.stream()
                .filter(op -> idEmpresa == null || op.getIdEmpresa() == idEmpresa.intValue())
                .filter(Operario::isEstaActivo)
                .collect(Collectors.toList());
    }

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
    public void modificarOperario(Operario operario) throws ServiceException {
        if (operario == null || operario.getId() == 0) {
            throw new ServiceException("Operario inválido para modificar.");
        }
        validarDatosOperario(operario);

        Operario existente = operarioRepository.findById(operario.getId()).orElse(null);
        if (existente == null) {
            throw new ServiceException("El operario a modificar no existe.");
        }

        operarioRepository.save(operario);
    }

    @Override
    public List<Operario> listarPorEmpresa(Integer idEmpresa) throws ServiceException {
        if (idEmpresa == null) throw new ServiceException("El ID de empresa es obligatorio.");
        return operarioRepository.findByIdEmpresa(idEmpresa);
    }

    @Override
    @Transactional
    public void altaOperario(Operario operario) throws ServiceException {
        if (operario == null) throw new ServiceException("El operario no puede ser nulo.");
        validarDatosOperario(operario);
        operarioRepository.save(operario);
    }

    @Override
    @Transactional
    public void establecerDisponibilidad(Integer idOperario, boolean disponible) throws ServiceException {
        if (idOperario == null) throw new ServiceException("ID de operario obligatorio.");
        Operario op = operarioRepository.findById(idOperario).orElse(null);
        if (op == null) throw new ServiceException("Operario no encontrado.");

        op.setEstaActivo(disponible);
        operarioRepository.save(op);
    }

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
}

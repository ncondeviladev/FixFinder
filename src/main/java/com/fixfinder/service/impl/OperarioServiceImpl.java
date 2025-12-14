package com.fixfinder.service.impl;

import com.fixfinder.data.DataRepository;
import com.fixfinder.data.DataRepositoryImpl;
import com.fixfinder.data.interfaces.OperarioDAO;
import com.fixfinder.modelos.Operario;
import com.fixfinder.service.interfaz.OperarioService;
import com.fixfinder.utilidades.DataAccessException;
import com.fixfinder.utilidades.ServiceException;
import java.util.List;
import java.util.stream.Collectors;

public class OperarioServiceImpl implements OperarioService {

    private final OperarioDAO operarioDAO;

    public OperarioServiceImpl() {
        DataRepository repo = new DataRepositoryImpl();
        this.operarioDAO = repo.getOperarioDAO();
    }

    @Override
    public List<Operario> listarDisponibles(Integer idEmpresa) throws ServiceException {
        try {
            // Como el DAO de operario no tiene método específico, filtramos en memoria
            List<Operario> todos = operarioDAO.obtenerTodos();
            return todos.stream()
                    .filter(op -> idEmpresa == null || op.getIdEmpresa() == idEmpresa.intValue())
                    .filter(Operario::isEstaActivo)
                    .collect(Collectors.toList());
        } catch (DataAccessException e) {
            throw new ServiceException("Error al listar operarios disponibles.", e);
        }
    }

    @Override
    public List<Operario> buscarPorEspecialidad(String especialidad) throws ServiceException {
        try {
            if (especialidad == null || especialidad.isEmpty()) {
                throw new ServiceException("La especialidad es obligatoria para la búsqueda.");
            }
            List<Operario> todos = operarioDAO.obtenerTodos();
            return todos.stream()
                    .filter(op -> op.getEspecialidad() != null &&
                            op.getEspecialidad().name().equalsIgnoreCase(especialidad))
                    .collect(Collectors.toList());
        } catch (DataAccessException e) {
            throw new ServiceException("Error al buscar operarios por especialidad.", e);
        }
    }

    @Override
    public void modificarOperario(Operario operario) throws ServiceException {
        if (operario == null || operario.getId() == 0) {
            throw new ServiceException("Operario inválido para modificar.");
        }
        validarDatosOperario(operario);

        try {
            // Verificar existencia
            Operario existente = operarioDAO.obtenerPorId(operario.getId());
            if (existente == null) {
                throw new ServiceException("El operario a modificar no existe.");
            }

            operarioDAO.actualizar(operario);
        } catch (DataAccessException e) {
            throw new ServiceException("Error al modificar el operario.", e);
        }
    }

    @Override
    public List<Operario> listarPorEmpresa(Integer idEmpresa) throws ServiceException {
        try {
            if (idEmpresa == null)
                throw new ServiceException("El ID de empresa es obligatorio.");
            List<Operario> todos = operarioDAO.obtenerTodos();
            return todos.stream()
                    .filter(op -> op.getIdEmpresa() == idEmpresa.intValue())
                    .collect(Collectors.toList());
        } catch (DataAccessException e) {
            throw new ServiceException("Error al listar operarios por empresa.", e);
        }
    }

    @Override
    public void altaOperario(Operario operario) throws ServiceException {
        if (operario == null)
            throw new ServiceException("El operario no puede ser nulo.");

        validarDatosOperario(operario);

        try {
            operarioDAO.insertar(operario);
        } catch (DataAccessException e) {
            throw new ServiceException("Error al dar de alta el operario.", e);
        }
    }

    @Override
    public void establecerDisponibilidad(Integer idOperario, boolean disponible) throws ServiceException {
        try {
            if (idOperario == null)
                throw new ServiceException("ID de operario obligatorio.");
            Operario op = operarioDAO.obtenerPorId(idOperario);
            if (op == null)
                throw new ServiceException("Operario no encontrado.");

            op.setEstaActivo(disponible);
            operarioDAO.actualizar(op);
        } catch (DataAccessException e) {
            throw new ServiceException("Error al cambiar disponibilidad del operario.", e);
        }
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
        // Validar campos heredados de Usuario si es necesario (telefono, foto)
        if (op.getTelefono() != null && !op.getTelefono().matches("^\\d{9}$")) {
            throw new ServiceException("El teléfono debe tener 9 dígitos.");
        }
    }
}

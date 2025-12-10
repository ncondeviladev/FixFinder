package com.fixfinder.service.impl;

import com.fixfinder.data.DataRepository;
import com.fixfinder.data.DataRepositoryImpl;
import com.fixfinder.data.interfaces.OperarioDAO;
import com.fixfinder.modelos.Operario;
import com.fixfinder.service.interfaz.OperarioService;
import com.fixfinder.utilidades.DataAccessException;
import com.fixfinder.utilidades.ServiceException;
import java.util.List;

public class OperarioServiceImpl implements OperarioService {

    private final OperarioDAO operarioDAO;

    public OperarioServiceImpl() {
        DataRepository repo = new DataRepositoryImpl();
        this.operarioDAO = repo.getOperarioDAO();
    }

    @Override
    public List<Operario> listarDisponibles(Integer idEmpresa) throws ServiceException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Operario> buscarPorEspecialidad(String especialidad) throws ServiceException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void modificarOperario(Operario operario) throws ServiceException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Operario> listarPorEmpresa(Integer idEmpresa) throws ServiceException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void altaOperario(Operario operario) throws ServiceException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void establecerDisponibilidad(Integer idOperario, boolean disponible) throws ServiceException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}

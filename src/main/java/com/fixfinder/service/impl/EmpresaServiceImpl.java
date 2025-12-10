package com.fixfinder.service.impl;

import com.fixfinder.data.DataRepository;
import com.fixfinder.data.DataRepositoryImpl;
import com.fixfinder.data.interfaces.EmpresaDAO;
import com.fixfinder.modelos.Empresa;
import com.fixfinder.service.interfaz.EmpresaService;
import com.fixfinder.utilidades.DataAccessException;
import com.fixfinder.utilidades.ServiceException;
import java.util.List;

public class EmpresaServiceImpl implements EmpresaService {

    private final EmpresaDAO empresaDAO;

    public EmpresaServiceImpl() {
        DataRepository repo = new DataRepositoryImpl();
        this.empresaDAO = repo.getEmpresaDAO();
    }

    @Override
    public List<Empresa> listarTodas() throws ServiceException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Empresa obtenerEstadisticas(Integer idEmpresa) throws ServiceException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void bajaEmpresa(Integer idEmpresa) throws ServiceException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void registrarEmpresa(Empresa empresa) throws ServiceException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void modificarEmpresa(Empresa empresa) throws ServiceException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}

package com.fixfinder.service.impl;

import com.fixfinder.data.DataRepository;
import com.fixfinder.data.DataRepositoryImpl;
import com.fixfinder.data.interfaces.TrabajoDAO;
import com.fixfinder.modelos.Trabajo;
import com.fixfinder.service.interfaz.TrabajoService;
import com.fixfinder.utilidades.DataAccessException;
import com.fixfinder.utilidades.ServiceException;
import java.util.List;

public class TrabajoServiceImpl implements TrabajoService {

    private final TrabajoDAO trabajoDAO;

    public TrabajoServiceImpl() {
        DataRepository repo = new DataRepositoryImpl();
        this.trabajoDAO = repo.getTrabajoDAO();
    }

    @Override
    public Trabajo solicitarReparacion(Integer idCliente, String descripcion, int urgencia) throws ServiceException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void asignarOperario(Integer idTrabajo, Integer idOperario) throws ServiceException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void iniciarTrabajo(Integer idTrabajo) throws ServiceException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void finalizarTrabajo(Integer idTrabajo, String informeTecnico) throws ServiceException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void cancelarTrabajo(Integer idTrabajo, String motivo) throws ServiceException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Trabajo> listarPendientes(Integer idEmpresa) throws ServiceException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Trabajo> historialCliente(Integer idCliente) throws ServiceException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Trabajo> historialOperario(Integer idOperario) throws ServiceException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}

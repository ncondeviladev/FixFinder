package com.fixfinder.service.impl;

import com.fixfinder.data.DataRepository;
import com.fixfinder.data.DataRepositoryImpl;
import com.fixfinder.data.interfaces.PresupuestoDAO;
import com.fixfinder.modelos.Presupuesto;
import com.fixfinder.service.interfaz.PresupuestoService;
import com.fixfinder.utilidades.ServiceException;
import java.util.List;

public class PresupuestoServiceImpl implements PresupuestoService {

    private final PresupuestoDAO presupuestoDAO;

    public PresupuestoServiceImpl() {
        DataRepository repo = new DataRepositoryImpl();
        this.presupuestoDAO = repo.getPresupuestoDAO();
    }

    @Override
    public Presupuesto crearPresupuesto(Integer idTrabajo, double monto, String detalles) throws ServiceException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void aceptarPresupuesto(Integer idPresupuesto) throws ServiceException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void rechazarPresupuesto(Integer idPresupuesto, String motivo) throws ServiceException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Presupuesto obtenerPorTrabajo(Integer idTrabajo) throws ServiceException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}

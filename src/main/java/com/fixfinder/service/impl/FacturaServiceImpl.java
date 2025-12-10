package com.fixfinder.service.impl;

import com.fixfinder.data.DataRepository;
import com.fixfinder.data.DataRepositoryImpl;
import com.fixfinder.data.interfaces.FacturaDAO;
import com.fixfinder.modelos.Factura;
import com.fixfinder.service.interfaz.FacturaService;
import com.fixfinder.utilidades.ServiceException;

public class FacturaServiceImpl implements FacturaService {

    private final FacturaDAO facturaDAO;

    public FacturaServiceImpl() {
        DataRepository repo = new DataRepositoryImpl();
        this.facturaDAO = repo.getFacturaDAO();
    }

    @Override
    public Factura generarFactura(Integer idTrabajo) throws ServiceException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void marcarPagada(Integer idFactura) throws ServiceException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Factura obtenerFactura(Integer idTrabajo) throws ServiceException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}

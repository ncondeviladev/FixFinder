package com.fixfinder.service.interfaz;

import com.fixfinder.modelos.Factura;
import com.fixfinder.utilidades.ServiceException;

public interface FacturaService {
    Factura generarFactura(int idTrabajo) throws ServiceException;

    Factura obtenerPorTrabajo(int idTrabajo) throws ServiceException;

    void marcarComoPagada(int idFactura) throws ServiceException;
}

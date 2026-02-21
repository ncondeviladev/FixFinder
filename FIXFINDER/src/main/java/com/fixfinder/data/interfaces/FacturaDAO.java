package com.fixfinder.data.interfaces;

import com.fixfinder.modelos.Factura;
import com.fixfinder.utilidades.DataAccessException;

public interface FacturaDAO extends BaseDAO<Factura> {
    Factura obtenerPorTrabajo(int idTrabajo) throws DataAccessException;
}

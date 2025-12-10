package com.fixfinder.data.interfaces;

import com.fixfinder.modelos.Presupuesto;
import com.fixfinder.utilidades.DataAccessException;
import java.util.List;

public interface PresupuestoDAO extends BaseDAO<Presupuesto> {
    List<Presupuesto> obtenerPorTrabajo(int idTrabajo) throws DataAccessException;
}

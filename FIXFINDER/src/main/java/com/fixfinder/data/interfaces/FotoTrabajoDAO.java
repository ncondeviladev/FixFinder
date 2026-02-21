package com.fixfinder.data.interfaces;

import com.fixfinder.modelos.componentes.FotoTrabajo;
import com.fixfinder.utilidades.DataAccessException;
import java.util.List;

public interface FotoTrabajoDAO extends BaseDAO<FotoTrabajo> {
    List<FotoTrabajo> obtenerPorTrabajo(int idTrabajo) throws DataAccessException;
}

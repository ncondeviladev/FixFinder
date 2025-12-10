package com.fixfinder.data.interfaces;

import com.fixfinder.modelos.Trabajo;
import com.fixfinder.modelos.enums.CategoriaServicio;
import com.fixfinder.utilidades.DataAccessException;
import java.util.List;

public interface TrabajoDAO extends BaseDAO<Trabajo> {
    List<Trabajo> obtenerPendientesPorCategoria(CategoriaServicio categoria) throws DataAccessException;
}

package com.fixfinder.data.interfaces;

import com.fixfinder.modelos.Operario;
import com.fixfinder.utilidades.DataAccessException;
import java.util.List;

public interface OperarioDAO extends BaseDAO<Operario> {
    void eliminarPorEmpresa(int idEmpresa) throws DataAccessException;
    List<Operario> obtenerPorEmpresa(int idEmpresa) throws DataAccessException;
}

package com.fixfinder.data;

import com.fixfinder.data.interfaces.EmpresaDAO;
import com.fixfinder.data.interfaces.FacturaDAO;
import com.fixfinder.data.interfaces.FotoTrabajoDAO;
import com.fixfinder.data.interfaces.OperarioDAO;
import com.fixfinder.data.interfaces.PresupuestoDAO;
import com.fixfinder.data.interfaces.TrabajoDAO;
import com.fixfinder.data.interfaces.UsuarioDAO;
import com.fixfinder.data.interfaces.ClienteDAO;

/**
 * Repositorio Central de Datos.
 * Gestiona el acceso a todos los DAOs del sistema.
 */
public interface DataRepository {
    UsuarioDAO getUsuarioDAO();

    EmpresaDAO getEmpresaDAO();

    TrabajoDAO getTrabajoDAO();

    OperarioDAO getOperarioDAO();

    FacturaDAO getFacturaDAO();

    PresupuestoDAO getPresupuestoDAO();

    FotoTrabajoDAO getFotoTrabajoDAO();

    ClienteDAO getClienteDAO();

}

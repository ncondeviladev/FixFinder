package com.fixfinder.data;

import com.fixfinder.data.dao.ClienteDAOImpl;
import com.fixfinder.data.dao.EmpresaDAOImpl;
import com.fixfinder.data.dao.FacturaDAOImpl;
import com.fixfinder.data.dao.FotoTrabajoDAOImpl;
import com.fixfinder.data.dao.OperarioDAOImpl;
import com.fixfinder.data.dao.PresupuestoDAOImpl;
import com.fixfinder.data.dao.TrabajoDAOImpl;
import com.fixfinder.data.dao.UsuarioDAOImpl;
import com.fixfinder.data.interfaces.ClienteDAO;
import com.fixfinder.data.interfaces.EmpresaDAO;
import com.fixfinder.data.interfaces.FacturaDAO;
import com.fixfinder.data.interfaces.FotoTrabajoDAO;
import com.fixfinder.data.interfaces.OperarioDAO;
import com.fixfinder.data.interfaces.PresupuestoDAO;
import com.fixfinder.data.interfaces.TrabajoDAO;
import com.fixfinder.data.interfaces.UsuarioDAO;

/**
 * Implementación básica del Repositorio.
 * Simplemente instancia los DAOs concretos.
 */
public class DataRepositoryImpl implements DataRepository {

    private final UsuarioDAO usuarioDAO;
    private final EmpresaDAO empresaDAO;
    private final TrabajoDAO trabajoDAO;
    private final OperarioDAO operarioDAO;
    private final FacturaDAO facturaDAO;
    private final PresupuestoDAO presupuestoDAO;
    private final FotoTrabajoDAO fotoTrabajoDAO;
    private final ClienteDAO clienteDAO;

    public DataRepositoryImpl() {
        this.usuarioDAO = new UsuarioDAOImpl();
        this.empresaDAO = new EmpresaDAOImpl();
        this.trabajoDAO = new TrabajoDAOImpl();
        this.operarioDAO = new OperarioDAOImpl();
        this.facturaDAO = new FacturaDAOImpl();
        this.presupuestoDAO = new PresupuestoDAOImpl();
        this.fotoTrabajoDAO = new FotoTrabajoDAOImpl();
        this.clienteDAO = new ClienteDAOImpl();
    }

    @Override
    public UsuarioDAO getUsuarioDAO() {
        return usuarioDAO;
    }

    @Override
    public EmpresaDAO getEmpresaDAO() {
        return empresaDAO;
    }

    @Override
    public TrabajoDAO getTrabajoDAO() {
        return trabajoDAO;
    }

    @Override
    public OperarioDAO getOperarioDAO() {
        return operarioDAO;
    }

    @Override
    public FacturaDAO getFacturaDAO() {
        return facturaDAO;
    }

    @Override
    public PresupuestoDAO getPresupuestoDAO() {
        return presupuestoDAO;
    }

    @Override
    public FotoTrabajoDAO getFotoTrabajoDAO() {
        return fotoTrabajoDAO;
    }

    @Override
    public ClienteDAO getClienteDAO() {
        return clienteDAO;
    }
}

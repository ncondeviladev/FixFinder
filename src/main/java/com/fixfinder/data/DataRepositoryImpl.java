package com.fixfinder.data;

import com.fixfinder.data.dao.EmpresaDAOImpl;
import com.fixfinder.data.dao.FacturaDAOImpl;
import com.fixfinder.data.dao.FotoTrabajoDAOImpl;
import com.fixfinder.data.dao.OperarioDAOImpl;
import com.fixfinder.data.dao.PresupuestoDAOImpl;
import com.fixfinder.data.dao.TrabajoDAOImpl;
import com.fixfinder.data.dao.UsuarioDAOImpl;
import com.fixfinder.data.interfaces.EmpresaDAO;
import com.fixfinder.data.interfaces.FacturaDAO;
import com.fixfinder.data.interfaces.FotoTrabajoDAO;
import com.fixfinder.data.interfaces.OperarioDAO;
import com.fixfinder.data.interfaces.PresupuestoDAO;
import com.fixfinder.data.interfaces.TrabajoDAO;
import com.fixfinder.data.interfaces.UsuarioDAO;

public class DataRepositoryImpl implements DataRepository {

    // Instancias Singleton de los DAOs
    private final UsuarioDAO usuarioDAO = new UsuarioDAOImpl();
    private final EmpresaDAO empresaDAO = new EmpresaDAOImpl();
    private final TrabajoDAO trabajoDAO = new TrabajoDAOImpl();
    private final OperarioDAO operarioDAO = new OperarioDAOImpl();
    private final FacturaDAO facturaDAO = new FacturaDAOImpl();
    private final PresupuestoDAO presupuestoDAO = new PresupuestoDAOImpl();
    private final FotoTrabajoDAO fotoTrabajoDAO = new FotoTrabajoDAOImpl();

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
}

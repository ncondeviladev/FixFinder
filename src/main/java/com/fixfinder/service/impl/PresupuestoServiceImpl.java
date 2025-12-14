package com.fixfinder.service.impl;

import com.fixfinder.data.DataRepository;
import com.fixfinder.data.DataRepositoryImpl;
import com.fixfinder.data.interfaces.PresupuestoDAO;
import com.fixfinder.data.interfaces.TrabajoDAO;
import com.fixfinder.modelos.Presupuesto;
import com.fixfinder.modelos.Trabajo;
import com.fixfinder.service.interfaz.PresupuestoService;
import com.fixfinder.utilidades.ServiceException;
import java.util.Date;

public class PresupuestoServiceImpl implements PresupuestoService {

    private final PresupuestoDAO presupuestoDAO;
    private final TrabajoDAO trabajoDAO;

    public PresupuestoServiceImpl() {
        DataRepository repo = new DataRepositoryImpl();
        this.presupuestoDAO = repo.getPresupuestoDAO();
        this.trabajoDAO = repo.getTrabajoDAO();
    }

    @Override
    public Presupuesto crearPresupuesto(Integer idTrabajo, double monto, String detalles) throws ServiceException {
        if (idTrabajo == null)
            throw new ServiceException("El ID del trabajo es obligatorio.");

        try {
            Trabajo trabajo = trabajoDAO.obtenerPorId(idTrabajo);
            if (trabajo == null) {
                throw new ServiceException("Trabajo no encontrado con ID: " + idTrabajo);
            }

            if (trabajo.getCliente() == null) {
                throw new ServiceException("El trabajo no tiene cliente asociado (Datos corruptos).");
            }

            int idEmpresa = trabajo.getCliente().getIdEmpresa();
            
            com.fixfinder.modelos.Empresa empresaRef = new com.fixfinder.modelos.Empresa();
            empresaRef.setId(idEmpresa);

            Presupuesto p = new Presupuesto();
            p.setTrabajo(trabajo);
            p.setEmpresa(empresaRef);
            p.setMonto(monto);
            p.setFechaEnvio(new Date());
            
            presupuestoDAO.insertar(p);
            return p;
        } catch (Exception e) {
            throw new ServiceException("Error al crear presupuesto.", e);
        }
    }

    @Override
    public void aceptarPresupuesto(Integer idPresupuesto) throws ServiceException {
        try {
            Presupuesto p = presupuestoDAO.obtenerPorId(idPresupuesto);
            if (p == null)
                throw new ServiceException("Presupuesto no encontrado.");
        } catch (Exception e) {
            throw new ServiceException("Error al aceptar presupuesto.", e);
        }
    }

    @Override
    public void rechazarPresupuesto(Integer idPresupuesto, String motivo) throws ServiceException {
        try {
            if (idPresupuesto == null)
                throw new ServiceException("ID obligatorio.");
            Presupuesto p = presupuestoDAO.obtenerPorId(idPresupuesto);
            if (p == null)
                throw new ServiceException("Presupuesto no encontrado.");

            presupuestoDAO.eliminar(idPresupuesto);
        } catch (Exception e) {
            throw new ServiceException("Error al rechazar presupuesto.", e);
        }
    }

    @Override
    public Presupuesto obtenerPorTrabajo(Integer idTrabajo) throws ServiceException {
        try {
            return presupuestoDAO.obtenerTodos().stream()
                    .filter(p -> p.getTrabajo() != null && p.getTrabajo().getId() == idTrabajo.intValue())
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            throw new ServiceException("Error al obtener presupuesto por trabajo.", e);
        }
    }
}

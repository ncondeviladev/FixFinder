package com.fixfinder.service.impl;

import com.fixfinder.data.DataRepository;
import com.fixfinder.data.DataRepositoryImpl;
import com.fixfinder.data.interfaces.EmpresaDAO;
import com.fixfinder.data.interfaces.PresupuestoDAO;
import com.fixfinder.data.interfaces.TrabajoDAO;
import com.fixfinder.modelos.Presupuesto;
import com.fixfinder.modelos.Trabajo;
import com.fixfinder.modelos.enums.EstadoPresupuesto;
import com.fixfinder.modelos.enums.EstadoTrabajo;
import com.fixfinder.service.interfaz.PresupuestoService;
import com.fixfinder.utilidades.DataAccessException;
import com.fixfinder.utilidades.ServiceException;

import java.time.LocalDateTime;
import java.util.List;

public class PresupuestoServiceImpl implements PresupuestoService {

    private final PresupuestoDAO presupuestoDAO;
    private final TrabajoDAO trabajoDAO;

    public PresupuestoServiceImpl() {
        DataRepository repo = new DataRepositoryImpl();
        this.presupuestoDAO = repo.getPresupuestoDAO();
        this.trabajoDAO = repo.getTrabajoDAO();
    }

    @Override
    public void crearPresupuesto(Presupuesto presupuesto) throws ServiceException {
        System.out.println("[DEBUG-SERVICE] Entrando en crearPresupuesto");
        try {
            if (presupuesto == null || presupuesto.getTrabajo() == null || presupuesto.getEmpresa() == null) {
                System.out.println("[DEBUG-SERVICE] Datos incompletos: " + presupuesto);
                throw new ServiceException("Datos de presupuesto incompletos.");
            }
            if (presupuesto.getMonto() <= 0) {
                throw new ServiceException("El monto debe ser positivo.");
            }

            // Validar que el trabajo existe y está disponible
            Trabajo t = trabajoDAO.obtenerPorId(presupuesto.getTrabajo().getId());
            if (t == null) {
                System.out.println("[DEBUG-SERVICE] Trabajo no encontrado ID: " + presupuesto.getTrabajo().getId());
                throw new ServiceException("Trabajo no encontrado.");
            }

            if (t.getEstado() != EstadoTrabajo.PENDIENTE && t.getEstado() != EstadoTrabajo.PRESUPUESTADO) {
                System.out
                        .println("[DEBUG-SERVICE] Trabajo no admite más presupuestos. Estado actual: " + t.getEstado());
                throw new ServiceException("El trabajo ya no admite presupuestos (Estado: " + t.getEstado() + ")");
            }

            presupuesto.setFechaEnvio(LocalDateTime.now());
            presupuesto.setEstado(EstadoPresupuesto.PENDIENTE);

            presupuestoDAO.insertar(presupuesto);
            System.out.println("[DEBUG-SERVICE] Presupuesto insertado OK. ID: " + presupuesto.getId());

            // Si el trabajo estaba PENDIENTE, pasarlo a PRESUPUESTADO
            if (t.getEstado() == EstadoTrabajo.PENDIENTE) {
                t.setEstado(EstadoTrabajo.PRESUPUESTADO);
                trabajoDAO.actualizar(t);
                System.out.println("[DEBUG-SERVICE] Trabajo ID " + t.getId() + " pasó a PRESUPUESTADO");
            }

        } catch (DataAccessException e) {
            System.out.println("[DEBUG-SERVICE] Error DAO: " + e.getMessage());
            e.printStackTrace();
            throw new ServiceException("Error al crear presupuesto", e);
        }
    }

    @Override
    public List<Presupuesto> listarPorTrabajo(int idTrabajo) throws ServiceException {
        try {
            return presupuestoDAO.obtenerPorTrabajo(idTrabajo);
        } catch (DataAccessException e) {
            throw new ServiceException("Error al listar presupuestos", e);
        }
    }

    @Override
    public void aceptarPresupuesto(int idPresupuesto) throws ServiceException {
        try {
            Presupuesto pAceptado = presupuestoDAO.obtenerPorId(idPresupuesto);
            if (pAceptado == null)
                throw new ServiceException("Presupuesto no encontrado.");

            // 1. Marcar este como ACEPTADO
            pAceptado.setEstado(EstadoPresupuesto.ACEPTADO);
            presupuestoDAO.actualizar(pAceptado);

            // 2. Marcar resto como RECHAZADO
            List<Presupuesto> todos = presupuestoDAO.obtenerPorTrabajo(pAceptado.getTrabajo().getId());
            for (Presupuesto p : todos) {
                if (p.getId() != idPresupuesto && p.getEstado() == EstadoPresupuesto.PENDIENTE) {
                    p.setEstado(EstadoPresupuesto.RECHAZADO);
                    presupuestoDAO.actualizar(p);
                }
            }

            System.out.println("[DEBUG-SERVICE] Buscando trabajo ID: " + pAceptado.getTrabajo().getId());
            Trabajo t = trabajoDAO.obtenerPorId(pAceptado.getTrabajo().getId());
            if (t == null) {
                throw new ServiceException("Trabajo asociado al presupuesto no encontrado.");
            }

            t.setEstado(EstadoTrabajo.ACEPTADO);
            trabajoDAO.actualizar(t);
            System.out.println("[DEBUG-SERVICE] Trabajo ID " + t.getId() + " pasó a ACEPTADO satisfactoriamente.");

        } catch (DataAccessException e) {
            System.err.println("[DEBUG-SERVICE] Error de acceso a datos al aceptar presupuesto: " + e.getMessage());
            e.printStackTrace();
            throw new ServiceException("Error al aceptar presupuesto en la base de datos", e);
        }
    }

    @Override
    public void rechazarPresupuesto(int idPresupuesto) throws ServiceException {
        try {
            Presupuesto p = presupuestoDAO.obtenerPorId(idPresupuesto);
            if (p != null) {
                p.setEstado(EstadoPresupuesto.RECHAZADO);
                presupuestoDAO.actualizar(p);

                // Si no quedan presupuestos pendientes, volver trabajo a PENDIENTE
                List<Presupuesto> restantes = presupuestoDAO.obtenerPorTrabajo(p.getTrabajo().getId());
                boolean quedaAlguno = restantes.stream()
                        .anyMatch(pr -> pr.getId() != idPresupuesto && pr.getEstado() == EstadoPresupuesto.PENDIENTE);

                if (!quedaAlguno) {
                    Trabajo t = trabajoDAO.obtenerPorId(p.getTrabajo().getId());
                    if (t.getEstado() == EstadoTrabajo.PRESUPUESTADO) {
                        t.setEstado(EstadoTrabajo.PENDIENTE);
                        trabajoDAO.actualizar(t);
                    }
                }
            }
        } catch (DataAccessException e) {
            throw new ServiceException("Error al rechazar presupuesto", e);
        }
    }
}

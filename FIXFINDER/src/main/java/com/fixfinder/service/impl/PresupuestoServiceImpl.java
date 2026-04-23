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
            
            // Inyección estructurada de las notas del presupuesto en el bloque GERENTE
            if (pAceptado.getNotas() != null && !pAceptado.getNotas().trim().isEmpty()) {
                String descActual = t.getDescripcion();
                String marcadorInicio = "💰 GERENTE:\n";
                String marcadorFin = "\n==============================";
                
                int indexInicio = descActual.indexOf(marcadorInicio);
                if (indexInicio != -1) {
                    int posInicioTexto = indexInicio + marcadorInicio.length();
                    int indexFin = descActual.indexOf(marcadorFin, posInicioTexto);
                    
                    if (indexFin != -1) {
                        StringBuilder sb = new StringBuilder();
                        sb.append(descActual, 0, posInicioTexto);
                        sb.append(pAceptado.getNotas().trim());
                        sb.append(descActual.substring(indexFin));
                        t.setDescripcion(sb.toString());
                        System.out.println("✅ [DEBUG-BUDGET] Inyección estructurada completada en bloque GERENTE.");
                    } else {
                        // Fallback por si la estructura está rota
                        t.setDescripcion(descActual + "\n\n[NOTAS GERENTE]: " + pAceptado.getNotas().trim());
                        System.err.println("⚠️ [DEBUG-BUDGET] Estructura de descripción no encontrada, aplicando fallback.");
                    }
                } else {
                    // Fallback si no existe el marcador
                    t.setDescripcion(descActual + "\n\n💰 GERENTE:\n" + pAceptado.getNotas().trim());
                    System.err.println("⚠️ [DEBUG-BUDGET] Marcador '💰 GERENTE:' no encontrado, añadiendo al final.");
                }
            }
            
            trabajoDAO.actualizar(t);
            System.out.println("🚀 [DEBUG-BUDGET] Trabajo ID " + t.getId() + " marcado como ACEPTADO.");

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

                Trabajo t = trabajoDAO.obtenerPorId(p.getTrabajo().getId());
                
                // Si rechazamos el presupuesto que ya estaba ACEPTADO, hay que limpiar y revertir
                if (t.getEstado() == EstadoTrabajo.ACEPTADO) {
                    // 1. Limpiar rastro del gerente en la descripción
                    String desc = t.getDescripcion();
                    int indexGerente = desc.indexOf("💰 GERENTE:\n");
                    int indexCierre = desc.indexOf("\n==============================", indexGerente);
                    
                    if (indexGerente != -1 && indexCierre != -1) {
                        String nuevaDesc = desc.substring(0, indexGerente + "💰 GERENTE:\n".length()) + 
                                          desc.substring(indexCierre);
                        t.setDescripcion(nuevaDesc);
                    }
                }

                // Determinar nuevo estado del trabajo
                List<Presupuesto> restantes = presupuestoDAO.obtenerPorTrabajo(t.getId());
                boolean tienePendientes = restantes.stream()
                        .anyMatch(pr -> pr.getId() != idPresupuesto && pr.getEstado() == EstadoPresupuesto.PENDIENTE);

                if (tienePendientes) {
                    t.setEstado(EstadoTrabajo.PRESUPUESTADO);
                } else {
                    t.setEstado(EstadoTrabajo.PENDIENTE);
                }
                
                trabajoDAO.actualizar(t);
            }
        } catch (DataAccessException e) {
            throw new ServiceException("Error al rechazar presupuesto", e);
        }
    }

    @Override
    public Presupuesto obtenerPorId(int idPresupuesto) throws ServiceException {
        try {
            return presupuestoDAO.obtenerPorId(idPresupuesto);
        } catch (DataAccessException e) {
            throw new ServiceException("Error al obtener presupuesto por ID", e);
        }
    }
}

package com.fixfinder.service.impl;

import com.fixfinder.modelos.Presupuesto;
import com.fixfinder.modelos.Trabajo;
import com.fixfinder.modelos.enums.EstadoPresupuesto;
import com.fixfinder.modelos.enums.EstadoTrabajo;
import com.fixfinder.repository.PresupuestoRepository;
import com.fixfinder.repository.TrabajoRepository;
import com.fixfinder.service.NotificationService;
import com.fixfinder.service.interfaz.PresupuestoService;
import com.fixfinder.utilidades.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementación del servicio de gestión de presupuestos.
 * Maneja la creación, aceptación y rechazo de ofertas económicas emitidas por las empresas.
 */
@Service
public class PresupuestoServiceImpl implements PresupuestoService {

    private final PresupuestoRepository presupuestoRepository;
    private final TrabajoRepository trabajoRepository;
    private final NotificationService notificationService;

    @Autowired
    public PresupuestoServiceImpl(PresupuestoRepository presupuestoRepository, TrabajoRepository trabajoRepository, NotificationService notificationService) {
        this.presupuestoRepository = presupuestoRepository;
        this.trabajoRepository = trabajoRepository;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public void crearPresupuesto(Presupuesto presupuesto) throws ServiceException {
        if (presupuesto == null || presupuesto.getTrabajo() == null || presupuesto.getEmpresa() == null) {
            throw new ServiceException("Datos de presupuesto incompletos.");
        }
        if (presupuesto.getMonto() <= 0) {
            throw new ServiceException("El monto debe ser positivo.");
        }

        Trabajo t = trabajoRepository.findById(presupuesto.getTrabajo().getId()).orElse(null);
        if (t == null) {
            throw new ServiceException("Trabajo no encontrado.");
        }

        if (t.getEstado() != EstadoTrabajo.PENDIENTE && t.getEstado() != EstadoTrabajo.PRESUPUESTADO) {
            throw new ServiceException("El trabajo ya no admite presupuestos (Estado: " + t.getEstado() + ")");
        }

        presupuesto.setFechaEnvio(LocalDateTime.now());
        presupuesto.setEstado(EstadoPresupuesto.PENDIENTE);

        presupuestoRepository.save(presupuesto);

        if (t.getEstado() == EstadoTrabajo.PENDIENTE) {
            t.setEstado(EstadoTrabajo.PRESUPUESTADO);
            trabajoRepository.save(t);
        }
        
        // Notificar nuevo presupuesto
        notificationService.difundirEventoPresupuesto("NUEVO", t.getId(), t.getCliente().getId(), presupuesto.getEmpresa().getId(), "Nuevo presupuesto recibido");
    }

    @Override
    public List<Presupuesto> listarPorTrabajo(int idTrabajo) throws ServiceException {
        return presupuestoRepository.findByTrabajoId(idTrabajo);
    }

    @Override
    @Transactional
    public void aceptarPresupuesto(int idPresupuesto) throws ServiceException {
        Presupuesto pAceptado = presupuestoRepository.findById(idPresupuesto).orElse(null);
        if (pAceptado == null) throw new ServiceException("Presupuesto no encontrado.");

        pAceptado.setEstado(EstadoPresupuesto.ACEPTADO);
        presupuestoRepository.save(pAceptado);

        List<Presupuesto> todos = presupuestoRepository.findByTrabajoId(pAceptado.getTrabajo().getId());
        for (Presupuesto p : todos) {
            if (p.getId() != idPresupuesto && p.getEstado() == EstadoPresupuesto.PENDIENTE) {
                p.setEstado(EstadoPresupuesto.RECHAZADO);
                presupuestoRepository.save(p);
            }
        }

        Trabajo t = trabajoRepository.findById(pAceptado.getTrabajo().getId()).orElse(null);
        if (t == null) {
            throw new ServiceException("Trabajo asociado al presupuesto no encontrado.");
        }

        t.setEstado(EstadoTrabajo.ACEPTADO);
        
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
                } else {
                    t.setDescripcion(descActual + "\n\n[NOTAS GERENTE]: " + pAceptado.getNotas().trim());
                }
            } else {
                t.setDescripcion(descActual + "\n\n💰 GERENTE:\n" + pAceptado.getNotas().trim());
            }
        }
        
        trabajoRepository.save(t);
        
        // Notificar aceptación
        notificationService.difundirEventoPresupuesto("ACEPTADO", t.getId(), t.getCliente().getId(), pAceptado.getEmpresa().getId(), "Presupuesto aceptado por el cliente");
    }

    @Override
    @Transactional
    public void rechazarPresupuesto(int idPresupuesto) throws ServiceException {
        Presupuesto p = presupuestoRepository.findById(idPresupuesto).orElse(null);
        if (p != null) {
            p.setEstado(EstadoPresupuesto.RECHAZADO);
            presupuestoRepository.save(p);

            Trabajo t = trabajoRepository.findById(p.getTrabajo().getId()).orElse(null);
            if (t != null) {
                if (t.getEstado() == EstadoTrabajo.ACEPTADO) {
                    String desc = t.getDescripcion();
                    int indexGerente = desc.indexOf("💰 GERENTE:\n");
                    int indexCierre = desc.indexOf("\n==============================", indexGerente);
                    
                    if (indexGerente != -1 && indexCierre != -1) {
                        String nuevaDesc = desc.substring(0, indexGerente + "💰 GERENTE:\n".length()) + 
                                          desc.substring(indexCierre);
                        t.setDescripcion(nuevaDesc);
                    }
                }

                List<Presupuesto> restantes = presupuestoRepository.findByTrabajoId(t.getId());
                boolean tienePendientes = restantes.stream()
                        .anyMatch(pr -> pr.getId() != idPresupuesto && pr.getEstado() == EstadoPresupuesto.PENDIENTE);

                if (tienePendientes) {
                    t.setEstado(EstadoTrabajo.PRESUPUESTADO);
                } else {
                    t.setEstado(EstadoTrabajo.PENDIENTE);
                }
                
                trabajoRepository.save(t);
                
                // Notificar rechazo
                notificationService.difundirEventoPresupuesto("RECHAZADO", t.getId(), t.getCliente().getId(), p.getEmpresa().getId(), "Presupuesto rechazado");
            }
        }
    }

    @Override
    public Presupuesto obtenerPorId(int idPresupuesto) throws ServiceException {
        return presupuestoRepository.findById(idPresupuesto).orElse(null);
    }
}

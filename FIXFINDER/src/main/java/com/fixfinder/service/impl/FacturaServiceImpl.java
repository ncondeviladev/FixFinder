package com.fixfinder.service.impl;

import com.fixfinder.modelos.Factura;
import com.fixfinder.modelos.Presupuesto;
import com.fixfinder.modelos.Trabajo;
import com.fixfinder.modelos.enums.EstadoPresupuesto;
import com.fixfinder.modelos.enums.EstadoTrabajo;
import com.fixfinder.repository.FacturaRepository;
import com.fixfinder.repository.PresupuestoRepository;
import com.fixfinder.repository.TrabajoRepository;
import com.fixfinder.service.interfaz.FacturaService;
import com.fixfinder.utilidades.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class FacturaServiceImpl implements FacturaService {

    private final FacturaRepository facturaRepository;
    private final TrabajoRepository trabajoRepository;
    private final PresupuestoRepository presupuestoRepository;

    private static final double IVA_PORCENTAJE = 0.21;

    @Autowired
    public FacturaServiceImpl(FacturaRepository facturaRepository, TrabajoRepository trabajoRepository, PresupuestoRepository presupuestoRepository) {
        this.facturaRepository = facturaRepository;
        this.trabajoRepository = trabajoRepository;
        this.presupuestoRepository = presupuestoRepository;
    }

    @Override
    @Transactional
    public Factura generarFactura(int idTrabajo) throws ServiceException {
        Trabajo trabajo = trabajoRepository.findById(idTrabajo).orElse(null);
        if (trabajo == null) throw new ServiceException("Trabajo no encontrado.");

        Factura existente = facturaRepository.findByTrabajoId(idTrabajo);
        if (existente != null) {
            return existente;
        }

        List<Presupuesto> presupuestos = presupuestoRepository.findByTrabajoId(idTrabajo);
        Presupuesto aceptado = null;
        for (Presupuesto p : presupuestos) {
            if (p.getEstado() == EstadoPresupuesto.ACEPTADO) {
                aceptado = p;
                break;
            }
        }

        if (aceptado == null) {
            throw new ServiceException("No se puede facturar un trabajo sin presupuesto aceptado.");
        }

        if (trabajo.getEstado() != EstadoTrabajo.REALIZADO && trabajo.getEstado() != EstadoTrabajo.FINALIZADO) {
            throw new ServiceException("El trabajo debe estar REALIZADO (técnicamente terminado) para poder facturar.");
        }

        Factura factura = new Factura();
        factura.setTrabajo(trabajo);
        factura.setNumeroFactura("F-" + LocalDateTime.now().getYear() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        factura.setFechaEmision(LocalDateTime.now());

        double base = aceptado.getMonto();
        double iva = base * IVA_PORCENTAJE;
        double total = base + iva;

        factura.setBaseImponible(base);
        factura.setIva(iva);
        factura.setTotal(total);
        factura.setPagada(false);
        factura.setRutaPdf("/docs/facturas/" + factura.getNumeroFactura() + ".pdf"); 

        facturaRepository.save(factura);

        if (trabajo.getEstado() != EstadoTrabajo.FINALIZADO) {
            trabajo.setEstado(EstadoTrabajo.FINALIZADO);
            trabajo.setFechaFinalizacion(LocalDateTime.now());
            trabajoRepository.save(trabajo);
        }

        return factura;
    }

    @Override
    public Factura obtenerPorTrabajo(int idTrabajo) throws ServiceException {
        return facturaRepository.findByTrabajoId(idTrabajo);
    }

    @Override
    @Transactional
    public void marcarComoPagada(int idFactura) throws ServiceException {
        Factura f = facturaRepository.findById(idFactura).orElse(null);
        if (f == null) throw new ServiceException("Factura no encontrada.");

        f.setPagada(true);
        facturaRepository.save(f);

        if (f.getTrabajo() != null) {
            Trabajo t = trabajoRepository.findById(f.getTrabajo().getId()).orElse(null);
            if (t != null) {
                t.setEstado(EstadoTrabajo.PAGADO);
                trabajoRepository.save(t);
            }
        }
    }
}

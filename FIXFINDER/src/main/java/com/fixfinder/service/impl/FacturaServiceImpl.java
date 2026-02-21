package com.fixfinder.service.impl;

import com.fixfinder.data.DataRepository;
import com.fixfinder.data.DataRepositoryImpl;
import com.fixfinder.data.interfaces.FacturaDAO;
import com.fixfinder.data.interfaces.PresupuestoDAO;
import com.fixfinder.data.interfaces.TrabajoDAO;
import com.fixfinder.modelos.Factura;
import com.fixfinder.modelos.Presupuesto;
import com.fixfinder.modelos.Trabajo;
import com.fixfinder.modelos.enums.EstadoPresupuesto;
import com.fixfinder.modelos.enums.EstadoTrabajo;
import com.fixfinder.service.interfaz.FacturaService;
import com.fixfinder.utilidades.DataAccessException;
import com.fixfinder.utilidades.ServiceException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class FacturaServiceImpl implements FacturaService {

    private final FacturaDAO facturaDAO;
    private final TrabajoDAO trabajoDAO;
    private final PresupuestoDAO presupuestoDAO;

    private static final double IVA_PORCENTAJE = 0.21;

    public FacturaServiceImpl() {
        DataRepository repo = new DataRepositoryImpl();
        this.facturaDAO = repo.getFacturaDAO();
        this.trabajoDAO = repo.getTrabajoDAO();
        this.presupuestoDAO = repo.getPresupuestoDAO();
    }

    @Override
    public Factura generarFactura(int idTrabajo) throws ServiceException {
        try {
            Trabajo trabajo = trabajoDAO.obtenerPorId(idTrabajo);
            if (trabajo == null)
                throw new ServiceException("Trabajo no encontrado.");

            // Verificamos si ya existe factura
            Factura existente = facturaDAO.obtenerPorTrabajo(idTrabajo);
            if (existente != null) {
                return existente; // Ya generada
            }

            // Buscamos el presupuesto aceptado
            List<Presupuesto> presupuestos = presupuestoDAO.obtenerPorTrabajo(idTrabajo);
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

            Factura factura = new Factura();
            factura.setTrabajo(trabajo);
            factura.setNumeroFactura("F-" + LocalDateTime.now().getYear() + "-"
                    + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            factura.setFechaEmision(LocalDateTime.now());

            double base = aceptado.getMonto();
            double iva = base * IVA_PORCENTAJE;
            double total = base + iva;

            factura.setBaseImponible(base);
            factura.setIva(iva);
            factura.setTotal(total);
            factura.setPagada(false);
            factura.setRutaPdf("/docs/facturas/" + factura.getNumeroFactura() + ".pdf"); // Simulado

            facturaDAO.insertar(factura);

            // Validar estado previo (Debe estar REALIZADO)
            if (trabajo.getEstado() != EstadoTrabajo.REALIZADO && trabajo.getEstado() != EstadoTrabajo.FINALIZADO) {
                throw new ServiceException(
                        "El trabajo debe estar REALIZADO (técnicamente terminado) para poder facturar.");
            }

            // Actualizamos estado trabajo a FINALIZADO (Ciclo cerrado y facturado)
            if (trabajo.getEstado() != EstadoTrabajo.FINALIZADO) {
                trabajo.setEstado(EstadoTrabajo.FINALIZADO);
                trabajo.setFechaFinalizacion(LocalDateTime.now());
                trabajoDAO.actualizar(trabajo);
                System.out.println(
                        "[DEBUG-SERVICE] Trabajo ID " + trabajo.getId() + " pasado a FINALIZADO por facturación.");
            }

            return factura;

        } catch (DataAccessException e) {
            throw new ServiceException("Error al generar factura", e);
        }
    }

    @Override
    public Factura obtenerPorTrabajo(int idTrabajo) throws ServiceException {
        try {
            return facturaDAO.obtenerPorTrabajo(idTrabajo);
        } catch (DataAccessException e) {
            throw new ServiceException("Error al obtener factura", e);
        }
    }

    @Override
    public void marcarComoPagada(int idFactura) throws ServiceException {
        try {
            Factura f = facturaDAO.obtenerPorId(idFactura);
            if (f == null)
                throw new ServiceException("Factura no encontrada.");

            f.setPagada(true);
            facturaDAO.actualizar(f);

            // Actualizar trabajo a PAGADO
            if (f.getTrabajo() != null) {
                Trabajo t = trabajoDAO.obtenerPorId(f.getTrabajo().getId());
                if (t != null) {
                    t.setEstado(EstadoTrabajo.PAGADO);
                    trabajoDAO.actualizar(t);
                    System.out.println("[DEBUG-SERVICE] Trabajo ID " + t.getId() + " pasado a PAGADO.");
                }
            }
        } catch (DataAccessException e) {
            throw new ServiceException("Error al pagar factura", e);
        }
    }
}

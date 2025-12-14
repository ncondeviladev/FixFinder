package com.fixfinder.service.impl;

import com.fixfinder.data.DataRepository;
import com.fixfinder.data.DataRepositoryImpl;
import com.fixfinder.data.interfaces.FacturaDAO;
import com.fixfinder.modelos.Factura;
import com.fixfinder.modelos.Trabajo;
import com.fixfinder.service.interfaz.FacturaService;
import com.fixfinder.utilidades.ServiceException;
import java.time.LocalDateTime;

public class FacturaServiceImpl implements FacturaService {

    private final FacturaDAO facturaDAO;

    public FacturaServiceImpl() {
        DataRepository repo = new DataRepositoryImpl();
        this.facturaDAO = repo.getFacturaDAO();
    }

    @Override
    public Factura generarFactura(Integer idTrabajo) throws ServiceException {
        if (idTrabajo == null)
            throw new ServiceException("ID de trabajo obligatorio.");
        try {
            Factura existing = obtenerFactura(idTrabajo); // Reutilizamos lógica de búsqueda
            if (existing != null) {
                return existing;
            }

            // Creamos referencia a Trabajo para asignarlo a la Factura
            Trabajo trabajoRef = new Trabajo();
            trabajoRef.setId(idTrabajo);

            // Lógica simple de generación
            Factura factura = new Factura();
            factura.setTrabajo(trabajoRef);
            factura.setFechaEmision(LocalDateTime.now());
            factura.setBaseImponible(100.0);
            factura.setIva(21.0);
            factura.setTotal(121.0);
            factura.setPagada(false);
            factura.setNumeroFactura("FACT-" + System.currentTimeMillis());

            facturaDAO.insertar(factura);
            return factura;

        } catch (Exception e) {
            throw new ServiceException("Error al generar la factura.", e);
        }
    }

    @Override
    public void marcarPagada(Integer idFactura) throws ServiceException {
        try {
            if (idFactura == null)
                throw new ServiceException("ID de factura obligatorio.");
            Factura f = facturaDAO.obtenerPorId(idFactura);
            if (f == null)
                throw new ServiceException("Factura no encontrada.");

            f.setPagada(true);
            facturaDAO.actualizar(f);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("Error al marcar factura como pagada.", e);
        }
    }

    @Override
    public Factura obtenerFactura(Integer idTrabajo) throws ServiceException {
        try {
            // Buscamos iterando si el DAO no tiene busqueda específica
            return facturaDAO.obtenerTodos().stream()
                    .filter(f -> f.getTrabajo() != null && f.getTrabajo().getId() == idTrabajo.intValue())
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            throw new ServiceException("Error al obtener la factura.", e);
        }
    }
}

package com.fixfinder.service.interfaz;

import com.fixfinder.modelos.Factura;
import com.fixfinder.utilidades.ServiceException;

/**
 * Interfaz del Servicio de Facturación.
 * Gestiona la creación y cálculo de facturas.
 */
public interface FacturaService {

    /**
     * Genera una factura para un trabajo finalizado.
     *
     * @param idTrabajo ID del trabajo completado.
     * @return La factura generada y guardada.
     * @throws ServiceException Si el trabajo no está finalizado o ya tiene factura.
     */
    Factura generarFactura(Integer idTrabajo) throws ServiceException;

    /**
     * Marca una factura como pagada.
     *
     * @param idFactura ID de la factura.
     * @throws ServiceException Error al actualizar.
     */
    void marcarPagada(Integer idFactura) throws ServiceException;

    /**
     * Obtiene la factura asociada a un trabajo.
     *
     * @param idTrabajo ID del trabajo.
     * @return La factura, o null si no existe.
     * @throws ServiceException Error de acceso.
     */
    Factura obtenerFactura(Integer idTrabajo) throws ServiceException;

    // Nota: El método calcularTotal se considera interno de la implementación
    // al generar la factura, pero podría exponerse si se necesita simular precios.
}

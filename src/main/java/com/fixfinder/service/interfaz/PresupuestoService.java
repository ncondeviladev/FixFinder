package com.fixfinder.service.interfaz;

import com.fixfinder.modelos.Presupuesto;
import com.fixfinder.utilidades.ServiceException;

/**
 * Interfaz del Servicio de Presupuestos.
 * Gestiona la evaluación económica de los trabajos antes de su ejecución.
 */
public interface PresupuestoService {

    /**
     * Crea un nuevo presupuesto para un trabajo solicitado.
     *
     * @param idTrabajo ID del trabajo asociado.
     * @param monto     Importe propuesto.
     * @param detalles  Detalles de lo que incluye.
     * @return Presupuesto creado (Estado PENDIENTE).
     * @throws ServiceException Si ya existe presupuesto o trabajo no válido.
     */
    Presupuesto crearPresupuesto(Integer idTrabajo, double monto, String detalles) throws ServiceException;

    /**
     * Cliente acepta un presupuesto.
     *
     * @param idPresupuesto ID del presupuesto.
     * @throws ServiceException Error al actualizar.
     */
    void aceptarPresupuesto(Integer idPresupuesto) throws ServiceException;

    /**
     * Cliente rechaza un presupuesto.
     *
     * @param idPresupuesto ID del presupuesto.
     * @param motivo        Razón del rechazo.
     * @throws ServiceException Error al actualizar.
     */
    void rechazarPresupuesto(Integer idPresupuesto, String motivo) throws ServiceException;

    /**
     * Obtiene el presupuesto asociado a un trabajo.
     *
     * @param idTrabajo ID del trabajo.
     * @return El presupuesto o null.
     * @throws ServiceException Error lectura.
     */
    Presupuesto obtenerPorTrabajo(Integer idTrabajo) throws ServiceException;
}

package com.fixfinder.service.interfaz;

import com.fixfinder.modelos.Presupuesto;
import com.fixfinder.utilidades.ServiceException;
import java.util.List;

public interface PresupuestoService {
    void crearPresupuesto(Presupuesto presupuesto) throws ServiceException;

    List<Presupuesto> listarPorTrabajo(int idTrabajo) throws ServiceException;

    void aceptarPresupuesto(int idPresupuesto) throws ServiceException;

    void rechazarPresupuesto(int idPresupuesto) throws ServiceException;
}

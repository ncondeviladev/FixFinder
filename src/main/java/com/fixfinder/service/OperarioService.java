package com.fixfinder.service;

import com.fixfinder.modelos.Operario;  
import com.fixfinder.utilidades.ServiceException;
import java.util.List;

/**
 * Interfaz del Servicio de Operarios.
 * Gestiona la plantilla de trabajadores y su disponibilidad.
 */
public interface OperarioService {

    /**
     * Da de alta un nuevo operario en la empresa.
     *
     * @param operario Datos del operario.
     * @throws ServiceException Error de validación.
     */
    void altaOperario(Operario operario) throws ServiceException;

    /**
     * Modifica datos de un operario.
     *
     * @param operario Operario actualizado.
     * @throws ServiceException Error en BD.
     */
    void modificarOperario(Operario operario) throws ServiceException;

    /**
     * Lista todos los operarios de una empresa.
     *
     * @param idEmpresa ID de la empresa.
     * @return Lista de operarios.
     * @throws ServiceException Error de acceso.
     */
    List<Operario> listarPorEmpresa(Integer idEmpresa) throws ServiceException;

    /**
     * Lista operarios que están actualmente disponibles (no ocupados ni de baja).
     *
     * @param idEmpresa ID de la empresa.
     * @return Lista de operarios libres.
     * @throws ServiceException Error de acceso.
     */
    List<Operario> listarDisponibles(Integer idEmpresa) throws ServiceException;

    /**
     * Busca operarios por su especialidad técnica.
     *
     * @param especialidad Nombre o ID de la especialidad.
     * @return Lista de operarios.
     * @throws ServiceException Error de acceso.
     */
    List<Operario> buscarPorEspecialidad(String especialidad) throws ServiceException;

    /**
     * Cambia el estado de disponibilidad de un operario manual o automáticamente.
     *
     * @param idOperario ID del operario.
     * @param disponible true si está disponible, false si está ocupado/baja.
     * @throws ServiceException Error al actualizar.
     */
    void establecerDisponibilidad(Integer idOperario, boolean disponible) throws ServiceException;
}

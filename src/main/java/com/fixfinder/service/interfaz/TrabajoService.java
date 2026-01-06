package com.fixfinder.service.interfaz;

import com.fixfinder.modelos.Trabajo;
import com.fixfinder.modelos.enums.CategoriaServicio;
import com.fixfinder.utilidades.ServiceException;
import java.util.List;

/**
 * Interfaz del Servicio de Trabajos.
 * Core del negocio: Solicitudes, asignaciones y finalización.
 */
public interface TrabajoService {

    /**
     * Crea una nueva solicitud de reparación por parte de un cliente.
     *
     * @param idCliente   ID del cliente que solicita.
     * @param descripcion Descripción del problema.
     * @param direccion   Dirección donde se realizará el trabajo.
     * @param urgencia    Nivel de urgencia (1-5 ó enum).
     * @return El trabajo creado con estado PENDIENTE.
     * @throws ServiceException Error al crear.
     */
    Trabajo solicitarReparacion(Integer idCliente, String titulo, CategoriaServicio categoria, String descripcion,
            String direccion,
            int urgencia)
            throws ServiceException;

    /**
     * Asigna un operario a un trabajo existente.
     *
     * @param idTrabajo  ID del trabajo.
     * @param idOperario ID del operario.
     * @throws ServiceException Si el trabajo no existe o el operario no está
     *                          disponible.
     */
    void asignarOperario(Integer idTrabajo, Integer idOperario) throws ServiceException;

    /**
     * El operario marca el comienzo del trabajo.
     *
     * @param idTrabajo ID del trabajo.
     * @throws ServiceException Si el estado no permite iniciar.
     */
    void iniciarTrabajo(Integer idTrabajo) throws ServiceException;

    /**
     * Finaliza un trabajo, añadiendo el informe técnico.
     *
     * @param idTrabajo      ID del trabajo.
     * @param informeTecnico Descripción de la solución.
     * @throws ServiceException Error al finalizar.
     */
    void finalizarTrabajo(Integer idTrabajo, String informeTecnico) throws ServiceException;

    /**
     * Cancela una solicitud de trabajo.
     *
     * @param idTrabajo ID del trabajo.
     * @param motivo    Motivo de cancelación.
     * @throws ServiceException Si ya está finalizado no se puede cancelar.
     */
    void cancelarTrabajo(Integer idTrabajo, String motivo) throws ServiceException;

    /**
     * Lista trabajos pendientes de asignar.
     *
     * @param idEmpresa ID de la empresa (opcional para filtrar).
     * @return Lista de trabajos.
     * @throws ServiceException Error de acceso.
     */
    List<Trabajo> listarPendientes(Integer idEmpresa) throws ServiceException;

    /**
     * Historial de trabajos de un cliente.
     *
     * @param idCliente ID del cliente.
     * @return Lista de trabajos.
     * @throws ServiceException Error de acceso.
     */
    List<Trabajo> historialCliente(Integer idCliente) throws ServiceException;

    /**
     * Historial de trabajos realizados por un operario.
     *
     * @param idOperario ID del operario.
     * @return Lista de trabajos.
     * @throws ServiceException Error de acceso.
     */
    List<Trabajo> historialOperario(Integer idOperario) throws ServiceException;

    /**
     * Lista TODOS los trabajos del sistema (Vista Admin/Gerente).
     * 
     * @return Lista completa de trabajos.
     * @throws ServiceException Error de acceso.
     */
    List<Trabajo> listarTodos() throws ServiceException;
}

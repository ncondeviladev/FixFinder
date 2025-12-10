package com.fixfinder.service.interfaz;

import com.fixfinder.modelos.Empresa;
import com.fixfinder.utilidades.ServiceException;
import java.util.List;

/**
 * Interfaz del Servicio de Empresas.
 * Gestiona el alta, baja y modificación de empresas proveedoras.
 */
public interface EmpresaService {

    /**
     * Registra una nueva empresa en el sistema.
     *
     * @param empresa Datos de la empresa.
     * @throws ServiceException Si el CIF ya existe.
     */
    void registrarEmpresa(Empresa empresa) throws ServiceException;

    /**
     * Modifica los datos de una empresa existente.
     *
     * @param empresa Empresa con datos actualizados.
     * @throws ServiceException Si error en BD.
     */
    void modificarEmpresa(Empresa empresa) throws ServiceException;

    /**
     * Da de baja una empresa.
     *
     * @param idEmpresa ID de la empresa.
     * @throws ServiceException Si tiene trabajos pendientes.
     */
    void bajaEmpresa(Integer idEmpresa) throws ServiceException;

    /**
     * Lista todas las empresas del sistema.
     *
     * @return Lista de empresas.
     * @throws ServiceException Error de acceso.
     */
    List<Empresa> listarTodas() throws ServiceException;

    /**
     * Obtiene estadísticas o datos agregados de la empresa.
     *
     * @param idEmpresa ID de la empresa.
     * @return Objeto empresa con datos (podría ser un DTO en el futuro).
     * @throws ServiceException Error de acceso.
     */
    Empresa obtenerEstadisticas(Integer idEmpresa) throws ServiceException;
}

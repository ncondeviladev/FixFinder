package com.fixfinder.service.impl;

import com.fixfinder.data.DataRepository;
import com.fixfinder.data.DataRepositoryImpl;
import com.fixfinder.data.interfaces.EmpresaDAO;
import com.fixfinder.modelos.Empresa;
import com.fixfinder.service.interfaz.EmpresaService;
import com.fixfinder.utilidades.DataAccessException;
import com.fixfinder.utilidades.ServiceException;
import java.util.List;

public class EmpresaServiceImpl implements EmpresaService {

    private final EmpresaDAO empresaDAO;

    public EmpresaServiceImpl() {
        DataRepository repo = new DataRepositoryImpl();
        this.empresaDAO = repo.getEmpresaDAO();
    }

    @Override
    public List<Empresa> listarTodas() throws ServiceException {
        try {
            return empresaDAO.obtenerTodos();
        } catch (DataAccessException e) {
            throw new ServiceException("Error al listar las empresas.", e);
        }
    }

    @Override
    public Empresa obtenerEstadisticas(Integer idEmpresa) throws ServiceException {

        try {
            if (idEmpresa == null)
                throw new ServiceException("El ID de empresa es obligatorio.");
            Empresa empresa = empresaDAO.obtenerPorId(idEmpresa);
            if (empresa == null)
                throw new ServiceException("Empresa no encontrada.");
            return empresa;
        } catch (DataAccessException e) {
            throw new ServiceException("Error al obtener estadísticas de empresa.", e);
        }
    }

    @Override
    public void bajaEmpresa(Integer idEmpresa) throws ServiceException {
        try {
            if (idEmpresa == null)
                throw new ServiceException("El ID de empresa es obligatorio para dar de baja.");
            // Verificamos existencia antes de borrar
            Empresa existente = empresaDAO.obtenerPorId(idEmpresa);
            if (existente == null)
                throw new ServiceException("No existe empresa con el ID proporcionado.");

            empresaDAO.eliminar(idEmpresa);
        } catch (DataAccessException e) {
            throw new ServiceException("Error al dar de baja la empresa.", e);
        }
    }

    @Override
    public void registrarEmpresa(Empresa empresa) throws ServiceException {
        try {
            validarDatosEmpresa(empresa);

            empresaDAO.insertar(empresa);
        } catch (DataAccessException e) {
            throw new ServiceException("Error al registrar la empresa.", e);
        }
    }

    @Override
    public void modificarEmpresa(Empresa empresa) throws ServiceException {
        try {
            if (empresa.getId() == 0)
                throw new ServiceException("El ID de empresa es necesario para modificar.");
            validarDatosEmpresa(empresa);

            // Verificamos que exista
            Empresa existente = empresaDAO.obtenerPorId(empresa.getId());
            if (existente == null)
                throw new ServiceException("La empresa a modificar no existe.");

            empresaDAO.actualizar(empresa);
        } catch (DataAccessException e) {
            throw new ServiceException("Error al modificar la empresa.", e);
        }
    }

    /**
     * Valida los datos obligatorios y formatos de la empresa.
     * 
     * @param empresa Objeto Empresa a validar.
     * @throws ServiceException Si algún dato es inválido.
     */
    private void validarDatosEmpresa(Empresa empresa) throws ServiceException {
        if (empresa == null)
            throw new ServiceException("El objeto empresa no puede ser nulo.");

        if (empresa.getNombre() == null || empresa.getNombre().trim().isEmpty()) {
            throw new ServiceException("El nombre de la empresa es obligatorio.");
        }

        if (empresa.getCif() == null || empresa.getCif().trim().isEmpty()) {
            throw new ServiceException("El CIF es obligatorio.");
        }

        if (empresa.getEmailContacto() != null && !empresa.getEmailContacto().isEmpty()) {
            if (!empresa.getEmailContacto().matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
                throw new ServiceException("El formato del email de contacto no es válido.");
            }
        }

        // Validación básica de foto
        if (empresa.getUrlFoto() != null && empresa.getUrlFoto().length() > 255) {
            throw new ServiceException("La URL de la foto excede los 255 caracteres.");
        }
    }
}

package com.fixfinder.service.impl;

import com.fixfinder.data.DataRepository;
import com.fixfinder.data.DataRepositoryImpl;
import com.fixfinder.data.interfaces.OperarioDAO;
import com.fixfinder.data.interfaces.TrabajoDAO;
import com.fixfinder.data.interfaces.UsuarioDAO;
import com.fixfinder.modelos.Operario;
import com.fixfinder.modelos.Trabajo;
import com.fixfinder.modelos.Usuario;
import com.fixfinder.modelos.enums.CategoriaServicio;
import com.fixfinder.modelos.enums.EstadoTrabajo;
import com.fixfinder.service.interfaz.TrabajoService;
import com.fixfinder.utilidades.DataAccessException;
import com.fixfinder.utilidades.ServiceException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class TrabajoServiceImpl implements TrabajoService {

    private final TrabajoDAO trabajoDAO;
    private final UsuarioDAO usuarioDAO;
    private final OperarioDAO operarioDAO;

    public TrabajoServiceImpl() {
        DataRepository repo = new DataRepositoryImpl();
        this.trabajoDAO = repo.getTrabajoDAO();
        this.usuarioDAO = repo.getUsuarioDAO();
        this.operarioDAO = repo.getOperarioDAO();
    }

    @Override
    public Trabajo solicitarReparacion(Integer idCliente, String titulo, CategoriaServicio categoria,
            String descripcion,
            String direccion, int urgencia)
            throws ServiceException {
        try {
            if (idCliente == null)
                throw new ServiceException("El ID del cliente no puede ser nulo.");
            if (descripcion == null || descripcion.trim().isEmpty())
                throw new ServiceException("La descripción es obligatoria.");

            Usuario cliente = usuarioDAO.obtenerPorId(idCliente);
            if (cliente == null)
                throw new ServiceException("Cliente no encontrado con ID: " + idCliente);

            Trabajo trabajo = new Trabajo();
            trabajo.setCliente(cliente);
            trabajo.setCategoria(categoria != null ? categoria : CategoriaServicio.OTROS);

            // Título: Usar el recibido o generarlo si está vacío/nulo
            if (titulo == null || titulo.trim().isEmpty()) {
                String tituloBase = descripcion;
                // Intentar limpiar tags si el usuario no dio título pero sí descripción con tag
                if (descripcion.startsWith("[URGENTE") || descripcion.startsWith("[PRIORIDAD")) {
                    int finTag = descripcion.indexOf("] ");
                    if (finTag != -1) {
                        tituloBase = descripcion.substring(finTag + 2);
                    }
                }
                trabajo.setTitulo(tituloBase.length() > 20 ? tituloBase.substring(0, 20) + "..." : tituloBase);
            } else {
                trabajo.setTitulo(titulo);
            }

            trabajo.setDescripcion(descripcion);
            trabajo.setDireccion(direccion != null && !direccion.isEmpty() ? direccion : "Sin dirección especificada");
            trabajo.setEstado(EstadoTrabajo.PENDIENTE);
            trabajo.setFechaCreacion(LocalDateTime.now());
            // La urgencia podría usarse para priorizar, por ahora no está en el modelo
            // Trabajo explícitamente más allá de descripción o prioridad futura.

            trabajoDAO.insertar(trabajo);
            return trabajo;

        } catch (DataAccessException e) {
            throw new ServiceException("Error al registrar la solicitud de reparación.", e);
        }
    }

    @Override
    public void asignarOperario(Integer idTrabajo, Integer idOperario) throws ServiceException {
        try {
            Trabajo trabajo = trabajoDAO.obtenerPorId(idTrabajo);
            if (trabajo == null)
                throw new ServiceException("Trabajo no encontrado.");

            if (trabajo.getEstado() == EstadoTrabajo.FINALIZADO || trabajo.getEstado() == EstadoTrabajo.CANCELADO) {
                throw new ServiceException("No se puede asignar operario a un trabajo finalizado o cancelado.");
            }

            Operario operario = operarioDAO.obtenerPorId(idOperario);
            if (operario == null)
                throw new ServiceException("Operario no encontrado.");

            trabajo.setOperarioAsignado(operario);
            trabajo.setEstado(EstadoTrabajo.ASIGNADO);
            trabajoDAO.actualizar(trabajo);

        } catch (DataAccessException e) {
            throw new ServiceException("Error al asignar operario.", e);
        }
    }

    @Override
    public void iniciarTrabajo(Integer idTrabajo) throws ServiceException {
        try {
            Trabajo trabajo = trabajoDAO.obtenerPorId(idTrabajo);
            if (trabajo == null)
                throw new ServiceException("Trabajo no encontrado.");

            if (trabajo.getOperarioAsignado() == null)
                throw new ServiceException("No se puede iniciar un trabajo sin operario asignado.");

            trabajo.setEstado(EstadoTrabajo.EN_PROCESO);
            trabajoDAO.actualizar(trabajo);

        } catch (DataAccessException e) {
            throw new ServiceException("Error al iniciar el trabajo.", e);
        }
    }

    @Override
    public void finalizarTrabajo(Integer idTrabajo, String informeTecnico) throws ServiceException {
        try {
            Trabajo trabajo = trabajoDAO.obtenerPorId(idTrabajo);
            if (trabajo == null)
                throw new ServiceException("Trabajo no encontrado.");

            if (trabajo.getEstado() != EstadoTrabajo.EN_PROCESO) {
                // Se podría permitir finalizar desde Asignado si fue express, pero lo ideal es
                // seguir flujo
                // throw new ServiceException("El trabajo debe estar EN_PROCESO para
                // finalizarse.");
            }

            trabajo.setEstado(EstadoTrabajo.FINALIZADO);
            trabajo.setFechaFinalizacion(LocalDateTime.now());
            // Si el modelo tuviera campo para 'informeTecnico', lo setearíamos aquí.
            // Por ahora lo añadimos a la descripción o comentarios si es necesario.
            // trabajo.setInforme(informeTecnico);

            trabajoDAO.actualizar(trabajo);

        } catch (DataAccessException e) {
            throw new ServiceException("Error al finalizar el trabajo.", e);
        }
    }

    @Override
    public void cancelarTrabajo(Integer idTrabajo, String motivo) throws ServiceException {
        try {
            Trabajo trabajo = trabajoDAO.obtenerPorId(idTrabajo);
            if (trabajo == null)
                throw new ServiceException("Trabajo no encontrado.");

            if (trabajo.getEstado() == EstadoTrabajo.FINALIZADO) {
                throw new ServiceException("No se puede cancelar un trabajo ya finalizado.");
            }

            if (trabajo.getEstado() == EstadoTrabajo.EN_PROCESO) {
                throw new ServiceException(
                        "No se puede cancelar un trabajo que ya está en proceso. Contacte con la empresa.");
            }

            // Si tenía operario asignado (pero no empezó), lo liberamos
            if (trabajo.getOperarioAsignado() != null) {
                trabajo.setOperarioAsignado(null);
            }

            trabajo.setEstado(EstadoTrabajo.CANCELADO);
            trabajo.setDescripcion(trabajo.getDescripcion() + " [CANCELADO: " + motivo + "]");
            trabajoDAO.actualizar(trabajo);

        } catch (DataAccessException e) {
            throw new ServiceException("Error al cancelar el trabajo.", e);
        }
    }

    @Override
    public List<Trabajo> listarPendientes(Integer idEmpresa) throws ServiceException {
        try {
            List<Trabajo> todos = trabajoDAO.obtenerTodos();

            return todos.stream()
                    .filter(t -> t.getEstado() == EstadoTrabajo.PENDIENTE)
                    // Filtro de empresa eliminado porque el Cliente ya no pertenece a una empresa.
                    // Ahora los trabajos pendientes son visibles para todos (Modelo Marketplace)
                    // .filter(t -> idEmpresa == null || ... )
                    .collect(Collectors.toList());

        } catch (DataAccessException e) {
            throw new ServiceException("Error al listar trabajos pendientes.", e);
        }
    }

    @Override
    public List<Trabajo> historialCliente(Integer idCliente) throws ServiceException {
        try {
            List<Trabajo> todos = trabajoDAO.obtenerTodos();
            return todos.stream()
                    .filter(t -> t.getCliente() != null && t.getCliente().getId() == idCliente.intValue())
                    .collect(Collectors.toList());
        } catch (DataAccessException e) {
            throw new ServiceException("Error al obtener historial del cliente.", e);
        }
    }

    @Override
    public List<Trabajo> historialOperario(Integer idOperario) throws ServiceException {
        try {
            List<Trabajo> todos = trabajoDAO.obtenerTodos();
            return todos.stream()
                    .filter(t -> t.getOperarioAsignado() != null
                            && t.getOperarioAsignado().getId() == idOperario.intValue())
                    .collect(Collectors.toList());
        } catch (DataAccessException e) {
            throw new ServiceException("Error al obtener historial del operario.", e);
        }
    }
}

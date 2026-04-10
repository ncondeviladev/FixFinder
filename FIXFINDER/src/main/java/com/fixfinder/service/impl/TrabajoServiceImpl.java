package com.fixfinder.service.impl;

import com.fixfinder.data.DataRepository;
import com.fixfinder.data.DataRepositoryImpl;
import com.fixfinder.data.interfaces.OperarioDAO;
import com.fixfinder.data.interfaces.TrabajoDAO;
import com.fixfinder.data.interfaces.UsuarioDAO;
import com.fixfinder.data.dao.TrabajoDAOImpl;
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

            // Estructura predefinida de la descripción
            String descEstructurada = "==============================\n" +
                    "📝 CLIENTE:\n" + descripcion.trim() + "\n" +
                    "==============================\n" +
                    "💰 GERENTE:\n(Sin presupuesto redactado)\n" +
                    "==============================\n" +
                    "🛠 OPERARIO:\n(Sin informe de trabajo)\n" +
                    "==============================";

            // Si no viene dirección, usar la del cliente registrado
            String dirFinal = (direccion != null && !direccion.trim().isEmpty())
                    ? direccion
                    : (cliente.getDireccion() != null && !cliente.getDireccion().trim().isEmpty()
                            ? cliente.getDireccion()
                            : "Sin dirección especificada");

            trabajo.setDescripcion(descEstructurada);
            trabajo.setDireccion(dirFinal);
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

            // Lógica de DESASIGNACIÓN (Si idOperario es null o <= 0)
            if (idOperario == null || idOperario <= 0) {
                trabajo.setOperarioAsignado(null);
                // Si estaba asignado, lo lógico es que vuelva a ACEPTADO (presupuesto aceptado)
                // para que otro técnico pueda ser asignado. PENDIENTE es para trabajos sin
                // presupuesto.
                if (trabajo.getEstado() == EstadoTrabajo.ASIGNADO) {
                    trabajo.setEstado(EstadoTrabajo.ACEPTADO);
                } else {
                    trabajo.setEstado(EstadoTrabajo.PENDIENTE);
                }
            }
            // Lógica de ASIGNACIÓN
            else {
                Operario operario = operarioDAO.obtenerPorId(idOperario);
                if (operario == null)
                    throw new ServiceException("Operario no encontrado.");

                trabajo.setOperarioAsignado(operario);
                trabajo.setEstado(EstadoTrabajo.ASIGNADO);
            }

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

            // ASIGNADO ya implica que está en proceso
            if (trabajo.getEstado() != EstadoTrabajo.ASIGNADO) {
                trabajo.setEstado(EstadoTrabajo.ASIGNADO);
                trabajoDAO.actualizar(trabajo);
            }

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

            if (trabajo.getEstado() != EstadoTrabajo.ASIGNADO) {
                throw new ServiceException("El trabajo debe estar ASIGNADO para finalizarse.");
            }

            trabajo.setEstado(EstadoTrabajo.REALIZADO);
            trabajo.setFechaFinalizacion(LocalDateTime.now());

            if (informeTecnico != null && !informeTecnico.trim().isEmpty()
                    && !informeTecnico.equals("Trabajo finalizado correctamente (Simulador).")) {
                // Insertar el informe en el bloque correspondiente de la descripción
                String descActual = trabajo.getDescripcion();
                if (descActual.contains("🛠 OPERARIO:")) {
                    String[] partes = descActual.split("🛠 OPERARIO:");
                    String parteSuperior = partes[0];
                    String parteInferior = partes.length > 1 ? partes[1] : "";

                    // Si hay un delimitador de cierre después de OPERARIO, lo respetamos
                    if (parteInferior.contains("==============================")) {
                        int posCierre = parteInferior.indexOf("==============================");
                        String resto = parteInferior.substring(posCierre);
                        trabajo.setDescripcion(
                                parteSuperior + "🛠 OPERARIO:\n" + informeTecnico.trim() + "\n" + resto);
                    } else {
                        trabajo.setDescripcion(parteSuperior + "🛠 OPERARIO:\n" + informeTecnico.trim());
                    }
                } else {
                    // Fallback si no tiene la estructura (no debería pasar)
                    trabajo.setDescripcion(descActual + "\n\n🛠 INFORME TÉCNICO:\n" + informeTecnico);
                }
            }

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

            if (trabajo.getEstado() == EstadoTrabajo.ASIGNADO) {
                throw new ServiceException(
                        "No se puede cancelar un trabajo que ya está asignado. Contacte con la empresa.");
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
    public void modificarTrabajo(Integer idTrabajo, String titulo, String descripcion, String direccion,
            CategoriaServicio categoria, int urgencia) throws ServiceException {
        try {
            Trabajo trabajo = trabajoDAO.obtenerPorId(idTrabajo);
            if (trabajo == null)
                throw new ServiceException("Trabajo no encontrado.");

            if (trabajo.getEstado() == EstadoTrabajo.FINALIZADO || trabajo.getEstado() == EstadoTrabajo.CANCELADO) {
                throw new ServiceException("No se pueden modificar trabajos finalizados o cancelados.");
            }

            if (titulo != null && !titulo.trim().isEmpty())
                trabajo.setTitulo(titulo);
            if (descripcion != null && !descripcion.trim().isEmpty())
                trabajo.setDescripcion(descripcion);
            if (direccion != null && !direccion.trim().isEmpty())
                trabajo.setDireccion(direccion);
            if (categoria != null)
                trabajo.setCategoria(categoria);

            trabajoDAO.actualizar(trabajo);

        } catch (DataAccessException e) {
            throw new ServiceException("Error al modificar el trabajo.", e);
        }
    }

    @Override
    public void valorarTrabajo(Integer idTrabajo, int valoracion, String comentarioCliente) throws ServiceException {
        try {
            Trabajo trabajo = trabajoDAO.obtenerPorId(idTrabajo);
            if (trabajo == null)
                throw new ServiceException("Trabajo no encontrado.");

            if (trabajo.getEstado() != EstadoTrabajo.FINALIZADO && trabajo.getEstado() != EstadoTrabajo.REALIZADO
                    && trabajo.getEstado() != EstadoTrabajo.PAGADO) {
                throw new ServiceException("Solo se pueden valorar trabajos en estado FINALIZADO o superado.");
            }
            if (valoracion < 1 || valoracion > 5) {
                throw new ServiceException("La valoración debe estar entre 1 y 5 estrellas.");
            }

            trabajo.setValoracion(valoracion);
            trabajo.setComentarioCliente(comentarioCliente);

            trabajoDAO.actualizar(trabajo);

        } catch (DataAccessException e) {
            throw new ServiceException("Error al valorar el trabajo.", e);
        }
    }

    @Override
    public List<Trabajo> listarPendientes(Integer idEmpresa) throws ServiceException {
        try {
            List<Trabajo> todos = trabajoDAO.obtenerTodos();

            return todos.stream()
                    .filter(t -> t.getEstado() == EstadoTrabajo.PENDIENTE || t.getEstado() == EstadoTrabajo.PRESUPUESTADO)
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
            // Usamos query filtrada en SQL para evitar fallos cuando getCliente() es null
            return ((TrabajoDAOImpl) trabajoDAO).obtenerPorCliente(idCliente);
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

    @Override
    public List<Trabajo> listarTodos() throws ServiceException {
        try {
            return trabajoDAO.obtenerTodos();
        } catch (DataAccessException e) {
            throw new ServiceException("Error al listar todos los trabajos.", e);
        }
    }
}

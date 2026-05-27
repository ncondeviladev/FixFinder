package com.fixfinder.service.impl;

import com.fixfinder.modelos.Operario;
import com.fixfinder.modelos.Trabajo;
import com.fixfinder.modelos.Usuario;
import com.fixfinder.modelos.componentes.FotoTrabajo;
import com.fixfinder.modelos.enums.CategoriaServicio;
import com.fixfinder.modelos.enums.EstadoTrabajo;
import com.fixfinder.repository.FotoTrabajoRepository;
import com.fixfinder.repository.OperarioRepository;
import com.fixfinder.repository.PresupuestoRepository;
import com.fixfinder.repository.TrabajoRepository;
import com.fixfinder.repository.UsuarioRepository;
import com.fixfinder.service.NotificationService;
import com.fixfinder.service.interfaz.TrabajoService;
import com.fixfinder.utilidades.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de gestión de trabajos e incidencias.
 * Centraliza la creación de trabajos, asignación a operarios y cambios de estado a lo largo del ciclo de vida.
 */
@Service
public class TrabajoServiceImpl implements TrabajoService {

    private final TrabajoRepository trabajoRepository;
    private final UsuarioRepository usuarioRepository;
    private final OperarioRepository operarioRepository;
    private final FotoTrabajoRepository fotoTrabajoRepository;
    private final PresupuestoRepository presupuestoRepository;
    private final NotificationService notificationService;

    @Autowired
    public TrabajoServiceImpl(TrabajoRepository trabajoRepository, UsuarioRepository usuarioRepository, 
                             OperarioRepository operarioRepository, FotoTrabajoRepository fotoTrabajoRepository,
                             PresupuestoRepository presupuestoRepository, NotificationService notificationService) {
        this.trabajoRepository = trabajoRepository;
        this.usuarioRepository = usuarioRepository;
        this.operarioRepository = operarioRepository;
        this.fotoTrabajoRepository = fotoTrabajoRepository;
        this.presupuestoRepository = presupuestoRepository;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public Trabajo solicitarReparacion(Integer idCliente, String titulo, CategoriaServicio categoria, String descripcion, String direccion, int urgencia, List<String> urlsFotos) throws ServiceException {
        if (idCliente == null) throw new ServiceException("El ID del cliente no puede ser nulo.");
        if (descripcion == null || descripcion.trim().isEmpty()) throw new ServiceException("La descripción es obligatoria.");

        Usuario cliente = usuarioRepository.findById(idCliente).orElse(null);
        if (cliente == null) throw new ServiceException("Cliente no encontrado con ID: " + idCliente);

        Trabajo trabajo = new Trabajo();
        trabajo.setCliente(cliente);
        trabajo.setCategoria(categoria != null ? categoria : CategoriaServicio.OTROS);

        if (titulo == null || titulo.trim().isEmpty()) {
            String tituloBase = descripcion;
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

        String descEstructurada = "==============================\n" +
                "📝 CLIENTE:\n" + descripcion.trim() + "\n" +
                "==============================\n" +
                "💰 GERENTE:\n(Sin presupuesto redactado)\n" +
                "==============================\n" +
                "🛠 OPERARIO:\n(Sin informe de trabajo)\n" +
                "==============================";

        String dirFinal = (direccion != null && !direccion.trim().isEmpty())
                ? direccion
                : (cliente.getDireccion() != null && !cliente.getDireccion().trim().isEmpty()
                        ? cliente.getDireccion()
                        : "Sin dirección especificada");

        trabajo.setDescripcion(descEstructurada);
        trabajo.setDireccion(dirFinal);
        trabajo.setEstado(EstadoTrabajo.PENDIENTE);
        
        Trabajo saved = trabajoRepository.save(trabajo);
        
        // Procesar fotos si se proporcionaron
        if (urlsFotos != null && !urlsFotos.isEmpty()) {
            procesarFotos(urlsFotos, saved.getId());
        }
        
        // Notificar nuevo trabajo
        notificationService.difundirEventoTrabajo("NUEVO", saved.getId(), idCliente, 0, 0, "Nueva solicitud: " + saved.getTitulo());
        
        return saved;
    }

    @Override
    @Transactional
    public void asignarOperario(Integer idTrabajo, Integer idOperario) throws ServiceException {
        Trabajo trabajo = trabajoRepository.findById(idTrabajo).orElse(null);
        if (trabajo == null) throw new ServiceException("Trabajo no encontrado.");

        if (trabajo.getEstado() == EstadoTrabajo.FINALIZADO || trabajo.getEstado() == EstadoTrabajo.CANCELADO) {
            throw new ServiceException("No se puede asignar operario a un trabajo finalizado o cancelado.");
        }

        if (idOperario == null || idOperario <= 0) {
            trabajo.setOperarioAsignado(null);
            if (trabajo.getEstado() == EstadoTrabajo.ASIGNADO) {
                trabajo.setEstado(EstadoTrabajo.ACEPTADO);
            } else {
                trabajo.setEstado(EstadoTrabajo.PENDIENTE);
            }
        } else {
            Operario operario = operarioRepository.findById(idOperario).orElse(null);
            if (operario == null) throw new ServiceException("Operario no encontrado.");

            trabajo.setOperarioAsignado(operario);
            trabajo.setEstado(EstadoTrabajo.ASIGNADO);
        }
        trabajoRepository.save(trabajo);
        
        // Notificar asignación
        int idEmpresa = (trabajo.getOperarioAsignado() != null) ? trabajo.getOperarioAsignado().getIdEmpresa() : 0;
        int idOp = (trabajo.getOperarioAsignado() != null) ? trabajo.getOperarioAsignado().getId() : 0;
        notificationService.difundirEventoTrabajo("ASIGNACION", idTrabajo, trabajo.getCliente().getId(), 
                idOp, idEmpresa, 
                idOp > 0 ? "Operario asignado" : "Operario desasignado");
    }

    @Override
    @Transactional
    public void iniciarTrabajo(Integer idTrabajo) throws ServiceException {
        Trabajo trabajo = trabajoRepository.findById(idTrabajo).orElse(null);
        if (trabajo == null) throw new ServiceException("Trabajo no encontrado.");

        if (trabajo.getOperarioAsignado() == null) throw new ServiceException("No se puede iniciar un trabajo sin operario asignado.");

        if (trabajo.getEstado() != EstadoTrabajo.ASIGNADO) {
            trabajo.setEstado(EstadoTrabajo.ASIGNADO);
            trabajoRepository.save(trabajo);
            
            notificationService.difundirEventoTrabajo("INICIO", idTrabajo, trabajo.getCliente().getId(), 
                    trabajo.getOperarioAsignado().getId(), trabajo.getOperarioAsignado().getIdEmpresa(), "Trabajo iniciado");
        }
    }

    @Override
    @Transactional
    public void finalizarTrabajo(Integer idTrabajo, String informeTecnico, List<String> urlsFotos) throws ServiceException {
        Trabajo trabajo = trabajoRepository.findById(idTrabajo).orElse(null);
        if (trabajo == null) throw new ServiceException("Trabajo no encontrado.");

        if (trabajo.getEstado() != EstadoTrabajo.ASIGNADO) {
            throw new ServiceException("El trabajo debe estar ASIGNADO para finalizarse.");
        }

        trabajo.setEstado(EstadoTrabajo.FINALIZADO);
        trabajo.setFechaFinalizacion(LocalDateTime.now());

        if (informeTecnico != null && !informeTecnico.trim().isEmpty() && !informeTecnico.equals("Trabajo finalizado correctamente (Simulador).")) {
            String descActual = trabajo.getDescripcion();
            if (descActual.contains("🛠 OPERARIO:")) {
                String[] partes = descActual.split("🛠 OPERARIO:");
                String parteSuperior = partes[0];
                String parteInferior = partes.length > 1 ? partes[1] : "";

                if (parteInferior.contains("==============================")) {
                    int posCierre = parteInferior.indexOf("==============================");
                    String resto = parteInferior.substring(posCierre);
                    trabajo.setDescripcion(parteSuperior + "🛠 OPERARIO:\n" + informeTecnico.trim() + "\n" + resto);
                } else {
                    trabajo.setDescripcion(parteSuperior + "🛠 OPERARIO:\n" + informeTecnico.trim());
                }
            } else {
                trabajo.setDescripcion(descActual + "\n\n==============================\n🛠 OPERARIO:\n" + informeTecnico.trim() + "\n==============================");
            }
        }
        trabajoRepository.save(trabajo);
        
        // Procesar fotos si se proporcionaron
        if (urlsFotos != null && !urlsFotos.isEmpty()) {
            procesarFotos(urlsFotos, idTrabajo);
        }
        
        notificationService.difundirEventoTrabajo("FINALIZADO", idTrabajo, trabajo.getCliente().getId(), 
                trabajo.getOperarioAsignado().getId(), trabajo.getOperarioAsignado().getIdEmpresa(), "Trabajo finalizado por el técnico");
    }

    @Override
    @Transactional
    public void cancelarTrabajo(Integer idTrabajo, String motivo) throws ServiceException {
        Trabajo trabajo = trabajoRepository.findById(idTrabajo).orElse(null);
        if (trabajo == null) throw new ServiceException("Trabajo no encontrado.");

        if (trabajo.getEstado() == EstadoTrabajo.FINALIZADO) {
            throw new ServiceException("No se puede cancelar un trabajo ya finalizado.");
        }
        if (trabajo.getEstado() == EstadoTrabajo.ASIGNADO) {
            throw new ServiceException("No se puede cancelar un trabajo que ya está asignado. Contacte con la empresa.");
        }

        int idEmpresa = (trabajo.getOperarioAsignado() != null) ? trabajo.getOperarioAsignado().getIdEmpresa() : 0;
        int idOperario = (trabajo.getOperarioAsignado() != null) ? trabajo.getOperarioAsignado().getId() : 0;
        
        if (trabajo.getOperarioAsignado() != null) {
            trabajo.setOperarioAsignado(null);
        }

        trabajo.setEstado(EstadoTrabajo.CANCELADO);
        trabajo.setDescripcion(trabajo.getDescripcion() + " [CANCELADO: " + motivo + "]");
        trabajoRepository.save(trabajo);
        
        notificationService.difundirEventoTrabajo("CANCELADO", idTrabajo, trabajo.getCliente().getId(), idOperario, idEmpresa, "Trabajo cancelado: " + motivo);
    }

    @Override
    @Transactional
    public void modificarTrabajo(Integer idTrabajo, String titulo, String descripcion, String direccion, CategoriaServicio categoria, int urgencia, List<String> urlsFotos) throws ServiceException {
        Trabajo trabajo = trabajoRepository.findById(idTrabajo).orElse(null);
        if (trabajo == null) throw new ServiceException("Trabajo no encontrado.");

        if (trabajo.getEstado() == EstadoTrabajo.FINALIZADO || trabajo.getEstado() == EstadoTrabajo.CANCELADO) {
            throw new ServiceException("No se pueden modificar trabajos finalizados o cancelados.");
        }

        if (titulo != null && !titulo.trim().isEmpty()) trabajo.setTitulo(titulo);
        if (descripcion != null && !descripcion.trim().isEmpty()) trabajo.setDescripcion(descripcion);
        if (direccion != null && !direccion.trim().isEmpty()) trabajo.setDireccion(direccion);
        if (categoria != null) trabajo.setCategoria(categoria);

        trabajoRepository.save(trabajo);
        
        // Procesar fotos si se proporcionaron
        if (urlsFotos != null && !urlsFotos.isEmpty()) {
            procesarFotos(urlsFotos, idTrabajo);
        }
        
        int idOperario = (trabajo.getOperarioAsignado() != null) ? trabajo.getOperarioAsignado().getId() : 0;
        int idEmpresa = (trabajo.getOperarioAsignado() != null) ? trabajo.getOperarioAsignado().getIdEmpresa() : 0;
        notificationService.difundirEventoTrabajo("MODIFICADO", idTrabajo, trabajo.getCliente().getId(), idOperario, idEmpresa, "Trabajo modificado");
    }

    @Override
    @Transactional
    public void valorarTrabajo(Integer idTrabajo, int valoracion, String comentarioCliente) throws ServiceException {
        Trabajo trabajo = trabajoRepository.findById(idTrabajo).orElse(null);
        if (trabajo == null) throw new ServiceException("Trabajo no encontrado.");

        if (trabajo.getEstado() != EstadoTrabajo.FINALIZADO && trabajo.getEstado() != EstadoTrabajo.PAGADO) {
            throw new ServiceException("Solo se pueden valorar trabajos en estado FINALIZADO o superado.");
        }
        if (valoracion < 1 || valoracion > 5) {
            throw new ServiceException("La valoración debe estar entre 1 y 5 estrellas.");
        }

        trabajo.setValoracion(valoracion);
        trabajo.setComentarioCliente(comentarioCliente);

        trabajoRepository.save(trabajo);
        
        int idEmpresa = (trabajo.getOperarioAsignado() != null) ? trabajo.getOperarioAsignado().getIdEmpresa() : 0;
        int idOperario = (trabajo.getOperarioAsignado() != null) ? trabajo.getOperarioAsignado().getId() : 0;
        notificationService.difundirEventoTrabajo("VALORACION", idTrabajo, trabajo.getCliente().getId(), idOperario, idEmpresa, "Nueva valoración recibida");
    }

    @Override
    public List<Trabajo> listarPendientes(Integer idEmpresa) throws ServiceException {
        List<Trabajo> todos = trabajoRepository.findAll();
        
        if (idEmpresa == null || idEmpresa <= 0) {
            return todos.stream()
                    .filter(t -> t.getEstado() == EstadoTrabajo.PENDIENTE || t.getEstado() == EstadoTrabajo.PRESUPUESTADO)
                    .collect(Collectors.toList());
        }
        
        List<Trabajo> visibles = new ArrayList<>();
        for (Trabajo t : todos) {
            if (t.getEstado() == EstadoTrabajo.PENDIENTE || t.getEstado() == EstadoTrabajo.PRESUPUESTADO) {
                visibles.add(t);
                continue;
            }
            
            if (t.getEstado() == EstadoTrabajo.ACEPTADO) {
                boolean esDeLaEmpresa = false;
                List<com.fixfinder.modelos.Presupuesto> presupuestos = presupuestoRepository.findByTrabajoId(t.getId());
                if (presupuestos != null) {
                    for (com.fixfinder.modelos.Presupuesto p : presupuestos) {
                        if ("ACEPTADO".equalsIgnoreCase(p.getEstado().toString()) && p.getEmpresa() != null && p.getEmpresa().getId() == idEmpresa) {
                            esDeLaEmpresa = true;
                            break;
                        }
                    }
                }
                if (esDeLaEmpresa) {
                    visibles.add(t);
                    continue;
                }
            }
            
            if (t.getOperarioAsignado() != null && t.getOperarioAsignado().getIdEmpresa() == idEmpresa) {
                visibles.add(t);
            }
        }
        return visibles;
    }

    @Override
    public List<Trabajo> historialCliente(Integer idCliente) throws ServiceException {
        return trabajoRepository.findByClienteId(idCliente);
    }

    @Override
    public List<Trabajo> historialOperario(Integer idOperario) throws ServiceException {
        return trabajoRepository.findByOperarioAsignadoId(idOperario);
    }

    @Override
    public List<Trabajo> listarTodos() throws ServiceException {
        return trabajoRepository.findAll();
    }

    @Override
    public Trabajo obtenerPorId(Integer idTrabajo) throws ServiceException {
        return trabajoRepository.findById(idTrabajo).orElse(null);
    }

    private void procesarFotos(List<String> urlsNuevas, int idTrabajo) {
        if (urlsNuevas == null || urlsNuevas.isEmpty()) return;
        
        List<FotoTrabajo> actuales = fotoTrabajoRepository.findByIdTrabajo(idTrabajo);
        List<String> urlsActuales = new ArrayList<>();
        if (actuales != null) {
            for (FotoTrabajo ft : actuales) {
                urlsActuales.add(ft.getUrl());
            }
        }
        
        for (String url : urlsNuevas) {
            if (url != null && !url.isBlank() && !urlsActuales.contains(url)) {
                fotoTrabajoRepository.save(new FotoTrabajo(0, idTrabajo, url));
            }
        }
    }
}

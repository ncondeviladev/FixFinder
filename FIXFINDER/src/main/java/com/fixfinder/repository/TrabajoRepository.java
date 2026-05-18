package com.fixfinder.repository;

import com.fixfinder.modelos.Trabajo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * Repositorio JPA para {@link Trabajo}.
 * Proporciona consultas personalizadas para buscar trabajos por cliente, empresa u operario.
 */
public interface TrabajoRepository extends JpaRepository<Trabajo, Integer> {
    
    /** Busca los trabajos solicitados por un cliente. */
    List<Trabajo> findByClienteId(int clienteId);
    
    /** Busca los trabajos asignados a operarios de una empresa concreta. */
    List<Trabajo> findByOperarioAsignadoIdEmpresa(int idEmpresa);
    
    /** Busca los trabajos asignados a un operario específico. */
    List<Trabajo> findByOperarioAsignadoId(int operarioId);
}

package com.fixfinder.repository;

import com.fixfinder.modelos.Presupuesto;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/** Repositorio JPA para {@link Presupuesto}. Hereda los métodos CRUD de JpaRepository. */
public interface PresupuestoRepository extends JpaRepository<Presupuesto, Integer> {

    /** Lista los presupuestos de un trabajo concreto. */
    List<Presupuesto> findByTrabajoId(int trabajoId);

    /** Lista los presupuestos emitidos por una empresa. */
    List<Presupuesto> findByEmpresaId(int empresaId);
}

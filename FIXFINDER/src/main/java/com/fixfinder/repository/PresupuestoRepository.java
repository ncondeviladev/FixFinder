package com.fixfinder.repository;

import com.fixfinder.modelos.Presupuesto;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PresupuestoRepository extends JpaRepository<Presupuesto, Integer> {
    List<Presupuesto> findByTrabajoId(int trabajoId);
    List<Presupuesto> findByEmpresaId(int empresaId);
}

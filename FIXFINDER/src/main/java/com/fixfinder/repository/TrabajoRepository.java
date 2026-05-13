package com.fixfinder.repository;

import com.fixfinder.modelos.Trabajo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TrabajoRepository extends JpaRepository<Trabajo, Integer> {
    List<Trabajo> findByClienteId(int clienteId);
    List<Trabajo> findByOperarioAsignadoIdEmpresa(int idEmpresa);
    List<Trabajo> findByOperarioAsignadoId(int operarioId);
}

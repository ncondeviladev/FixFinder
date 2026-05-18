package com.fixfinder.repository;

import com.fixfinder.modelos.Operario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/** Repositorio JPA para {@link Operario}. Hereda los métodos CRUD de JpaRepository. */
public interface OperarioRepository extends JpaRepository<Operario, Integer> {

    /** Lista todos los operarios de una empresa. Equivale a: SELECT * FROM operario WHERE id_empresa = ? */
    List<Operario> findByIdEmpresa(int idEmpresa);
}

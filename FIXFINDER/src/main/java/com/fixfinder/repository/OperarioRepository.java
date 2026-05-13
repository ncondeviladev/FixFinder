package com.fixfinder.repository;

import com.fixfinder.modelos.Operario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OperarioRepository extends JpaRepository<Operario, Integer> {
    List<Operario> findByIdEmpresa(int idEmpresa);
}

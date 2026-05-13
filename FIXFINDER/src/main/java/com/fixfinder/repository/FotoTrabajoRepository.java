package com.fixfinder.repository;

import com.fixfinder.modelos.componentes.FotoTrabajo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FotoTrabajoRepository extends JpaRepository<FotoTrabajo, Integer> {
    List<FotoTrabajo> findByIdTrabajo(int idTrabajo);
}

package com.fixfinder.repository;

import com.fixfinder.modelos.componentes.FotoTrabajo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/** Repositorio JPA para fotos adjuntas a trabajos. Hereda los métodos CRUD de JpaRepository. */
public interface FotoTrabajoRepository extends JpaRepository<FotoTrabajo, Integer> {

    /** Lista todas las fotos asociadas a un trabajo. */
    List<FotoTrabajo> findByIdTrabajo(int idTrabajo);
}

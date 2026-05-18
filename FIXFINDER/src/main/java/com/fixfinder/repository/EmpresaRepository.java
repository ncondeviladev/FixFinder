package com.fixfinder.repository;

import com.fixfinder.modelos.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio JPA para {@link Empresa}. Soporta el modelo multi-tenant del sistema. */
public interface EmpresaRepository extends JpaRepository<Empresa, Integer> {
}

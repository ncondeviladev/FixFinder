package com.fixfinder.repository;

import com.fixfinder.modelos.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio JPA para {@link Cliente}. Sin métodos adicionales: los heredados de JpaRepository son suficientes. */
public interface ClienteRepository extends JpaRepository<Cliente, Integer> {
}

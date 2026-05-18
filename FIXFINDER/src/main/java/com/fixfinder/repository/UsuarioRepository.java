package com.fixfinder.repository;

import com.fixfinder.modelos.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio JPA para {@link Usuario}.
 * Hereda los métodos CRUD de JpaRepository y añade búsquedas por email y DNI.
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    /** Busca un usuario por email. Genera: SELECT * FROM usuario WHERE email = ? */
    Usuario findByEmail(String email);

    /** Busca un usuario por DNI. */
    Usuario findByDni(String dni);

    /** Devuelve {@code true} si ya existe un usuario registrado con ese email. */
    boolean existsByEmail(String email);
}

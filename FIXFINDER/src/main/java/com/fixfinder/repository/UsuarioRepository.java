package com.fixfinder.repository;

import com.fixfinder.modelos.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    Usuario findByEmail(String email);
    Usuario findByDni(String dni);
    boolean existsByEmail(String email);
}

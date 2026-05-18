package com.fixfinder.controller;

import com.fixfinder.modelos.Usuario;
import com.fixfinder.service.interfaz.EmpresaService;
import com.fixfinder.service.interfaz.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controlador REST de autenticación y registro de usuarios y empresas.
 * Expone los endpoints públicos que no requieren sesión. Ruta base: {@code /api/auth}.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private EmpresaService empresaService;

    /**
     * POST /api/auth/login — Autentica al usuario con email y password.
     * Devuelve el objeto Usuario completo o 401 si las credenciales son incorrectas.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        try {
            Usuario user = usuarioService.login(credentials.get("email"), credentials.get("password"));
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/auth/register — Registra un nuevo cliente u operario en el sistema.
     * Devuelve 400 si el email ya está en uso o los datos no son válidos.
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Usuario usuario) {
        try {
            usuarioService.registrarUsuario(usuario);
            return ResponseEntity.ok(Map.of("message", "Usuario registrado correctamente"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/auth/register-empresa — Registra una empresa y su gerente en una transacción atómica.
     * Si la creación del gerente falla, la empresa tampoco se guarda.
     */
    @PostMapping("/register-empresa")
    public ResponseEntity<?> registerEmpresa(@RequestBody Map<String, Object> data) {
        try {
            empresaService.registrarEmpresaConGerente(data);
            return ResponseEntity.ok(Map.of("message", "Empresa y Gerente registrados correctamente"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

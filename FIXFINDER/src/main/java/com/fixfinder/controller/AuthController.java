package com.fixfinder.controller;

import com.fixfinder.modelos.Usuario;
import com.fixfinder.service.interfaz.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private com.fixfinder.service.interfaz.EmpresaService empresaService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        try {
            Usuario user = usuarioService.login(credentials.get("email"), credentials.get("password"));
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Usuario usuario) {
        try {
            usuarioService.registrarUsuario(usuario);
            return ResponseEntity.ok(Map.of("message", "Usuario registrado correctamente"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

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

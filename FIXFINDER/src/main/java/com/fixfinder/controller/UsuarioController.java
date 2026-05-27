package com.fixfinder.controller;

import com.fixfinder.modelos.Usuario;
import com.fixfinder.service.interfaz.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controlador REST para la gestión del perfil de usuario.
 * Expone endpoints para consultar y modificar datos personales. Ruta base: {@code /api/usuarios}.
 */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable int id) {
        try {
            return ResponseEntity.ok(usuarioService.obtenerPorId(id));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> modificarUsuario(@PathVariable int id, @RequestBody Usuario usuario) {
        try {
            usuario.setId(id);
            usuarioService.modificarUsuario(usuario);
            return ResponseEntity.ok(Map.of("message", "Usuario modificado"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/password")
    public ResponseEntity<?> cambiarPassword(@PathVariable int id, @RequestBody Map<String, String> body) {
        try {
            usuarioService.cambiarPassword(id, body.get("oldPassword"), body.get("newPassword"));
            return ResponseEntity.ok(Map.of("message", "Password cambiado"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @RequestMapping(value = "/{id}/foto", method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<?> actualizarFoto(@PathVariable int id, @RequestBody Map<String, String> body) {
        try {
            String url = body.containsKey("url_foto") ? body.get("url_foto") : body.get("urlFoto");
            usuarioService.actualizarFotoPerfil(id, url);
            return ResponseEntity.ok(Map.of("message", "Foto actualizada", "url_foto", url, "urlFoto", url));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

package com.fixfinder.controller;

import com.fixfinder.modelos.Empresa;
import com.fixfinder.service.interfaz.EmpresaService;
import com.fixfinder.service.interfaz.OperarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para la gestión de empresas y sus operarios.
 * Expone operaciones de consulta, registro, modificación y baja. Ruta base: {@code /api/empresas}.
 */
@RestController
@RequestMapping("/api/empresas")
public class EmpresaController {

    @Autowired
    private EmpresaService empresaService;

    @Autowired
    private OperarioService operarioService;

    @GetMapping("/{id}/operarios")
    public ResponseEntity<List<com.fixfinder.modelos.Operario>> listarOperarios(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(operarioService.listarPorEmpresa(id));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<Empresa>> listarTodas() {
        try {
            return ResponseEntity.ok(empresaService.listarTodas());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Empresa> obtenerEmpresa(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(empresaService.obtenerPorId(id));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/estadisticas")
    public ResponseEntity<?> obtenerEstadisticas(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(empresaService.obtenerEstadisticas(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> registrarEmpresa(@RequestBody Empresa empresa) {
        try {
            empresaService.registrarEmpresa(empresa);
            return ResponseEntity.ok(Map.of("message", "Empresa registrada"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> modificarEmpresa(@PathVariable int id, @RequestBody Map<String, Object> body) {
        try {
            Empresa empresa = new Empresa();
            empresa.setId(id);
            if (body.containsKey("nombre")) empresa.setNombre((String) body.get("nombre"));
            if (body.containsKey("cif")) empresa.setCif((String) body.get("cif"));
            if (body.containsKey("emailContacto")) empresa.setEmailContacto((String) body.get("emailContacto"));
            if (body.containsKey("email")) empresa.setEmailContacto((String) body.get("email"));
            if (body.containsKey("telefono")) empresa.setTelefono((String) body.get("telefono"));
            if (body.containsKey("direccion")) empresa.setDireccion((String) body.get("direccion"));
            // Acepta urlFoto (camelCase del Dashboard) y url_foto (snake_case de la App)
            String urlFoto = body.containsKey("urlFoto") ? (String) body.get("urlFoto")
                           : body.containsKey("url_foto") ? (String) body.get("url_foto") : null;
            if (urlFoto != null && !urlFoto.isBlank()) empresa.setUrlFoto(urlFoto);
            empresaService.modificarEmpresa(empresa);
            return ResponseEntity.ok(Map.of("message", "Empresa actualizada"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> bajaEmpresa(@PathVariable Integer id) {
        try {
            empresaService.bajaEmpresa(id);
            return ResponseEntity.ok(Map.of("message", "Empresa dada de baja"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

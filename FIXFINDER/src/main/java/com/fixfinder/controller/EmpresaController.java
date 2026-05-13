package com.fixfinder.controller;

import com.fixfinder.modelos.Empresa;
import com.fixfinder.service.interfaz.EmpresaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/empresas")
public class EmpresaController {

    @Autowired
    private EmpresaService empresaService;

    @Autowired
    private com.fixfinder.service.interfaz.OperarioService operarioService;

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
    public ResponseEntity<?> modificarEmpresa(@PathVariable int id, @RequestBody Empresa empresa) {
        try {
            empresa.setId(id);
            empresaService.modificarEmpresa(empresa);
            return ResponseEntity.ok(Map.of("message", "Empresa modificada"));
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

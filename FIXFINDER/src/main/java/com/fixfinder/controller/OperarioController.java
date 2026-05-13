package com.fixfinder.controller;

import com.fixfinder.modelos.Operario;
import com.fixfinder.service.interfaz.OperarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/operarios")
public class OperarioController {

    @Autowired
    private OperarioService operarioService;

    @GetMapping("/empresa/{idEmpresa}")
    public ResponseEntity<List<Operario>> listarPorEmpresa(@PathVariable int idEmpresa) {
        try {
            return ResponseEntity.ok(operarioService.listarPorEmpresa(idEmpresa));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> modificarOperario(@PathVariable int id, @RequestBody Operario operario) {
        try {
            operario.setId(id);
            operarioService.modificarOperario(operario);
            return ResponseEntity.ok(Map.of("mensaje", "Operario actualizado correctamente", "status", 200));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", e.getMessage(), "status", 400));
        }
    }
}

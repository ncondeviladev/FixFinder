package com.fixfinder.controller;

import com.fixfinder.modelos.Trabajo;
import com.fixfinder.modelos.enums.CategoriaServicio;
import com.fixfinder.service.interfaz.TrabajoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/trabajos")
public class TrabajoController {

    @Autowired
    private TrabajoService trabajoService;

    @PostMapping("/solicitar")
    public ResponseEntity<?> solicitarReparacion(@RequestBody Map<String, Object> body) {
        try {
            Integer idCliente = (Integer) body.get("idCliente");
            String titulo = (String) body.get("titulo");
            String categoriaStr = (String) body.get("categoria");
            CategoriaServicio categoria = categoriaStr != null ? CategoriaServicio.valueOf(categoriaStr) : null;
            String descripcion = (String) body.get("descripcion");
            String direccion = (String) body.get("direccion");
            Integer urgencia = (Integer) body.getOrDefault("urgencia", 0);

            Trabajo trabajo = trabajoService.solicitarReparacion(idCliente, titulo, categoria, descripcion, direccion, urgencia);
            return ResponseEntity.ok(trabajo);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/asignar/{idOperario}")
    public ResponseEntity<?> asignarOperario(@PathVariable int id, @PathVariable int idOperario) {
        try {
            trabajoService.asignarOperario(id, idOperario);
            return ResponseEntity.ok(Map.of("message", "Operario asignado"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/finalizar")
    public ResponseEntity<?> finalizarTrabajo(@PathVariable int id, @RequestBody Map<String, String> body) {
        try {
            trabajoService.finalizarTrabajo(id, body.get("informe"));
            return ResponseEntity.ok(Map.of("message", "Trabajo finalizado"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/pendientes")
    public ResponseEntity<List<Trabajo>> listarPendientes(@RequestParam(required = false) Integer idEmpresa) {
        try {
            return ResponseEntity.ok(trabajoService.listarPendientes(idEmpresa));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/cliente/{id}")
    public ResponseEntity<List<Trabajo>> historialCliente(@PathVariable int id) {
        try {
            return ResponseEntity.ok(trabajoService.historialCliente(id));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/operario/{id}")
    public ResponseEntity<List<Trabajo>> historialOperario(@PathVariable int id) {
        try {
            return ResponseEntity.ok(trabajoService.historialOperario(id));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}

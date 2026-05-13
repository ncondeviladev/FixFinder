package com.fixfinder.controller;

import com.fixfinder.modelos.Presupuesto;
import com.fixfinder.service.interfaz.PresupuestoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/presupuestos")
public class PresupuestoController {

    @Autowired
    private PresupuestoService presupuestoService;

    @PostMapping
    public ResponseEntity<?> crearPresupuesto(@RequestBody Presupuesto presupuesto) {
        try {
            presupuestoService.crearPresupuesto(presupuesto);
            return ResponseEntity.ok(Map.of("message", "Presupuesto creado", "id", presupuesto.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/trabajo/{idTrabajo}")
    public ResponseEntity<List<Presupuesto>> listarPorTrabajo(@PathVariable int idTrabajo) {
        try {
            return ResponseEntity.ok(presupuestoService.listarPorTrabajo(idTrabajo));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{id}/aceptar")
    public ResponseEntity<?> aceptarPresupuesto(@PathVariable int id) {
        try {
            presupuestoService.aceptarPresupuesto(id);
            return ResponseEntity.ok(Map.of("message", "Presupuesto aceptado"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/rechazar")
    public ResponseEntity<?> rechazarPresupuesto(@PathVariable int id) {
        try {
            presupuestoService.rechazarPresupuesto(id);
            return ResponseEntity.ok(Map.of("message", "Presupuesto rechazado"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

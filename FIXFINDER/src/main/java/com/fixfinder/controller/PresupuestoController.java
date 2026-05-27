package com.fixfinder.controller;

import com.fixfinder.modelos.Presupuesto;
import com.fixfinder.service.interfaz.PresupuestoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para la gestión de presupuestos.
 * Permite a las empresas enviar ofertas y a los clientes aceptarlas o rechazarlas. Ruta base: {@code /api/presupuestos}.
 */
@RestController
@RequestMapping("/api/presupuestos")
public class PresupuestoController {

    @Autowired
    private PresupuestoService presupuestoService;

    @Autowired
    private com.fixfinder.service.interfaz.TrabajoService trabajoService;

    @Autowired
    private com.fixfinder.service.interfaz.EmpresaService empresaService;

    @PostMapping
    public ResponseEntity<?> crearPresupuesto(@RequestBody Map<String, Object> body) {
        try {
            int idTrabajo = body.containsKey("idTrabajo") && body.get("idTrabajo") != null ? ((Number) body.get("idTrabajo")).intValue() : 0;
            int idEmpresa = body.containsKey("idEmpresa") && body.get("idEmpresa") != null ? ((Number) body.get("idEmpresa")).intValue() : 0;
            double monto = body.containsKey("monto") && body.get("monto") != null ? ((Number) body.get("monto")).doubleValue() : 0.0;
            String notas = (String) body.get("notas");

            Presupuesto p = new Presupuesto();
            p.setMonto(monto);
            p.setNotas(notas);

            com.fixfinder.modelos.Trabajo t = trabajoService.obtenerPorId(idTrabajo);
            com.fixfinder.modelos.Empresa e = empresaService.obtenerPorId(idEmpresa);
            p.setTrabajo(t);
            p.setEmpresa(e);

            presupuestoService.crearPresupuesto(p);
            return ResponseEntity.ok(Map.of("message", "Presupuesto creado", "id", p.getId()));
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

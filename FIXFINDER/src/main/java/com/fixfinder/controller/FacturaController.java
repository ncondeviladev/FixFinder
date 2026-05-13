package com.fixfinder.controller;

import com.fixfinder.modelos.Factura;
import com.fixfinder.service.interfaz.FacturaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/facturas")
public class FacturaController {

    @Autowired
    private FacturaService facturaService;

    @PostMapping("/generar/{idTrabajo}")
    public ResponseEntity<?> generarFactura(@PathVariable int idTrabajo) {
        try {
            Factura factura = facturaService.generarFactura(idTrabajo);
            return ResponseEntity.ok(factura);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/trabajo/{idTrabajo}")
    public ResponseEntity<?> obtenerPorTrabajo(@PathVariable int idTrabajo) {
        try {
            Factura factura = facturaService.obtenerPorTrabajo(idTrabajo);
            if (factura == null) return ResponseEntity.notFound().build();
            return ResponseEntity.ok(factura);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/pagar")
    public ResponseEntity<?> marcarComoPagada(@PathVariable int id) {
        try {
            facturaService.marcarComoPagada(id);
            return ResponseEntity.ok(Map.of("message", "Factura marcada como pagada"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

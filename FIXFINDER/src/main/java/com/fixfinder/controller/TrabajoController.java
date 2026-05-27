package com.fixfinder.controller;

import com.fixfinder.modelos.Trabajo;
import com.fixfinder.modelos.enums.CategoriaServicio;
import com.fixfinder.service.interfaz.TrabajoService;
import com.fixfinder.utilidades.TrabajoResponseMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.fixfinder.service.interfaz.OperarioService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/trabajos")
public class TrabajoController {

    @Autowired
    private TrabajoService trabajoService;

    @Autowired
    private OperarioService operarioService;

    @Autowired
    private TrabajoResponseMapper mapper;

    @PostMapping("/solicitar")
    public ResponseEntity<?> solicitarReparacion(@RequestBody Map<String, Object> body) {
        try {
            Integer idCliente = body.containsKey("idCliente") && body.get("idCliente") != null ? ((Number) body.get("idCliente")).intValue() : null;
            String titulo = (String) body.get("titulo");
            String categoriaStr = (String) body.get("categoria");
            CategoriaServicio categoria = categoriaStr != null ? CategoriaServicio.valueOf(categoriaStr) : null;
            String descripcion = (String) body.get("descripcion");
            String direccion = (String) body.get("direccion");
            Integer urgencia = body.containsKey("urgencia") && body.get("urgencia") != null ? ((Number) body.get("urgencia")).intValue() : 0;
            
            @SuppressWarnings("unchecked")
            List<String> urlsFotos = (List<String>) body.get("urls_fotos");

            Trabajo trabajo = trabajoService.solicitarReparacion(idCliente, titulo, categoria, descripcion, direccion, urgencia, urlsFotos);
            return ResponseEntity.ok(mapper.mapearTrabajoEnriquecido(trabajo));
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
    public ResponseEntity<?> finalizarTrabajo(@PathVariable int id, @RequestBody Map<String, Object> body) {
        try {
            String informe = (String) body.get("informe");
            @SuppressWarnings("unchecked")
            List<String> urlsFotos = (List<String>) body.get("fotos");
            trabajoService.finalizarTrabajo(id, informe, urlsFotos);
            return ResponseEntity.ok(Map.of("message", "Trabajo finalizado"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/pendientes")
    public ResponseEntity<List<Map<String, Object>>> listarPendientes(@RequestParam(required = false) Integer idEmpresa) {
        try {
            List<Trabajo> trabajos = trabajoService.listarPendientes(idEmpresa);
            return ResponseEntity.ok(mapper.mapearListaTrabajos(trabajos, -1, idEmpresa != null ? idEmpresa : -1));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/cliente/{id}")
    public ResponseEntity<List<Map<String, Object>>> historialCliente(@PathVariable int id) {
        try {
            List<Trabajo> trabajos = trabajoService.historialCliente(id);
            return ResponseEntity.ok(mapper.mapearListaTrabajos(trabajos, id, -1));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/operario/{id}")
    public ResponseEntity<List<Map<String, Object>>> historialOperario(@PathVariable int id) {
        try {
            List<Trabajo> trabajos = trabajoService.historialOperario(id);
            int idEmpresa = -1;
            
            // Buscamos el operario directamente por id de usuario para evitar pasar null al service
            com.fixfinder.modelos.Operario operario = operarioService.obtenerPorId(id);
            if (operario != null) {
                idEmpresa = operario.getIdEmpresa();
            }
            return ResponseEntity.ok(mapper.mapearListaTrabajos(trabajos, id, idEmpresa));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<?> cancelarTrabajo(@PathVariable int id, @RequestBody Map<String, String> body) {
        try {
            trabajoService.cancelarTrabajo(id, body.get("motivo"));
            return ResponseEntity.ok(Map.of("message", "Trabajo cancelado"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> modificarTrabajo(@PathVariable int id, @RequestBody Map<String, Object> body) {
        try {
            String titulo = (String) body.get("titulo");
            String descripcion = (String) body.get("descripcion");
            String direccion = (String) body.get("direccion");
            String categoriaStr = (String) body.get("categoria");
            CategoriaServicio categoria = categoriaStr != null ? CategoriaServicio.valueOf(categoriaStr) : null;
            int urgencia = body.containsKey("urgencia") && body.get("urgencia") != null ? ((Number) body.get("urgencia")).intValue() : 0;
            
            @SuppressWarnings("unchecked")
            List<String> urlsFotos = (List<String>) body.get("urls_fotos");

            trabajoService.modificarTrabajo(id, titulo, descripcion, direccion, categoria, urgencia, urlsFotos);
            return ResponseEntity.ok(Map.of("message", "Trabajo modificado"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/valorar")
    public ResponseEntity<?> valorarTrabajo(@PathVariable int id, @RequestBody Map<String, Object> body) {
        try {
            int valoracion = body.containsKey("valoracion") && body.get("valoracion") != null ? ((Number) body.get("valoracion")).intValue() : 0;
            String comentarioCliente = (String) body.get("comentarioCliente");
            trabajoService.valorarTrabajo(id, valoracion, comentarioCliente);
            return ResponseEntity.ok(Map.of("message", "Trabajo valorado"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

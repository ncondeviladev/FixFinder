package com.fixfinder.red.procesadores;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fixfinder.service.interfaz.FacturaService;
import com.fixfinder.service.interfaz.PresupuestoService;
import com.fixfinder.service.interfaz.TrabajoService;
import com.fixfinder.service.interfaz.UsuarioService;
import com.fixfinder.red.procesadores.trabajos.ManejadorCicloVidaTrabajo;
import com.fixfinder.red.procesadores.trabajos.ManejadorLecturaTrabajo;

/**
 * Orquestador de peticiones relacionadas con Trabajos (Incidencias).
 * Actúa como un mediador que delega la lógica a manejadores especializados.
 */
public class ProcesadorTrabajos {

    private final ManejadorLecturaTrabajo lectura;
    private final ManejadorCicloVidaTrabajo cicloVida;

    public ProcesadorTrabajos(TrabajoService trabajoService, UsuarioService usuarioService,
            PresupuestoService presupuestoService, FacturaService facturaService) {
        
        this.lectura = new ManejadorLecturaTrabajo(trabajoService, usuarioService, presupuestoService);
        this.cicloVida = new ManejadorCicloVidaTrabajo(trabajoService, facturaService);
    }

    public void procesarCrearTrabajo(JsonNode datos, ObjectNode respuesta) {
        cicloVida.procesarCrearTrabajo(datos, respuesta);
    }

    public void procesarListarTrabajos(JsonNode datos, ObjectNode respuesta) {
        lectura.procesarListarTrabajos(datos, respuesta);
    }

    public void procesarAsignarOperario(JsonNode datos, ObjectNode respuesta) {
        cicloVida.procesarAsignarOperario(datos, respuesta);
    }

    public void procesarFinalizarTrabajo(JsonNode datos, ObjectNode respuesta) {
        cicloVida.procesarFinalizarTrabajo(datos, respuesta);
    }

    public void procesarCancelarTrabajo(JsonNode datos, ObjectNode respuesta) {
        cicloVida.procesarCancelarTrabajo(datos, respuesta);
    }

    public void procesarModificarTrabajo(JsonNode datos, ObjectNode respuesta) {
        cicloVida.procesarModificarTrabajo(datos, respuesta);
    }

    public void procesarValorarTrabajo(JsonNode datos, ObjectNode respuesta) {
        cicloVida.procesarValorarTrabajo(datos, respuesta);
    }
}

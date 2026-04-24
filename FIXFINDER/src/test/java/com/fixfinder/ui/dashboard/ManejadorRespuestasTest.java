package com.fixfinder.ui.dashboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fixfinder.ui.dashboard.modelos.TrabajoFX;
import com.fixfinder.ui.dashboard.red.ManejadorRespuestas;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test de validación del contrato de datos entre Servidor y Dashboard.
 * Verifica que el ManejadorRespuestas procese correctamente los JSON estándar 
 * sin necesidad de mocks complejos o modificar el código original.
 */
public class ManejadorRespuestasTest {

    private ManejadorRespuestas manejador;
    private ObservableList<TrabajoFX> todosTrabajos;
    private ObjectMapper mapper = new ObjectMapper();
    
    // Variables para capturar callbacks
    private String actividadRegistrada;
    private boolean refrescoSolicitado;

    @BeforeEach
    void setUp() {
        todosTrabajos = FXCollections.observableArrayList();
        actividadRegistrada = null;
        refrescoSolicitado = false;

        // Callbacks manuales (Stubbing)
        Consumer<String> alRegistrarActividad = s -> actividadRegistrada = s;
        Runnable alSolicitarRefresco = () -> refrescoSolicitado = true;
        Consumer<String> alNavegarA = s -> {};
        BiConsumer<String, String> alActualizarPerfil = (n, f) -> {};

        manejador = new ManejadorRespuestas(
                todosTrabajos,
                FXCollections.observableArrayList(),
                new HashMap<>(),
                alRegistrarActividad,
                alSolicitarRefresco,
                alNavegarA,
                alActualizarPerfil,
                1,  // idEmpresaLogueada
                100 // idUsuarioLogueado
        );
    }

    @Test
    void testValidacionContratoListadoTrabajos() {
        ArrayNode datos = mapper.createArrayNode();
        ObjectNode t1 = datos.addObject();
        t1.put("id", 50);
        t1.put("titulo", "Fuga en cocina");
        t1.put("estado", "PENDIENTE");

        // Simulamos la llegada de la respuesta de red
        manejador.procesar("LISTAR_TRABAJOS", "OK", datos, 200, null, null);

        assertEquals(1, todosTrabajos.size(), "Debería haber mapeado un trabajo");
        assertEquals("Fuga en cocina", todosTrabajos.get(0).getTitulo());
        assertEquals(50, todosTrabajos.get(0).getId());
    }

    @Test
    void testReaccionABroadcastTrabajo() {
        ObjectNode datos = mapper.createObjectNode();
        datos.put("categoria", "TRABAJO");
        datos.put("subtipo", "NUEVO");
        datos.put("info", "¡Nueva incidencia urgente!");
        datos.put("idEmpresa", 1); // Mi empresa

        manejador.procesar("BROADCAST", "Notificación Push", datos, 200, null, null);

        assertTrue(refrescoSolicitado, "El dashboard debería pedir refresco de datos al recibir un broadcast");
        assertNotNull(actividadRegistrada);
        assertTrue(actividadRegistrada.contains("urgente"));
    }
}
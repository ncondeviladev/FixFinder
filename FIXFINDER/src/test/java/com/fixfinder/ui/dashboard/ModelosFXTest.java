package com.fixfinder.ui.dashboard;

import com.fixfinder.ui.dashboard.modelos.TrabajoFX;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test de modelos reactivos para la Interfaz Gráfica (JavaFX).
 * 
 * Verifica que el objeto TrabajoFX mantenga el estado sincronizado con las 
 * propiedades observables de la UI, permitiendo que las tablas y detalles 
 * se actualicen en tiempo real sin recargar la pantalla.
 */
public class ModelosFXTest {

    /**
     * Prueba que al actualizar el estado de un trabajo, la propiedad vinculada 
     * a la tabla (JavaFX Property) cambie automáticamente.
     * Criterio de éxito: El getter debe devolver el valor que se puso en la Property.
     */
    @Test
    void testPropiedadesTrabajoFX() {
        // Usamos el constructor real de TrabajoFX (10 parámetros mínimos)
        TrabajoFX t = new TrabajoFX(1, "Gotera", "Juan", "FONTANERIA", "PENDIENTE", 
                                   "Mario", "2024-01-01", "Fuga", "Calle A", -1);

        assertEquals(1, t.getId());
        assertEquals("Gotera", t.getTitulo());

        // Cambiamos el estado (simulando una respuesta del servidor)
        t.estadoProperty().set("REALIZADO");
        assertEquals("REALIZADO", t.getEstado(), "El getter debe reflejar el cambio en la propiedad");
    }
}
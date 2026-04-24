package com.fixfinder.red;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixfinder.modelos.Cliente;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test de serialización a JSON para la capa de red.
 * 
 * Verifica que los objetos del dominio (como Cliente) se conviertan correctamente 
 * al formato JSON esperado por el protocolo de comunicación Sockets, y viceversa.
 * 
 * Es vital para asegurar que el Dashboard y el Servidor siempre entiendan lo mismo.
 */
public class ResponseMapperTest {

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Prueba que un objeto Java se convierta en el JSON correcto.
     * Criterio de éxito: Las claves JSON deben coincidir con los campos de la clase.
     */
    @Test
    void testClienteAJson() {
        Cliente u = new Cliente();
        u.setId(7);
        u.setNombreCompleto("Empresa de Prueba");
        u.setEmail("test@empresa.com");
        u.setRol(com.fixfinder.modelos.enums.Rol.CLIENTE);

        // Convertimos a JSON usando el mapper de Jackson
        JsonNode node = mapper.valueToTree(u);

        assertEquals(7, node.get("id").asInt());
        assertEquals("Empresa de Prueba", node.get("nombreCompleto").asText());
        assertEquals("CLIENTE", node.get("rol").asText());
    }

    /**
     * Prueba que un String JSON se convierta en un objeto Java utilizable.
     * Criterio de éxito: El objeto resultante debe tener los mismos datos que el JSON.
     */
    @Test
    void testJsonACliente() throws Exception {
        String json = "{\"id\":99, \"nombreCompleto\":\"Juan\", \"rol\":\"CLIENTE\"}";
        Cliente u = mapper.readValue(json, Cliente.class);

        assertEquals(99, u.getId());
        assertEquals("Juan", u.getNombreCompleto());
    }
}
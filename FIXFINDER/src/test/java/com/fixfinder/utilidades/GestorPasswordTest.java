package com.fixfinder.utilidades;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test de seguridad para la gestión de contraseñas.
 * 
 * Verifica que el sistema de seguridad basado en PBKDF2 sea irreversible 
 * y que el proceso de verificación de identidad sea preciso.
 * 
 * Este test es fundamental para demostrar que las credenciales de los 
 * usuarios nunca se guardan en texto plano (Ley de Protección de Datos).
 */
public class GestorPasswordTest {

    /**
     * Verifica que una contraseña hasheada sea distinta al texto plano
     * y siga el formato seguro de la aplicación (salt:hash).
     */
    @Test
    void testHashIrreversible() {
        String passOriginal = "miPasswordSecreta123";
        String hash = GestorPassword.hashearPassword(passOriginal);

        assertNotNull(hash, "El hash no debe ser nulo");
        assertNotEquals(passOriginal, hash, "El hash nunca debe ser igual a la clave en texto plano");
        assertTrue(hash.contains(":"), "El hash debe incluir el salt separado por dos puntos");
    }

    /**
     * Valida que el sistema reconozca la contraseña correcta tras el hasheado.
     */
    @Test
    void testVerificacionExitosa() {
        String passOriginal = "admin123";
        String hash = GestorPassword.hashearPassword(passOriginal);

        assertTrue(GestorPassword.verificarPassword(passOriginal, hash), "La verificación debería ser exitosa con la clave correcta");
    }

    /**
     * Asegura que el sistema bloquee accesos con contraseñas erróneas.
     */
    @Test
    void testVerificacionFallida() {
        String passOriginal = "admin123";
        String hash = GestorPassword.hashearPassword(passOriginal);

        assertFalse(GestorPassword.verificarPassword("otraClave", hash), "La verificación debería fallar con una clave incorrecta");
    }
}
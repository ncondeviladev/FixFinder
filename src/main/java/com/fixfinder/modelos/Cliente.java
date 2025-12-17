package com.fixfinder.modelos;

import com.fixfinder.modelos.enums.Rol;

/**
 * Representa a un cliente final que solicita trabajos.
 * 
 * Extiende de {@link Usuario}. Actualmente no tiene campos adicionales
 * pero se mantiene como entidad separada para futura expansión (ej: métodos de
 * pago, fidelización).
 */
public class Cliente extends Usuario {

    public Cliente() {
        super();
    }

    public Cliente(int id, String email, String passwordHash, Rol rol, String nombreCompleto, String dni) {
        super(id, email, passwordHash, rol, nombreCompleto, dni);
    }

    // Aquí se añadirían getters y setters específicos de Cliente si los hubiera en
    // el futuro
}

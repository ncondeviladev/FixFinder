package com.fixfinder.cliente.servicios;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fixfinder.cliente.ClienteSocket;
import java.io.IOException;

/**
 * Servicio de Cliente para acciones relacionadas con Usuarios.
 * Se encarga de construir los mensajes JSON y enviarlos a través del socket.
 * No maneja la respuesta; eso se hace mediante los listeners del socket.
 */
public class UsuarioServiceCliente {

    private final ClienteSocket socket;

    public UsuarioServiceCliente(ClienteSocket socket) {
        this.socket = socket;
    }

    /**
     * Envía una petición de LOGIN al servidor.
     *
     * @param email    Email del usuario.
     * @param password Contraseña.
     * @throws IOException Si falla el envío por el socket.
     */
    public void login(String email, String password) throws IOException {
        ObjectNode datos = socket.getMapper().createObjectNode();
        datos.put("email", email);
        datos.put("password", password);

        socket.enviar("LOGIN", datos);
    }
}

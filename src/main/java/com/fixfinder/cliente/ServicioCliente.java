package com.fixfinder.cliente;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fixfinder.utilidades.ClienteException;

import java.io.IOException;
import java.util.function.Consumer;

public class ServicioCliente {

    private ClienteSocket socket;
    private ObjectMapper mapper;
    private String tokenActual; // Guardar el token de sesión

    public ServicioCliente() {
        this.socket = new ClienteSocket();
        this.mapper = new ObjectMapper();
    }

    // --- GESTIÓN CONEXIÓN ---

    public void conectar(String host, int puerto) throws IOException {
        socket.conectar(host, puerto);
    }

    public void desconectar() throws IOException {
        socket.desconectar();
    }

    public void logout() {
        this.tokenActual = null;
    }

    public boolean isConectado() {
        return socket.isConectado();
    }

    public void setOnMensajeRecibido(Consumer<String> callback) {
        socket.setOnMensajeRecibido(callback);
    }

    // --- PETICIONES (Lógica movida del Controller) ---

    public void enviarPing() throws IOException {
        enviarJson("PING", mapper.createObjectNode());
    }

    public void enviarLogin(String email, String password) throws IOException {
        ObjectNode datos = mapper.createObjectNode();
        datos.put("email", email);
        datos.put("password", password);
        enviarJson("LOGIN", datos);
    }

    public void enviarRegistroEmpresa(String nombre, String cif, String email, String direccion,
            String nombreGerente, String emailGerente, String passGerente, String dniGerente) throws IOException {
        ObjectNode datos = mapper.createObjectNode();
        datos.put("tipo", "EMPRESA");
        datos.put("nombreEmpresa", nombre);
        datos.put("cif", cif);
        datos.put("emailEmpresa", email);
        datos.put("direccion", direccion);

        datos.put("nombreGerente", nombreGerente);
        datos.put("emailGerente", emailGerente);
        datos.put("password", passGerente);
        datos.put("dniGerente", dniGerente);

        enviarJson("REGISTRO", datos);
    }

    // Método versátil para Cliente u Operario
    public void enviarRegistroUsuario(boolean esOperario, String nombre, String dni, String email, String pass,
            String telefono, String direccion, String idEmpresa) throws IOException {
        ObjectNode datos = mapper.createObjectNode();
        datos.put("tipo", esOperario ? "OPERARIO" : "CLIENTE");

        if (esOperario) {
            datos.put("nombreOperario", nombre);
            datos.put("dniOperario", dni);
            datos.put("emailOperario", email);
            datos.put("passwordOperario", pass);
            datos.put("telefonoOperario", telefono);
            datos.put("idEmpresa", idEmpresa);
        } else {
            datos.put("nombre", nombre);
            datos.put("dni", dni);
            datos.put("email", email);
            datos.put("password", pass);
            datos.put("telefono", telefono);
            datos.put("direccion", direccion);
        }

        enviarJson("REGISTRO", datos);
    }

    public void enviarCrearTrabajo(int idCliente, String titulo, String descripcion, String direccion,
            int urgencia, String categoria) throws IOException {
        ObjectNode datos = mapper.createObjectNode();
        datos.put("idCliente", idCliente);
        datos.put("titulo", titulo);
        datos.put("descripcion", descripcion);
        datos.put("direccion", direccion);
        datos.put("urgencia", urgencia);
        datos.put("categoria", categoria);

        enviarJson("CREAR_TRABAJO", datos);
    }

    public void solicitarListaTrabajos(int idUsuario, String rol) throws IOException {
        ObjectNode datos = mapper.createObjectNode();
        datos.put("idUsuario", idUsuario);
        datos.put("rol", rol);
        enviarJson("LISTAR_TRABAJOS", datos);
    }

    public void enviarAsignarOperario(int idTrabajo, int idOperario, int idGerente) throws IOException {
        ObjectNode datos = mapper.createObjectNode();
        datos.put("idTrabajo", idTrabajo);
        datos.put("idOperario", idOperario);
        datos.put("idGerente", idGerente);
        enviarJson("ASIGNAR_OPERARIO", datos);
    }

    public void solicitarListaOperarios(int idEmpresa) throws IOException {
        ObjectNode datos = mapper.createObjectNode();
        datos.put("idEmpresa", idEmpresa);
        enviarJson("GET_OPERARIOS", datos);
    }

    private void enviarJson(String accion, ObjectNode datos) throws IOException {
        socket.enviar(accion, datos, tokenActual);
    }

    // --- PROCESAMIENTO RESPUESTA ---

    public RespuestaServidor interpretarRespuesta(String json) throws ClienteException {
        try {
            JsonNode root = mapper.readTree(json);
            int status = root.has("status") ? root.get("status").asInt() : 0;
            String mensaje = root.has("mensaje") ? root.get("mensaje").asText() : "";
            JsonNode datos = root.has("datos") ? root.get("datos") : null;
            String tokenRespuesta = root.has("token") ? root.get("token").asText() : null;

            // Si el servidor envía un token (normalmente en el Login), lo guardamos
            if (tokenRespuesta != null) {
                this.tokenActual = tokenRespuesta;
            }

            return new RespuestaServidor(status, mensaje, datos, tokenRespuesta);
        } catch (Exception e) {
            throw new ClienteException("Error procesando JSON respuesta", e);
        }
    }
}

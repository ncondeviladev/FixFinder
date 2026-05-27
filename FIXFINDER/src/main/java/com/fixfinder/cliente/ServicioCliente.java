package com.fixfinder.cliente;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fixfinder.config.GlobalConfig;
import com.fixfinder.utilidades.ClienteException;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Servicio de comunicación adaptado para Spring Boot.
 * Utiliza HTTP (REST) para peticiones síncronas y STOMP (WebSockets) para notificaciones.
 * Mantiene la interfaz original para no romper el Dashboard.
 */
public class ServicioCliente {

    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final WebSocketStompClient stompClient;
    private StompSession stompSession;
    private Consumer<String> onMensajeRecibido;
    private String tokenActual;

    public ServicioCliente() {
        this.httpClient = HttpClient.newBuilder().build();
        this.mapper = new ObjectMapper();
        
        WebSocketClient client = new StandardWebSocketClient();
        this.stompClient = new WebSocketStompClient(client);
        this.stompClient.setMessageConverter(new MappingJackson2MessageConverter());
    }

    // --- GESTIÓN CONEXIÓN ---

    public void conectar(String host, int puerto) {
        // En REST la conexión es stateless, pero iniciamos el WebSocket para el tiempo real
        String wsUrl = GlobalConfig.getWsUrl();
        CompletableFuture<StompSession> future = stompClient.connectAsync(wsUrl, new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                stompSession = session;
                System.out.println("🔌 [WS] Conectado a STOMP!");
                
                // Suscribirse a temas generales
                session.subscribe("/topic/trabajos", this);
                session.subscribe("/topic/presupuestos", this);
                session.subscribe("/topic/gerentes", this);
            }

            @Override
            public java.lang.reflect.Type getPayloadType(StompHeaders headers) {
                return java.util.Map.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                try {
                    String json = mapper.writeValueAsString(payload);
                    if (onMensajeRecibido != null) {
                        onMensajeRecibido.accept(json);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
                System.err.println("❌ [WS] Error: " + exception.getMessage());
            }
        });
    }

    public void desconectar() {
        if (stompSession != null && stompSession.isConnected()) {
            stompSession.disconnect();
        }
    }

    public boolean isConectado() {
        return true; // En REST siempre se puede intentar una petición
    }

    public void setOnMensajeRecibido(Consumer<String> callback) {
        this.onMensajeRecibido = callback;
    }
    
    public void suscribirEmpresa(int idEmpresa) {
        if (stompSession != null && idEmpresa > 0) {
            stompSession.subscribe("/topic/empresa/" + idEmpresa, new StompSessionHandlerAdapter() {
                @Override
                public java.lang.reflect.Type getPayloadType(StompHeaders headers) {
                    return java.util.Map.class;
                }
                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    try {
                        String json = mapper.writeValueAsString(payload);
                        if (onMensajeRecibido != null) {
                            onMensajeRecibido.accept(json);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    // --- PETICIONES (Adaptadas a REST) ---

    public void enviarLogin(String email, String password) {
        Map<String, String> body = new HashMap<>();
        body.put("email", email);
        body.put("password", password);
        
        postAsync("/auth/login", body, "LOGIN");
    }

    public void solicitarListaTrabajos(int idUsuario, String rol) {
        // Siempre pasamos el idEmpresa para que el servidor incluya los trabajos ACEPTADO de nuestra empresa
        getAsync("/trabajos/pendientes", "LISTAR_TRABAJOS");
    }

    public void solicitarListaTrabajosPorEmpresa(int idEmpresa) {
        getAsync("/trabajos/pendientes?idEmpresa=" + idEmpresa, "LISTAR_TRABAJOS");
    }

    public void solicitarListaOperarios(int idEmpresa) {
        getAsync("/empresas/" + idEmpresa + "/operarios", "GET_OPERARIOS");
    }

    public void enviarAsignarOperario(int idTrabajo, int idOperario, int idGerente) {
        postAsync("/trabajos/" + idTrabajo + "/asignar/" + idOperario, null, "ASIGNAR_OPERARIO");
    }

    public void enviarCrearPresupuesto(int idTrabajo, int idEmpresa, double monto, String notas) {
        Map<String, Object> body = new HashMap<>();
        body.put("idTrabajo", idTrabajo);
        body.put("idEmpresa", idEmpresa);
        body.put("monto", monto);
        body.put("notas", notas);
        postAsync("/presupuestos", body, "CREAR_PRESUPUESTO");
    }

    public void enviarModificarEmpresa(int id, String nombre, String cif, String email, String tel, String dir, String urlLogo) {
        Map<String, Object> body = new HashMap<>();
        body.put("nombre", nombre);
        body.put("cif", cif);
        body.put("emailContacto", email);
        body.put("telefono", tel);
        body.put("direccion", dir);
        body.put("urlFoto", urlLogo);
        
        putAsync("/empresas/" + id, body, "MODIFICAR_EMPRESA");
    }

    public void enviarRegistroEmpresa(String nombre, String cif, String email, String tel, String dir,
            String nomG, String emailG, String passG, String dniG, String telG) {
        Map<String, Object> body = new HashMap<>();
        body.put("nombreEmpresa", nombre);
        body.put("cif", cif);
        body.put("emailEmpresa", email);
        body.put("telefonoEmpresa", tel);
        body.put("direccion", dir);
        body.put("nombreGerente", nomG);
        body.put("emailGerente", emailG);
        body.put("passwordGerente", passG);
        body.put("dniGerente", dniG);
        body.put("telefonoGerente", telG);
        postAsync("/auth/register-empresa", body, "REGISTRO");
    }

    public void enviarRegistroUsuario(boolean esOperario, String nombre, String dni, String email, String pass,
            String tel, String dir, String idEmpresa, String esp) {
        Map<String, Object> body = new HashMap<>();
        body.put("tipo", esOperario ? "OPERARIO" : "CLIENTE");
        body.put("nombreCompleto", nombre);
        body.put("dni", dni);
        body.put("email", email);
        body.put("passwordHash", pass);
        body.put("telefono", tel);
        body.put("direccion", dir);
        body.put("idEmpresa", idEmpresa);
        body.put("especialidad", esp);
        postAsync("/auth/register", body, "REGISTRO");
    }

    public void enviarCrearTrabajo(int idCliente, String titulo, String desc, String dir, int urg, String cat) {
        Map<String, Object> body = new HashMap<>();
        body.put("idCliente", idCliente);
        body.put("titulo", titulo);
        body.put("descripcion", desc);
        body.put("direccion", dir);
        body.put("urgencia", urg);
        body.put("categoria", cat);
        postAsync("/trabajos/solicitar", body, "CREAR_TRABAJO");
    }

    public void enviarActualizarFotoPerfil(int idUsuario, String url) {
        Map<String, String> body = Map.of("urlFoto", url);
        putAsync("/usuarios/" + idUsuario + "/foto", body, "ACTUALIZAR_FOTO_PERFIL");
    }

    public void obtenerDatosEmpresa(int idEmpresa) {
        getAsync("/empresas/" + idEmpresa + "/estadisticas", "GET_EMPRESA");
    }

    public void enviarModificarOperario(int id, String nom, String dni, String email, String tel, String esp, boolean activo) {
        Map<String, Object> body = new HashMap<>();
        body.put("nombreCompleto", nom);
        body.put("dni", dni);
        body.put("email", email);
        body.put("telefono", tel);
        body.put("especialidad", esp);
        body.put("estaActivo", activo);
        putAsync("/operarios/" + id, body, "MODIFICAR_OPERARIO");
    }

    public void enviarPing() {
        // No necesario en REST, pero lo mantenemos para compatibilidad
    }

    public void logout() {
        this.tokenActual = null;
        desconectar();
    }

    // --- UTILS HTTP ---

    private void postAsync(String path, Object body, String accion) {
        try {
            String jsonBody = body != null ? mapper.writeValueAsString(body) : "{}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GlobalConfig.getApiUrl() + path))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            executeRequest(request, accion);
        } catch (Exception e) {
            notificarError(accion, e.getMessage());
        }
    }

    private void getAsync(String path, String accion) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GlobalConfig.getApiUrl() + path))
                .GET()
                .build();
        executeRequest(request, accion);
    }

    private void putAsync(String path, Object body, String accion) {
        try {
            String jsonBody = mapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GlobalConfig.getApiUrl() + path))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            executeRequest(request, accion);
        } catch (Exception e) {
            notificarError(accion, e.getMessage());
        }
    }

    private void executeRequest(HttpRequest request, String accion) {
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    try {
                        ObjectNode root = mapper.createObjectNode();
                        root.put("status", response.statusCode());
                        root.put("accion", accion);
                        
                        JsonNode responseBody = mapper.readTree(response.body());
                        root.set("datos", responseBody);
                        
                        if (response.statusCode() >= 400) {
                            root.put("mensaje", responseBody.has("error") ? responseBody.get("error").asText() : "Error en servidor");
                        } else {
                            root.put("mensaje", "Operación exitosa");
                        }

                        return mapper.writeValueAsString(root);
                    } catch (Exception e) {
                        return "{\"status\": 500, \"mensaje\": \"Error parseando respuesta\", \"accion\": \"" + accion + "\"}";
                    }
                })
                .thenAccept(json -> {
                    if (onMensajeRecibido != null) {
                        onMensajeRecibido.accept(json);
                    }
                });
    }

    private void notificarError(String accion, String msg) {
        if (onMensajeRecibido != null) {
            onMensajeRecibido.accept("{\"status\": 500, \"mensaje\": \"" + msg + "\", \"accion\": \"" + accion + "\"}");
        }
    }

    public RespuestaServidor parseRespuesta(String json) throws ClienteException {
        try {
            JsonNode root = mapper.readTree(json);
            return new RespuestaServidor(
                root.path("status").asInt(),
                root.path("mensaje").asText(),
                root.path("datos"),
                root.path("token").asText(null),
                root.path("accion").asText(null)
            );
        } catch (Exception e) {
            throw new ClienteException("Error parsing JSON", e);
        }
    }
}

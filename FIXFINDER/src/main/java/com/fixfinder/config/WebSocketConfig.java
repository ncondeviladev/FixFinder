package com.fixfinder.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuración de WebSockets mediante STOMP.
 * Permite la comunicación bidireccional y el envío de notificaciones push
 * a los clientes (Dashboard y App móvil).
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Habilitar un broker simple para los prefijos /topic (broadcast) y /queue (mensajes privados)
        config.enableSimpleBroker("/topic", "/queue");
        // Prefijo para los mensajes enviados desde el cliente al servidor (@MessageMapping)
        config.setApplicationDestinationPrefixes("/app");
        // Prefijo para los mensajes enviados a usuarios específicos
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint de conexión para WebSockets
        registry.addEndpoint("/ws")
                .setAllowedOrigins("*");
        
        // Soporte para navegadores o clientes que no soportan WebSockets nativos
        registry.addEndpoint("/ws")
                .setAllowedOrigins("*")
                .withSockJS();
    }
}

package com.fixfinder.red;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestiona las sesiones activas en el servidor.
 * Guarda la relación entre un Token (UUID) y el ID del usuario.
 */
public class SessionManager {

    // Mapa seguro para hilos: Token -> ID Usuario
    private static final Map<String, Integer> sesiones = new ConcurrentHashMap<>();

    /**
     * Crea una sesión nueva y devuelve el token.
     */
    public static String crearSesion(int idUsuario) {
        String token = UUID.randomUUID().toString();
        sesiones.put(token, idUsuario);
        return token;
    }

    /**
     * Verifica si un token es válido.
     */
    public static boolean esTokenValido(String token) {
        return token != null && sesiones.containsKey(token);
    }

    /**
     * Obtiene el ID de usuario asociado a un token.
     */
    public static Integer obtenerUsuario(String token) {
        return sesiones.get(token);
    }

    /**
     * Cierra la sesión (elimina el token).
     */
    public static void cerrarSesion(String token) {
        if (token != null) {
            sesiones.remove(token);
        }
    }
}

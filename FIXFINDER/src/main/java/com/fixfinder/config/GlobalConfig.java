package com.fixfinder.config;

/**
 * Configuración centralizada para cambiar entre entornos LOCAL y NUBE (AWS).
 */
public class GlobalConfig {
    // EL INTERRUPTOR FINAL: Cambiar a 'true' para usar AWS, 'false' para Docker Local
    public static final boolean MODO_NUBE = true; 

    // --- CONFIGURACIÓN DE RED (IPs y Puertos) ---
    public static final String LOCAL_IP = "127.0.0.1";
    public static final String CLOUD_IP = "51.48.92.76";
    public static final int PORT = 5000;

    // --- CONFIGURACIÓN DE BASE DE DATOS (Local vs RDS) ---
    private static final String DB_LOCAL_URL = "jdbc:mysql://localhost:3306/fixfinder?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DB_LOCAL_USER = "user";
    private static final String DB_LOCAL_PASS = "user";

    private static final String DB_CLOUD_URL = "jdbc:mysql://ffrds.cle4w4mummno.eu-south-2.rds.amazonaws.com:3306/fixfinder?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DB_CLOUD_USER = "ffrds";
    private static final String DB_CLOUD_PASS = "12345678";

    /**
     * Devuelve la IP del servidor según el modo activo.
     */
    public static String getServerIp() {
        return MODO_NUBE ? CLOUD_IP : LOCAL_IP;
    }

    /**
     * Devuelve la URL de la base de datos según el modo activo.
     */
    public static String getDbUrl() {
        return MODO_NUBE ? DB_CLOUD_URL : DB_LOCAL_URL;
    }

    /**
     * Devuelve el usuario de la base de datos según el modo activo.
     */
    public static String getDbUser() {
        return MODO_NUBE ? DB_CLOUD_USER : DB_LOCAL_USER;
    }

    /**
     * Devuelve la contraseña de la base de datos según el modo activo.
     */
    public static String getDbPass() {
        return MODO_NUBE ? DB_CLOUD_PASS : DB_LOCAL_PASS;
    }
}

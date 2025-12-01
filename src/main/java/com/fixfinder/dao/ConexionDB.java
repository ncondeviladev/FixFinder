package com.fixfinder.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Gestiona la conexión a la base de datos MySQL usando el patrón Singleton.
 *
 * Esto asegura que reutilizamos la misma lógica de conexión en toda la app.
 */
public class ConexionDB {

    // 1. Configuración de la conexión (coincide con tu docker-compose.yml)
    private static final String URL = "jdbc:mysql://localhost:3306/fixfinder?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String USER = "user";
    private static final String PASSWORD = "user";

    // 2. La variable estática que guardará la ÚNICA conexión
    private static Connection conexion;

    // 3. Constructor privado: Nadie puede hacer 'new ConexionDB()' desde fuera
    private ConexionDB() {
    }

    /**
     * Obtiene la conexión activa. Si no existe o está cerrada, la crea.
     * 
     * 
     *  * @throws SQLException Si falla el acceso a la BD.
     */
    public static Connection getConnection() throws SQLException {
        if (conexion == null || conexion.isClosed()) {
            try {
                // Carga el driver explícitamente
                Class.forName("com.mysql.cj.jdbc.Driver");
                
                // Establece la conexión
                conexion = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("✅ Conexión a base de datos establecida correctamente.");
                
            } catch (ClassNotFoundException e) {
                System.err.println("❌ Error: No se encontró el Driver de MySQL.");
                throw new SQLException("Driver no encontrado", e);
            }
        }
        return conexion;
    }

    /**
     * Cierra la conexión si está abierta.
     */
    public static void cerrarConexion() {
        if (conexion != null) {
            try {
                conexion.close();
                System.out.println("🔒 Conexión cerrada.");
            } catch (SQLException e) {
                System.err.println("Error al cerrar la conexión: " + e.getMessage());
            }
        }
    }
}

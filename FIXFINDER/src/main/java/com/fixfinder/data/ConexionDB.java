package com.fixfinder.data;

import com.fixfinder.config.GlobalConfig;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Gestiona la conexión a la base de datos MySQL con soporte para multi-hilo.
 * Utiliza ThreadLocal para que cada hilo tenga su propia conexión independiente.
 */
public class ConexionDB {

    private static final String URL = GlobalConfig.getDbUrl();
    private static final String USER = GlobalConfig.getDbUser();
    private static final String PASSWORD = GlobalConfig.getDbPass();

    // Cada hilo del servidor tendrá su propia conexión aislada
    private static final ThreadLocal<Connection> threadLocalConnection = new ThreadLocal<>();

    private ConexionDB() {
    }

    /**
     * Obtiene la conexión asociada al hilo actual. Si no existe, la crea.
     */
    public static Connection getConnection() throws SQLException {
        Connection conn = threadLocalConnection.get();
        if (conn == null || conn.isClosed()) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                conn = DriverManager.getConnection(URL, USER, PASSWORD);
                threadLocalConnection.set(conn);
                // System.out.println("[DB] Nueva conexión creada para el hilo: " + Thread.currentThread().getName());
            } catch (ClassNotFoundException e) {
                throw new SQLException("Driver MySQL no encontrado", e);
            }
        }
        return conn;
    }

    /**
     * Cierra la conexión del hilo actual. 
     * Debe llamarse al finalizar el procesamiento de una petición.
     */
    public static void cerrarConexion() {
        Connection conn = threadLocalConnection.get();
        if (conn != null) {
            try {
                if (!conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {
                System.err.println("Error al cerrar conexión de hilo: " + e.getMessage());
            } finally {
                threadLocalConnection.remove();
            }
        }
    }
}

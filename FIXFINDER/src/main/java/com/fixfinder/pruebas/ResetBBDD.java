package com.fixfinder.pruebas;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class ResetBBDD {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/fixfinder";
        String user = "root";
        String pass = "root";

        try (Connection conn = DriverManager.getConnection(url, user, pass);
                Statement stmt = conn.createStatement()) {

            System.out.println("⏳ Eliminando datos...");

            // Desactivar checks para poder borrar sin líos de orden
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");

            stmt.executeUpdate("TRUNCATE TABLE trabajo");
            stmt.executeUpdate("TRUNCATE TABLE operario");
            stmt.executeUpdate("TRUNCATE TABLE cliente");
            stmt.executeUpdate("TRUNCATE TABLE usuario");
            stmt.executeUpdate("TRUNCATE TABLE empresa");

            // Reactivar checks
            stmt.execute("SET FOREIGN_KEY_CHECKS = 1");

            System.out.println("✅ Base de Datos LIMPIA y lista para empezar de cero.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

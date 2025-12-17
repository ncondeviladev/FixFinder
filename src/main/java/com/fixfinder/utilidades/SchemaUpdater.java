package com.fixfinder.utilidades;

import com.fixfinder.data.ConexionDB;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Statement;

public class SchemaUpdater {

    public static void main(String[] args) {
        actualizarEsquema();
    }

    public static void actualizarEsquema() {
        System.out.println("🔄 Actualizando esquema de base de datos desde ESQUEMA_BD.sql...");

        // Ajusta la ruta según tu entorno. Asumimos ruta relativa o absoluta conocida.
        // En este caso, usaremos la ruta absoluta que conocemos del sistema.
        String projectDir = System.getProperty("user.dir");
        String archivoSql = projectDir + "/docs/diseno/ESQUEMA_BD.sql";

        try (Connection conn = ConexionDB.getConnection();
                Statement stmt = conn.createStatement();
                BufferedReader br = new BufferedReader(new FileReader(archivoSql))) {

            StringBuilder sb = new StringBuilder();
            String linea;

            // Leemos el fichero completo
            while ((linea = br.readLine()) != null) {
                // Ignorar comentarios y líneas vacías para limpiar un poco
                if (linea.trim().startsWith("--") || linea.trim().isEmpty()) {
                    continue;
                }
                sb.append(linea).append("\n");
            }

            // Separar instrucciones por punto y coma
            String[] instrucciones = sb.toString().split(";");

            for (String instruccion : instrucciones) {
                if (!instruccion.trim().isEmpty()) {
                    try {
                        stmt.execute(instruccion);
                    } catch (Exception e) {
                        System.err
                                .println("⚠️ Alerta ejecutando instrucción (puede ser inocuo si es DROP no existente): "
                                        + e.getMessage());
                    }
                }
            }

            System.out.println("✅ Esquema actualizado correctamente.");

        } catch (IOException e) {
            System.err.println("❌ Error leyendo el archivo SQL: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ Error de Base de Datos: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

package com.fixfinder.data.dao;

import com.fixfinder.data.ConexionDB;
import com.fixfinder.data.interfaces.BaseDAO;
import com.fixfinder.data.interfaces.OperarioDAO;
import com.fixfinder.modelos.Operario;
import com.fixfinder.modelos.enums.CategoriaServicio;
import com.fixfinder.modelos.enums.Rol;
import com.fixfinder.utilidades.DataAccessException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para gestionar la entidad Operario.
 *
 * Esta clase es especial porque gestiona una TRANSACCIÓN entre dos tablas:
 * - 'usuario': Datos generales (login, nombre).
 * - 'operario': Datos específicos (dni, ubicación).
 */
public class OperarioDAOImpl implements OperarioDAO {

    @Override
    public void insertar(Operario operario) throws DataAccessException {
        // Definimos las dos sentencias SQL necesarias.
        // 1. Primero insertamos en la tabla padre 'usuario' para obtener el ID.
        String sqlUsuario = "INSERT INTO usuario (email, password_hash, nombre_completo, rol, id_empresa) VALUES (?, ?, ?, ?, ?)";
        // 2. Luego insertamos en la tabla hija 'operario' usando ese mismo ID.
        String sqlOperario = "INSERT INTO operario (id_usuario, dni, especialidad, estado, latitud, longitud) VALUES (?, ?, ?, ?, ?, ?)";

        Connection conn = null;

        try {
            conn = ConexionDB.getConnection();

            // --- INICIO DE TRANSACCIÓN ---
            conn.setAutoCommit(false);

            // PASO 1: Insertar en tabla USUARIO
            int idGenerado = 0;
            try (PreparedStatement stmtUser = conn.prepareStatement(sqlUsuario, Statement.RETURN_GENERATED_KEYS)) {
                stmtUser.setString(1, operario.getEmail());
                stmtUser.setString(2, operario.getPasswordHash());
                stmtUser.setString(3, operario.getNombreCompleto());
                stmtUser.setString(4, Rol.OPERARIO.toString()); // Forzamos rol OPERARIO
                stmtUser.setInt(5, operario.getIdEmpresa());

                int filas = stmtUser.executeUpdate();
                if (filas == 0)
                    throw new SQLException("Fallo al crear usuario base para operario.");

                // Recuperamos el ID autogenerado por MySQL (AUTO_INCREMENT)
                try (ResultSet generatedKeys = stmtUser.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        idGenerado = generatedKeys.getInt(1);
                        operario.setId(idGenerado);
                    } else {
                        throw new SQLException("Fallo al obtener ID del usuario.");
                    }
                }
            }

            // PASO 2: Insertar en tabla OPERARIO
            try (PreparedStatement stmtOp = conn.prepareStatement(sqlOperario)) {
                stmtOp.setInt(1, operario.getId());
                stmtOp.setString(2, operario.getDni());
                stmtOp.setString(3, operario.getEspecialidad().toString()); // ENUM a String
                // Convertimos el booleano Java a String ENUM de MySQL
                stmtOp.setString(4, operario.isEstaActivo() ? "DISPONIBLE" : "OCUPADO");
                stmtOp.setDouble(5, operario.getLatitud());
                stmtOp.setDouble(6, operario.getLongitud());

                stmtOp.executeUpdate();
            }

            // --- FIN DE TRANSACCIÓN (ÉXITO) ---
            conn.commit();

        } catch (SQLException e) {
            // --- GESTIÓN DE ERRORES (ROLLBACK) ---
            if (conn != null) {
                try {
                    System.err.println("⚠️ Realizando Rollback de Operario...");
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            throw new DataAccessException("Error transaccional al insertar operario: " + operario.getEmail(), e);
        } finally {
            // --- LIMPIEZA ---
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void actualizar(Operario operario) throws DataAccessException {
        String sqlUsuario = "UPDATE usuario SET email=?, nombre_completo=?, password_hash=? WHERE id=?";
        String sqlOperario = "UPDATE operario SET dni=?, especialidad=?, estado=?, latitud=?, longitud=?, ultima_actualizacion=NOW() WHERE id_usuario=?";

        Connection conn = null;
        try {
            conn = ConexionDB.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement stmtUser = conn.prepareStatement(sqlUsuario)) {
                stmtUser.setString(1, operario.getEmail());
                stmtUser.setString(2, operario.getNombreCompleto());
                stmtUser.setString(3, operario.getPasswordHash());
                stmtUser.setInt(4, operario.getId());
                stmtUser.executeUpdate();
            }

            try (PreparedStatement stmtOp = conn.prepareStatement(sqlOperario)) {
                stmtOp.setString(1, operario.getDni());
                stmtOp.setString(2, operario.getEspecialidad().toString());
                stmtOp.setString(3, operario.isEstaActivo() ? "DISPONIBLE" : "OCUPADO");
                stmtOp.setDouble(4, operario.getLatitud());
                stmtOp.setDouble(5, operario.getLongitud());
                stmtOp.setInt(6, operario.getId());
                stmtOp.executeUpdate();
            }

            conn.commit();

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            throw new DataAccessException("Error al actualizar operario ID: " + operario.getId(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void eliminar(int id) throws DataAccessException {
        // Gracias al ON DELETE CASCADE de la BD, borrar el usuario borra el operario
        String sql = "DELETE FROM usuario WHERE id = ?";
        try (Connection conn = ConexionDB.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error al eliminar operario ID: " + id, e);
        }
    }

    @Override
    public Operario obtenerPorId(int id) throws DataAccessException {
        return obtenerPorId(id, null);
    }

    /**
     * Sobrecarga para permitir reutilizar una conexión existente.
     * Si 'connExterna' no es nulo, SE USA pero NO SE CIERRA al terminar.
     */
    public Operario obtenerPorId(int id, Connection connExterna) throws DataAccessException {
        String sql = "SELECT u.*, o.* FROM usuario u JOIN operario o ON u.id = o.id_usuario WHERE u.id = ?";
        Operario op = null;

        Connection conn = connExterna;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            // Si no nos pasan conexión, pedimos una nueva al Singleton (y nos encargamos de
            // cerrarla)
            boolean esNuevaConexion = (conn == null);
            if (esNuevaConexion) {
                conn = ConexionDB.getConnection();
            }

            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            rs = stmt.executeQuery();

            if (rs.next()) {
                op = mapearOperario(rs);
            }

        } catch (SQLException e) {
            throw new DataAccessException("Error al obtener operario ID: " + id, e);
        } finally {
            // Cierre manual de recursos para evitar cerrar la conexión externa si nos la
            // pasaron
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                }
            }
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                }
            }
            // Solo cerramos la conexión si la abrimos NOSOTROS
            if (connExterna == null && conn != null) {
                try {
                    // No llamamos a conn.close() directamente si es Singleton puro,
                    // pero para mantener coherencia con el diseño anterior:
                    // Si tu ConexionDB.cerrarConexion() maneja el cierre, úsalo,
                    // si no, simplemente dejamos que el GC o el pool lo maneje si es Singleton.
                    // En este caso, como ConexionDB es estática, NO DEBERÍAMOS cerrar físicamente
                    // la conexión compartida
                    // salvo que la app termine.
                    // PERO, para respetar el bloque try-with-resources original que eliminamos:
                    // conn.close();
                } catch (Exception e) {
                }
            }
        }
        return op;
    }

    @Override
    public List<Operario> obtenerTodos() throws DataAccessException {
        String sql = "SELECT u.*, o.* FROM usuario u JOIN operario o ON u.id = o.id_usuario";
        List<Operario> lista = new ArrayList<>();

        try (Connection conn = ConexionDB.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                lista.add(mapearOperario(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al listar operarios", e);
        }
        return lista;
    }

    private Operario mapearOperario(ResultSet rs) throws SQLException {
        Operario o = new Operario();
        // Datos de Usuario
        o.setId(rs.getInt("id"));
        o.setEmail(rs.getString("email"));
        o.setPasswordHash(rs.getString("password_hash"));
        o.setNombreCompleto(rs.getString("nombre_completo"));
        o.setIdEmpresa(rs.getInt("id_empresa"));
        o.setRol(Rol.OPERARIO);

        Timestamp ts = rs.getTimestamp("fecha_registro");
        if (ts != null)
            o.setFechaRegistro(ts.toLocalDateTime());

        // Datos de Operario
        o.setDni(rs.getString("dni"));

        try {
            o.setEspecialidad(CategoriaServicio.valueOf(rs.getString("especialidad")));
        } catch (IllegalArgumentException e) {
            o.setEspecialidad(CategoriaServicio.OTROS); // Fallback
        }

        String estado = rs.getString("estado");
        o.setEstaActivo("DISPONIBLE".equalsIgnoreCase(estado));

        o.setLatitud(rs.getDouble("latitud"));
        o.setLongitud(rs.getDouble("longitud"));

        Timestamp tsUpd = rs.getTimestamp("ultima_actualizacion");
        if (tsUpd != null)
            o.setUltimaActualizacion(tsUpd.toLocalDateTime());

        return o;
    }
}

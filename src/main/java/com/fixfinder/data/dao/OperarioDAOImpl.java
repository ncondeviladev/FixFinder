package com.fixfinder.data.dao;

import com.fixfinder.data.ConexionDB;
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
 * Esta clase gestiona una TRANSACCIÓN entre dos tablas:
 * - 'usuario': Datos generales (dni, nombre, login).
 * - 'operario': Datos específicos (id_empresa, especialidad).
 */
public class OperarioDAOImpl implements OperarioDAO {

    @Override
    public void insertar(Operario operario) throws DataAccessException {
        // 1. Insertar en 'usuario' (Datos comunes + DNI)
        String sqlUsuario = "INSERT INTO usuario (email, password_hash, nombre_completo, rol, dni) VALUES (?, ?, ?, ?, ?)";
        // 2. Insertar en 'operario' (Datos específicos + id_empresa)
        String sqlOperario = "INSERT INTO operario (id_usuario, id_empresa, especialidad, estado, latitud, longitud) VALUES (?, ?, ?, ?, ?, ?)";

        Connection conn = null;

        try {
            conn = ConexionDB.getConnection();
            conn.setAutoCommit(false); // INICIO TRANSACCIÓN

            // PASO 1: Insertar en USUARIO
            int idGenerado = 0;
            try (PreparedStatement stmtUser = conn.prepareStatement(sqlUsuario, Statement.RETURN_GENERATED_KEYS)) {
                stmtUser.setString(1, operario.getEmail());
                stmtUser.setString(2, operario.getPasswordHash());
                stmtUser.setString(3, operario.getNombreCompleto());
                stmtUser.setString(4, Rol.OPERARIO.toString());
                stmtUser.setString(5, operario.getDni()); // DNI va a usuario ahora

                int filas = stmtUser.executeUpdate();
                if (filas == 0)
                    throw new SQLException("Fallo al crear usuario base.");

                try (ResultSet generatedKeys = stmtUser.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        idGenerado = generatedKeys.getInt(1);
                        operario.setId(idGenerado);
                    } else {
                        throw new SQLException("Fallo al obtener ID del usuario.");
                    }
                }
            }

            // PASO 2: Insertar en OPERARIO
            try (PreparedStatement stmtOp = conn.prepareStatement(sqlOperario)) {
                stmtOp.setInt(1, operario.getId());
                stmtOp.setInt(2, operario.getIdEmpresa()); // Ahora va aquí
                stmtOp.setString(3, operario.getEspecialidad().toString());
                stmtOp.setString(4, operario.isEstaActivo() ? "DISPONIBLE" : "OCUPADO");
                stmtOp.setDouble(5, operario.getLatitud());
                stmtOp.setDouble(6, operario.getLongitud());

                stmtOp.executeUpdate();
            }

            conn.commit(); // CONFIRMAR TRANSACCIÓN

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            throw new DataAccessException("Error transaccional al insertar operario: " + operario.getEmail(), e);
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
    public void actualizar(Operario operario) throws DataAccessException {
        // Actualizar datos base (Usuario)
        String sqlUsuario = "UPDATE usuario SET email=?, nombre_completo=?, password_hash=?, dni=? WHERE id=?";
        // Actualizar datos específicos (Operario)
        String sqlOperario = "UPDATE operario SET id_empresa=?, especialidad=?, estado=?, latitud=?, longitud=?, ultima_actualizacion=NOW() WHERE id_usuario=?";

        Connection conn = null;
        try {
            conn = ConexionDB.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement stmtUser = conn.prepareStatement(sqlUsuario)) {
                stmtUser.setString(1, operario.getEmail());
                stmtUser.setString(2, operario.getNombreCompleto());
                stmtUser.setString(3, operario.getPasswordHash());
                stmtUser.setString(4, operario.getDni());
                stmtUser.setInt(5, operario.getId());
                stmtUser.executeUpdate();
            }

            try (PreparedStatement stmtOp = conn.prepareStatement(sqlOperario)) {
                stmtOp.setInt(1, operario.getIdEmpresa());
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
        // ON DELETE CASCADE se encarga de borrar en 'operario' al borrar 'usuario'
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

    public Operario obtenerPorId(int id, Connection connExterna) throws DataAccessException {
        String sql = "SELECT u.*, o.* FROM usuario u JOIN operario o ON u.id = o.id_usuario WHERE u.id = ?";
        Operario op = null;

        Connection conn = connExterna;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
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
            // Manejo de cierre de conexión externa omitido para brevedad/consistencia
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
        o.setId(rs.getInt("id")); // Usar alias o nombre columna directo si es único
        o.setEmail(rs.getString("email"));
        o.setPasswordHash(rs.getString("password_hash"));
        o.setNombreCompleto(rs.getString("nombre_completo"));
        o.setDni(rs.getString("dni")); // Ahora viene de usuario
        o.setRol(Rol.OPERARIO);

        Timestamp ts = rs.getTimestamp("fecha_registro");
        if (ts != null)
            o.setFechaRegistro(ts.toLocalDateTime());

        // Datos de Operario
        o.setIdEmpresa(rs.getInt("id_empresa")); // Ahora viene de operario

        try {
            o.setEspecialidad(CategoriaServicio.valueOf(rs.getString("especialidad")));
        } catch (IllegalArgumentException e) {
            o.setEspecialidad(CategoriaServicio.OTROS);
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

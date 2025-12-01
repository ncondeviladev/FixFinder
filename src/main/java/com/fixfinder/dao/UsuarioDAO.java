package com.fixfinder.dao;

import com.fixfinder.modelos.Usuario;
import com.fixfinder.modelos.enums.Rol;
import com.fixfinder.utilidades.DataAccessException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación del DAO para la entidad Usuario.
 *
 * Gestiona la persistencia en la tabla 'usuario'.
 */
public class UsuarioDAO implements BaseDAO<Usuario> {

    @Override
    public void insertar(Usuario usuario) throws DataAccessException {
        String sql = "INSERT INTO usuario (email, password_hash, nombre_completo, rol, id_empresa) VALUES (?, ?, ?, ?, ?)";

        // Try-with-resources: Cierra automáticamente PreparedStatement y ResultSet
        try (Connection conn = ConexionDB.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, usuario.getEmail());
            stmt.setString(2, usuario.getPasswordHash());
            stmt.setString(3, usuario.getNombreCompleto());
            stmt.setString(4, usuario.getRol().toString());
            stmt.setInt(5, usuario.getIdEmpresa());

            int filasAfectadas = stmt.executeUpdate();
            if (filasAfectadas == 0) {
                throw new DataAccessException("No se pudo insertar el usuario, ninguna fila afectada.");
            }

            // Recuperar el ID generado automáticamente (AUTO_INCREMENT)
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    usuario.setId(generatedKeys.getInt(1));
                } else {
                    throw new DataAccessException("No se pudo obtener el ID del usuario insertado.");
                }
            }

        } catch (SQLException e) {
            // Elevamos la excepción SQL a nuestra excepción personalizada
            throw new DataAccessException("Error al insertar usuario: " + usuario.getEmail(), e);
        }
    }

    @Override
    public void actualizar(Usuario usuario) throws DataAccessException {
        String sql = "UPDATE usuario SET email = ?, password_hash = ?, nombre_completo = ?, rol = ?, id_empresa = ? WHERE id = ?";

        try (Connection conn = ConexionDB.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, usuario.getEmail());
            stmt.setString(2, usuario.getPasswordHash());
            stmt.setString(3, usuario.getNombreCompleto());
            stmt.setString(4, usuario.getRol().toString());
            stmt.setInt(5, usuario.getIdEmpresa());
            stmt.setInt(6, usuario.getId());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("Error al actualizar usuario ID: " + usuario.getId(), e);
        }
    }

    @Override
    public void eliminar(int id) throws DataAccessException {
        String sql = "DELETE FROM usuario WHERE id = ?";

        try (Connection conn = ConexionDB.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("Error al eliminar usuario ID: " + id, e);
        }
    }

    @Override
    public Usuario obtenerPorId(int id) throws DataAccessException {
        String sql = "SELECT * FROM usuario WHERE id = ?";
        Usuario usuario = null;

        try (Connection conn = ConexionDB.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    usuario = mapearResultSet(rs);
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Error al obtener usuario ID: " + id, e);
        }
        return usuario;
    }

    @Override
    public List<Usuario> obtenerTodos() throws DataAccessException {
        String sql = "SELECT * FROM usuario";
        List<Usuario> lista = new ArrayList<>();

        try (Connection conn = ConexionDB.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                lista.add(mapearResultSet(rs));
            }

        } catch (SQLException e) {
            throw new DataAccessException("Error al listar usuarios", e);
        }
        return lista;
    }

    /**
     * Método auxiliar para convertir una fila de la BD en un objeto Usuario.
     * Evita repetir código en cada consulta.
     */
    private Usuario mapearResultSet(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setId(rs.getInt("id"));
        u.setEmail(rs.getString("email"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setNombreCompleto(rs.getString("nombre_completo"));
        u.setIdEmpresa(rs.getInt("id_empresa"));

        // Convertir String a Enum de forma segura
        try {
            u.setRol(Rol.valueOf(rs.getString("rol")));
        } catch (IllegalArgumentException e) {
            // Si la BD tiene un rol desconocido, asignamos CLIENTE por defecto o lanzamos
            // error
            u.setRol(Rol.CLIENTE);
        }

        // Convertir Timestamp SQL a LocalDateTime Java
        Timestamp ts = rs.getTimestamp("fecha_registro");
        if (ts != null) {
            u.setFechaRegistro(ts.toLocalDateTime());
        }

        return u;
    }
}

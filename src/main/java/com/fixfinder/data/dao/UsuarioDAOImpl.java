package com.fixfinder.data.dao;

import com.fixfinder.data.ConexionDB;
import com.fixfinder.data.interfaces.UsuarioDAO;
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
public class UsuarioDAOImpl implements UsuarioDAO {

    @Override
    public void insertar(Usuario usuario) throws DataAccessException {
        // SQL actualizada: dni incuido, id_empresa eliminado
        String sql = "INSERT INTO usuario (email, password_hash, nombre_completo, rol, telefono, direccion, url_foto, dni) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = ConexionDB.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, usuario.getEmail());
            stmt.setString(2, usuario.getPasswordHash());
            stmt.setString(3, usuario.getNombreCompleto());
            stmt.setString(4, usuario.getRol().toString());
            stmt.setString(5, usuario.getTelefono());
            stmt.setString(6, usuario.getDireccion());
            stmt.setString(7, usuario.getUrlFoto());
            stmt.setString(8, usuario.getDni());

            int filasAfectadas = stmt.executeUpdate();
            if (filasAfectadas == 0) {
                throw new DataAccessException("No se pudo insertar el usuario, ninguna fila afectada.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    usuario.setId(generatedKeys.getInt(1));
                } else {
                    throw new DataAccessException("No se pudo obtener el ID del usuario insertado.");
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Error al insertar usuario: " + usuario.getEmail(), e);
        }
    }

    @Override
    public void actualizar(Usuario usuario) throws DataAccessException {
        // SQL actualizada
        String sql = "UPDATE usuario SET email = ?, password_hash = ?, nombre_completo = ?, rol = ?, telefono = ?, direccion = ?, url_foto = ?, dni = ? WHERE id = ?";

        try (Connection conn = ConexionDB.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, usuario.getEmail());
            stmt.setString(2, usuario.getPasswordHash());
            stmt.setString(3, usuario.getNombreCompleto());
            stmt.setString(4, usuario.getRol().toString());
            stmt.setString(5, usuario.getTelefono());
            stmt.setString(6, usuario.getDireccion());
            stmt.setString(7, usuario.getUrlFoto());
            stmt.setString(8, usuario.getDni());
            stmt.setInt(9, usuario.getId());

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
        return obtenerPorId(id, null);
    }

    public Usuario obtenerPorId(int id, Connection connExterna) throws DataAccessException {
        String sql = "SELECT * FROM usuario WHERE id = ?";
        Usuario usuario = null;

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
                usuario = mapearResultSet(rs);
            }

        } catch (SQLException e) {
            throw new DataAccessException("Error al obtener usuario ID: " + id, e);
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

    @Override
    public Usuario obtenerPorEmail(String email) throws DataAccessException {
        String sql = "SELECT * FROM usuario WHERE email = ?";
        Usuario usuario = null;

        try (Connection conn = ConexionDB.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    usuario = mapearResultSet(rs);
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Error al obtener usuario por email: " + email, e);
        }
        return usuario;
    }

    /**
     * Mapea un ResultSet a un objeto Usuario (Operario o Cliente).
     */
    private Usuario mapearResultSet(ResultSet rs) throws SQLException {
        Rol rol;
        try {
            rol = Rol.valueOf(rs.getString("rol"));
        } catch (IllegalArgumentException e) {
            rol = Rol.CLIENTE; // Default
        }

        Usuario u;
        if (rol == Rol.OPERARIO) {
            u = new com.fixfinder.modelos.Operario();
        } else {
            // Cliente, Admin, Gerente, etc. se tratan como Cliente/Usuario genérico por
            // ahora
            u = new com.fixfinder.modelos.Cliente();
        }

        u.setRol(rol);
        u.setId(rs.getInt("id"));
        u.setEmail(rs.getString("email"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setNombreCompleto(rs.getString("nombre_completo"));
        u.setTelefono(rs.getString("telefono"));
        u.setDireccion(rs.getString("direccion"));
        u.setUrlFoto(rs.getString("url_foto"));
        u.setDni(rs.getString("dni"));

        Timestamp ts = rs.getTimestamp("fecha_registro");
        if (ts != null) {
            u.setFechaRegistro(ts.toLocalDateTime());
        }

        return u;
    }
}

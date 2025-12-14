package com.fixfinder.data.dao;

import com.fixfinder.data.ConexionDB;
import com.fixfinder.data.interfaces.BaseDAO;
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
        String sql = "INSERT INTO usuario (email, password_hash, nombre_completo, rol, id_empresa, telefono, direccion, url_foto) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        // Try-with-resources: Cierra automáticamente PreparedStatement y ResultSet
        try (Connection conn = ConexionDB.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, usuario.getEmail());
            stmt.setString(2, usuario.getPasswordHash());
            stmt.setString(3, usuario.getNombreCompleto());
            stmt.setString(4, usuario.getRol().toString());
            stmt.setInt(5, usuario.getIdEmpresa());
            stmt.setString(6, usuario.getTelefono());
            stmt.setString(7, usuario.getDireccion());
            stmt.setString(8, usuario.getUrlFoto());

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
        String sql = "UPDATE usuario SET email = ?, password_hash = ?, nombre_completo = ?, rol = ?, id_empresa = ?, telefono = ?, direccion = ?, url_foto = ? WHERE id = ?";

        try (Connection conn = ConexionDB.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, usuario.getEmail());
            stmt.setString(2, usuario.getPasswordHash());
            stmt.setString(3, usuario.getNombreCompleto());
            stmt.setString(4, usuario.getRol().toString());
            stmt.setInt(5, usuario.getIdEmpresa());
            stmt.setString(6, usuario.getTelefono());
            stmt.setString(7, usuario.getDireccion());
            stmt.setString(8, usuario.getUrlFoto());
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
            // No cerramos conexión compartida
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
     * Busca un usuario por su email.
     * Útil para el proceso de login.
     *
     * @param email Email del usuario.
     * @return El objeto Usuario si existe, null en caso contrario.
     * @throws DataAccessException Si ocurre un error SQL.
     */
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
        u.setTelefono(rs.getString("telefono"));
        u.setDireccion(rs.getString("direccion"));
        u.setUrlFoto(rs.getString("url_foto"));

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

package com.fixfinder.dao;

import com.fixfinder.modelos.Empresa;
import com.fixfinder.utilidades.DataAccessException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para gestionar la entidad Empresa.
 */
public class EmpresaDAO implements BaseDAO<Empresa> {

    @Override
    public void insertar(Empresa empresa) throws DataAccessException {
        String sql = "INSERT INTO empresa (nombre, cif, direccion, telefono, email_contacto) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = ConexionDB.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, empresa.getNombre());
            stmt.setString(2, empresa.getCif());
            stmt.setString(3, empresa.getDireccion());
            stmt.setString(4, empresa.getTelefono());
            stmt.setString(5, empresa.getEmailContacto());

            int filas = stmt.executeUpdate();
            if (filas == 0) {
                throw new DataAccessException("Error al insertar empresa: Ninguna fila afectada.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    empresa.setId(generatedKeys.getInt(1));
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Error SQL al insertar empresa: " + empresa.getNombre(), e);
        }
    }

    @Override
    public void actualizar(Empresa empresa) throws DataAccessException {
        String sql = "UPDATE empresa SET nombre=?, cif=?, direccion=?, telefono=?, email_contacto=? WHERE id=?";

        try (Connection conn = ConexionDB.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, empresa.getNombre());
            stmt.setString(2, empresa.getCif());
            stmt.setString(3, empresa.getDireccion());
            stmt.setString(4, empresa.getTelefono());
            stmt.setString(5, empresa.getEmailContacto());
            stmt.setInt(6, empresa.getId());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("Error al actualizar empresa ID: " + empresa.getId(), e);
        }
    }

    @Override
    public void eliminar(int id) throws DataAccessException {
        String sql = "DELETE FROM empresa WHERE id=?";

        try (Connection conn = ConexionDB.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("Error al eliminar empresa ID: " + id, e);
        }
    }

    @Override
    public Empresa obtenerPorId(int id) throws DataAccessException {
        String sql = "SELECT * FROM empresa WHERE id=?";
        Empresa empresa = null;

        try (Connection conn = ConexionDB.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    empresa = mapear(rs);
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Error al obtener empresa ID: " + id, e);
        }
        return empresa;
    }

    @Override
    public List<Empresa> obtenerTodos() throws DataAccessException {
        String sql = "SELECT * FROM empresa";
        List<Empresa> lista = new ArrayList<>();

        try (Connection conn = ConexionDB.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                lista.add(mapear(rs));
            }

        } catch (SQLException e) {
            throw new DataAccessException("Error al listar empresas", e);
        }
        return lista;
    }

    private Empresa mapear(ResultSet rs) throws SQLException {
        Empresa e = new Empresa();
        e.setId(rs.getInt("id"));
        e.setNombre(rs.getString("nombre"));
        e.setCif(rs.getString("cif"));
        e.setDireccion(rs.getString("direccion"));
        e.setTelefono(rs.getString("telefono"));
        e.setEmailContacto(rs.getString("email_contacto"));
        // e.setFechaAlta(...) si fuera necesario
        return e;
    }
}

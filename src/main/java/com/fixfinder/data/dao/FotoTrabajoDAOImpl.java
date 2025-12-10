package com.fixfinder.data.dao;

import com.fixfinder.data.ConexionDB;
import com.fixfinder.data.interfaces.BaseDAO;
import com.fixfinder.data.interfaces.FotoTrabajoDAO;
import com.fixfinder.modelos.componentes.FotoTrabajo;
import com.fixfinder.utilidades.DataAccessException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO interno para gestionar las fotos asociadas a un trabajo.
 * No suele llamarse directamente desde la UI, sino desde TrabajoDAO.
 */
public class FotoTrabajoDAOImpl implements FotoTrabajoDAO {

    @Override
    public void insertar(FotoTrabajo foto) throws DataAccessException {
        String sql = "INSERT INTO foto_trabajo (id_trabajo, url_archivo) VALUES (?, ?)";

        try (Connection conn = ConexionDB.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, foto.getIdTrabajo());
            stmt.setString(2, foto.getUrl());

            int filas = stmt.executeUpdate();
            if (filas == 0)
                throw new DataAccessException("Error: Ninguna fila afectada al insertar foto.");

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    foto.setId(generatedKeys.getInt(1));
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Error al insertar foto: " + foto.getUrl(), e);
        }
    }

    @Override
    public void actualizar(FotoTrabajo foto) throws DataAccessException {
        // Raramente se actualiza una foto, se suele borrar y subir nueva.
        String sql = "UPDATE foto_trabajo SET id_trabajo=?, url_archivo=? WHERE id=?";
        try (Connection conn = ConexionDB.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, foto.getIdTrabajo());
            stmt.setString(2, foto.getUrl());
            stmt.setInt(3, foto.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error updating foto ID: " + foto.getId(), e);
        }
    }

    @Override
    public void eliminar(int id) throws DataAccessException {
        String sql = "DELETE FROM foto_trabajo WHERE id=?";
        try (Connection conn = ConexionDB.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error deleting foto ID: " + id, e);
        }
    }

    @Override
    public FotoTrabajo obtenerPorId(int id) throws DataAccessException {
        return null; // No suelo necesitar obtener una foto individual por ID, sino todas las de un
                     // trabajo.
    }

    @Override
    public List<FotoTrabajo> obtenerTodos() throws DataAccessException {
        return new ArrayList<>(); // No suelo necesitar todas las fotos del sistema.
    }

    /**
     * Recupera todas las fotos de un trabajo específico.
     */
    public List<FotoTrabajo> obtenerPorTrabajo(int idTrabajo) throws DataAccessException {
        String sql = "SELECT * FROM foto_trabajo WHERE id_trabajo = ?";
        List<FotoTrabajo> lista = new ArrayList<>();

        try (Connection conn = ConexionDB.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idTrabajo);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(new FotoTrabajo(
                            rs.getInt("id"),
                            rs.getInt("id_trabajo"),
                            rs.getString("url_archivo")));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error obteniendo fotos del trabajo " + idTrabajo, e);
        }
        return lista;
    }
}

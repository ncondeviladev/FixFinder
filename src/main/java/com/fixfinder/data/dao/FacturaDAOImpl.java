package com.fixfinder.data.dao;

import com.fixfinder.data.ConexionDB;
import com.fixfinder.data.interfaces.BaseDAO;
import com.fixfinder.data.interfaces.FacturaDAO;
import com.fixfinder.modelos.Factura;
import com.fixfinder.modelos.Trabajo;
import com.fixfinder.utilidades.DataAccessException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para gestión de Facturas.
 * Se encarga de guardar el PDF (ruta) y marcar como pagada.
 */
public class FacturaDAOImpl implements FacturaDAO {

    private final TrabajoDAOImpl trabajoDAO = new TrabajoDAOImpl();

    @Override
    public void insertar(Factura factura) throws DataAccessException {
        String sql = "INSERT INTO factura (id_trabajo, numero_factura, base_imponible, iva, total, ruta_pdf, pagada) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = ConexionDB.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, factura.getTrabajo().getId());
            stmt.setString(2, factura.getNumeroFactura());
            stmt.setDouble(3, factura.getBaseImponible());
            stmt.setDouble(4, factura.getIva());
            stmt.setDouble(5, factura.getTotal());
            stmt.setString(6, factura.getRutaPdf());
            stmt.setBoolean(7, factura.isPagada());

            int filas = stmt.executeUpdate();
            if (filas == 0)
                throw new DataAccessException("Error insertando factura.");

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    factura.setId(rs.getInt(1));
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Error SQL insertando factura " + factura.getNumeroFactura(), e);
        }
    }

    @Override
    public void actualizar(Factura factura) throws DataAccessException {
        String sql = "UPDATE factura SET ruta_pdf=?, pagada=? WHERE id=?";
        try (Connection conn = ConexionDB.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, factura.getRutaPdf());
            stmt.setBoolean(2, factura.isPagada());
            stmt.setInt(3, factura.getId());
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("Error actualizando factura ID " + factura.getId(), e);
        }
    }

    @Override
    public void eliminar(int id) throws DataAccessException {
        String sql = "DELETE FROM factura WHERE id=?";
        try (Connection conn = ConexionDB.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error borrando factura ID " + id, e);
        }
    }

    @Override
    public Factura obtenerPorId(int id) throws DataAccessException {
        String sql = "SELECT * FROM factura WHERE id=?";
        Factura f = null;
        try (Connection conn = ConexionDB.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next())
                    f = mapear(rs);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error obteniendo factura ID " + id, e);
        }
        return f;
    }

    @Override
    public List<Factura> obtenerTodos() throws DataAccessException {
        String sql = "SELECT * FROM factura";
        List<Factura> lista = new ArrayList<>();
        try (Connection conn = ConexionDB.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
            while (rs.next())
                lista.add(mapear(rs));
        } catch (SQLException e) {
            throw new DataAccessException("Error listando facturas", e);
        }
        return lista;
    }

    public Factura obtenerPorTrabajo(int idTrabajo) throws DataAccessException {
        String sql = "SELECT * FROM factura WHERE id_trabajo=?";
        Factura f = null;
        try (Connection conn = ConexionDB.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idTrabajo);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next())
                    f = mapear(rs);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error obteniendo factura por trabajo", e);
        }
        return f;
    }

    private Factura mapear(ResultSet rs) throws SQLException {
        Factura f = new Factura();
        f.setId(rs.getInt("id"));
        f.setNumeroFactura(rs.getString("numero_factura"));
        f.setBaseImponible(rs.getDouble("base_imponible"));
        f.setIva(rs.getDouble("iva"));
        f.setTotal(rs.getDouble("total"));
        f.setRutaPdf(rs.getString("ruta_pdf"));
        f.setPagada(rs.getBoolean("pagada"));
        Timestamp ts = rs.getTimestamp("fecha_emision");
        if (ts != null)
            f.setFechaEmision(ts.toLocalDateTime());

        // Cargar Trabajo asociado
        try {
            int idTrabajo = rs.getInt("id_trabajo");
            Trabajo t = trabajoDAO.obtenerPorId(idTrabajo);
            f.setTrabajo(t);
        } catch (DataAccessException e) {
            // log
        }
        return f;
    }
}

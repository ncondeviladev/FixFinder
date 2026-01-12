package com.fixfinder.data.dao;

import com.fixfinder.data.ConexionDB;
import com.fixfinder.data.interfaces.BaseDAO;
import com.fixfinder.data.interfaces.PresupuestoDAO;
import com.fixfinder.modelos.Empresa;
import com.fixfinder.modelos.Presupuesto;
import com.fixfinder.modelos.Trabajo;
import com.fixfinder.utilidades.DataAccessException;

import com.fixfinder.modelos.enums.EstadoPresupuesto;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PresupuestoDAOImpl implements PresupuestoDAO {

    private final TrabajoDAOImpl trabajoDAO = new TrabajoDAOImpl();
    private final EmpresaDAOImpl empresaDAO = new EmpresaDAOImpl();

    @Override
    public void insertar(Presupuesto presupuesto) throws DataAccessException {
        String sql = "INSERT INTO presupuesto (id_trabajo, id_empresa, monto, estado, fecha_envio) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = ConexionDB.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, presupuesto.getTrabajo().getId());
            stmt.setInt(2, presupuesto.getEmpresa().getId());
            stmt.setDouble(3, presupuesto.getMonto());

            String estado = (presupuesto.getEstado() != null) ? presupuesto.getEstado().toString()
                    : EstadoPresupuesto.PENDIENTE.toString();
            stmt.setString(4, estado);

            Timestamp fecha = (presupuesto.getFechaEnvio() != null) ? Timestamp.valueOf(presupuesto.getFechaEnvio())
                    : Timestamp.valueOf(LocalDateTime.now());
            stmt.setTimestamp(5, fecha);

            int filas = stmt.executeUpdate();
            if (filas == 0)
                throw new DataAccessException("No se pudo insertar el presupuesto.");

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    presupuesto.setId(generatedKeys.getInt(1));
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Error al insertar presupuesto", e);
        }
    }

    @Override
    public void actualizar(Presupuesto presupuesto) throws DataAccessException {
        String sql = "UPDATE presupuesto SET id_trabajo=?, id_empresa=?, monto=?, estado=?, fecha_envio=? WHERE id=?";

        try (Connection conn = ConexionDB.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, presupuesto.getTrabajo().getId());
            stmt.setInt(2, presupuesto.getEmpresa().getId());
            stmt.setDouble(3, presupuesto.getMonto());
            stmt.setString(4, presupuesto.getEstado().toString());
            stmt.setTimestamp(5, Timestamp.valueOf(presupuesto.getFechaEnvio()));
            stmt.setInt(6, presupuesto.getId());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("Error al actualizar presupuesto ID: " + presupuesto.getId(), e);
        }
    }

    @Override
    public void eliminar(int id) throws DataAccessException {
        String sql = "DELETE FROM presupuesto WHERE id=?";
        try (Connection conn = ConexionDB.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error al eliminar presupuesto ID: " + id, e);
        }
    }

    @Override
    public Presupuesto obtenerPorId(int id) throws DataAccessException {
        String sql = "SELECT * FROM presupuesto WHERE id=?";
        Presupuesto p = null;

        try (Connection conn = ConexionDB.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    p = mapear(rs);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al obtener presupuesto ID: " + id, e);
        }

        cargarRelaciones(p);
        return p;
    }

    @Override
    public List<Presupuesto> obtenerTodos() throws DataAccessException {
        String sql = "SELECT * FROM presupuesto";
        List<Presupuesto> lista = new ArrayList<>();

        try (Connection conn = ConexionDB.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al listar presupuestos", e);
        }

        for (Presupuesto p : lista) {
            cargarRelaciones(p);
        }
        return lista;
    }

    public List<Presupuesto> obtenerPorTrabajo(int idTrabajo) throws DataAccessException {
        String sql = "SELECT * FROM presupuesto WHERE id_trabajo = ?";
        List<Presupuesto> lista = new ArrayList<>();

        try (Connection conn = ConexionDB.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idTrabajo);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al listar presupuestos por trabajo", e);
        }
        for (Presupuesto p : lista) {
            cargarRelaciones(p);
        }
        return lista;
    }

    private Presupuesto mapear(ResultSet rs) throws SQLException {
        Presupuesto p = new Presupuesto();
        p.setId(rs.getInt("id"));
        p.setMonto(rs.getDouble("monto"));

        Timestamp ts = rs.getTimestamp("fecha_envio");
        if (ts != null) {
            p.setFechaEnvio(ts.toLocalDateTime());
        }

        String estadoStr = rs.getString("estado");
        if (estadoStr != null) {
            try {
                p.setEstado(EstadoPresupuesto.valueOf(estadoStr));
            } catch (IllegalArgumentException e) {
                p.setEstado(EstadoPresupuesto.PENDIENTE);
            }
        } else {
            p.setEstado(EstadoPresupuesto.PENDIENTE);
        }

        // Mapeo lazy de IDs para cargar relaciones después
        int idTrabajo = rs.getInt("id_trabajo");
        Trabajo t = new Trabajo();
        t.setId(idTrabajo);
        p.setTrabajo(t);

        int idEmpresa = rs.getInt("id_empresa");
        Empresa e = new Empresa();
        e.setId(idEmpresa);
        p.setEmpresa(e);

        return p;
    }

    private void cargarRelaciones(Presupuesto p) {
        if (p == null)
            return;

        try {
            if (p.getTrabajo() != null && p.getTrabajo().getId() > 0) {
                Trabajo t = trabajoDAO.obtenerPorId(p.getTrabajo().getId());
                p.setTrabajo(t);
            }
        } catch (DataAccessException e) {
            System.err.println("Error cargando trabajo para presupuesto " + p.getId());
        }

        try {
            if (p.getEmpresa() != null && p.getEmpresa().getId() > 0) {
                Empresa e = empresaDAO.obtenerPorId(p.getEmpresa().getId());
                p.setEmpresa(e);
            }
        } catch (DataAccessException e) {
            System.err.println("Error cargando empresa para presupuesto " + p.getId());
        }
    }
}

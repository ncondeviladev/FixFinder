package com.fixfinder.data.dao;

import com.fixfinder.data.ConexionDB;
import com.fixfinder.data.interfaces.BaseDAO;
import com.fixfinder.data.interfaces.PresupuestoDAO;
import com.fixfinder.modelos.Empresa;
import com.fixfinder.modelos.Presupuesto;
import com.fixfinder.modelos.Trabajo;
import com.fixfinder.utilidades.DataAccessException;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PresupuestoDAOImpl implements PresupuestoDAO {

    private final TrabajoDAOImpl trabajoDAO = new TrabajoDAOImpl();
    private final EmpresaDAOImpl empresaDAO = new EmpresaDAOImpl();

    @Override
    public void insertar(Presupuesto presupuesto) throws DataAccessException {
        String sql = "INSERT INTO presupuesto (id_trabajo, id_empresa, monto, estado) VALUES (?, ?, ?, ?)";

        try (Connection conn = ConexionDB.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, presupuesto.getTrabajo().getId());
            stmt.setInt(2, presupuesto.getEmpresa().getId());
            stmt.setDouble(3, presupuesto.getMonto());
            // En modelo Presupuesto aún no tenemos el Enum o String de estado, cuidado.
            // Asumimos que lo añadirás. De momento hardcodeamos o usamos getter si existe.
            // stmt.setString(4, presupuesto.getEstado());
            stmt.setString(4, "PENDIENTE"); // Provisional hasta que actualices el modelo Presupuesto

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
        String sql = "UPDATE presupuesto SET id_trabajo=?, id_empresa=?, monto=?, estado=? WHERE id=?";

        try (Connection conn = ConexionDB.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, presupuesto.getTrabajo().getId());
            stmt.setInt(2, presupuesto.getEmpresa().getId());
            stmt.setDouble(3, presupuesto.getMonto());
            stmt.setString(4, "PENDIENTE"); // Provisional
            stmt.setInt(5, presupuesto.getId());

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
        return lista;
    }

    private Presupuesto mapear(ResultSet rs) throws SQLException {
        Presupuesto p = new Presupuesto();
        p.setId(rs.getInt("id"));
        p.setMonto(rs.getDouble("monto"));
        // p.setEstado(rs.getString("estado"));
        p.setFechaEnvio(rs.getTimestamp("fecha_envio"));

        // Recuperar objetos relacionados
        try {
            int idTrabajo = rs.getInt("id_trabajo");
            // Cuidado con bucle infinito si Trabajo carga Presupuestos y viceversa.
            // Aquí cargamos Trabajo "Lazy" o completo según necesidad.
            // Para evitar recursión infinita simple, podríamos crear un metodo
            // "obtenerTrabajoSimple"
            // Pero de momento usamos el normal.
            Trabajo t = trabajoDAO.obtenerPorId(idTrabajo);
            p.setTrabajo(t);
        } catch (DataAccessException e) {
            // Log
        }

        try {
            int idEmpresa = rs.getInt("id_empresa");
            Empresa e = empresaDAO.obtenerPorId(idEmpresa);
            p.setEmpresa(e);
        } catch (DataAccessException e) {
            // Log
        }

        return p;
    }
}

package com.fixfinder.data.dao;

import com.fixfinder.data.ConexionDB;
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

    private static final String SQL_CON_RELACIONES = "SELECT p.*, " +
            " e.nombre AS emp_nombre, e.cif AS emp_cif, e.email_contacto AS emp_email, e.telefono AS emp_telefono, " +
            " e.direccion AS emp_direccion, e.url_foto AS emp_logo_url " +
            "FROM presupuesto p " +
            "LEFT JOIN empresa e ON p.id_empresa = e.id";

    @Override
    public void insertar(Presupuesto presupuesto) throws DataAccessException {
        String sql = "INSERT INTO presupuesto (id_trabajo, id_empresa, monto, estado, fecha_envio, notas) VALUES (?, ?, ?, ?, ?, ?)";

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
            stmt.setString(6, presupuesto.getNotas());

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
        String sql = "UPDATE presupuesto SET id_trabajo=?, id_empresa=?, monto=?, estado=?, fecha_envio=?, notas=? WHERE id=?";

        try (Connection conn = ConexionDB.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, presupuesto.getTrabajo().getId());
            stmt.setInt(2, presupuesto.getEmpresa().getId());
            stmt.setDouble(3, presupuesto.getMonto());
            stmt.setString(4, presupuesto.getEstado().toString());
            stmt.setTimestamp(5, Timestamp.valueOf(presupuesto.getFechaEnvio()));
            stmt.setString(6, presupuesto.getNotas());
            stmt.setInt(7, presupuesto.getId());

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
    public void eliminarPorEmpresa(int idEmpresa) throws DataAccessException {
        String sql = "DELETE FROM presupuesto WHERE id_empresa = ?";
        try (Connection conn = ConexionDB.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idEmpresa);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error al eliminar presupuestos de la empresa: " + idEmpresa, e);
        }
    }

    @Override
    public Presupuesto obtenerPorId(int id) throws DataAccessException {
        String sql = SQL_CON_RELACIONES + " WHERE p.id=?";
        Presupuesto p = null;

        try (Connection conn = ConexionDB.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    p = mapearConRelaciones(rs);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al obtener presupuesto ID: " + id, e);
        }

        // El trabajo se carga por separado para evitar JOINs circulares complejos
        hidratarTrabajo(p);
        return p;
    }

    @Override
    public List<Presupuesto> obtenerTodos() throws DataAccessException {
        List<Presupuesto> lista = new ArrayList<>();

        try (Connection conn = ConexionDB.getConnection();
                PreparedStatement stmt = conn.prepareStatement(SQL_CON_RELACIONES);
                ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                lista.add(mapearConRelaciones(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al listar presupuestos", e);
        }

        for (Presupuesto p : lista) {
            hidratarTrabajo(p);
        }
        return lista;
    }

    public List<Presupuesto> obtenerPorTrabajo(int idTrabajo) throws DataAccessException {
        String sql = SQL_CON_RELACIONES + " WHERE p.id_trabajo = ?";
        List<Presupuesto> lista = new ArrayList<>();

        try (Connection conn = ConexionDB.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idTrabajo);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearConRelaciones(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al listar presupuestos por trabajo", e);
        }

        // Carga de trabajo fuera del bucle para evitar cierres de conexión
        for (Presupuesto p : lista) {
            hidratarTrabajo(p);
        }
        return lista;
    }

    private Presupuesto mapear(ResultSet rs) throws SQLException {
        Presupuesto p = new Presupuesto();
        p.setId(rs.getInt("id"));
        p.setMonto(rs.getDouble("monto"));
        p.setNotas(rs.getString("notas"));

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

    private Presupuesto mapearConRelaciones(ResultSet rs) throws SQLException {
        Presupuesto p = mapear(rs);

        // Hidratar Empresa desde el JOIN
        int idEmp = rs.getInt("id_empresa");
        if (idEmp > 0) {
            Empresa e = new Empresa();
            e.setId(idEmp);
            e.setNombre(rs.getString("emp_nombre"));
            e.setCif(rs.getString("emp_cif"));
            e.setEmailContacto(rs.getString("emp_email"));
            e.setTelefono(rs.getString("emp_telefono"));
            e.setDireccion(rs.getString("emp_direccion"));
            e.setUrlFoto(rs.getString("emp_logo_url"));
            p.setEmpresa(e);
        }
        return p;
    }

    private void hidratarTrabajo(Presupuesto p) {
        if (p == null || p.getTrabajo() == null)
            return;
        try {
            // Se carga fuera del bucle del ResultSet para evitar el error de conexión
            // cerrada
            p.setTrabajo(trabajoDAO.obtenerPorId(p.getTrabajo().getId()));
        } catch (DataAccessException e) {
            System.err.println("Error hidratando trabajo en presupuesto " + p.getId());
        }
    }
}

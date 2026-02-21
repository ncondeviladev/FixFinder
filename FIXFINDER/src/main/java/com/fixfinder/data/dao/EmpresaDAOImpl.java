package com.fixfinder.data.dao;

import com.fixfinder.data.ConexionDB;
import com.fixfinder.data.interfaces.EmpresaDAO;
import com.fixfinder.modelos.Empresa;
import com.fixfinder.modelos.enums.CategoriaServicio;
import com.fixfinder.utilidades.DataAccessException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EmpresaDAOImpl implements EmpresaDAO {

    @Override
    public void insertar(Empresa empresa) throws DataAccessException {
        String sql = "INSERT INTO empresa (nombre, cif, direccion, telefono, email_contacto, url_foto) VALUES (?, ?, ?, ?, ?, ?)";
        String sqlEspec = "INSERT INTO empresa_especialidad (id_empresa, categoria) VALUES (?, ?)";

        Connection conn = null;
        try {
            conn = ConexionDB.getConnection();
            conn.setAutoCommit(false); // Inicio Transacción

            int idGenerado = 0;
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, empresa.getNombre());
                stmt.setString(2, empresa.getCif());
                stmt.setString(3, empresa.getDireccion());
                stmt.setString(4, empresa.getTelefono());
                stmt.setString(5, empresa.getEmailContacto());
                stmt.setString(6, empresa.getUrlFoto());

                int filas = stmt.executeUpdate();
                if (filas == 0)
                    throw new SQLException("Fallo al insertar empresa, ninguna fila afectada.");

                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        idGenerado = generatedKeys.getInt(1);
                        empresa.setId(idGenerado);
                    } else {
                        throw new SQLException("Fallo al insertar empresa, no se obtuvo ID.");
                    }
                }
            }

            // Insertar especialidades
            if (empresa.getEspecialidades() != null && !empresa.getEspecialidades().isEmpty()) {
                try (PreparedStatement stmtSpec = conn.prepareStatement(sqlEspec)) {
                    for (CategoriaServicio cat : empresa.getEspecialidades()) {
                        stmtSpec.setInt(1, idGenerado);
                        stmtSpec.setString(2, cat.toString());
                        stmtSpec.addBatch();
                    }
                    stmtSpec.executeBatch();
                }
            }

            conn.commit(); // Confirmar Transacción

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            throw new DataAccessException("Error transaccional al insertar empresa: " + empresa.getNombre(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    @Override
    public void actualizar(Empresa empresa) throws DataAccessException {
        String sql = "UPDATE empresa SET nombre=?, cif=?, direccion=?, telefono=?, email_contacto=?, url_foto=? WHERE id=?";
        // Estrategia simple: Borrar todas las especialidades y re-insertar
        String sqlDelSpec = "DELETE FROM empresa_especialidad WHERE id_empresa=?";
        String sqlInsSpec = "INSERT INTO empresa_especialidad (id_empresa, categoria) VALUES (?, ?)";

        Connection conn = null;
        try {
            conn = ConexionDB.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, empresa.getNombre());
                stmt.setString(2, empresa.getCif());
                stmt.setString(3, empresa.getDireccion());
                stmt.setString(4, empresa.getTelefono());
                stmt.setString(5, empresa.getEmailContacto());
                stmt.setString(6, empresa.getUrlFoto());
                stmt.setInt(7, empresa.getId());
                stmt.executeUpdate();
            }

            // Actualizar especialidades
            try (PreparedStatement stmtDel = conn.prepareStatement(sqlDelSpec)) {
                stmtDel.setInt(1, empresa.getId());
                stmtDel.executeUpdate();
            }

            if (empresa.getEspecialidades() != null && !empresa.getEspecialidades().isEmpty()) {
                try (PreparedStatement stmtSpec = conn.prepareStatement(sqlInsSpec)) {
                    for (CategoriaServicio cat : empresa.getEspecialidades()) {
                        stmtSpec.setInt(1, empresa.getId());
                        stmtSpec.setString(2, cat.toString());
                        stmtSpec.addBatch();
                    }
                    stmtSpec.executeBatch();
                }
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
            throw new DataAccessException("Error actualizando empresa ID: " + empresa.getId(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
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
        Empresa emp = null;

        try (Connection conn = ConexionDB.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    emp = mapear(rs, conn);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al obtener empresa ID: " + id, e);
        }
        return emp;
    }

    @Override
    public List<Empresa> obtenerTodos() throws DataAccessException {
        String sql = "SELECT * FROM empresa";
        List<Empresa> lista = new ArrayList<>();

        try (Connection conn = ConexionDB.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs, conn));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al listar empresas", e);
        }
        return lista;
    }

    // Sobrecarga de mapear para recibir conexión y reusarla para subconsultas
    private Empresa mapear(ResultSet rs, Connection conn) throws SQLException {
        Empresa e = new Empresa();
        e.setId(rs.getInt("id"));
        e.setNombre(rs.getString("nombre"));
        e.setCif(rs.getString("cif"));
        e.setDireccion(rs.getString("direccion"));
        e.setTelefono(rs.getString("telefono"));
        e.setEmailContacto(rs.getString("email_contacto"));
        e.setUrlFoto(rs.getString("url_foto"));

        // Cargar Especialidades
        String sqlSpec = "SELECT categoria FROM empresa_especialidad WHERE id_empresa=?";
        try (PreparedStatement stmt = conn.prepareStatement(sqlSpec)) {
            stmt.setInt(1, e.getId());
            try (ResultSet rsSpec = stmt.executeQuery()) {
                while (rsSpec.next()) {
                    try {
                        e.getEspecialidades().add(CategoriaServicio.valueOf(rsSpec.getString("categoria")));
                    } catch (IllegalArgumentException ex) {
                        // Ignorar categorías desconocidas/viejas
                    }
                }
            }
        }

        return e;
    }
}

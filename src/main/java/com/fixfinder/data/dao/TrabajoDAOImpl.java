package com.fixfinder.data.dao;

import com.fixfinder.data.ConexionDB;
import com.fixfinder.data.interfaces.*;
import com.fixfinder.modelos.Operario;
import com.fixfinder.modelos.Trabajo;
import com.fixfinder.modelos.componentes.FotoTrabajo;
import com.fixfinder.modelos.componentes.Ubicacion;
import com.fixfinder.modelos.enums.CategoriaServicio;
import com.fixfinder.modelos.enums.EstadoTrabajo;
import com.fixfinder.utilidades.DataAccessException;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO Central para la gestión de incidencias/trabajos.
 * Orquesta la carga de fotos y relaciones.
 */
public class TrabajoDAOImpl implements TrabajoDAO {

    private final UsuarioDAOImpl usuarioDAO = new UsuarioDAOImpl();
    private final OperarioDAOImpl operarioDAO = new OperarioDAOImpl();
    private final FotoTrabajoDAOImpl fotoDAO = new FotoTrabajoDAOImpl();

    @Override
    public void insertar(Trabajo trabajo) throws DataAccessException {
        // 1. Insertar el Trabajo Base. NOTA: id_categoria ahora es 'categoria' (ENUM)
        String sql = "INSERT INTO trabajo (id_cliente, categoria, titulo, descripcion, direccion, ubicacion_lat, ubicacion_lon, estado, valoracion, comentario_cliente) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;

        try {
            conn = ConexionDB.getConnection();
            conn.setAutoCommit(false); // Transacción para asegurar fotos + trabajo

            int idGenerado = 0;

            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, trabajo.getCliente().getId());
                // ENUM a String
                stmt.setString(2, trabajo.getCategoria().toString());
                stmt.setString(3, trabajo.getTitulo());
                stmt.setString(4, trabajo.getDescripcion());
                stmt.setString(5, trabajo.getDireccion());

                if (trabajo.getUbicacion() != null) {
                    stmt.setDouble(6, trabajo.getUbicacion().getLatitud());
                    stmt.setDouble(7, trabajo.getUbicacion().getLongitud());
                } else {
                    stmt.setNull(6, Types.DOUBLE);
                    stmt.setNull(7, Types.DOUBLE);
                }

                stmt.setString(8, trabajo.getEstado() != null ? trabajo.getEstado().toString()
                        : EstadoTrabajo.PENDIENTE.toString());
                stmt.setInt(9, trabajo.getValoracion());
                stmt.setString(10, trabajo.getComentarioCliente());

                stmt.executeUpdate();

                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        idGenerado = generatedKeys.getInt(1);
                        trabajo.setId(idGenerado);
                    } else {
                        throw new SQLException("Fallo al obtener ID del trabajo.");
                    }
                }
            }

            // 2. Insertar las fotos (Recorriendo la lista)
            if (trabajo.getFotos() != null && !trabajo.getFotos().isEmpty()) {
                String sqlFoto = "INSERT INTO foto_trabajo (id_trabajo, url_archivo) VALUES (?, ?)";
                try (PreparedStatement stmtFoto = conn.prepareStatement(sqlFoto)) {
                    for (FotoTrabajo foto : trabajo.getFotos()) {
                        stmtFoto.setInt(1, idGenerado);
                        stmtFoto.setString(2, foto.getUrl());
                        stmtFoto.addBatch(); // Optimización: Batch Insert
                    }
                    stmtFoto.executeBatch();
                }
            }

            conn.commit(); // Todo OK

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            throw new DataAccessException("Error transaccional al insertar trabajo: " + trabajo.getTitulo(), e);
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
    public void actualizar(Trabajo trabajo) throws DataAccessException {
        String sql = "UPDATE trabajo SET id_cliente=?, id_operario=?, categoria=?, titulo=?, descripcion=?, " +
                "direccion=?, ubicacion_lat=?, ubicacion_lon=?, estado=?, fecha_finalizacion=?, valoracion=?, comentario_cliente=? WHERE id=?";

        try (Connection conn = ConexionDB.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, trabajo.getCliente().getId());

            if (trabajo.getOperarioAsignado() != null) {
                stmt.setInt(2, trabajo.getOperarioAsignado().getId());
            } else {
                stmt.setNull(2, Types.INTEGER);
            }

            stmt.setString(3, trabajo.getCategoria().toString());
            stmt.setString(4, trabajo.getTitulo());
            stmt.setString(5, trabajo.getDescripcion());
            stmt.setString(6, trabajo.getDireccion());

            if (trabajo.getUbicacion() != null) {
                stmt.setDouble(7, trabajo.getUbicacion().getLatitud());
                stmt.setDouble(8, trabajo.getUbicacion().getLongitud());
            } else {
                stmt.setNull(7, Types.DOUBLE);
                stmt.setNull(8, Types.DOUBLE);
            }

            stmt.setString(9, trabajo.getEstado().toString());
            System.out.println(
                    "[DEBUG-DAO] Actualizando trabajo ID " + trabajo.getId() + " a estado: " + trabajo.getEstado());

            if (trabajo.getFechaFinalizacion() != null) {
                stmt.setTimestamp(10, Timestamp.valueOf(trabajo.getFechaFinalizacion()));
            } else {
                stmt.setNull(10, Types.TIMESTAMP);
            }

            stmt.setInt(11, trabajo.getValoracion());
            stmt.setString(12, trabajo.getComentarioCliente());

            stmt.setInt(13, trabajo.getId());

            stmt.executeUpdate();

            // Nota: Aquí no actualizamos las fotos. Si se quieren añadir fotos nuevas, se
            // debería tener un método addFoto().

        } catch (SQLException e) {
            throw new DataAccessException("Error al actualizar trabajo ID: " + trabajo.getId(), e);
        }
    }

    @Override
    public void eliminar(int id) throws DataAccessException {
        String sql = "DELETE FROM trabajo WHERE id=?";
        try (Connection conn = ConexionDB.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error al eliminar trabajo ID: " + id, e);
        }
    }

    @Override
    public Trabajo obtenerPorId(int id) throws DataAccessException {
        String sql = "SELECT * FROM trabajo WHERE id=?";
        Trabajo t = null;

        try (Connection conn = ConexionDB.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    t = mapear(rs);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al obtener trabajo ID: " + id, e);
        }

        // Cargar relaciones fuera del bloque try-with-resources del ResultSet principal
        if (t != null) {
            cargarRelaciones(t);
        }

        return t;
    }

    @Override
    public List<Trabajo> obtenerTodos() throws DataAccessException {
        String sql = "SELECT * FROM trabajo";
        List<Trabajo> lista = new ArrayList<>();

        try (Connection conn = ConexionDB.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al listar trabajos", e);
        }

        // Cargar relaciones para cada elemento
        for (Trabajo t : lista) {
            cargarRelaciones(t);
        }

        return lista;
    }

    /**
     * Obtiene los trabajos pendientes de una categoría específica.
     * Útil para que los operarios vean qué hay disponible.
     */
    public List<Trabajo> obtenerPendientesPorCategoria(CategoriaServicio categoria) throws DataAccessException {
        String sql = "SELECT * FROM trabajo WHERE estado = 'PENDIENTE' AND categoria = ?";
        List<Trabajo> lista = new ArrayList<>();

        try (Connection conn = ConexionDB.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, categoria.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al listar trabajos pendientes por categoría", e);
        }

        // Cargar relaciones
        for (Trabajo t : lista) {
            cargarRelaciones(t);
        }

        return lista;
    }

    /**
     * Convierte el ResultSet actual en un objeto Trabajo.
     * IMPORTANTE: No cierra el ResultSet ni ejecuta consultas anidadas sobre la
     * misma conexión
     * si eso implica cerrar el ResultSet padre.
     */
    private Trabajo mapear(ResultSet rs) throws SQLException {
        Trabajo t = new Trabajo();
        t.setId(rs.getInt("id"));
        t.setTitulo(rs.getString("titulo"));
        t.setDescripcion(rs.getString("descripcion"));
        t.setDireccion(rs.getString("direccion"));
        t.setValoracion(rs.getInt("valoracion"));
        t.setComentarioCliente(rs.getString("comentario_cliente"));

        // Mapeo Enum Categoria
        try {
            t.setCategoria(CategoriaServicio.valueOf(rs.getString("categoria")));
        } catch (IllegalArgumentException e) {
            t.setCategoria(CategoriaServicio.OTROS);
        }

        // Mapeo Enum Estado
        try {
            t.setEstado(EstadoTrabajo.valueOf(rs.getString("estado")));
        } catch (IllegalArgumentException e) {
            t.setEstado(EstadoTrabajo.PENDIENTE);
        }

        Timestamp tsCrea = rs.getTimestamp("fecha_creacion");
        if (tsCrea != null)
            t.setFechaCreacion(tsCrea.toLocalDateTime());

        Timestamp tsFin = rs.getTimestamp("fecha_finalizacion");
        if (tsFin != null)
            t.setFechaFinalizacion(tsFin.toLocalDateTime());

        double lat = rs.getDouble("ubicacion_lat");
        double lon = rs.getDouble("ubicacion_lon");
        if (!rs.wasNull()) {
            t.setUbicacion(new Ubicacion(lat, lon));
        }

        return t;
    }

    // Método auxiliar para cargar relaciones DESPUÉS de haber leído el ResultSet
    // principal
    // para evitar conflictos de "Operation not allowed after ResultSet closed".
    private void cargarRelaciones(Trabajo t) {
        String sql = "SELECT id_cliente, id_operario FROM trabajo WHERE id = ?";
        try (Connection conn = ConexionDB.getConnection(); // Abrimos conexión (o reusamos del pool)
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, t.getId());
            ResultSet rs = stmt.executeQuery();

            // Usamos try-finally para asegurar cierre de RS local
            try {
                if (rs.next()) {
                    int idCliente = rs.getInt("id_cliente");
                    if (idCliente > 0)
                        // Pasamos la conn actual para que NO la cierren
                        t.setCliente(usuarioDAO.obtenerPorId(idCliente, conn));

                    int idOperario = rs.getInt("id_operario");
                    if (idOperario > 0)
                        t.setOperarioAsignado(operarioDAO.obtenerPorId(idOperario, conn));
                }
            } finally {
                if (rs != null)
                    rs.close();
            }

            // Cargar fotos (Podemos dejar que use su propia lógica o actualizar FotoDAO
            // también,
            // pero las fotos suelen ser menos críticas en transacciones anidadas de lectura
            // simple).
            t.setFotos(fotoDAO.obtenerPorTrabajo(t.getId()));

        } catch (SQLException | DataAccessException e) {
            System.err.println("Advertencia: Fallo al cargar dependencias del trabajo " + t.getId());
            e.printStackTrace(); // Ver traza completa si falla
        }
    }
}

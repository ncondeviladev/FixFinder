package com.fixfinder.data.dao;

import com.fixfinder.data.ConexionDB;
import com.fixfinder.data.interfaces.*;
import com.fixfinder.modelos.*;
import com.fixfinder.modelos.componentes.*;
import com.fixfinder.modelos.enums.*;
import com.fixfinder.utilidades.DataAccessException;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO Central para la gestión de incidencias/trabajos.
 * Orquesta la carga de fotos y relaciones.
 */
public class TrabajoDAOImpl implements TrabajoDAO {

    // Solo se mantiene fotoDAO ya que las fotos son 1:N y requieren query separada
    private final FotoTrabajoDAOImpl fotoDAO = new FotoTrabajoDAOImpl();

    // SQL maestra con LEFT JOIN: evita el problema N+1 para cliente y operario
    private static final String SQL_CON_RELACIONES =
        "SELECT t.id AS t_id, t.id_cliente, t.id_operario, t.categoria, t.titulo, t.descripcion, " +
        "  t.direccion, t.ubicacion_lat, t.ubicacion_lon, t.estado, t.fecha_creacion, " +
        "  t.fecha_finalizacion, t.valoracion, t.comentario_cliente, " +
        "  uc.id AS cli_id, uc.nombre_completo AS cli_nombre, uc.email AS cli_email, " +
        "  uc.telefono AS cli_telefono, uc.direccion AS cli_direccion, " +
        "  uc.url_foto AS cli_url_foto, uc.dni AS cli_dni, " +
        "  uo.id AS op_id, uo.nombre_completo AS op_nombre, uo.email AS op_email, " +
        "  uo.telefono AS op_telefono, uo.rol AS op_rol, " +
        "  uo.url_foto AS op_url_foto, uo.dni AS op_dni, " +
        "  op.id_empresa AS op_id_empresa, op.especialidad AS op_especialidad " +
        "FROM trabajo t " +
        "LEFT JOIN usuario uc ON t.id_cliente = uc.id " +
        "LEFT JOIN usuario uo ON t.id_operario = uo.id " +
        "LEFT JOIN operario op ON t.id_operario = op.id_usuario";

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
    public void eliminarPorEmpresa(int idEmpresa) throws DataAccessException {
        // En el esquema actual, el trabajo no tiene id_empresa directo.
        // Lo vinculamos a través del operario asignado.
        String sql = "DELETE FROM trabajo WHERE id_operario IN (SELECT id_usuario FROM operario WHERE id_empresa = ?)";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idEmpresa);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error al eliminar trabajos asociados a la empresa: " + idEmpresa, e);
        }
    }

    @Override
    public Trabajo obtenerPorId(int id) throws DataAccessException {
        String sql = SQL_CON_RELACIONES + " WHERE t.id = ?";
        Trabajo t = null;

        try (Connection conn = ConexionDB.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    t = mapearConRelaciones(rs);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al obtener trabajo ID: " + id, e);
        }

        if (t != null) {
            t.setFotos(fotoDAO.obtenerPorTrabajo(t.getId()));
        }
        return t;
    }

    @Override
    public List<Trabajo> obtenerTodos() throws DataAccessException {
        List<Trabajo> lista = new ArrayList<>();

        try (Connection conn = ConexionDB.getConnection();
                PreparedStatement stmt = conn.prepareStatement(SQL_CON_RELACIONES);
                ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                lista.add(mapearConRelaciones(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al listar trabajos", e);
        }

        // Cargamos las fotos DESPUÉS de cerrar el ResultSet de trabajos
        // para evitar el error de 'ResultSet closed' al reusar la conexión del ThreadLocal
        for (Trabajo t : lista) {
            t.setFotos(fotoDAO.obtenerPorTrabajo(t.getId()));
        }

        return lista;
    }

    /**
     * Obtiene todos los trabajos de un cliente concreto filtrando en SQL.
     * Evita el problema de getCliente()==null por fallos de cargarRelaciones.
     */
    public List<Trabajo> obtenerPorCliente(int idCliente) throws DataAccessException {
        String sql = SQL_CON_RELACIONES + " WHERE t.id_cliente = ?";
        List<Trabajo> lista = new ArrayList<>();

        try (Connection conn = ConexionDB.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idCliente);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearConRelaciones(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al listar trabajos del cliente " + idCliente, e);
        }

        for (Trabajo t : lista) {
            t.setFotos(fotoDAO.obtenerPorTrabajo(t.getId()));
        }

        return lista;
    }

    /**
     * Obtiene solo los trabajos que tienen valoración y pertenecen a la empresa indicada.
     * Optimizado mediante JOIN para evitar filtrado en memoria en el servidor.
     */
    @Override
    public List<Trabajo> obtenerValoracionesPorEmpresa(int idEmpresa) throws DataAccessException {
        String sql = SQL_CON_RELACIONES +
                     " WHERE op.id_empresa = ? AND t.valoracion > 0";
        List<Trabajo> lista = new ArrayList<>();

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idEmpresa);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearConRelaciones(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al obtener valoraciones de la empresa " + idEmpresa, e);
        }

        for (Trabajo t : lista) {
            t.setFotos(fotoDAO.obtenerPorTrabajo(t.getId()));
        }

        return lista;
    }

    /**
     * Obtiene los trabajos pendientes de una categoría específica.
     * Útil para que los operarios vean qué hay disponible.
     */
    @Override
    public List<Trabajo> obtenerPendientesPorCategoria(CategoriaServicio categoria) throws DataAccessException {
        String sql = SQL_CON_RELACIONES +
                     " WHERE (t.estado = 'PENDIENTE' OR t.estado = 'PRESUPUESTADO') AND t.categoria = ?";
        List<Trabajo> lista = new ArrayList<>();

        try (Connection conn = ConexionDB.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, categoria.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearConRelaciones(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al listar trabajos pendientes por categoría", e);
        }

        for (Trabajo t : lista) {
            t.setFotos(fotoDAO.obtenerPorTrabajo(t.getId()));
        }

        return lista;
    }

    /**
     * Mapea un ResultSet del SELECT simple (sin JOINs) en un objeto Trabajo.
     * Usado solo por insertar() y actualizar() donde no necesitamos relaciones.
     */
    private Trabajo mapear(ResultSet rs) throws SQLException {
        Trabajo t = new Trabajo();
        // Con la SQL_CON_RELACIONES usamos alias t_id para evitar ambigüedades
        try {
            t.setId(rs.getInt("t_id"));
        } catch (SQLException e) {
            // Fallback para queries simples sin alias
            t.setId(rs.getInt("id"));
        }
        t.setTitulo(rs.getString("titulo"));
        t.setDescripcion(rs.getString("descripcion"));
        t.setDireccion(rs.getString("direccion"));
        t.setValoracion(rs.getInt("valoracion"));
        t.setComentarioCliente(rs.getString("comentario_cliente"));

        try {
            t.setCategoria(CategoriaServicio.valueOf(rs.getString("categoria")));
        } catch (IllegalArgumentException e) {
            t.setCategoria(CategoriaServicio.OTROS);
        }
        try {
            t.setEstado(EstadoTrabajo.valueOf(rs.getString("estado")));
        } catch (IllegalArgumentException e) {
            t.setEstado(EstadoTrabajo.PENDIENTE);
        }

        Timestamp tsCrea = rs.getTimestamp("fecha_creacion");
        if (tsCrea != null) t.setFechaCreacion(tsCrea.toLocalDateTime());

        Timestamp tsFin = rs.getTimestamp("fecha_finalizacion");
        if (tsFin != null) t.setFechaFinalizacion(tsFin.toLocalDateTime());

        double lat = rs.getDouble("ubicacion_lat");
        double lon = rs.getDouble("ubicacion_lon");
        if (!rs.wasNull()) t.setUbicacion(new Ubicacion(lat, lon));

        return t;
    }

    /**
     * Mapea un ResultSet del SQL_CON_RELACIONES (con LEFT JOINs) en un Trabajo
     * con su Cliente y Operario ya hidratados. Elimina el problema N+1:
     * en lugar de una query extra por cada trabajo, los datos vienen en el JOIN.
     */
    private Trabajo mapearConRelaciones(ResultSet rs) throws SQLException {
        // 1. Mapear los campos del propio trabajo
        Trabajo t = mapear(rs);

        // 2. Hidratar Cliente desde las columnas prefijadas con 'cli_'
        int cliId = rs.getInt("cli_id");
        if (cliId > 0) {
            com.fixfinder.modelos.Cliente cli = new com.fixfinder.modelos.Cliente();
            cli.setId(cliId);
            cli.setNombreCompleto(rs.getString("cli_nombre"));
            cli.setEmail(rs.getString("cli_email"));
            cli.setTelefono(rs.getString("cli_telefono"));
            cli.setDireccion(rs.getString("cli_direccion"));
            cli.setUrlFoto(rs.getString("cli_url_foto"));
            cli.setDni(rs.getString("cli_dni"));
            cli.setRol(com.fixfinder.modelos.enums.Rol.CLIENTE);
            t.setCliente(cli);
        }

        // 3. Hidratar Operario desde las columnas prefijadas con 'op_'
        int opId = rs.getInt("op_id");
        if (opId > 0) {
            Operario op = new Operario();
            op.setId(opId);
            op.setNombreCompleto(rs.getString("op_nombre"));
            op.setEmail(rs.getString("op_email"));
            op.setTelefono(rs.getString("op_telefono"));
            op.setUrlFoto(rs.getString("op_url_foto"));
            op.setDni(rs.getString("op_dni"));
            try {
                op.setRol(com.fixfinder.modelos.enums.Rol.valueOf(rs.getString("op_rol")));
            } catch (IllegalArgumentException e) {
                op.setRol(com.fixfinder.modelos.enums.Rol.OPERARIO);
            }
            op.setIdEmpresa(rs.getInt("op_id_empresa"));
            try {
                op.setEspecialidad(CategoriaServicio.valueOf(rs.getString("op_especialidad")));
            } catch (IllegalArgumentException | NullPointerException e) {
                op.setEspecialidad(CategoriaServicio.OTROS);
            }
            t.setOperarioAsignado(op);
        }

        return t;
    }
}

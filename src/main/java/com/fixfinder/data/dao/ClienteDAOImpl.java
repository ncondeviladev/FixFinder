package com.fixfinder.data.dao;

import com.fixfinder.data.ConexionDB;
import com.fixfinder.data.interfaces.ClienteDAO;
import com.fixfinder.modelos.Cliente;
import com.fixfinder.modelos.Usuario;
import com.fixfinder.modelos.enums.Rol;
import com.fixfinder.utilidades.DataAccessException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ClienteDAOImpl implements ClienteDAO {

    private final UsuarioDAOImpl usuarioDAO;

    public ClienteDAOImpl() {
        this.usuarioDAO = new UsuarioDAOImpl();
    }

    @Override
    public void insertar(Cliente cliente) throws DataAccessException {
        Connection conn = null;
        try {
            conn = ConexionDB.getConnection();
            conn.setAutoCommit(false); // Iniciar transacción

            // 1. Insertar datos base de Usuario
            // Para reutilizar UsuarioDAOImpl, necesitamos un método que acepte conexión
            // o replicar la lógica. UsuarioDAOImpl.insertar no acepta conexión actualmente.
            // Opción: Replicar lógica de inserción de usuario aquí para controlar la
            // transacción.

            String sqlUsuario = "INSERT INTO usuario (email, password_hash, nombre_completo, rol, telefono, direccion, url_foto, dni) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            int idGenerado;

            try (PreparedStatement stmt = conn.prepareStatement(sqlUsuario, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, cliente.getEmail());
                stmt.setString(2, cliente.getPasswordHash());
                stmt.setString(3, cliente.getNombreCompleto());
                stmt.setString(4, cliente.getRol().toString());
                stmt.setString(5, cliente.getTelefono());
                stmt.setString(6, cliente.getDireccion());
                stmt.setString(7, cliente.getUrlFoto());
                stmt.setString(8, cliente.getDni());

                stmt.executeUpdate();

                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        idGenerado = rs.getInt(1);
                        cliente.setId(idGenerado);
                    } else {
                        throw new DataAccessException("No se pudo obtener el ID del usuario insertado.");
                    }
                }
            }

            // 2. Insertar datos específicos de Cliente
            String sqlCliente = "INSERT INTO cliente (id_usuario) VALUES (?)";
            try (PreparedStatement stmt = conn.prepareStatement(sqlCliente)) {
                stmt.setInt(1, idGenerado);
                stmt.executeUpdate();
            }

            conn.commit(); // Confirmar transacción

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            throw new DataAccessException("Error al insertar cliente: " + cliente.getEmail(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void actualizar(Cliente cliente) throws DataAccessException {
        // En este caso, como Cliente no tiene campos propios editables, solo
        // actualizamos Usuario
        usuarioDAO.actualizar(cliente);
    }

    @Override
    public void eliminar(int id) throws DataAccessException {
        // Al borrar usuario, el ON DELETE CASCADE de la BD debería borrar el cliente
        usuarioDAO.eliminar(id);
    }

    @Override
    public Cliente obtenerPorId(int id) throws DataAccessException {
        // Obtenemos el usuario base
        Usuario usuario = usuarioDAO.obtenerPorId(id);
        if (usuario == null)
            return null;

        // Verificamos si existe en la tabla cliente
        String sql = "SELECT 1 FROM cliente WHERE id_usuario = ?";
        try (Connection conn = ConexionDB.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    // Es un cliente, convertimos
                    return mapearCliente(usuario);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al verificar si es cliente ID: " + id, e);
        }

        return null; // Existe usuario pero no es cliente
    }

    @Override
    public List<Cliente> obtenerTodos() throws DataAccessException {
        List<Cliente> clientes = new ArrayList<>();
        // Hacemos JOIN para traer solo los que son clientes
        String sql = "SELECT u.* FROM usuario u JOIN cliente c ON u.id = c.id_usuario";

        try (Connection conn = ConexionDB.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                // Para simplificar, podemos usar el mapeo interno si duplicamos el de
                // UsuarioDAO
                // o instanciar UsuarioDAOImpl y usar un método auxiliar si lo tuviera.
                // Aquí replicaremos mapeo básico por simplicidad y desacople de momento.
                Usuario u = new UsuarioDAOImpl().obtenerPorId(rs.getInt("id"), conn);
                if (u != null) {
                    clientes.add(mapearCliente(u));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al listar clientes", e);
        }
        return clientes;
    }

    private Cliente mapearCliente(Usuario u) {
        Cliente c = new Cliente();
        c.setId(u.getId());
        c.setEmail(u.getEmail());
        c.setPasswordHash(u.getPasswordHash());
        c.setNombreCompleto(u.getNombreCompleto());
        c.setRol(u.getRol());
        c.setTelefono(u.getTelefono());
        c.setDireccion(u.getDireccion());
        c.setUrlFoto(u.getUrlFoto());
        c.setDni(u.getDni());
        return c;
    }
}

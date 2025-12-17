package com.fixfinder.integracion;

import com.fixfinder.data.ConexionDB;
import com.fixfinder.data.DataRepository;
import com.fixfinder.data.DataRepositoryImpl;
import com.fixfinder.data.interfaces.*;
import com.fixfinder.data.interfaces.ClienteDAO;
import com.fixfinder.modelos.*;
import com.fixfinder.modelos.Cliente;
import com.fixfinder.modelos.enums.*;
import com.fixfinder.utilidades.DataAccessException;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Random;

public class TestHelper {

    private final UsuarioDAO usuarioDAO;
    private final OperarioDAO operarioDAO;
    private final EmpresaDAO empresaDAO;
    private final ClienteDAO clienteDAO;
    private final Random random;

    public TestHelper() {
        DataRepository repo = new DataRepositoryImpl();
        this.usuarioDAO = repo.getUsuarioDAO();
        this.operarioDAO = repo.getOperarioDAO();
        this.empresaDAO = repo.getEmpresaDAO();
        this.clienteDAO = repo.getClienteDAO(); // Inicializar
        this.random = new Random();
    }

    // ... (omitiendo metodos intermedios no cambiados) ...

    public void generarClientesSimulados(int cantidad, int idEmpresa) {
        System.out.println("🤖 Generando " + cantidad + " clientes simulados...");
        for (int i = 0; i < cantidad; i++) {
            try {
                Cliente u = new Cliente();
                String id = String.valueOf(System.currentTimeMillis() + i);
                u.setNombreCompleto("Cliente Test " + i);
                u.setEmail("cliente" + id + "@test.com");
                u.setPasswordHash("pass123");
                u.setRol(Rol.CLIENTE);
                u.setDni(id.substring(id.length() - 8) + "C"); // Necesario DNI

                clienteDAO.insertar(u);
            } catch (DataAccessException e) {
                System.err.println("   - Error creando cliente test: " + e.getMessage());
            }
        }
    }

    /**
     * Limpia todas las tablas de la base de datos y resetea los auto-increment.
     */
    public void limpiarBaseDeDatos() {
        System.out.println("[TEST HELPER] Limpiando base de datos...");
        String[] tablas = { "mensaje_chat", "factura", "foto_trabajo", "presupuesto", "trabajo", "cliente", "operario",
                "usuario",
                "empresa_especialidad", "empresa" };

        try (Connection conn = ConexionDB.getConnection()) {
            // Verificar y Crear Tabla Presupuesto si no existe (Hotfix para tests)
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS presupuesto (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY," +
                        "id_trabajo INT NOT NULL," +
                        "id_empresa INT NOT NULL," +
                        "monto DECIMAL(10, 2) NOT NULL," +
                        "estado VARCHAR(50) DEFAULT 'PENDIENTE'," +
                        "fecha_envio TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "FOREIGN KEY (id_trabajo) REFERENCES trabajo (id) ON DELETE CASCADE," +
                        "FOREIGN KEY (id_empresa) REFERENCES empresa (id)" +
                        ")");
            } catch (Exception e) {
                System.out.println(
                        "Nota: No se pudo verificar/crear tabla presupuesto (puede que ya exista): " + e.getMessage());
            }

            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("SET FOREIGN_KEY_CHECKS=0");
                for (String tabla : tablas) {
                    try {
                        System.out.println("   -> Limpiando tabla: " + tabla);
                        stmt.executeUpdate("DELETE FROM " + tabla);
                        stmt.executeUpdate("ALTER TABLE " + tabla + " AUTO_INCREMENT = 1");
                    } catch (Exception e) {
                        System.err.println("   [WARN] No se pudo limpiar tabla '" + tabla + "': " + e.getMessage());
                    }
                }
                stmt.execute("SET FOREIGN_KEY_CHECKS=1");
            }
            conn.commit();
        } catch (Exception e) {
            throw new RuntimeException("Fallo crítico al limpiar BD: " + e.getMessage(), e);
        }
    }

    public Empresa crearEmpresaTest() throws DataAccessException {
        Empresa emp = new Empresa();
        emp.setNombre("FixFinder Test S.L.");
        emp.setCif("B" + System.currentTimeMillis());
        emp.setDireccion("Calle Test 123");
        emp.setTelefono("600000000");
        emp.setEmailContacto("test@fixfinder.com");
        emp.setUrlFoto("http://example.com/logo.png");
        emp.getEspecialidades().add(CategoriaServicio.FONTANERIA);
        emp.getEspecialidades().add(CategoriaServicio.ELECTRICIDAD);
        empresaDAO.insertar(emp);
        return emp;
    }

    public void generarOperariosSimulados(int cantidad, int idEmpresa) {
        System.out.println("👷 Generando " + cantidad + " operarios simulados...");
        CategoriaServicio[] especialidades = CategoriaServicio.values();

        for (int i = 0; i < cantidad; i++) {
            try {
                Operario op = new Operario();
                String id = String.valueOf(System.currentTimeMillis() + i);

                op.setNombreCompleto("Técnico Test " + i);
                op.setEmail("tecnico" + id + "@fixfinder.com");
                op.setPasswordHash("pass123");
                op.setIdEmpresa(idEmpresa);
                op.setRol(Rol.OPERARIO);

                op.setDni(id.substring(id.length() - 8) + "X");
                op.setEspecialidad(especialidades[random.nextInt(especialidades.length)]);
                op.setEstaActivo(true);
                op.setLatitud(39.46);
                op.setLongitud(-0.37);

                operarioDAO.insertar(op);
            } catch (DataAccessException e) {
                System.err.println("   - Error creando operario test: " + e.getMessage());
            }
        }
    }
}

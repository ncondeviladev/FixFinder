package test.java.com.fixfinder.integracion;

import com.fixfinder.data.ConexionDB;
import com.fixfinder.data.DataRepository;
import com.fixfinder.data.DataRepositoryImpl;
import com.fixfinder.data.interfaces.*;
import com.fixfinder.modelos.*;
import com.fixfinder.modelos.enums.*;
import com.fixfinder.utilidades.DataAccessException;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Random;

public class TestHelper {

    private final UsuarioDAO usuarioDAO;
    private final OperarioDAO operarioDAO;
    private final EmpresaDAO empresaDAO;
    private final Random random;

    public TestHelper() {
        DataRepository repo = new DataRepositoryImpl();
        this.usuarioDAO = repo.getUsuarioDAO();
        this.operarioDAO = repo.getOperarioDAO();
        this.empresaDAO = repo.getEmpresaDAO();
        this.random = new Random();
    }

    /**
     * Limpia todas las tablas de la base de datos y resetea los auto-increment.
     */
    public void limpiarBaseDeDatos() {
        System.out.println("[TEST HELPER] Limpiando base de datos...");
        String[] tablas = { "mensaje_chat", "factura", "foto_trabajo", "trabajo", "operario", "usuario",
                "empresa_especialidad", "empresa" };

        try (Connection conn = ConexionDB.getConnection()) {
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("SET FOREIGN_KEY_CHECKS=0");
                for (String tabla : tablas) {
                    try {
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
        emp.getEspecialidades().add(CategoriaServicio.FONTANERIA);
        emp.getEspecialidades().add(CategoriaServicio.ELECTRICIDAD);
        empresaDAO.insertar(emp);
        return emp;
    }

    public void generarClientesSimulados(int cantidad, int idEmpresa) {
        System.out.println("🤖 Generando " + cantidad + " clientes simulados...");
        for (int i = 0; i < cantidad; i++) {
            try {
                Usuario u = new Usuario();
                String id = String.valueOf(System.currentTimeMillis() + i);
                u.setNombreCompleto("Cliente Test " + i);
                u.setEmail("cliente" + id + "@test.com");
                u.setPasswordHash("pass123");
                u.setRol(Rol.CLIENTE);
                u.setIdEmpresa(idEmpresa);

                usuarioDAO.insertar(u);
            } catch (DataAccessException e) {
                System.err.println("   - Error creando cliente test: " + e.getMessage());
            }
        }
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

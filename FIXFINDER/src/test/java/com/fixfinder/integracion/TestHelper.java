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
import com.fixfinder.utilidades.GestorPassword;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Random;

public class TestHelper {

    private final UsuarioDAO usuarioDAO;
    private final OperarioDAO operarioDAO;
    private final EmpresaDAO empresaDAO;
    private final ClienteDAO clienteDAO;
    private final TrabajoDAO trabajoDAO;
    private final Random random;

    public TestHelper() {
        DataRepository repo = new DataRepositoryImpl();
        this.usuarioDAO = repo.getUsuarioDAO();
        this.operarioDAO = repo.getOperarioDAO();
        this.empresaDAO = repo.getEmpresaDAO();
        this.clienteDAO = repo.getClienteDAO(); // Inicializar
        this.trabajoDAO = repo.getTrabajoDAO();
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
     * Ahora también carga los datos de integración automáticamente.
     */
    public void limpiarBaseDeDatos() {
        System.out.println("[TEST HELPER] Limpiando base de datos...");
        String[] tablas = { "mensaje_chat", "factura", "foto_trabajo", "presupuesto", "trabajo", "cliente", "operario",
                "usuario", "empresa_especialidad", "empresa" };

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
            System.out.println("🧹 Base de datos limpia.");

            // CARGAR DATOS DE INTEGRACIÓN AUTOMÁTICAMENTE
            cargarDatosIntegracion();

        } catch (Exception e) {
            throw new RuntimeException("Fallo crítico al limpiar/poblar BD: " + e.getMessage(), e);
        }
    }

    /**
     * Carga el set de datos de integración simplificado.
     */
    public void cargarDatosIntegracion() {
        System.out.println("🚀 Cargando datos de INTEGRACIÓN (Escenario Simplificado)...");
        try {
            // -------------------------------------------------------------------
            // 1. EMPRESAS
            // -------------------------------------------------------------------
            Empresa empA = new Empresa();
            empA.setNombre("Servicios Técnicos Levante");
            empA.setCif("B12345678");
            empA.setDireccion("Av. del Cid 45, Valencia");
            empA.setTelefono("960000001");
            empA.setEmailContacto("contacto@levante.com");
            empA.getEspecialidades().add(CategoriaServicio.OTROS);
            empresaDAO.insertar(empA);

            Empresa empB = new Empresa();
            empB.setNombre("Reformas Express S.L.");
            empB.setCif("B87654321");
            empB.setDireccion("Calle Colón 10, Valencia");
            empB.setTelefono("960000002");
            empB.setEmailContacto("contacto@express.com");
            empB.getEspecialidades().add(CategoriaServicio.OTROS);
            empresaDAO.insertar(empB);

            // -------------------------------------------------------------------
            // 2. OPERARIOS (4 total: 2 gerentes, 2 técnicos)
            // -------------------------------------------------------------------
            // Empresa A
            Operario gerA = new Operario();
            gerA.setNombreCompleto("Carlos Gerente A");
            gerA.setEmail("gerente.a@levante.com");
            gerA.setPasswordHash(GestorPassword.hashearPassword("1234"));
            gerA.setRol(Rol.GERENTE);
            gerA.setIdEmpresa(empA.getId());
            gerA.setEspecialidad(CategoriaServicio.ELECTRICIDAD);
            gerA.setDni("11111111G");
            operarioDAO.insertar(gerA);

            Operario tecA = new Operario();
            tecA.setNombreCompleto("Paco Operario A");
            tecA.setEmail("paco@levante.com");
            tecA.setPasswordHash(GestorPassword.hashearPassword("1234"));
            tecA.setRol(Rol.OPERARIO);
            tecA.setIdEmpresa(empA.getId());
            tecA.setEspecialidad(CategoriaServicio.FONTANERIA);
            tecA.setDni("22222222O");
            tecA.setEstaActivo(true);
            operarioDAO.insertar(tecA);

            // Empresa B
            Operario gerB = new Operario();
            gerB.setNombreCompleto("Manolo Gerente B");
            gerB.setEmail("gerente.b@express.com");
            gerB.setPasswordHash(GestorPassword.hashearPassword("1234"));
            gerB.setRol(Rol.GERENTE);
            gerB.setIdEmpresa(empB.getId());
            gerB.setEspecialidad(CategoriaServicio.OTROS);
            gerB.setDni("33333333G");
            operarioDAO.insertar(gerB);

            Operario tecB = new Operario();
            tecB.setNombreCompleto("Benito Operario B");
            tecB.setEmail("benito@express.com");
            tecB.setPasswordHash(GestorPassword.hashearPassword("1234"));
            tecB.setRol(Rol.OPERARIO);
            tecB.setIdEmpresa(empB.getId());
            tecB.setEspecialidad(CategoriaServicio.OTROS);
            tecB.setDni("44444444O");
            tecB.setEstaActivo(true);
            operarioDAO.insertar(tecB);

            // -------------------------------------------------------------------
            // 3. CLIENTES (2 total)
            // -------------------------------------------------------------------
            Cliente c1 = new Cliente();
            c1.setNombreCompleto("Marta Cliente");
            c1.setEmail("marta@gmail.com");
            c1.setPasswordHash(GestorPassword.hashearPassword("1234"));
            c1.setRol(Rol.CLIENTE);
            c1.setDni("55555555M");
            clienteDAO.insertar(c1);

            Cliente c2 = new Cliente();
            c2.setNombreCompleto("Juan Cliente");
            c2.setEmail("juan@hotmail.com");
            c2.setPasswordHash(GestorPassword.hashearPassword("1234"));
            c2.setRol(Rol.CLIENTE);
            c2.setDni("66666666J");
            clienteDAO.insertar(c2);

            // -------------------------------------------------------------------
            // 4. TRABAJOS (Incidencias: 2 para c1, 3 para c2)
            // -------------------------------------------------------------------
            // Marta (2 trabajos)
            crearTrabajoDirecto(c1, "Fuga en fregadero", "Gotea mucho en la cocina.",
                    CategoriaServicio.FONTANERIA, "Calle Paz 1", EstadoTrabajo.PENDIENTE);
            crearTrabajoDirecto(c1, "Revisión caldera", "Hacer mantenimiento anual.",
                    CategoriaServicio.OTROS, "Calle Paz 1", EstadoTrabajo.PENDIENTE);

            // Juan (3 trabajos)
            crearTrabajoDirecto(c2, "Enchufe suelto", "Peligro en habitación niños.",
                    CategoriaServicio.ELECTRICIDAD, "Av. Puerto 10", EstadoTrabajo.PENDIENTE);
            crearTrabajoDirecto(c2, "Persiana rota", "No sube ni baja.",
                    CategoriaServicio.OTROS, "Av. Puerto 10", EstadoTrabajo.PENDIENTE);
            crearTrabajoDirecto(c2, "Grifo que gotea", "Pierde agua en el baño.",
                    CategoriaServicio.FONTANERIA, "Av. Puerto 10", EstadoTrabajo.PENDIENTE);

            System.out.println("✅ Datos de integración simplificados cargados (Contraseña: 1234).");

        } catch (Exception e) {
            System.err.println("❌ Error cargando datos: " + e.getMessage());
        }
    }

    private void crearTrabajoDirecto(Usuario cliente, String titulo, String desc, CategoriaServicio cat, String dir,
            EstadoTrabajo est) throws DataAccessException {
        Trabajo t = new Trabajo();
        t.setCliente(cliente);
        t.setTitulo(titulo);
        t.setDescripcion(desc);
        t.setCategoria(cat);
        t.setDireccion(dir);
        t.setEstado(est);
        t.setFechaCreacion(java.time.LocalDateTime.now());
        trabajoDAO.insertar(t);
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

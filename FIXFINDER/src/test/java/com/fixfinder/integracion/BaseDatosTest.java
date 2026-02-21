package com.fixfinder.integracion;

import com.fixfinder.data.DataRepository;
import com.fixfinder.data.DataRepositoryImpl;
import com.fixfinder.data.interfaces.TrabajoDAO;
import com.fixfinder.modelos.*;
import com.fixfinder.modelos.enums.*;
import com.fixfinder.utilidades.DataAccessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BaseDatosTest {

    private TestHelper helper;
    private DataRepository repo;

    @BeforeEach
    void setUp() {
        helper = new TestHelper();
        repo = new DataRepositoryImpl();
        // Limpiamos la BD antes de cada test para asegurar un estado limpio
        helper.limpiarBaseDeDatos();
    }

    @Test
    @DisplayName("Flujo Completo: Crear Empresa, Operario, Cliente y Ciclo de Trabajo")
    void testFlujoCompleto() {
        try {
            // 1. Crear EMPRESA
            Empresa empresa = helper.crearEmpresaTest();
            assertNotNull(empresa.getId(), "La empresa debería tener un ID asignado tras la inserción");
            assertTrue(empresa.getId() > 0, "El ID de la empresa debe ser mayor que 0");
            assertEquals("http://example.com/logo.png", empresa.getUrlFoto(),
                    "La URL de la foto de la empresa debería coincidir");

            // 2. Crear OPERARIO
            Operario op = new Operario();
            op.setIdEmpresa(empresa.getId());
            op.setEmail("operario.test@fixfinder.com");
            op.setPasswordHash("pass123");
            op.setNombreCompleto("Juan Técnico Test");
            op.setRol(Rol.OPERARIO);
            op.setDni("12345678Z");
            op.setEspecialidad(CategoriaServicio.FONTANERIA);
            op.setEstaActivo(true);
            op.setLatitud(39.0);
            op.setLongitud(-0.3);

            repo.getOperarioDAO().insertar(op);
            assertNotNull(op.getId(), "El operario debería tener ID");

            // 3. Crear CLIENTE
            Cliente cliente = new Cliente();
            cliente.setEmail("cliente.test@gmail.com");
            cliente.setPasswordHash("pass123");
            cliente.setNombreCompleto("María Cliente Test");
            cliente.setRol(Rol.CLIENTE);
            cliente.setTelefono("611223344");
            cliente.setDireccion("Calle Cliente 1");
            cliente.setUrlFoto("http://example.com/cliente.jpg");
            cliente.setDni("87654321X"); // DNI es obligatorio ahora

            repo.getClienteDAO().insertar(cliente);
            assertNotNull(cliente.getId(), "El cliente debería tener ID");

            Usuario clienteLeido = repo.getUsuarioDAO().obtenerPorId(cliente.getId());
            assertEquals("611223344", clienteLeido.getTelefono());
            assertEquals("Calle Cliente 1", clienteLeido.getDireccion());
            assertEquals("http://example.com/cliente.jpg", clienteLeido.getUrlFoto());

            // 4. Crear TRABAJO
            Trabajo trabajo = new Trabajo();
            trabajo.setCliente(cliente);
            trabajo.setCategoria(CategoriaServicio.FONTANERIA);
            trabajo.setTitulo("Fuga Urgente");
            trabajo.setDescripcion("Fuga de agua en cocina");
            trabajo.setDireccion("Calle Falsa 123");
            trabajo.setEstado(EstadoTrabajo.PENDIENTE);

            TrabajoDAO trabajoDAO = repo.getTrabajoDAO();
            trabajoDAO.insertar(trabajo);
            assertNotNull(trabajo.getId(), "El trabajo debería tener ID");
            assertEquals(EstadoTrabajo.PENDIENTE, trabajo.getEstado());

            // 5. Asignar Operario
            trabajo.setOperarioAsignado(op);
            trabajo.setEstado(EstadoTrabajo.ASIGNADO);
            trabajoDAO.actualizar(trabajo);

            // Verificar actualización leyendo de BD
            Trabajo trabajoLeido = trabajoDAO.obtenerPorId(trabajo.getId());
            assertNotNull(trabajoLeido.getOperarioAsignado(), "El operario debería estar asignado en la BD");
            assertEquals(op.getId(), trabajoLeido.getOperarioAsignado().getId());
            assertEquals(EstadoTrabajo.ASIGNADO, trabajoLeido.getEstado());

            // 6. Finalizar Trabajo
            trabajo.setEstado(EstadoTrabajo.FINALIZADO);
            trabajo.setValoracion(5);
            trabajo.setComentarioCliente("Excelente trabajo");
            trabajoDAO.actualizar(trabajo);

            Trabajo trabajoFinalizado = trabajoDAO.obtenerPorId(trabajo.getId());
            assertEquals(EstadoTrabajo.FINALIZADO, trabajoFinalizado.getEstado());
            assertEquals(Integer.valueOf(5), trabajoFinalizado.getValoracion());

        } catch (Exception e) {
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            System.out.println("EXCEPTION IN testFlujoCompleto:");
            e.printStackTrace(System.out);

            // DUMP TABLE STATE
            try (java.sql.Connection conn = com.fixfinder.data.ConexionDB.getConnection();
                    java.sql.Statement stmt = conn.createStatement()) {

                System.out.println("--- DUMP TABLA USUARIO ---");
                try (java.sql.ResultSet rs = stmt.executeQuery("SELECT * FROM usuario")) {
                    while (rs.next())
                        System.out.println("ID: " + rs.getInt("id") + " Email: " + rs.getString("email"));
                }

                System.out.println("--- DUMP TABLA CLIENTE ---");
                try (java.sql.ResultSet rs = stmt.executeQuery("SELECT * FROM cliente")) {
                    while (rs.next())
                        System.out.println("ID_USUARIO: " + rs.getInt("id_usuario"));
                }

                System.out.println("--- DUMP TABLA OPERARIO ---");
                try (java.sql.ResultSet rs = stmt.executeQuery("SELECT * FROM operario")) {
                    while (rs.next())
                        System.out.println("ID_USUARIO: " + rs.getInt("id_usuario"));
                }

            } catch (Exception ex) {
                System.out.println("Error dumping DB: " + ex.getMessage());
            }

            if (e.getCause() != null) {
                System.out.println("CAUSE: " + e.getCause());
            }
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Simulación de Datos Masiva")
    void testSimulacionMasiva() throws DataAccessException {
        Empresa empresa = helper.crearEmpresaTest();

        assertDoesNotThrow(() -> helper.generarClientesSimulados(5, empresa.getId()));
        assertDoesNotThrow(() -> helper.generarOperariosSimulados(3, empresa.getId()));
    }
}

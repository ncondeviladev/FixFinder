package com.fixfinder.integracion;

import com.fixfinder.data.DataRepository;
import com.fixfinder.data.DataRepositoryImpl;
import com.fixfinder.data.interfaces.*;
import com.fixfinder.modelos.*;
import com.fixfinder.modelos.enums.*;
import com.fixfinder.utilidades.DataAccessException;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integración para la capa de persistencia (Base de Datos).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BaseDatosTest {

    private TestHelper helper;
    private EmpresaDAO empresaDAO;
    private OperarioDAO operarioDAO;
    private ClienteDAO clienteDAO;
    private TrabajoDAO trabajoDAO;

    @BeforeAll
    void setup() {
        helper = new TestHelper();
        DataRepository repo = new DataRepositoryImpl();
        empresaDAO = repo.getEmpresaDAO();
        operarioDAO = repo.getOperarioDAO();
        clienteDAO = repo.getClienteDAO();
        trabajoDAO = repo.getTrabajoDAO();
    }

    @Test
    @DisplayName("Flujo Completo: Crear Empresa, Operario, Cliente y Ciclo de Trabajo")
    void testFlujoCompleto() throws DataAccessException {
        System.out.println("🚀 Iniciando Test de Flujo Completo...");
        Empresa empresa = helper.crearEmpresaTest();
        Cliente cliente = null;

        try {
            // 2. Crear OPERARIO
            Operario op = new Operario();
            op.setIdEmpresa(empresa.getId());
            op.setEmail(helper.generarEmailUnico());
            op.setPasswordHash("pass123");
            op.setNombreCompleto("Juan Técnico Test");
            op.setRol(Rol.OPERARIO);
            op.setDni(helper.generarDniUnico());
            op.setEspecialidad(CategoriaServicio.FONTANERIA);
            operarioDAO.insertar(op);
            helper.registrarUsuario(op.getId());
            System.out.println("✅ Operario creado: " + op.getEmail());

            // 3. Crear CLIENTE
            cliente = new Cliente();
            cliente.setEmail(helper.generarEmailUnico());
            cliente.setPasswordHash("pass123");
            cliente.setNombreCompleto("María Cliente Test");
            cliente.setRol(Rol.CLIENTE);
            cliente.setDni(helper.generarDniUnico());
            clienteDAO.insertar(cliente);
            helper.registrarUsuario(cliente.getId());
            System.out.println("✅ Cliente creado: " + cliente.getEmail());

            // 4. Crear TRABAJO
            Trabajo trabajo = new Trabajo();
            trabajo.setCliente(cliente);
            trabajo.setTitulo("Fuga de prueba");
            trabajo.setDescripcion("Descripción de prueba");
            trabajo.setCategoria(CategoriaServicio.FONTANERIA);
            trabajo.setDireccion("Calle Test 123");
            trabajo.setEstado(EstadoTrabajo.PENDIENTE);
            trabajoDAO.insertar(trabajo);
            helper.registrarTrabajo(trabajo.getId());

            // 5. Flujo: ASIGNAR
            trabajo.setOperarioAsignado(op);
            trabajo.setEstado(EstadoTrabajo.ASIGNADO);
            trabajoDAO.actualizar(trabajo);


            // 6. FINALIZAR y VALORAR
            trabajo.setEstado(EstadoTrabajo.FINALIZADO);
            trabajo.setValoracion(5);
            trabajo.setComentarioCliente("Excelente trabajo");
            trabajoDAO.actualizar(trabajo);

            Trabajo trabajoFinalizado = trabajoDAO.obtenerPorId(trabajo.getId());
            assertEquals(EstadoTrabajo.FINALIZADO, trabajoFinalizado.getEstado());
            assertEquals(Integer.valueOf(5), trabajoFinalizado.getValoracion());
            System.out.println("🏁 Test finalizado correctamente.");
        } finally {
            helper.limpiarTodoLoGenerado();
        }
    }

    @Test
    @DisplayName("Simulación de Datos Masiva")
    void testSimulacionMasiva() throws DataAccessException {
        System.out.println("📊 Iniciando Test de Simulación Masiva...");
        Empresa empresaSim = helper.crearEmpresaTest();

        try {
            helper.generarOperariosSimulados(5, empresaSim.getId());
            helper.generarClientesSimulados(10, empresaSim.getId());

            List<Operario> ops = (List<Operario>) operarioDAO.obtenerPorEmpresa(empresaSim.getId());
            assertEquals(5, ops.size(), "Deberían haberse creado 5 operarios");
            System.out.println("✅ Simulación masiva verificada.");
        } finally {
            helper.limpiarTodoLoGenerado();
        }
    }
}

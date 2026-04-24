package com.fixfinder.integracion;

import com.fixfinder.data.DataRepository;
import com.fixfinder.data.DataRepositoryImpl;
import com.fixfinder.data.interfaces.PresupuestoDAO;
import com.fixfinder.data.interfaces.TrabajoDAO;
import com.fixfinder.modelos.Cliente;
import com.fixfinder.modelos.Empresa;
import com.fixfinder.modelos.Presupuesto;
import com.fixfinder.modelos.Trabajo;
import com.fixfinder.modelos.enums.EstadoPresupuesto;
import com.fixfinder.modelos.enums.EstadoTrabajo;
import com.fixfinder.modelos.enums.Rol;
import com.fixfinder.service.interfaz.PresupuestoService;
import com.fixfinder.service.impl.PresupuestoServiceImpl;
import com.fixfinder.utilidades.DataAccessException;
import com.fixfinder.utilidades.ServiceException;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test de Integración de Licitación Múltiple.
 * 
 * Este test valida uno de los procesos de negocio más críticos de FixFinder:
 * la capacidad de que múltiples empresas compitan por un mismo trabajo y que,
 * al aceptar una propuesta, el sistema gestione automáticamente el rechazo
 * de las demás para mantener la coherencia del estado del trabajo.
 * 
 * Escenario:
 * 1. Existencia de una incidencia PENDIENTE.
 * 2. Recepción de dos presupuestos técnicos diferentes.
 * 3. Aceptación de uno de ellos por parte del usuario.
 * 4. Verificación de que el trabajo cambia de estado y se actualiza la
 * documentación técnica.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MultiPresupuestoIntegracionTest {

    private TestHelper helper;
    private PresupuestoService presupuestoService;
    private DataRepository repo;
    private TrabajoDAO trabajoDAO;
    private PresupuestoDAO presupuestoDAO;

    @BeforeAll
    void setup() {
        helper = new TestHelper();
        repo = new DataRepositoryImpl();
        trabajoDAO = repo.getTrabajoDAO();
        presupuestoDAO = repo.getPresupuestoDAO();
        presupuestoService = new PresupuestoServiceImpl();
    }

    @BeforeEach
    void limpiar() {
        // No limpiamos toda la base de datos para respetar datos existentes del usuario
    }

    @Test
    @DisplayName("Escenario de Multipresupuesto: Licitación, Aceptación y Rechazo Automático")
    void testFlujoMultiPresupuesto() throws DataAccessException, ServiceException {
        System.out.println("⚖️ Iniciando Test de Multipresupuesto...");
        
        // 1. Crear entorno propio (Empresas de prueba)
        Empresa masterEmpresa = helper.crearEmpresaTest();
        Empresa empA = helper.crearEmpresaTest();
        Empresa empB = helper.crearEmpresaTest();
        Cliente cliente = null;

        try {
            cliente = new Cliente();
            cliente.setEmail(helper.generarEmailUnico());
            cliente.setPasswordHash("pass123");
            cliente.setNombreCompleto("Cliente Multipresupuesto");
            cliente.setRol(Rol.CLIENTE);
            cliente.setDni(helper.generarDniUnico());
            repo.getClienteDAO().insertar(cliente);
            helper.registrarUsuario(cliente.getId());

            Trabajo trabajo = new Trabajo();
            trabajo.setCliente(cliente);
            trabajo.setTitulo("Falla Eléctrica Multipresupuesto");
            trabajo.setDescripcion("Necesito varios presupuestos para comparar.");
            trabajo.setCategoria(com.fixfinder.modelos.enums.CategoriaServicio.ELECTRICIDAD);
            trabajo.setDireccion("Calle Test 456");
            trabajo.setEstado(EstadoTrabajo.PENDIENTE);
            trabajoDAO.insertar(trabajo);
            helper.registrarTrabajo(trabajo.getId());

            String descOriginal = trabajo.getDescripcion();

            // 2. Empresa A envía presupuesto
            Presupuesto preA = new Presupuesto();
            preA.setTrabajo(trabajo);
            preA.setEmpresa(empA);
            preA.setMonto(150.0);
            preA.setNotas("Propuesta A: Usaremos materiales premium.");
            preA.setEstado(EstadoPresupuesto.PENDIENTE);
            presupuestoDAO.insertar(preA);
            helper.registrarPresupuesto(preA.getId());

            // 3. Empresa B envía presupuesto
            Presupuesto preB = new Presupuesto();
            preB.setTrabajo(trabajo);
            preB.setEmpresa(empB);
            preB.setMonto(120.0);
            preB.setNotas("Propuesta B: Opción económica y rápida.");
            preB.setEstado(EstadoPresupuesto.PENDIENTE);
            presupuestoDAO.insertar(preB);
            helper.registrarPresupuesto(preB.getId());


            // 4. El cliente acepta el presupuesto de la Empresa B
            System.out.println("   -> Aceptando presupuesto de Empresa B (120€)...");
            presupuestoService.aceptarPresupuesto(preB.getId());

            // 5. VALIDACIONES POST-ACEPTACIÓN
            Presupuesto bFinal = presupuestoDAO.obtenerPorId(preB.getId());
            assertEquals(EstadoPresupuesto.ACEPTADO, bFinal.getEstado());

            Presupuesto aFinal = presupuestoDAO.obtenerPorId(preA.getId());
            assertEquals(EstadoPresupuesto.RECHAZADO, aFinal.getEstado(), 
                    "El otro presupuesto debería haberse rechazado automáticamente");

            Trabajo tFinal = trabajoDAO.obtenerPorId(trabajo.getId());
            assertEquals(EstadoTrabajo.ACEPTADO, tFinal.getEstado());

            // Verificamos que la propuesta técnica se ha añadido a la descripción
            assertTrue(tFinal.getDescripcion().contains(preB.getNotas()), 
                    "La descripción debería incluir la propuesta técnica aceptada");
            assertTrue(tFinal.getDescripcion().contains(descOriginal), 
                    "La descripción original del cliente no debe perderse");

            System.out.println("✅ Flujo de multipresupuesto verificado correctamente.");

        } finally {
            helper.limpiarTodoLoGenerado();
        }
    }
}

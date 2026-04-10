package com.fixfinder.integracion;

import com.fixfinder.DbClean;
import com.fixfinder.data.DataRepository;
import com.fixfinder.data.DataRepositoryImpl;
import com.fixfinder.data.interfaces.PresupuestoDAO;
import com.fixfinder.data.interfaces.TrabajoDAO;
import com.fixfinder.modelos.Empresa;
import com.fixfinder.modelos.Presupuesto;
import com.fixfinder.modelos.Trabajo;
import com.fixfinder.modelos.enums.EstadoPresupuesto;
import com.fixfinder.modelos.enums.EstadoTrabajo;
import com.fixfinder.service.interfaz.PresupuestoService;
import com.fixfinder.service.impl.PresupuestoServiceImpl;
import com.fixfinder.utilidades.DataAccessException;
import com.fixfinder.utilidades.ServiceException;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MultiPresupuestoIntegracionTest {

    private TestHelper helper;
    private PresupuestoService presupuestoService;
    private TrabajoDAO trabajoDAO;
    private PresupuestoDAO presupuestoDAO;

    @BeforeAll
    void setup() {
        helper = new TestHelper();
        DataRepository repo = new DataRepositoryImpl();
        trabajoDAO = repo.getTrabajoDAO();
        presupuestoDAO = repo.getPresupuestoDAO();
        presupuestoService = new PresupuestoServiceImpl();
    }

    @AfterAll
    static void restaurarBaseDeDatos() {
        System.out.println("♻️ Restaurando base de datos base (Seeder)...");
        try {
            DbClean.main(new String[0]);
            System.out.println("✅ Base de datos restaurada correctamente.");
        } catch (Exception e) {
            System.err.println("❌ Error restaurando base de datos: " + e.getMessage());
        }
    }

    @BeforeEach
    void limpiar() {
        helper.limpiarBaseDeDatos();
    }

    @Test
    @DisplayName("Escenario de Multipresupuesto: Licitación, Aceptación y Rechazo Automático")
    void testFlujoMultiPresupuesto() throws DataAccessException, ServiceException {
        // 1. Obtener una incidencia pendiente (cargada por
        // helper.cargarDatosIntegracion)
        List<Trabajo> trabajos = trabajoDAO.obtenerTodos();
        Trabajo trabajo = trabajos.stream()
                .filter(t -> t.getEstado() == EstadoTrabajo.PENDIENTE)
                .findFirst()
                .orElseThrow();

        String descOriginal = trabajo.getDescripcion();
        System.out.println("📝 Incidencia original: " + trabajo.getTitulo());

        // 2. Empresa A envía presupuesto
        Empresa empA = new Empresa();
        empA.setId(1);

        Presupuesto preA = new Presupuesto();
        preA.setTrabajo(trabajo);
        preA.setEmpresa(empA);
        preA.setMonto(150.0);
        preA.setNotas("Propuesta A: Usaremos materiales premium.");
        preA.setEstado(EstadoPresupuesto.PENDIENTE);
        presupuestoDAO.insertar(preA);

        // 3. Empresa B envía presupuesto
        Empresa empB = new Empresa();
        empB.setId(2);

        Presupuesto preB = new Presupuesto();
        preB.setTrabajo(trabajo);
        preB.setEmpresa(empB);
        preB.setMonto(120.0);
        preB.setNotas("Propuesta B: Opción económica y rápida.");
        preB.setEstado(EstadoPresupuesto.PENDIENTE);
        presupuestoDAO.insertar(preB);

        // Verificar que hay 2 presupuestos
        List<Presupuesto> listado = presupuestoDAO.obtenerPorTrabajo(trabajo.getId());
        assertEquals(2, listado.size(), "Debería haber 2 presupuestos registrados");

        // 4. El cliente acepta el presupuesto de la Empresa B (el más barato)
        System.out.println("⚖️ Aceptando presupuesto de Empresa B...");
        presupuestoService.aceptarPresupuesto(preB.getId());

        // 5. VALIDACIONES POST-ACEPTACIÓN

        // A) El presupuesto B debe estar ACEPTADO
        Presupuesto bFinal = presupuestoDAO.obtenerPorId(preB.getId());
        assertEquals(EstadoPresupuesto.ACEPTADO, bFinal.getEstado());

        // B) El presupuesto A debe haber sido RECHAZADO automáticamente por el trigger
        // de servicio
        Presupuesto aFinal = presupuestoDAO.obtenerPorId(preA.getId());
        assertEquals(EstadoPresupuesto.RECHAZADO, aFinal.getEstado(),
                "El otro presupuesto debería haberse rechazado automáticamente");

        // C) El trabajo debe estar en estado ACEPTADO
        Trabajo tFinal = trabajoDAO.obtenerPorId(trabajo.getId());
        assertEquals(EstadoTrabajo.ACEPTADO, tFinal.getEstado());

        // D) CRÍTICO: La descripción del trabajo debe contener las notas del gerente
        assertTrue(tFinal.getDescripcion().contains(preB.getNotas()),
                "La descripción debería incluir la propuesta técnica aceptada");
        assertTrue(tFinal.getDescripcion().contains(descOriginal),
                "La descripción original del cliente no debe perderse");

        System.out.println("✅ Test de multipresupuesto completado con éxito.");
        System.out.println("📄 Descripción final: " + tFinal.getDescripcion());
    }
}

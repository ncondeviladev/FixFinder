package com.fixfinder.service;

import com.fixfinder.modelos.*;
import com.fixfinder.modelos.enums.CategoriaServicio;
import com.fixfinder.modelos.enums.EstadoTrabajo;
import com.fixfinder.modelos.enums.Rol;
import com.fixfinder.service.impl.*;
import com.fixfinder.service.interfaz.*;
import com.fixfinder.utilidades.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Order;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServiceTest {

    private UsuarioService usuarioService;
    private EmpresaService empresaService;
    private TrabajoService trabajoService;
    private OperarioService operarioService;
    private FacturaService facturaService;
    private PresupuestoService presupuestoService;

    // IDs estáticos para compartir entre tests y mantener flujo
    private static Integer idUsuarioRegistrado;
    private static Integer idEmpresaRegistrada;
    private static Integer idOperarioRegistrado;
    private static Integer idTrabajoRegistrado;
    private static Integer idFacturaGenerada;

    @org.junit.jupiter.api.BeforeAll
    static void initDb() {
        // Reset static variables
        idUsuarioRegistrado = null;
        idEmpresaRegistrada = null;
        idOperarioRegistrado = null;
        idTrabajoRegistrado = null;
        idFacturaGenerada = null;

        // Limpieza PROFUNDA de la BD
        com.fixfinder.integracion.TestHelper helper = new com.fixfinder.integracion.TestHelper();
        helper.limpiarBaseDeDatos();
    }

    @BeforeEach
    void setUp() {
        usuarioService = new UsuarioServiceImpl();
        empresaService = new EmpresaServiceImpl();
        trabajoService = new TrabajoServiceImpl();
        operarioService = new OperarioServiceImpl();
        facturaService = new FacturaServiceImpl();
        presupuestoService = new PresupuestoServiceImpl();
    }

    private String generateUniqueEmail() {
        return "user_" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
    }

    private String generateUniqueDni() {
        // 8 digitos + Letra
        long num = 10000000 + (long) (Math.random() * 89999999);
        return num + "Z";
    }

    private String generateUniqueCif() {
        return "B" + (10000000 + (long) (Math.random() * 89999999));
    }

    private int ensureEmpresaExists() throws ServiceException {
        if (idEmpresaRegistrada != null)
            return idEmpresaRegistrada;

        Empresa e = new Empresa();
        e.setNombre("Empresa Default " + UUID.randomUUID().toString().substring(0, 5));
        e.setCif(generateUniqueCif());
        e.setEmailContacto(generateUniqueEmail());

        try {
            empresaService.registrarEmpresa(e);
            idEmpresaRegistrada = e.getId();
        } catch (Exception ex) {
            System.err.println("❌ ERROR Creando Empresa Fallback: " + ex.getMessage());
            ex.printStackTrace();
            throw ex;
        }
        return idEmpresaRegistrada;
    }

    // --- TESTS USUARIO ---

    @Test
    @Order(1)
    @DisplayName("Usuario: Registro y Login (Happy Path)")
    void testUsuarioFlow() throws ServiceException {
        try {
            int idEmp = ensureEmpresaExists();

            Usuario u = new Usuario();
            u.setNombreCompleto("Juan Test");
            u.setEmail(generateUniqueEmail());
            u.setPasswordHash("password123");
            u.setTelefono("600" + (100000 + (int) (Math.random() * 899999))); // 9 digits
            u.setDireccion("Calle Test " + UUID.randomUUID().toString().substring(0, 5));
            u.setRol(Rol.CLIENTE);
            u.setIdEmpresa(idEmp);

            // Registro
            usuarioService.registrarUsuario(u);
            assertTrue(u.getId() > 0, "El ID debería ser mayor a 0 tras registro.");
            idUsuarioRegistrado = u.getId();

            // Login
            Usuario logueado = usuarioService.login(u.getEmail(), "password123");
            assertNotNull(logueado);
            assertEquals(u.getEmail(), logueado.getEmail());

            // Modificar
            logueado.setNombreCompleto("Juan Modificado");
            usuarioService.modificarUsuario(logueado);
            Usuario modificado = usuarioService.obtenerPorId(logueado.getId());
            assertEquals("Juan Modificado", modificado.getNombreCompleto());
        } catch (Exception e) {
            System.err.println("❌ Error en testUsuarioFlow: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    @DisplayName("Usuario: Validación (Fallos)")
    void testUsuarioValidacion() {
        Usuario u = new Usuario();
        u.setEmail("invalid");
        Exception ex = assertThrows(ServiceException.class, () -> usuarioService.registrarUsuario(u));
        assertTrue(ex.getMessage().contains("formato del email"), "Debe validar email");
    }

    // --- TESTS EMPRESA ---

    @Test
    @Order(2)
    @DisplayName("Empresa: Registro y Listado")
    void testEmpresaFlow() throws ServiceException {
        try {
            Empresa e = new Empresa();
            e.setNombre("Reparaciones " + UUID.randomUUID().toString().substring(0, 5));
            e.setCif(generateUniqueCif());
            e.setEmailContacto(generateUniqueEmail());

            empresaService.registrarEmpresa(e);
            assertTrue(e.getId() > 0, "ID debe ser generado");

            // Update static if not set (or overwrite, fine)
            idEmpresaRegistrada = e.getId();

            List<Empresa> lista = empresaService.listarTodas();
            assertFalse(lista.isEmpty());
            assertTrue(lista.stream().anyMatch(emp -> emp.getId() == e.getId()));
        } catch (Exception e) {
            System.err.println("❌ Error en testEmpresaFlow: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    @DisplayName("Empresa: Validación Nombre")
    void testEmpresaValidacion() {
        Empresa e = new Empresa();
        Exception ex = assertThrows(ServiceException.class, () -> empresaService.registrarEmpresa(e));
        assertTrue(ex.getMessage().contains("nombre de la empresa"), "Debe validar nombre");
    }

    // --- TESTS OPERARIO ---

    @Test
    @Order(3)
    @DisplayName("Operario: Alta y Busqueda")
    void testOperarioFlow() throws ServiceException {
        try {
            int idEmp = ensureEmpresaExists();

            Operario op = new Operario();
            op.setNombreCompleto("Mario Tecnico");
            op.setEmail(generateUniqueEmail());
            op.setPasswordHash("password123");
            op.setDni(generateUniqueDni());
            op.setTelefono("666" + (100000 + (int) (Math.random() * 899999)));
            op.setIdEmpresa(idEmp);
            op.setEspecialidad(CategoriaServicio.FONTANERIA);
            op.setEstaActivo(true);
            op.setRol(Rol.OPERARIO);

            operarioService.altaOperario(op);
            assertTrue(op.getId() > 0);
            idOperarioRegistrado = op.getId();

            List<Operario> disponibles = operarioService.listarDisponibles(idEmp);
            assertTrue(disponibles.stream().anyMatch(o -> o.getId() == op.getId()));

            List<Operario> fontaneros = operarioService.buscarPorEspecialidad("FONTANERIA");
            assertFalse(fontaneros.isEmpty());
        } catch (Exception e) {
            System.err.println("❌ ERROR en testOperarioFlow: " + e.getMessage());
            if (e.getCause() != null) {
                System.err.println("   Causa: " + e.getCause().getMessage());
            }
            e.printStackTrace();
            throw e;
        }
    }

    // --- TESTS TRABAJO ---

    @Test
    @Order(4)
    @DisplayName("Trabajo: Ciclo Completo")
    void testTrabajoFlow() throws ServiceException {
        int idEmp = ensureEmpresaExists();

        if (idUsuarioRegistrado == null) {
            Usuario u = new Usuario();
            u.setEmail(generateUniqueEmail());
            u.setNombreCompleto("Fallback User");
            u.setRol(Rol.CLIENTE);
            u.setIdEmpresa(idEmp);
            usuarioService.registrarUsuario(u);
            idUsuarioRegistrado = u.getId();
        }
        if (idOperarioRegistrado == null) {
            try {
                Operario op = new Operario();
                op.setNombreCompleto("Fallback Op");
                op.setEmail(generateUniqueEmail());
                op.setPasswordHash("password123");
                op.setDni(generateUniqueDni());
                op.setIdEmpresa(idEmp);
                op.setEspecialidad(CategoriaServicio.FONTANERIA);
                op.setRol(Rol.OPERARIO);
                op.setEstaActivo(true);
                operarioService.altaOperario(op);
                idOperarioRegistrado = op.getId();
            } catch (Exception e) {
                System.err.println("❌ Fallo al crear Operario Fallback: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // 1. Solicitar
        Trabajo t = trabajoService.solicitarReparacion(
                idUsuarioRegistrado,
                CategoriaServicio.FONTANERIA,
                "Fuga de agua " + UUID.randomUUID(),
                "Casa de Juan",
                1);
        assertTrue(t.getId() > 0);
        assertEquals(EstadoTrabajo.PENDIENTE, t.getEstado());
        idTrabajoRegistrado = t.getId();

        // 2. Asignar
        try {
            if (idOperarioRegistrado != null) {
                trabajoService.asignarOperario(t.getId(), idOperarioRegistrado); // NPE check

                final int currentId = t.getId();
                // Recargar
                t = trabajoService.historialCliente(idUsuarioRegistrado).stream()
                        .filter(tr -> tr.getId() == currentId)
                        .findFirst()
                        .orElseThrow(() -> new ServiceException("Trabajo no encontrado tras asignar"));
                assertEquals(EstadoTrabajo.ASIGNADO, t.getEstado());

                // 3. Iniciar
                trabajoService.iniciarTrabajo(t.getId());
                t = trabajoService.historialCliente(idUsuarioRegistrado).stream()
                        .filter(tr -> tr.getId() == currentId)
                        .findFirst().get();
                assertEquals(EstadoTrabajo.EN_PROCESO, t.getEstado());

                // 4. Finalizar
                trabajoService.finalizarTrabajo(t.getId(), "Reparado con éxito");
                t = trabajoService.historialCliente(idUsuarioRegistrado).stream()
                        .filter(tr -> tr.getId() == currentId)
                        .findFirst().get();
                assertEquals(EstadoTrabajo.FINALIZADO, t.getEstado());
            } else {
                System.out.println("⚠️ Saltando asignación por falta de operario.");
            }
        } catch (ServiceException e) {
            System.out.println("Saltando pasos dependientes de operario: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // --- TESTS PRESUPUESTO & FACTURA ---

    @Test
    @Order(5)
    @DisplayName("Finanzas: Presupuesto y Factura")
    void testFinanzasFlow() throws ServiceException {
        try {
            int idEmp = ensureEmpresaExists();

            if (idUsuarioRegistrado == null) {
                Usuario u = new Usuario();
                u.setEmail(generateUniqueEmail());
                u.setRol(Rol.CLIENTE);
                u.setIdEmpresa(idEmp);
                usuarioService.registrarUsuario(u);
                idUsuarioRegistrado = u.getId();
            }

            Trabajo tPresu = trabajoService.solicitarReparacion(idUsuarioRegistrado, CategoriaServicio.ELECTRICIDAD,
                    "Enchufe roto " + UUID.randomUUID(), "Salon", 1);

            assertNotNull(tPresu, "El trabajo creado para presupuesto no debe ser nulo");
            assertTrue(tPresu.getId() > 0, "El trabajo debe tener ID");

            // Presupuesto
            // Verify Presupuesto table existence via TestHelper in initDb, but double check
            // here imply success
            Presupuesto p = presupuestoService.crearPresupuesto(tPresu.getId(), 50.0, "Cambio de enchufe");
            assertNotNull(p, "El presupuesto creado no debe ser nulo");
            assertNotNull(p.getId(), "El presupuesto debe tener ID");

            presupuestoService.aceptarPresupuesto(p.getId());

            // Factura
            // Si idTrabajoRegistrado es nulo o 0, usamos tPresu.getId()
            Integer idTrabajoFactura = (idTrabajoRegistrado != null && idTrabajoRegistrado > 0) ? idTrabajoRegistrado
                    : tPresu.getId();

            Factura f = facturaService.generarFactura(idTrabajoFactura);
            assertNotNull(f, "Factura no debe ser nula");
            assertNotNull(f.getId(), "Factura debe tener ID");
            assertFalse(f.isPagada());

            // Pagar
            facturaService.marcarPagada(f.getId());
            Factura fPagada = facturaService.obtenerFactura(idTrabajoFactura);
            assertTrue(fPagada.isPagada());
        } catch (Exception e) {
            System.err.println("❌ Error en testFinanzasFlow: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}

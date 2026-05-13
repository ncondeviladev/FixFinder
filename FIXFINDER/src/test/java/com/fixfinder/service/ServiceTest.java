package com.fixfinder.service;

import com.fixfinder.modelos.*;
import com.fixfinder.modelos.enums.CategoriaServicio;
import com.fixfinder.modelos.enums.EstadoTrabajo;
import com.fixfinder.modelos.enums.Rol;
import com.fixfinder.service.interfaz.*;
import com.fixfinder.utilidades.ServiceException;
import com.fixfinder.integracion.TestHelper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = com.fixfinder.red.ServidorCentral.class)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ServiceTest {

    @Autowired private UsuarioService usuarioService;
    @Autowired private EmpresaService empresaService;
    @Autowired private TrabajoService trabajoService;
    @Autowired private OperarioService operarioService;
    @Autowired private PresupuestoService presupuestoService;
    @Autowired private TestHelper helper;

    private Integer idUsuarioRegistrado;
    private Integer idEmpresaRegistrada;
    private Integer idOperarioRegistrado;
    private Integer idTrabajoRegistrado;

    @BeforeAll
    void init() {
        System.out.println("🛡️ Iniciando ServiceTest con Spring Boot Context.");
    }

    @AfterAll
    void cleanup() {
        System.out.println("🧹 Limpiando datos de prueba...");
        helper.limpiarTodoLoGenerado();
    }

    private int ensureEmpresaExists() throws ServiceException {
        if (idEmpresaRegistrada != null) return idEmpresaRegistrada;

        Empresa e = new Empresa();
        e.setNombre("Empresa Test " + UUID.randomUUID().toString().substring(0, 5));
        e.setCif(helper.generarCifUnico());
        e.setEmailContacto(helper.generarEmailUnico());

        empresaService.registrarEmpresa(e);
        idEmpresaRegistrada = e.getId();
        helper.registrarEmpresa(e.getId());
        return idEmpresaRegistrada;
    }

    @Test
    @Order(1)
    @DisplayName("Usuario: Registro y Login")
    void testUsuarioFlow() throws ServiceException {
        ensureEmpresaExists();

        Cliente u = new Cliente();
        u.setNombreCompleto("Juan Test");
        u.setEmail(helper.generarEmailUnico());
        u.setPasswordHash("password123");
        u.setTelefono("600123456");
        u.setRol(Rol.CLIENTE);

        usuarioService.registrarUsuario(u);
        assertTrue(u.getId() > 0);
        idUsuarioRegistrado = u.getId();
        helper.registrarUsuario(u.getId());

        Usuario logueado = usuarioService.login(u.getEmail(), "password123");
        assertNotNull(logueado);
        assertEquals(u.getEmail(), logueado.getEmail());
    }

    @Test
    @Order(2)
    @DisplayName("Empresa: Registro y Listado")
    void testEmpresaFlow() throws ServiceException {
        Empresa e = new Empresa();
        e.setNombre("Reparaciones Spring");
        e.setCif(helper.generarCifUnico());
        e.setEmailContacto(helper.generarEmailUnico());

        empresaService.registrarEmpresa(e);
        assertTrue(e.getId() > 0);
        helper.registrarEmpresa(e.getId());

        List<Empresa> lista = empresaService.listarTodas();
        assertTrue(lista.stream().anyMatch(emp -> emp.getId() == e.getId()));
    }

    @Test
    @Order(3)
    @DisplayName("Operario: Alta y Busqueda")
    void testOperarioFlow() throws ServiceException {
        int idEmp = ensureEmpresaExists();

        Operario op = new Operario();
        op.setNombreCompleto("Mario Tecnico");
        op.setEmail(helper.generarEmailUnico());
        op.setPasswordHash("password123");
        op.setDni(helper.generarDniUnico());
        op.setIdEmpresa(idEmp);
        op.setEspecialidad(CategoriaServicio.FONTANERIA);
        op.setEstaActivo(true);
        op.setRol(Rol.OPERARIO);

        operarioService.altaOperario(op);
        assertTrue(op.getId() > 0);
        idOperarioRegistrado = op.getId();
        helper.registrarUsuario(op.getId());

        List<Operario> disponibles = operarioService.listarDisponibles(idEmp);
        assertTrue(disponibles.stream().anyMatch(o -> o.getId() == op.getId()));
    }

    @Test
    @Order(4)
    @DisplayName("Trabajo: Ciclo de Vida")
    void testTrabajoFlow() throws ServiceException {
        ensureEmpresaExists();
        if (idUsuarioRegistrado == null) testUsuarioFlow();
        if (idOperarioRegistrado == null) testOperarioFlow();

        Trabajo t = trabajoService.solicitarReparacion(
                idUsuarioRegistrado, "Grifo Roto", CategoriaServicio.FONTANERIA,
                "Pierde agua", "Calle Falsa 123", 1);
        
        assertNotNull(t);
        idTrabajoRegistrado = t.getId();
        helper.registrarTrabajo(t.getId());

        trabajoService.asignarOperario(t.getId(), idOperarioRegistrado);
        
        // Verificar cambio a ASIGNADO
        Trabajo tAsignado = trabajoService.obtenerPorId(t.getId());
        assertEquals(EstadoTrabajo.ASIGNADO, tAsignado.getEstado());

        trabajoService.finalizarTrabajo(t.getId(), "Arreglado");
        Trabajo tFinal = trabajoService.obtenerPorId(t.getId());
        assertEquals(EstadoTrabajo.REALIZADO, tFinal.getEstado());
    }
}

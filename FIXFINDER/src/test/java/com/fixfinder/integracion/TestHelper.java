package com.fixfinder.integracion;

import com.fixfinder.data.DataRepository;
import com.fixfinder.data.DataRepositoryImpl;
import com.fixfinder.data.interfaces.*;
import com.fixfinder.modelos.*;
import com.fixfinder.modelos.enums.CategoriaServicio;
import com.fixfinder.modelos.enums.Rol;
import com.fixfinder.utilidades.DataAccessException;
import java.util.Random;
import java.util.List;
import java.util.ArrayList;

/**
 * Orquestador de Entorno de Pruebas (Test Helper).
 * 
 * Esta clase no es un test en sí misma, sino una caja de herramientas para:
 * 1. Limpieza controlada de la base de datos MySQL/MariaDB.
 * 2. Carga de datos de integración (Golden Data) para asegurar consistencia.
 * 3. Generación de datos simulados para pruebas de carga y concurrencia.
 * 4. Limpieza quirúrgica de datos de prueba para no afectar la producción.
 * 
 * Es utilizada por clases de test como BaseDatosTest o
 * MultiPresupuestoIntegracionTest.
 */
public class TestHelper {

    private final OperarioDAO operarioDAO;
    private final EmpresaDAO empresaDAO;
    private final ClienteDAO clienteDAO;
    private final TrabajoDAO trabajoDAO;
    private final PresupuestoDAO presupuestoDAO;
    private final UsuarioDAO usuarioDAO;
    private final FacturaDAO facturaDAO;
    private final Random random;

    // Rastreo de basura generado por el helper
    private final List<String> emailsCreados = new ArrayList<>();
    private final List<Integer> usuariosCreados = new ArrayList<>();
    private final List<Integer> empresasCreadas = new ArrayList<>();
    private final List<Integer> trabajosCreados = new ArrayList<>();
    private final List<Integer> presupuestosCreados = new ArrayList<>();

    public TestHelper() {
        DataRepository repo = new DataRepositoryImpl();
        this.operarioDAO = repo.getOperarioDAO();
        this.empresaDAO = repo.getEmpresaDAO();
        this.clienteDAO = repo.getClienteDAO();
        this.trabajoDAO = repo.getTrabajoDAO();
        this.presupuestoDAO = repo.getPresupuestoDAO();
        this.usuarioDAO = repo.getUsuarioDAO();
        this.facturaDAO = repo.getFacturaDAO();
        this.random = new Random();
    }

    /**
     * Crea una empresa de prueba con un nombre y CIF único.
     */
    public Empresa crearEmpresaTest() throws DataAccessException {
        Empresa e = new Empresa();
        e.setNombre("Empresa Test " + System.nanoTime());
        e.setCif(generarCifUnico());
        e.setDireccion("Calle de Pruebas 123");
        e.setTelefono("900000000");
        empresaDAO.insertar(e);
        empresasCreadas.add(e.getId());
        return e;
    }

    /**
     * Registra un email creado manualmente en el test para su posterior limpieza.
     */
    public void registrarEmail(String email) {
        if (email != null && !emailsCreados.contains(email))
            emailsCreados.add(email);
    }

    public void registrarUsuario(int id) {
        if (!usuariosCreados.contains(id))
            usuariosCreados.add(id);
    }

    public void registrarEmpresa(int id) {
        if (!empresasCreadas.contains(id))
            empresasCreadas.add(id);
    }

    public void registrarTrabajo(int id) {
        if (!trabajosCreados.contains(id))
            trabajosCreados.add(id);
    }

    public void registrarPresupuesto(int id) {
        if (!presupuestosCreados.contains(id))
            presupuestosCreados.add(id);
    }

    /**
     * Limpieza inteligente: Borra TODO lo que el helper ha rastreado en esta
     * sesión respetando la integridad referencial.
     */
    public void limpiarTodoLoGenerado() {
        System.out.println("🧹 Iniciando limpieza quirúrgica (" + trabajosCreados.size() + " trabajos, " 
                + usuariosCreados.size() + " usuarios, " + empresasCreadas.size() + " empresas)...");

        // 1. Borrar presupuestos específicos
        for (Integer id : presupuestosCreados) {
            try { presupuestoDAO.eliminar(id); } catch (Exception ignored) {}
        }

        // 2. Borrar trabajos específicos (esto borra fotos por cascada en BD)
        for (Integer id : trabajosCreados) {
            try { 
                // Primero intentar borrar factura si existe
                try { 
                    Factura f = facturaDAO.obtenerPorTrabajo(id);
                    if (f != null) facturaDAO.eliminar(f.getId());
                } catch (Exception ignored) {}
                trabajoDAO.eliminar(id); 
            } catch (Exception ignored) {}
        }

        // 3. Borrar usuarios (clientes/operarios) por ID para evitar fallos de email
        for (Integer id : usuariosCreados) {
            try { usuarioDAO.eliminar(id); } catch (Exception ignored) {}
        }

        // 4. Borrar por email (fallback legacy)
        for (String email : emailsCreados) {
            try { clienteDAO.eliminarPorEmail(email); } catch (Exception ignored) {}
        }

        // 5. Borrar Empresas
        for (Integer id : empresasCreadas) {
            limpiarSurgicamente(id);
        }

        limpiarListas();
    }

    private void limpiarListas() {
        emailsCreados.clear();
        usuariosCreados.clear();
        empresasCreadas.clear();
        trabajosCreados.clear();
        presupuestosCreados.clear();
    }

    /**
     * Limpieza completa de un escenario de test (Legacy support).
     */
    public void limpiarEscenarioCompleto(int idEmpresa, String emailCliente) {
        registrarEmail(emailCliente);
        empresasCreadas.add(idEmpresa);
        limpiarTodoLoGenerado();
    }

    /**
     * Elimina quirúrgicamente todos los datos asociados a una empresa.
     * Respeta la integridad referencial borrando en orden inverso.
     */
    public void limpiarSurgicamente(int idEmpresa) {
        try {
            // 1. Borrar presupuestos de los trabajos de la empresa
            presupuestoDAO.eliminarPorEmpresa(idEmpresa);
            // 2. Borrar trabajos vinculados a operarios de esta empresa
            trabajoDAO.eliminarPorEmpresa(idEmpresa);
            // 3. Borrar operarios
            operarioDAO.eliminarPorEmpresa(idEmpresa);
            // 4. Borrar empresa
            empresaDAO.eliminar(idEmpresa);
        } catch (Exception e) {
            System.err.println("⚠️ Error en limpieza quirúrgica de empresa " + idEmpresa + ": " + e.getMessage());
        }
    }

    public String generarDniUnico() {
        return (10000000 + random.nextInt(89999999)) + "T";
    }

    public String generarCifUnico() {
        return "B" + (10000000 + random.nextInt(89999999));
    }

    public String generarEmailUnico() {
        return "test_" + System.nanoTime() + "@fixfinder.com";
    }

    public void generarOperariosSimulados(int cantidad, int idEmpresa) throws DataAccessException {
        for (int i = 0; i < cantidad; i++) {
            Operario op = new Operario();
            op.setIdEmpresa(idEmpresa);
            op.setNombreCompleto("Operario Sim " + i);
            op.setEmail(generarEmailUnico());
            op.setPasswordHash("hash");
            op.setRol(Rol.OPERARIO);
            op.setDni(generarDniUnico());
            op.setEspecialidad(CategoriaServicio.OTROS);
            operarioDAO.insertar(op);
            registrarUsuario(op.getId());
        }
    }

    public void generarClientesSimulados(int cantidad, int idEmpresa) throws DataAccessException {
        for (int i = 0; i < cantidad; i++) {
            Cliente c = new Cliente();
            c.setNombreCompleto("Cliente Sim " + i);
            c.setEmail(generarEmailUnico());
            c.setPasswordHash("hash");
            c.setRol(Rol.CLIENTE);
            c.setDni(generarDniUnico());
            clienteDAO.insertar(c);
            registrarUsuario(c.getId());
        }
    }
}

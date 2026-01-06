package com.fixfinder.pruebas;

import com.fixfinder.data.ConexionDB;
import com.fixfinder.data.DataRepository;
import com.fixfinder.data.DataRepositoryImpl;
import com.fixfinder.data.interfaces.*;
import com.fixfinder.modelos.*;
import com.fixfinder.modelos.enums.*;
import com.fixfinder.utilidades.DataAccessException;
import com.fixfinder.utilidades.GestorPassword;

import java.sql.Connection;
import java.time.LocalDateTime;

public class PruebaIntegracion {

    public static void main(String[] args) {
        System.out.println("=========================================");
        System.out.println("   SEMILLA DE DATOS REALISTAS (SEEDER)");
        System.out.println("=========================================");

        try {
            // 0. Limpieza TOTAL
            limpiarBaseDeDatos();

            DataRepository repo = new DataRepositoryImpl();
            EmpresaDAO empresaDAO = repo.getEmpresaDAO();
            OperarioDAO operarioDAO = repo.getOperarioDAO();
            com.fixfinder.data.interfaces.ClienteDAO clienteDAO = repo.getClienteDAO();
            TrabajoDAO trabajoDAO = repo.getTrabajoDAO();
            PresupuestoDAO presupuestoDAO = repo.getPresupuestoDAO();

            // -------------------------------------------------------------------
            // 1. EMPRESAS
            // -------------------------------------------------------------------

            // TU EMPRESA
            Empresa miEmpresa = new Empresa();
            miEmpresa.setNombre("Servicios Técnicos Levante S.L.");
            miEmpresa.setCif("B98765432");
            miEmpresa.setDireccion("Gran Vía Marqués del Turia, 45, Valencia");
            miEmpresa.setTelefono("960112233");
            miEmpresa.setEmailContacto("contacto@levante.sl");
            miEmpresa.getEspecialidades().add(CategoriaServicio.FONTANERIA);
            miEmpresa.getEspecialidades().add(CategoriaServicio.ELECTRICIDAD);
            miEmpresa.getEspecialidades().add(CategoriaServicio.CLIMATIZACION);
            empresaDAO.insertar(miEmpresa);
            System.out.println("🏢 Empresa Creada: " + miEmpresa.getNombre());

            // LA COMPETENCIA
            Empresa competencia = new Empresa();
            competencia.setNombre("Reformas Manolo y Benito C.B.");
            competencia.setCif("B12312312");
            competencia.setDireccion("Calle de la Paella, 5, Valencia");
            competencia.setTelefono("666777888");
            competencia.setEmailContacto("info@reformasmanolo.com");
            competencia.getEspecialidades().add(CategoriaServicio.ALBANILERIA);
            competencia.getEspecialidades().add(CategoriaServicio.PINTURA);
            empresaDAO.insertar(competencia);
            System.out.println("🏢 Empresa Competencia Creada: " + competencia.getNombre());

            // EMPRESAS DE RELLENO (3 EXTRA)
            for (int i = 1; i <= 3; i++) {
                Empresa e = new Empresa();
                e.setNombre("Empresa Relleno " + i);
                e.setCif("R" + i + i + i + i + i);
                e.setDireccion("Calle Relleno " + i);
                e.setTelefono("90000000" + i);
                e.setEmailContacto("contacto@relleno" + i + ".com");
                e.getEspecialidades().add(CategoriaServicio.OTROS);
                empresaDAO.insertar(e);
            }
            System.out.println("🏢 +3 Empresas de relleno creadas.");

            // -------------------------------------------------------------------
            // 2. EQUIPO HUMANO (Gerentes y Operarios)
            // -------------------------------------------------------------------

            // TÚ (Gerente)
            crearOperario(operarioDAO, miEmpresa.getId(), "Carlos Martínez", "gerente@levante.com", Rol.GERENTE,
                    CategoriaServicio.CLIMATIZACION);

            // TUS OPERARIOS
            Operario miOp1 = crearOperario(operarioDAO, miEmpresa.getId(), "Paco El Fontanero", "paco@levante.com",
                    Rol.OPERARIO, CategoriaServicio.FONTANERIA);
            Operario miOp2 = crearOperario(operarioDAO, miEmpresa.getId(), "Laura Electricista", "laura@levante.com",
                    Rol.OPERARIO, CategoriaServicio.ELECTRICIDAD);
            crearOperario(operarioDAO, miEmpresa.getId(), "Javi El Becario", "javi@levante.com", Rol.OPERARIO,
                    CategoriaServicio.OTROS);

            // OPERARIO COMPETENCIA
            Operario opCompetencia = crearOperario(operarioDAO, competencia.getId(), "Benito Goteras",
                    "benito@manolo.com", Rol.OPERARIO, CategoriaServicio.ALBANILERIA);

            System.out.println("👷 Equipo y Usuarios creados.");

            // -------------------------------------------------------------------
            // 3. CLIENTES REALES
            // -------------------------------------------------------------------
            Cliente c1 = crearCliente(clienteDAO, "Marta López", "marta@gmail.com", "44556677A");
            Cliente c2 = crearCliente(clienteDAO, "Roberto Beltrán", "roberto@hotmail.com", "99887766B");
            Cliente c3 = crearCliente(clienteDAO, "Elena Nito", "elena@yahoo.es", "12344321C");

            // CLIENTES DE RELLENO (5 EXTRA)
            for (int i = 1; i <= 5; i++) {
                crearCliente(clienteDAO, "Cliente Extra " + i, "extra" + i + "@mail.com", "X" + i + i + "X");
            }
            System.out.println("👥 Clientes creados (+5 extra).");

            // -------------------------------------------------------------------
            // 4. TRABAJOS (EL ESCENARIO)
            // -------------------------------------------------------------------

            // A) TRABAJOS PENDIENTES (Mercado Libre - Todos los ven)
            crearTrabajo(trabajoDAO, c1, "Gotea un radiador",
                    "En el salón, el radiador de la derecha pierde agua y mancha el parquet.",
                    CategoriaServicio.FONTANERIA, "Calle Colón 10, 3º", EstadoTrabajo.PENDIENTE, null);

            crearTrabajo(trabajoDAO, c3, "Persiana atascada", "La persiana del dormitorio principal no sube ni baja.",
                    CategoriaServicio.OTROS, "Avda. Puerto 200, 1º", EstadoTrabajo.PENDIENTE, null);

            // B) TUS TRABAJOS - CON PRESUPUESTO (Ganados, sin asignar operario aún)
            Trabajo tPresu = crearTrabajo(trabajoDAO, c2, "Instalar Aire Acondicionado",
                    "Split 3000 frigorías en comedor. Tengo la máquina.",
                    CategoriaServicio.CLIMATIZACION, "Calle Xàtiva 5", EstadoTrabajo.PENDIENTE, null);
            // Creamos Presupuesto
            Presupuesto p = new Presupuesto();
            p.setEmpresa(miEmpresa);
            p.setTrabajo(tPresu);
            p.setMonto(250.00);
            p.setFechaEnvio(java.sql.Timestamp.valueOf(LocalDateTime.now().minusDays(1)));
            presupuestoDAO.insertar(p);
            System.out.println("   + Trabajo PRESUPUESTADO insertado (Instalar Aire).");

            // C) TUS TRABAJOS - EN PROCESO (Asignados a tu gente)
            crearTrabajo(trabajoDAO, c1, "Cortocircuito Cocina", "Saltan los plomos al encender el horno.",
                    CategoriaServicio.ELECTRICIDAD, "Calle Colón 10, 3º", EstadoTrabajo.EN_PROCESO, miOp2); // Laura

            crearTrabajo(trabajoDAO, c2, "Cambiar Grifo Bañera", "El grifo termostático no regula bien.",
                    CategoriaServicio.FONTANERIA, "Calle Xàtiva 5", EstadoTrabajo.ASIGNADO, miOp1); // Paco

            // D) TUS TRABAJOS - FINALIZADOS (Histórico)
            Trabajo tFin = crearTrabajo(trabajoDAO, c3, "Revisión Caldera Gas", "Revisión anual obligatoria.",
                    CategoriaServicio.CLIMATIZACION, "Avda. Puerto 200, 1º", EstadoTrabajo.FINALIZADO, miOp1);
            tFin.setValoracion(5);
            tFin.setComentarioCliente("Paco es un crack, muy rápido.");
            trabajoDAO.actualizar(tFin);

            // E) TRABAJOS DE LA COMPETENCIA (NO DEBERÍAS VERLOS)
            crearTrabajo(trabajoDAO, c1, "Pintar Salón", "Pintar de blanco mate, techo incl.",
                    CategoriaServicio.PINTURA, "Calle Colón 10, 3º", EstadoTrabajo.EN_PROCESO, opCompetencia); // Benito

            System.out.println("\n✅ SEMILLA COMPLETADA.");
            System.out.println("----------------------------------------------");
            System.out.println("👉 ACCESO GERENTE: gerente@levante.com  / 1234");
            System.out.println("👉 ACCESO PACO:    paco@levante.com     / 1234");
            System.out.println("👉 ACCESO CLIENTE: marta@gmail.com      / 1234");
            System.out.println("ℹ️  NOTA: Todos los usuarios tienen pass: '1234'");
            System.out.println("----------------------------------------------");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ConexionDB.cerrarConexion();
        }
    }

    // --- MÉTODOS AUXILIARES ---

    private static Operario crearOperario(OperarioDAO dao, int idEmpresa, String nombre, String email, Rol rol,
            CategoriaServicio especialidad) throws DataAccessException {
        Operario op = new Operario();
        op.setIdEmpresa(idEmpresa);
        op.setNombreCompleto(nombre);
        op.setEmail(email);
        // Hasheamos la contraseña para que el login funcione
        op.setPasswordHash(GestorPassword.hashearPassword("1234"));
        op.setRol(rol);
        op.setDni(System.currentTimeMillis() % 100000 + "X");
        op.setEspecialidad(especialidad);
        op.setEstaActivo(true);
        op.setLatitud(39.4);
        op.setLongitud(-0.3);
        dao.insertar(op);
        return op;
    }

    private static Cliente crearCliente(com.fixfinder.data.interfaces.ClienteDAO dao, String nombre, String email,
            String dni) throws DataAccessException {
        Cliente c = new Cliente();
        c.setNombreCompleto(nombre);
        c.setEmail(email);
        // Hasheamos la contraseña
        c.setPasswordHash(GestorPassword.hashearPassword("1234"));
        c.setRol(Rol.CLIENTE);
        c.setDni(dni);
        dao.insertar(c);
        return c;
    }

    private static Trabajo crearTrabajo(TrabajoDAO dao, Cliente cli, String titulo, String desc, CategoriaServicio cat,
            String dir, EstadoTrabajo estado, Operario op) throws DataAccessException {
        Trabajo t = new Trabajo();
        t.setCliente(cli);
        t.setTitulo(titulo);
        t.setDescripcion(desc);
        t.setCategoria(cat);
        t.setDireccion(dir);
        t.setEstado(estado);
        if (op != null)
            t.setOperarioAsignado(op);
        t.setFechaCreacion(LocalDateTime.now().minusDays((long) (Math.random() * 10))); // Fechas variadas
        dao.insertar(t);
        System.out.println("   + Trabajo: '" + titulo + "' [" + estado + "]");
        return t;
    }

    private static void limpiarBaseDeDatos() {
        System.out.println("🧹 Limpiando base de datos...");
        String[] tablas = { "presupuesto", "mensaje_chat", "factura", "foto_trabajo", "trabajo", "operario", "cliente",
                "usuario", "empresa_especialidad", "empresa" };

        try (Connection conn = ConexionDB.getConnection()) {
            conn.setAutoCommit(false);
            try (java.sql.Statement stmt = conn.createStatement()) {
                stmt.execute("SET FOREIGN_KEY_CHECKS=0");
                for (String tabla : tablas) {
                    try {
                        stmt.executeUpdate("DELETE FROM " + tabla);
                        stmt.executeUpdate("ALTER TABLE " + tabla + " AUTO_INCREMENT = 1");
                    } catch (Exception e) {
                    }
                }
                stmt.execute("SET FOREIGN_KEY_CHECKS=1");
            }
            conn.commit();
        } catch (Exception e) {
            System.err.println("Error limpiando: " + e.getMessage());
        }
    }
}

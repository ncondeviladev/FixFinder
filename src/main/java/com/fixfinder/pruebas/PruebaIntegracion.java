package com.fixfinder.pruebas;

import com.fixfinder.data.ConexionDB;
import com.fixfinder.data.DataRepository;
import com.fixfinder.data.DataRepositoryImpl;
import com.fixfinder.data.dao.*;
import com.fixfinder.data.interfaces.EmpresaDAO;
import com.fixfinder.data.interfaces.OperarioDAO;
import com.fixfinder.data.interfaces.TrabajoDAO;
import com.fixfinder.data.interfaces.UsuarioDAO;
import com.fixfinder.modelos.*;
import com.fixfinder.modelos.enums.CategoriaServicio;
import com.fixfinder.modelos.enums.EstadoTrabajo;
import com.fixfinder.modelos.enums.Rol;

import com.fixfinder.utilidades.GestorPassword;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

public class PruebaIntegracion {

        private static final AtomicInteger uniqueId = new AtomicInteger(100);

        public static void main(String[] args) {
                System.out.println("🚀 INICIANDO CARGA DE DATOS LIMPIOS Y SEGUROS...");

                try {
                        // 1. Recrear Esquema (Asegura ENUMs actualizados)
                        com.fixfinder.utilidades.SchemaUpdater.actualizarEsquema();

                        // 2. Limpiar datos viejos residuales (opcional pero seguro)
                        limpiarBaseDeDatos();

                        // 2. Inicializar DAOs
                        DataRepository repo = new DataRepositoryImpl();
                        EmpresaDAO empresaDAO = repo.getEmpresaDAO();
                        UsuarioDAO usuarioDAO = repo.getUsuarioDAO(); // Para clientes
                        OperarioDAO operarioDAO = repo.getOperarioDAO(); // Para operarios y gerentes
                        TrabajoDAO trabajoDAO = repo.getTrabajoDAO();

                        // -------------------------------------------------------------------
                        // 1. EMPRESAS (Usando categorias basicas para evitar errores SQL)
                        // -------------------------------------------------------------------
                        Empresa empresaA = new Empresa();
                        empresaA.setNombre("Servicios Técnicos Levante");
                        empresaA.setCif("B12345678");
                        empresaA.setDireccion("Av. del Cid 45, Valencia");
                        empresaA.setTelefono("960000001");
                        empresaA.setEmailContacto("contacto@levante.com");
                        // Usamos OTROS para asegurar compatibilidad BD
                        empresaA.getEspecialidades().add(CategoriaServicio.OTROS);
                        empresaDAO.insertar(empresaA);

                        Empresa empresaB = new Empresa();
                        empresaB.setNombre("Reformas Express S.L.");
                        empresaB.setCif("B87654321");
                        empresaB.setDireccion("Calle Colón 10, Valencia");
                        empresaB.setTelefono("960000002");
                        empresaB.setEmailContacto("contacto@express.com");
                        // Usamos OTROS para asegurar compatibilidad BD
                        empresaB.getEspecialidades().add(CategoriaServicio.OTROS);
                        empresaDAO.insertar(empresaB);

                        System.out.println(
                                        "🏢 Empresas creadas: " + empresaA.getNombre() + " y " + empresaB.getNombre());

                        // -------------------------------------------------------------------
                        // 2. GERENTES
                        // -------------------------------------------------------------------

                        // Gerente Empresa A (ELECTRICIDAD es segura)
                        crearOperario(operarioDAO, empresaA.getId(), "Carlos (Gerente A)", "gerente.a@levante.com",
                                        Rol.GERENTE,
                                        CategoriaServicio.ELECTRICIDAD);

                        // Gerente Empresa B (OTROS es seguro)
                        crearOperario(operarioDAO, empresaB.getId(), "Manolo (Gerente B)", "gerente.b@express.com",
                                        Rol.GERENTE,
                                        CategoriaServicio.OTROS);

                        // -------------------------------------------------------------------
                        // 3. OPERARIOS
                        // -------------------------------------------------------------------

                        // Equipo A
                        crearOperario(operarioDAO, empresaA.getId(), "Paco Fontanero", "paco@levante.com", Rol.OPERARIO,
                                        CategoriaServicio.FONTANERIA);
                        crearOperario(operarioDAO, empresaA.getId(), "Laura Electricista", "laura@levante.com",
                                        Rol.OPERARIO,
                                        CategoriaServicio.ELECTRICIDAD);

                        // Equipo B
                        crearOperario(operarioDAO, empresaB.getId(), "Benito Albañil", "benito@express.com",
                                        Rol.OPERARIO,
                                        CategoriaServicio.OTROS);
                        crearOperario(operarioDAO, empresaB.getId(), "Pepe Pintor", "pepe@express.com", Rol.OPERARIO,
                                        CategoriaServicio.OTROS);

                        System.out.println("👷 Equipo humano creado.");

                        // -------------------------------------------------------------------
                        // 4. CLIENTES
                        // -------------------------------------------------------------------
                        Usuario c1 = crearCliente(usuarioDAO, "Marta Cliente", "marta@gmail.com");
                        Usuario c2 = crearCliente(usuarioDAO, "Juan Cliente", "juan@hotmail.com");
                        Usuario c3 = crearCliente(usuarioDAO, "Elena Cliente", "elena@yahoo.com");

                        System.out.println("👤 Clientes creados.");

                        // -------------------------------------------------------------------
                        // 5. TRABAJOS (Todos PENDIENTES, usar categorias seguras)
                        // -------------------------------------------------------------------

                        crearTrabajo(trabajoDAO, c1, "Fuga de agua en cocina",
                                        "Pierde mucha agua bajo el fregadero, urge reparar.",
                                        CategoriaServicio.FONTANERIA, "Calle Paz 5, 2ºA", EstadoTrabajo.PENDIENTE);

                        crearTrabajo(trabajoDAO, c2, "Instalación de enchufes",
                                        "Necesito poner 3 enchufes nuevos en el salón.",
                                        CategoriaServicio.ELECTRICIDAD, "Av. del Puerto 120", EstadoTrabajo.PENDIENTE);

                        crearTrabajo(trabajoDAO, c3, "Reforma baño completo",
                                        "Picar azulejos y cambiar bañera por plato de ducha.",
                                        CategoriaServicio.OTROS, "Calle Xàtiva 22", EstadoTrabajo.PENDIENTE);

                        crearTrabajo(trabajoDAO, c1, "Revisión Aire Acondicionado",
                                        "No enfría bien, hace ruido extraño.",
                                        CategoriaServicio.OTROS, "Calle Paz 5, 2ºA", EstadoTrabajo.PENDIENTE);

                        crearTrabajo(trabajoDAO, c2, "Pintar habitación",
                                        "Pintar dormitorio de blanco, unos 15m2.",
                                        CategoriaServicio.OTROS, "Av. del Puerto 120", EstadoTrabajo.PENDIENTE);

                        System.out.println("📋 Trabajos creados (Todos PENDIENTES).");
                        System.out.println("✅ CARGA COMPLETADA EXITOSAMENTE.");

                } catch (Exception e) {
                        e.printStackTrace();
                }
        }

        // --- MÉTODOS AUXILIARES ---

        private static void limpiarBaseDeDatos() {
                try (Connection conn = ConexionDB.getConnection();
                                Statement stmt = conn.createStatement()) {
                        // Orden de borrado por FKs
                        stmt.executeUpdate("DELETE FROM mensaje_chat");
                        stmt.executeUpdate("DELETE FROM factura");
                        stmt.executeUpdate("DELETE FROM foto_trabajo");
                        stmt.executeUpdate("DELETE FROM presupuesto");
                        stmt.executeUpdate("DELETE FROM trabajo");

                        // Usuarios y Roles
                        stmt.executeUpdate("DELETE FROM cliente");
                        stmt.executeUpdate("DELETE FROM operario");
                        stmt.executeUpdate("DELETE FROM usuario");

                        // Empresas
                        stmt.executeUpdate("DELETE FROM empresa_especialidad");
                        stmt.executeUpdate("DELETE FROM empresa");
                        System.out.println("🧹 Base de datos limpiada.");
                } catch (Exception e) {
                        System.err.println("Error limpiando BD: " + e.getMessage());
                }
        }

        private static Operario crearOperario(OperarioDAO dao, int idEmpresa, String nombre, String email, Rol rol,
                        CategoriaServicio cat) throws Exception {
                Operario op = new Operario();
                op.setNombreCompleto(nombre);
                op.setEmail(email);
                op.setPasswordHash(GestorPassword.hashearPassword("1234")); // Hash Correcto
                // FIX: Identificador único garantizado
                op.setDni(uniqueId.getAndIncrement() + "X");
                op.setDireccion("Calle Test");
                op.setTelefono("600000000");
                op.setRol(rol);
                op.setIdEmpresa(idEmpresa);
                op.setEspecialidad(cat);
                op.setEstaActivo(true);
                dao.insertar(op);
                return op;
        }

        private static Usuario crearCliente(UsuarioDAO dao, String nombre, String email) throws Exception {
                Usuario u = new Cliente();
                u.setNombreCompleto(nombre);
                u.setEmail(email);
                u.setPasswordHash(GestorPassword.hashearPassword("1234")); // Hash Correcto
                // FIX: Identificador único garantizado
                u.setDni(uniqueId.getAndIncrement() + "X");
                u.setDireccion("Calle Cliente");
                u.setTelefono("699000000");
                u.setRol(Rol.CLIENTE);
                dao.insertar(u);
                return u;
        }

        private static Trabajo crearTrabajo(TrabajoDAO dao, Usuario cliente, String titulo, String descripcion,
                        CategoriaServicio cat, String direccion, EstadoTrabajo estado) throws Exception {
                Trabajo t = new Trabajo();
                t.setCliente(cliente);
                t.setTitulo(titulo);
                t.setDescripcion(descripcion);
                t.setCategoria(cat);
                t.setDireccion(direccion);
                t.setEstado(estado);
                t.setFechaCreacion(LocalDateTime.now());
                t.setOperarioAsignado(null);
                dao.insertar(t);
                return t;
        }
}

package com.fixfinder.pruebas;

import com.fixfinder.data.ConexionDB;
import com.fixfinder.data.DataRepository;
import com.fixfinder.data.DataRepositoryImpl;
import com.fixfinder.data.interfaces.EmpresaDAO;
import com.fixfinder.data.interfaces.OperarioDAO;
import com.fixfinder.data.interfaces.TrabajoDAO;
import com.fixfinder.data.interfaces.UsuarioDAO;
import com.fixfinder.modelos.*;
import com.fixfinder.modelos.enums.*;
import com.fixfinder.utilidades.DataAccessException;

import java.sql.Connection;

public class PruebaIntegracion {

    public static void main(String[] args) {
        System.out.println("=========================================");
        System.out.println("   INICIO DE PRUEBAS DE INTEGRACIÓN");
        System.out.println("=========================================");

        try {
            // 0. Limpieza inicial
            limpiarBaseDeDatos();

            // Instanciar Repositorio
            DataRepository repo = new DataRepositoryImpl();

            // 1. Crear EMPRESA
            System.out.println("\n--- [1] PRUEBA EMPRESA ---");
            EmpresaDAO empresaDAO = repo.getEmpresaDAO();
            Empresa emp = new Empresa();
            emp.setNombre("FixFinder Soluciones S.L.");
            emp.setCif("B12345678");
            emp.setDireccion("Av. Principal 123");
            emp.setTelefono("600123456");
            emp.setEmailContacto("contacto@fixfinder.test");

            // Añadir especialidades
            emp.getEspecialidades().add(CategoriaServicio.FONTANERIA);
            emp.getEspecialidades().add(CategoriaServicio.ELECTRICIDAD);

            empresaDAO.insertar(emp);
            System.out.println("[OK] Empresa creada: " + emp.getNombre() + " (ID: " + emp.getId() + ")");
            System.out.println("   Especialidades: " + emp.getEspecialidades());

            // 2. Crear OPERARIO (Extiende de Usuario)
            System.out.println("\n--- [2] PRUEBA OPERARIO ---");
            OperarioDAO operarioDAO = repo.getOperarioDAO();
            Operario op = new Operario();
            op.setIdEmpresa(emp.getId()); // Pertenece a la empresa creada
            op.setEmail("tecnico@fixfinder.test");
            op.setPasswordHash("pass123"); // En real usar BCrypt
            op.setNombreCompleto("Juan Tecnico");
            op.setRol(Rol.OPERARIO); // Aunque el DAO lo fuerza, lo ponemos

            op.setDni("87654321Z");
            op.setEspecialidad(CategoriaServicio.FONTANERIA);
            op.setEstaActivo(true);
            op.setLatitud(39.4699);
            op.setLongitud(-0.3763);

            operarioDAO.insertar(op);
            System.out.println("[OK] Operario creado: " + op.getNombreCompleto() + " (ID Usuario: " + op.getId() + ")");
            System.out.println("   Especialidad: " + op.getEspecialidad());

            // 3. Crear CLIENTE (Usuario normal)
            System.out.println("\n--- [3] PRUEBA CLIENTE ---");
            UsuarioDAO usuarioDAO = repo.getUsuarioDAO();
            Usuario cli = new Usuario();
            cli.setIdEmpresa(emp.getId());
            cli.setEmail("cliente@gmail.com");
            cli.setPasswordHash("pass123");
            cli.setNombreCompleto("Maria Cliente");
            cli.setRol(Rol.CLIENTE);

            usuarioDAO.insertar(cli);
            System.out.println("[OK] Cliente creado: " + cli.getNombreCompleto() + " (ID: " + cli.getId() + ")");

            // 4. Crear TRABAJO (Incidencia)
            System.out.println("\n--- [4] PRUEBA TRABAJO ---");
            TrabajoDAO trabajoDAO = repo.getTrabajoDAO();
            Trabajo t = new Trabajo();
            t.setCliente(cli);
            t.setCategoria(CategoriaServicio.FONTANERIA);
            t.setTitulo("Fuga en el bano");
            t.setDescripcion("El grifo del lavabo pierde mucha agua y moja el suelo.");
            t.setDireccion("C/ Valencia 10, 4A");
            t.setEstado(EstadoTrabajo.PENDIENTE);

            trabajoDAO.insertar(t);
            System.out.println("[OK] Trabajo creado ID: " + t.getId());

            // 5. Asignar Operario al Trabajo
            System.out.println("\n--- [5] ACTUALIZAR TRABAJO (ASIGNAR/FINALIZAR) ---");
            t.setOperarioAsignado(op);
            t.setEstado(EstadoTrabajo.EN_PROCESO);
            trabajoDAO.actualizar(t);
            System.out.println("[OK] Trabajo asignado a: " + op.getNombreCompleto());

            // Simulamos finalizar
            t.setEstado(EstadoTrabajo.FINALIZADO);
            t.setValoracion(5);
            t.setComentarioCliente("Muy buen servicio, rapido y limpio.");
            trabajoDAO.actualizar(t);
            System.out.println("[OK] Trabajo finalizado y valorado.");

            // 6. Verificar lectura
            System.out.println("\n--- [6] VERIFICACION DE LECTURA ---");
            Trabajo tLeido = trabajoDAO.obtenerPorId(t.getId());
            System.out.println("Datos leidos de DB:");
            System.out.println(" - Titulo: " + tLeido.getTitulo());
            System.out.println(" - Estado: " + tLeido.getEstado());
            System.out.println(" - Cliente: " + tLeido.getCliente().getNombreCompleto());
            System.out.println(" - Operario: "
                    + (tLeido.getOperarioAsignado() != null ? tLeido.getOperarioAsignado().getNombreCompleto()
                            : "N/A"));
            System.out.println(" - Valoracion: " + tLeido.getValoracion() + "/5");

            System.out.println("\n=========================================");
            System.out.println("   PRUEBAS COMPLETADAS CON EXITO");
            System.out.println("=========================================");

        } catch (DataAccessException e) {
            System.err.println("[ERROR] FATAL DE ACCESO A DATOS:");
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("[ERROR] NO CONTROLADO:");
            e.printStackTrace();
        } finally {
            ConexionDB.cerrarConexion();
        }
    }

    private static void limpiarBaseDeDatos() {
        System.out.println("[INFO] Limpiando base de datos previa...");
        String[] tablas = { "mensaje_chat", "factura", "foto_trabajo", "trabajo", "operario", "usuario",
                "empresa_especialidad", "empresa" };

        try (Connection conn = ConexionDB.getConnection()) {
            conn.setAutoCommit(false);
            try (java.sql.Statement stmt = conn.createStatement()) {
                stmt.execute("SET FOREIGN_KEY_CHECKS=0");
                for (String tabla : tablas) {
                    try {
                        stmt.executeUpdate("DELETE FROM " + tabla);
                        stmt.executeUpdate("ALTER TABLE " + tabla + " AUTO_INCREMENT = 1");
                    } catch (Exception e) {
                        System.err.println("   [INFO] No se pudo limpiar tabla '" + tabla + "': " + e.getMessage());
                    }
                }
                stmt.execute("SET FOREIGN_KEY_CHECKS=1");
            }
            conn.commit();
            System.out.println("   [OK] Limpieza completada.");
        } catch (Exception e) {
            System.err.println("   [WARN] Fallo al limpiar BD (" + e.getMessage() + ")");
        }
    }
}

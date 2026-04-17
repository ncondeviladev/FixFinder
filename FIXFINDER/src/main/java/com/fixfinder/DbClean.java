package com.fixfinder;

import com.fixfinder.config.GlobalConfig;

import com.fixfinder.data.ConexionDB;
import com.fixfinder.data.DataRepository;
import com.fixfinder.data.DataRepositoryImpl;
import com.fixfinder.data.interfaces.EmpresaDAO;
import com.fixfinder.data.interfaces.OperarioDAO;
import com.fixfinder.data.interfaces.TrabajoDAO;
import com.fixfinder.data.interfaces.UsuarioDAO;
import com.fixfinder.modelos.Cliente;
import com.fixfinder.modelos.Empresa;
import com.fixfinder.modelos.Operario;
import com.fixfinder.modelos.Trabajo;
import com.fixfinder.modelos.Usuario;
import com.fixfinder.modelos.enums.CategoriaServicio;
import com.fixfinder.modelos.enums.EstadoTrabajo;
import com.fixfinder.modelos.enums.Rol;
import com.fixfinder.utilidades.GestorPassword;
import com.fixfinder.utilidades.SchemaUpdater;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.StorageClient;

public class DbClean {

        public static void main(String[] args) {
                System.out.println("🚀 INICIANDO CARGA DE DATOS LIMPIOS Y SEGUROS...");

                try {
                        // 1. Recrear Esquema (Asegura ENUMs actualizados)
                        SchemaUpdater.actualizarEsquema();

                        // 2. Limpiar datos viejos residuales (SEGURIDAD ACTIVA)
                        java.util.Scanner sc = new java.util.Scanner(System.in);
                        if (GlobalConfig.MODO_NUBE) {
                            System.err.println("⚠️ MODO NUBE: ¿Formatear AWS RDS? (escribe 'confirmar')");
                            if (!sc.nextLine().equalsIgnoreCase("confirmar")) {
                                System.out.println("Abortado para proteger la nube.");
                                sc.close();
                                return;
                            }
                        }
                        
                        // Preguntar por Firebase Storage por separado (siempre es nube)
                        System.out.println("🔥 ¿Deseas limpiar también todas las imágenes en Firebase Storage? (si/no)");
                        if (sc.nextLine().equalsIgnoreCase("si")) {
                            limpiarFirebaseStorage();
                        }
                        sc.close();

                        limpiarBaseDeDatos();

                        // 3. Inicializar DAOs
                        DataRepository repo = new DataRepositoryImpl();
                        EmpresaDAO empresaDAO = repo.getEmpresaDAO();
                        UsuarioDAO usuarioDAO = repo.getUsuarioDAO();
                        OperarioDAO operarioDAO = repo.getOperarioDAO();
                        TrabajoDAO trabajoDAO = repo.getTrabajoDAO();

                        // -------------------------------------------------------------------
                        // 1. EMPRESAS
                        // -------------------------------------------------------------------
                        Empresa empresaA = new Empresa();
                        empresaA.setNombre("Servicios Técnicos Levante");
                        empresaA.setCif("B12345678");
                        empresaA.setDireccion("Av. del Cid 45, Valencia");
                        empresaA.setTelefono("963100001");
                        empresaA.setEmailContacto("contacto@levante.com");
                        empresaA.getEspecialidades().add(CategoriaServicio.OTROS);
                        empresaDAO.insertar(empresaA);

                        Empresa empresaB = new Empresa();
                        empresaB.setNombre("Reformas Express S.L.");
                        empresaB.setCif("B87654321");
                        empresaB.setDireccion("Calle Colón 10, Valencia");
                        empresaB.setTelefono("963200002");
                        empresaB.setEmailContacto("contacto@express.com");
                        empresaB.getEspecialidades().add(CategoriaServicio.OTROS);
                        empresaDAO.insertar(empresaB);

                        System.out.println(
                                        "🏢 Empresas creadas: " + empresaA.getNombre() + " y " + empresaB.getNombre());

                        // -------------------------------------------------------------------
                        // 2. GERENTES
                        // -------------------------------------------------------------------
                        Operario gerenteA = new Operario();
                        gerenteA.setNombreCompleto("Carlos Martínez (Gerente)");
                        gerenteA.setEmail("gerente.a@levante.com");
                        gerenteA.setPasswordHash(GestorPassword.hashearPassword("1234"));
                        gerenteA.setDni("12345678A");
                        gerenteA.setDireccion("Av. del Cid 45, Valencia");
                        gerenteA.setTelefono("612345678");
                        gerenteA.setRol(Rol.GERENTE);
                        gerenteA.setIdEmpresa(empresaA.getId());
                        gerenteA.setEspecialidad(CategoriaServicio.ELECTRICIDAD);
                        gerenteA.setEstaActivo(true);
                        operarioDAO.insertar(gerenteA);

                        Operario gerenteB = new Operario();
                        gerenteB.setNombreCompleto("Manuel García (Gerente)");
                        gerenteB.setEmail("gerente.b@express.com");
                        gerenteB.setPasswordHash(GestorPassword.hashearPassword("1234"));
                        gerenteB.setDni("87654321B");
                        gerenteB.setDireccion("Calle Colón 10, Valencia");
                        gerenteB.setTelefono("698765432");
                        gerenteB.setRol(Rol.GERENTE);
                        gerenteB.setIdEmpresa(empresaB.getId());
                        gerenteB.setEspecialidad(CategoriaServicio.OTROS);
                        gerenteB.setEstaActivo(true);
                        operarioDAO.insertar(gerenteB);

                        // -------------------------------------------------------------------
                        // 3. OPERARIOS
                        // -------------------------------------------------------------------
                        // Equipo A - Servicios Técnicos Levante
                        Operario pacoFontanero = new Operario();
                        pacoFontanero.setNombreCompleto("Francisco López (Fontanero)");
                        pacoFontanero.setEmail("paco@levante.com");
                        pacoFontanero.setPasswordHash(GestorPassword.hashearPassword("1234"));
                        pacoFontanero.setDni("11111111C");
                        pacoFontanero.setDireccion("Calle Sagunto 12, Valencia");
                        pacoFontanero.setTelefono("611111111");
                        pacoFontanero.setRol(Rol.OPERARIO);
                        pacoFontanero.setIdEmpresa(empresaA.getId());
                        pacoFontanero.setEspecialidad(CategoriaServicio.FONTANERIA);
                        pacoFontanero.setEstaActivo(true);
                        operarioDAO.insertar(pacoFontanero);

                        Operario lauraElectricista = new Operario();
                        lauraElectricista.setNombreCompleto("Laura Sánchez (Electricista)");
                        lauraElectricista.setEmail("laura@levante.com");
                        lauraElectricista.setPasswordHash(GestorPassword.hashearPassword("1234"));
                        lauraElectricista.setDni("22222222D");
                        lauraElectricista.setDireccion("Calle Blanquerías 5, Valencia");
                        lauraElectricista.setTelefono("622222222");
                        lauraElectricista.setRol(Rol.OPERARIO);
                        lauraElectricista.setIdEmpresa(empresaA.getId());
                        lauraElectricista.setEspecialidad(CategoriaServicio.ELECTRICIDAD);
                        lauraElectricista.setEstaActivo(true);
                        operarioDAO.insertar(lauraElectricista);

                        // Equipo B - Reformas Express
                        Operario benitoAlbanil = new Operario();
                        benitoAlbanil.setNombreCompleto("Benito Ruiz (Albañil)");
                        benitoAlbanil.setEmail("benito@express.com");
                        benitoAlbanil.setPasswordHash(GestorPassword.hashearPassword("1234"));
                        benitoAlbanil.setDni("33333333E");
                        benitoAlbanil.setDireccion("Calle Quart 18, Valencia");
                        benitoAlbanil.setTelefono("633333333");
                        benitoAlbanil.setRol(Rol.OPERARIO);
                        benitoAlbanil.setIdEmpresa(empresaB.getId());
                        benitoAlbanil.setEspecialidad(CategoriaServicio.ALBANILERIA);
                        benitoAlbanil.setEstaActivo(true);
                        operarioDAO.insertar(benitoAlbanil);

                        Operario pepePintor = new Operario();
                        pepePintor.setNombreCompleto("José Fernández (Pintor)");
                        pepePintor.setEmail("pepe@express.com");
                        pepePintor.setPasswordHash(GestorPassword.hashearPassword("1234"));
                        pepePintor.setDni("44444444F");
                        pepePintor.setDireccion("Gran Vía Fernando el Católico 7, Valencia");
                        pepePintor.setTelefono("644444444");
                        pepePintor.setRol(Rol.OPERARIO);
                        pepePintor.setIdEmpresa(empresaB.getId());
                        pepePintor.setEspecialidad(CategoriaServicio.PINTURA);
                        pepePintor.setEstaActivo(true);
                        operarioDAO.insertar(pepePintor);

                        System.out.println("👷 Equipo humano creado.");

                        // -------------------------------------------------------------------
                        // 4. CLIENTES
                        // -------------------------------------------------------------------
                        Usuario marta = new Cliente();
                        marta.setNombreCompleto("Marta Gómez");
                        marta.setEmail("marta@gmail.com");
                        marta.setPasswordHash(GestorPassword.hashearPassword("1234"));
                        marta.setDni("55555555G");
                        marta.setDireccion("Calle Paz 5, 2ºA, Valencia");
                        marta.setTelefono("655555555");
                        marta.setRol(Rol.CLIENTE);
                        usuarioDAO.insertar(marta);

                        Usuario juan = new Cliente();
                        juan.setNombreCompleto("Juan Pérez");
                        juan.setEmail("juan@hotmail.com");
                        juan.setPasswordHash(GestorPassword.hashearPassword("1234"));
                        juan.setDni("66666666H");
                        juan.setDireccion("Av. del Puerto 120, Valencia");
                        juan.setTelefono("666666666");
                        juan.setRol(Rol.CLIENTE);
                        usuarioDAO.insertar(juan);

                        Usuario elena = new Cliente();
                        elena.setNombreCompleto("Elena Torres");
                        elena.setEmail("elena@yahoo.com");
                        elena.setPasswordHash(GestorPassword.hashearPassword("1234"));
                        elena.setDni("77777777I");
                        elena.setDireccion("Calle Xàtiva 22, Valencia");
                        elena.setTelefono("677777777");
                        elena.setRol(Rol.CLIENTE);
                        usuarioDAO.insertar(elena);

                        System.out.println("👤 Clientes creados.");

                        // -------------------------------------------------------------------
                        // 5. TRABAJOS
                        // -------------------------------------------------------------------
                        crearTrabajo(trabajoDAO, marta,
                                        "Fuga de agua en cocina",
                                        "Pierde mucha agua bajo el fregadero, urge reparar.",
                                        CategoriaServicio.FONTANERIA, "Calle Paz 5, 2ºA, Valencia");

                        crearTrabajo(trabajoDAO, juan,
                                        "Instalación de enchufes",
                                        "Necesito poner 3 enchufes nuevos en el salón.",
                                        CategoriaServicio.ELECTRICIDAD, "Av. del Puerto 120, Valencia");

                        crearTrabajo(trabajoDAO, elena,
                                        "Reforma baño completo",
                                        "Picar azulejos y cambiar bañera por plato de ducha.",
                                        CategoriaServicio.ALBANILERIA, "Calle Xàtiva 22, Valencia");

                        crearTrabajo(trabajoDAO, marta,
                                        "Revisión Aire Acondicionado",
                                        "No enfría bien, hace ruido extraño al arrancar.",
                                        CategoriaServicio.CLIMATIZACION, "Calle Paz 5, 2ºA, Valencia");

                        crearTrabajo(trabajoDAO, juan,
                                        "Pintar habitación",
                                        "Pintar dormitorio de blanco, unos 15m2 aproximadamente.",
                                        CategoriaServicio.PINTURA, "Av. del Puerto 120, Valencia");

                        System.out.println("📋 Trabajos creados.");
                        System.out.println();
                        System.out.println("✅ CARGA COMPLETADA. Credenciales (password '1234' para todos):");
                        System.out.println("   GERENTES  → gerente.a@levante.com / gerente.b@express.com");
                        System.out.println(
                                        "   OPERARIOS → paco@levante.com (611111111) / laura@levante.com (622222222)");
                        System.out.println(
                                        "               benito@express.com (633333333) / pepe@express.com (644444444)");
                        System.out.println(
                                        "   CLIENTES  → marta@gmail.com (655555555) / juan@hotmail.com (666666666) / elena@yahoo.com (677777777)");

                } catch (Exception e) {
                        e.printStackTrace();
                }
        }

        // --- MÉTODOS AUXILIARES ---

        private static void limpiarBaseDeDatos() {
                try (Connection conn = ConexionDB.getConnection();
                                Statement stmt = conn.createStatement()) {
                        stmt.executeUpdate("DELETE FROM mensaje_chat");
                        stmt.executeUpdate("DELETE FROM factura");
                        stmt.executeUpdate("DELETE FROM foto_trabajo");
                        stmt.executeUpdate("DELETE FROM presupuesto");
                        stmt.executeUpdate("DELETE FROM trabajo");
                        stmt.executeUpdate("DELETE FROM cliente");
                        stmt.executeUpdate("DELETE FROM operario");
                        stmt.executeUpdate("DELETE FROM usuario");
                        stmt.executeUpdate("DELETE FROM empresa_especialidad");
                        stmt.executeUpdate("DELETE FROM empresa");
                        System.out.println("🧹 Base de datos limpiada.");
                } catch (Exception e) {
                        System.err.println("Error limpiando BD: " + e.getMessage());
                }
        }

        private static void limpiarFirebaseStorage() {
                try {
                        InputStream serviceAccount = DbClean.class.getResourceAsStream("/firebase-service-account.json");
                        
                        // Fallback: Buscar en la raíz del proyecto si no está en recursos
                        if (serviceAccount == null) {
                            java.io.File fileRaiz = new java.io.File("firebase-service-account.json");
                            if (fileRaiz.exists()) {
                                serviceAccount = new java.io.FileInputStream(fileRaiz);
                            }
                        }

                        if (serviceAccount == null) {
                            System.err.println("⚠️ FIREBASE STORAGE: Falta archivo 'firebase-service-account.json' en la raíz o en /resources/.");
                            System.err.println("   Para limpiar Firebase, debes descargar la clave JSON desde: Console -> Ajustes -> Cuentas de servicio.");
                            return;
                        }

                        if (FirebaseApp.getApps().isEmpty()) {
                                FirebaseOptions options = FirebaseOptions.builder()
                                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                                        .setStorageBucket("fixfinder-dbb81.firebasestorage.app") // <-- REEMPLAZA AQUÍ POR EL NOMBRE DE TU BUCKET
                                        .build();
                                FirebaseApp.initializeApp(options);
                        }

                        Bucket bucket = StorageClient.getInstance().bucket();
                        System.out.println("🔥 Conectado a Firebase Storage. Destruyendo fotos huérfanas...");

                        int contador = 0;
                        Iterable<Blob> todosLosBlobs = bucket.list().iterateAll();
                        for (Blob blob : todosLosBlobs) {
                                blob.delete();
                                contador++;
                        }
                        System.out.println("🧹 Firebase Storage: Vaciado total completado (" + contador + " archivos eliminados).");

                } catch (Exception e) {
                        System.err.println("❌ ERROR limpiando Firebase: " + e.getMessage());
                }
        }

        private static void crearTrabajo(TrabajoDAO dao, Usuario cliente, String titulo,
                        String descripcion, CategoriaServicio cat, String direccion) throws Exception {
                Trabajo t = new Trabajo();
                t.setCliente(cliente);
                t.setTitulo(titulo);

                String descEstructurada = "==============================\n" +
                                "📝 CLIENTE:\n" + descripcion.trim() + "\n" +
                                "==============================\n" +
                                "💰 GERENTE:\n(Sin presupuesto redactado)\n" +
                                "==============================\n" +
                                "🛠 OPERARIO:\n(Sin informe de trabajo)\n" +
                                "==============================";

                t.setDescripcion(descEstructurada);
                t.setCategoria(cat);
                t.setDireccion(direccion);
                t.setEstado(EstadoTrabajo.PENDIENTE);
                t.setFechaCreacion(LocalDateTime.now());
                t.setOperarioAsignado(null);
                dao.insertar(t);
        }
}

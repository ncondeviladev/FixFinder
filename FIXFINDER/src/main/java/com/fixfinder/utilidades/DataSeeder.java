package com.fixfinder.utilidades;

import com.fixfinder.modelos.*;
import com.fixfinder.modelos.enums.CategoriaServicio;
import com.fixfinder.modelos.enums.Rol;
import com.fixfinder.repository.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Replicación fiel del Seeder original (seeder.sql) adaptado a Spring Boot.
 */
public class DataSeeder {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(SeederConfig.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        ApplicationContext ctx = app.run(args);
        
        try {
            SeederRunner runner = ctx.getBean(SeederRunner.class);
            runner.run();
            System.out.println("✅ Seeder completado con éxito (Datos Legacy restaurados).");
        } catch (Exception e) {
            System.err.println("❌ Error en el seeder: " + e.getMessage());
            e.printStackTrace();
        } finally {
            SpringApplication.exit(ctx);
        }
    }

    @Configuration
    @ComponentScan(basePackages = "com.fixfinder")
    static class SeederConfig {}

    @Component
    static class SeederRunner {
        private final EmpresaRepository empresaRepo;
        private final UsuarioRepository usuarioRepo;
        private final OperarioRepository operarioRepo;
        private final ClienteRepository clienteRepo;
        private final TrabajoRepository trabajoRepo;

        private final String HASH_1234 = GestorPassword.hashearPassword("1234");

        public SeederRunner(EmpresaRepository empresaRepo, UsuarioRepository usuarioRepo, 
                            OperarioRepository operarioRepo, ClienteRepository clienteRepo, 
                            TrabajoRepository trabajoRepo) {
            this.empresaRepo = empresaRepo;
            this.usuarioRepo = usuarioRepo;
            this.operarioRepo = operarioRepo;
            this.clienteRepo = clienteRepo;
            this.trabajoRepo = trabajoRepo;
        }

        public void run() {
            System.out.println("🌱 Restaurando base de datos con datos de LEVANTE y EXPRESS FIX...");

            // Limpieza previa
            trabajoRepo.deleteAll();
            clienteRepo.deleteAll();
            operarioRepo.deleteAll();
            usuarioRepo.deleteAll();
            empresaRepo.deleteAll();

            // 1. EMPRESAS
            Empresa e1 = new Empresa();
            e1.setNombre("Levante Reparaciones S.L.");
            e1.setCif("B12345678");
            e1.setDireccion("Calle Mayor 10, Valencia");
            e1.setTelefono("961234567");
            e1.setEmailContacto("contacto@levante.com");
            e1 = empresaRepo.save(e1);

            Empresa e2 = new Empresa();
            e2.setNombre("Express Fix S.A.");
            e2.setCif("A87654321");
            e2.setDireccion("Av. del Puerto 45, Valencia");
            e2.setTelefono("963456789");
            e2.setEmailContacto("info@expressfix.com");
            e2 = empresaRepo.save(e2);

            // 2. ADMIN
            Usuario admin = new Usuario();
            admin.setEmail("admin@fixfinder.com");
            admin.setPasswordHash(HASH_1234);
            admin.setNombreCompleto("Admin Sistema");
            admin.setRol(Rol.ADMIN);
            admin.setDni("00000000A");
            admin.setTelefono("600000001");
            usuarioRepo.save(admin);

            // 3. GERENTES (Operarios de tipo Gerente en la lógica original)
            Operario g1 = new Operario();
            g1.setEmail("gerente.a@levante.com");
            g1.setPasswordHash(HASH_1234);
            g1.setNombreCompleto("Carlos Gerente");
            g1.setRol(Rol.GERENTE);
            g1.setDni("11111111B");
            g1.setTelefono("611111111");
            g1.setIdEmpresa(e1.getId());
            g1.setEspecialidad(CategoriaServicio.ELECTRICIDAD);
            g1.setEstaActivo(true);
            operarioRepo.save(g1);

            Operario g2 = new Operario();
            g2.setEmail("gerente.b@express.com");
            g2.setPasswordHash(HASH_1234);
            g2.setNombreCompleto("Manolo Gerente");
            g2.setRol(Rol.GERENTE);
            g2.setDni("22222222C");
            g2.setTelefono("622222222");
            g2.setIdEmpresa(e2.getId());
            g2.setEspecialidad(CategoriaServicio.CLIMATIZACION);
            g2.setEstaActivo(true);
            operarioRepo.save(g2);

            // 4. OPERARIOS
            Operario op1 = new Operario();
            op1.setEmail("paco@levante.com");
            op1.setPasswordHash(HASH_1234);
            op1.setNombreCompleto("Paco Fontanero");
            op1.setRol(Rol.OPERARIO);
            op1.setDni("33333333D");
            op1.setTelefono("633333333");
            op1.setIdEmpresa(e1.getId());
            op1.setEspecialidad(CategoriaServicio.FONTANERIA);
            op1.setEstaActivo(true);
            operarioRepo.save(op1);

            Operario op2 = new Operario();
            op2.setEmail("benito@express.com");
            op2.setPasswordHash(HASH_1234);
            op2.setNombreCompleto("Benito Pintor");
            op2.setRol(Rol.OPERARIO);
            op2.setDni("44444444E");
            op2.setTelefono("644444444");
            op2.setIdEmpresa(e2.getId());
            op2.setEspecialidad(CategoriaServicio.PINTURA);
            op2.setEstaActivo(true);
            operarioRepo.save(op2);

            // 5. CLIENTES
            Cliente c1 = new Cliente();
            c1.setEmail("marta@gmail.com");
            c1.setPasswordHash(HASH_1234);
            c1.setNombreCompleto("Marta Cliente");
            c1.setRol(Rol.CLIENTE);
            c1.setDni("55555555F");
            c1.setTelefono("655555555");
            c1.setDireccion("Calle Poeta Querol 3, Valencia");
            c1 = clienteRepo.save(c1);

            Cliente c2 = new Cliente();
            c2.setEmail("juan@hotmail.com");
            c2.setPasswordHash(HASH_1234);
            c2.setNombreCompleto("Juan Cliente");
            c2.setRol(Rol.CLIENTE);
            c2.setDni("66666666G");
            c2.setTelefono("666666666");
            c2.setDireccion("Avenida Blasco Ibañez 12, Valencia");
            c2 = clienteRepo.save(c2);

            // 6. TRABAJOS
            crearTrabajo(c1, CategoriaServicio.FONTANERIA, "Fuga en baño principal", "Hay una fuga de agua debajo del lavabo del baño principal.", "Calle Poeta Querol 3, Valencia");
            crearTrabajo(c1, CategoriaServicio.PINTURA, "Pintar salón completo", "El salón necesita una mano de pintura completa, paredes y techo.", "Calle Poeta Querol 3, Valencia");
            crearTrabajo(c1, CategoriaServicio.ELECTRICIDAD, "Enchufe roto en cocina", "El enchufe de la cocina no funciona, posible cortocircuito.", "Calle Poeta Querol 3, Valencia");
            crearTrabajo(c1, CategoriaServicio.ALBANILERIA, "Grieta en pared exterior", "Grieta de unos 30cm en la pared exterior del garaje.", "Calle Poeta Querol 3, Valencia");
            crearTrabajo(c2, CategoriaServicio.CLIMATIZACION, "Aire acondicionado no enfría", "El split del dormitorio no baja de 25 grados aunque esté al mínimo.", "Avenida Blasco Ibañez 12, Valencia");
        }

        private void crearTrabajo(Cliente c, CategoriaServicio cat, String titulo, String desc, String dir) {
            Trabajo t = new Trabajo();
            t.setCliente(c);
            t.setCategoria(cat);
            t.setTitulo(titulo);
            t.setDescripcion(desc);
            t.setDireccion(dir);
            t.setEstado(com.fixfinder.modelos.enums.EstadoTrabajo.PENDIENTE);
            t.setFechaCreacion(LocalDateTime.now());
            trabajoRepo.save(t);
        }
    }
}

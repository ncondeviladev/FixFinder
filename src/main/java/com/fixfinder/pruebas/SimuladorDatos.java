package com.fixfinder.pruebas;

import com.fixfinder.data.DataRepository;
import com.fixfinder.data.DataRepositoryImpl;
import com.fixfinder.data.interfaces.BaseDAO;
import com.fixfinder.data.interfaces.OperarioDAO;
import com.fixfinder.data.interfaces.UsuarioDAO;
import com.fixfinder.modelos.Operario;
import com.fixfinder.modelos.Usuario;
import com.fixfinder.modelos.enums.CategoriaServicio;
import com.fixfinder.modelos.enums.Rol;
import com.fixfinder.utilidades.DataAccessException;
import java.util.Random;

public class SimuladorDatos {

    private final UsuarioDAO usuarioDAO;
    private final OperarioDAO operarioDAO;
    private final Random random;

    public SimuladorDatos() {
        DataRepository repo = new DataRepositoryImpl();
        this.usuarioDAO = repo.getUsuarioDAO();
        this.operarioDAO = repo.getOperarioDAO();
        this.random = new Random();
    }

    public void generarClientes(int cantidad) {
        System.out.println("🤖 Generando " + cantidad + " clientes simulados...");
        for (int i = 0; i < cantidad; i++) {
            try {
                Usuario u = new Usuario();
                String id = String.valueOf(System.currentTimeMillis() + i);
                u.setNombreCompleto("Cliente Simulado " + i);
                u.setEmail("cliente" + id + "@test.com");
                u.setPasswordHash("pass123");
                u.setRol(Rol.CLIENTE);
                u.setIdEmpresa(1); // Asumimos empresa ID 1 existe

                usuarioDAO.insertar(u);
                System.out.println("   + Cliente creado: " + u.getEmail());
            } catch (DataAccessException e) {
                System.err.println("   - Error creando cliente: " + e.getMessage());
            }
        }
    }

    public void generarOperarios(int cantidad) {
        System.out.println("👷 Generando " + cantidad + " operarios simulados...");
        CategoriaServicio[] especialidades = CategoriaServicio.values();

        for (int i = 0; i < cantidad; i++) {
            try {
                Operario op = new Operario();
                String id = String.valueOf(System.currentTimeMillis() + i);

                // Datos Usuario
                op.setNombreCompleto("Técnico " + i);
                op.setEmail("tecnico" + id + "@fixfinder.com");
                op.setPasswordHash("pass123");
                op.setIdEmpresa(1);

                // Datos Operario
                op.setDni(id.substring(id.length() - 8) + "X");
                op.setEspecialidad(especialidades[random.nextInt(especialidades.length)]);
                op.setEstaActivo(true);
                // Coordenadas aleatorias cerca de Valencia (ejemplo)
                op.setLatitud(39.46 + (random.nextDouble() - 0.5) * 0.1);
                op.setLongitud(-0.37 + (random.nextDouble() - 0.5) * 0.1);

                operarioDAO.insertar(op);
                System.out
                        .println("   + Operario creado: " + op.getNombreCompleto() + " (" + op.getEspecialidad() + ")");
            } catch (DataAccessException e) {
                System.err.println("   - Error creando operario: " + e.getMessage());
            }
        }
    }
}

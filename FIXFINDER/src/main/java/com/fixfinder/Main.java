
package com.fixfinder;

import com.fixfinder.data.DataRepository;
import com.fixfinder.data.DataRepositoryImpl;
import com.fixfinder.data.interfaces.EmpresaDAO;
import com.fixfinder.data.interfaces.OperarioDAO;
import com.fixfinder.modelos.Empresa;
import com.fixfinder.modelos.Operario;
import com.fixfinder.modelos.enums.CategoriaServicio;
import com.fixfinder.utilidades.DataAccessException;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        System.out.println("🚀 Iniciando Test de DAOs...");

        DataRepository repo = new DataRepositoryImpl();
        EmpresaDAO empresaDAO = repo.getEmpresaDAO();
        OperarioDAO operarioDAO = repo.getOperarioDAO();

        try {
            // 1. Gestionar Empresa
            System.out.println("\n--- 1. Gestionando Empresa ---");
            Empresa miEmpresa = null;
            List<Empresa> existentes = empresaDAO.obtenerTodos();
            if (!existentes.isEmpty()) {
                miEmpresa = existentes.get(0);
                System.out.println("ℹ️ Usando empresa existente: " + miEmpresa.getNombre());
            } else {
                miEmpresa = new Empresa();
                miEmpresa.setNombre("Reparaciones Rápidas S.L.");
                miEmpresa.setCif("B12345678");
                miEmpresa.setEmailContacto("contacto@reparaciones.com");
                empresaDAO.insertar(miEmpresa);
                System.out.println("✅ Empresa insertada: " + miEmpresa.getId());
            }

            // 2. Insertar Operario (Transacción Compleja)
            System.out.println("\n--- 2. Insertando Operario (Transacción) ---");
            Operario op = new Operario();
            op.setNombreCompleto("Pepe Gotera");
            // Email y DNI aleatorios para evitar duplicados en tests
            long randomId = System.currentTimeMillis();
            op.setEmail("pepe" + randomId + "@reparaciones.com");
            op.setPasswordHash("hash123");
            op.setIdEmpresa(miEmpresa.getId());

            // Datos específicos de Operario
            op.setDni("DNI-" + (randomId % 10000));
            // FIXED: Usar Enum CategoriaServicio
            op.setEspecialidad(CategoriaServicio.FONTANERIA);
            op.setEstaActivo(true);
            op.setLatitud(40.416);
            op.setLongitud(-3.703);

            operarioDAO.insertar(op);
            System.out.println("✅ Operario insertado con ID: " + op.getId());

            // 3. Verificar Lectura (JOIN)
            System.out.println("\n--- 3. Verificando lectura Operario ---");
            Operario opLeido = operarioDAO.obtenerPorId(op.getId());
            if (opLeido != null) {
                System.out.println("📖 Operario leído: " + opLeido.getNombreCompleto());
                System.out.println("   - Especialidad: " + opLeido.getEspecialidad());
                System.out.println("   - Rol: " + opLeido.getRol());
                System.out.println("   - Coordenadas: " + opLeido.getLatitud() + ", " + opLeido.getLongitud());
            }

        } catch (DataAccessException e) {
            System.err.println("🔥 ERROR EN TEST:");
            e.printStackTrace();
        }
    }
}

package com.fixfinder.red.procesadores;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fixfinder.data.DataRepositoryImpl;
import com.fixfinder.data.interfaces.EmpresaDAO;
import com.fixfinder.modelos.Empresa;
import com.fixfinder.modelos.Operario;
import com.fixfinder.service.interfaz.OperarioService;
import com.fixfinder.utilidades.ServiceException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProcesadorUsuarios {

    private final OperarioService operarioService;
    private final EmpresaDAO empresaDAO;
    private final ObjectMapper mapper;

    public ProcesadorUsuarios(OperarioService operarioService) {
        this.operarioService = operarioService;
        this.empresaDAO = new DataRepositoryImpl().getEmpresaDAO();
        this.mapper = new ObjectMapper();
    }

    public void procesarListarOperarios(JsonNode datos, ObjectNode respuesta) {
        if (datos != null && datos.has("idEmpresa")) {
            try {
                int idEmpresa = datos.get("idEmpresa").asInt();
                System.out.println("[DEBUG-SERVER] Listando operarios para empresa ID: " + idEmpresa);

                List<Operario> lista = operarioService.listarPorEmpresa(idEmpresa);

                // Mappear a lista simple para JSON
                List<Map<String, Object>> listaSalida = new ArrayList<>();
                for (Operario op : lista) {
                    // Filtrar solo activos si se quiere, o todos. De momento todos.
                    if (!op.isEstaActivo())
                        continue;

                    Map<String, Object> map = new HashMap<>();
                    map.put("id", op.getId());
                    map.put("nombre", op.getNombreCompleto());
                    map.put("especialidad", op.getEspecialidad().toString());
                    listaSalida.add(map);
                }

                respuesta.put("status", 200);
                respuesta.put("mensaje", "Lista de operarios obtenida");
                respuesta.set("datos", mapper.valueToTree(listaSalida));

            } catch (ServiceException e) {
                respuesta.put("status", 500);
                respuesta.put("mensaje", "Error obteniendo operarios: " + e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
                respuesta.put("status", 500);
                respuesta.put("mensaje", "Error interno: " + e.getMessage());
            }
        } else {
            respuesta.put("status", 400);
            respuesta.put("mensaje", "Falta idEmpresa");
        }
    }

    public void procesarListarEmpresas(JsonNode datos, ObjectNode respuesta) {
        try {
            List<Empresa> lista = empresaDAO.obtenerTodos();
            List<Map<String, Object>> salida = new ArrayList<>();

            for (Empresa e : lista) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", e.getId());
                m.put("nombre", e.getNombre() != null ? e.getNombre() : "Empresa " + e.getId());
                salida.add(m);
            }

            respuesta.put("status", 200);
            respuesta.put("mensaje", "Lista de empresas obtenida");
            respuesta.set("datos", mapper.valueToTree(salida));

        } catch (Exception e) {
            e.printStackTrace();
            respuesta.put("status", 500);
            respuesta.put("mensaje", "Error listando empresas: " + e.getMessage());
        }
    }
}

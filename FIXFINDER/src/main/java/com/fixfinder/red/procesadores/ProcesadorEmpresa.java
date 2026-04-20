package com.fixfinder.red.procesadores;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fixfinder.data.ConexionDB;
import com.fixfinder.data.dao.TrabajoDAOImpl;
import com.fixfinder.data.interfaces.EmpresaDAO;
import com.fixfinder.data.interfaces.TrabajoDAO;
import com.fixfinder.data.dao.EmpresaDAOImpl;
import com.fixfinder.modelos.Empresa;
import com.fixfinder.modelos.Trabajo;
import com.fixfinder.service.interfaz.EmpresaService;
import com.fixfinder.utilidades.ServiceException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Procesador especializado en peticiones relacionadas con la gestión de empresas.
 * Desacopla la lógica de administración corporativa del procesamiento de usuarios.
 */
public class ProcesadorEmpresa {

    private final EmpresaService empresaService;
    private final EmpresaDAO empresaDAO;
    private final ObjectMapper mapper;

    public ProcesadorEmpresa(EmpresaService empresaService) {
        this.empresaService = empresaService;
        this.empresaDAO = new EmpresaDAOImpl();
        this.mapper = new ObjectMapper();
    }

    public void procesarListarEmpresas(JsonNode datos, ObjectNode respuesta) {
        try {
            List<Empresa> lista = empresaService.listarTodas();
            List<Map<String, Object>> salida = new ArrayList<>();

            for (Empresa e : lista) {
                salida.add(mapearEmpresaAMap(e, false));
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

    public void procesarObtenerEmpresa(JsonNode datos, ObjectNode respuesta) {
        if (datos != null && datos.has("idEmpresa")) {
            try {
                int id = datos.get("idEmpresa").asInt();
                Empresa e = empresaService.obtenerEstadisticas(id);
                if (e != null) {
                    Map<String, Object> m = mapearEmpresaAMap(e, true);
                    
                    // Lógica adicional para el gerente y valoraciones (movida de ProcesadorUsuarios)
                    enriquecerConGerenteYValoraciones(id, m);

                    respuesta.put("status", 200);
                    respuesta.put("mensaje", "Datos de empresa obtenidos");
                    respuesta.set("datos", mapper.valueToTree(m));
                } else {
                    respuesta.put("status", 404);
                    respuesta.put("mensaje", "Empresa no encontrada");
                }
            } catch (Exception e) {
                respuesta.put("status", 500);
                respuesta.put("mensaje", "Error: " + e.getMessage());
            }
        }
    }

    public void procesarModificarEmpresa(JsonNode datos, ObjectNode respuesta) {
        if (datos != null && datos.has("id")) {
            try {
                int id = datos.get("id").asInt();
                Empresa e = empresaDAO.obtenerPorId(id);
                
                if (e == null) throw new ServiceException("La empresa no existe.");

                if (datos.has("nombre")) e.setNombre(datos.get("nombre").asText());
                if (datos.has("cif")) e.setCif(datos.get("cif").asText());
                if (datos.has("email")) e.setEmailContacto(datos.get("email").asText());
                if (datos.has("telefono")) e.setTelefono(datos.get("telefono").asText());
                if (datos.has("direccion")) e.setDireccion(datos.get("direccion").asText());
                if (datos.has("url_foto")) e.setUrlFoto(datos.get("url_foto").asText());

                empresaService.modificarEmpresa(e);

                respuesta.put("status", 200);
                respuesta.put("mensaje", "Empresa actualizada correctamente");
            } catch (ServiceException e) {
                respuesta.put("status", 400);
                respuesta.put("mensaje", e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
                respuesta.put("status", 500);
                respuesta.put("mensaje", "Error interno al modificar empresa: " + e.getMessage());
            }
        } else {
            respuesta.put("status", 400);
            respuesta.put("mensaje", "Falta ID de empresa");
        }
    }

    private Map<String, Object> mapearEmpresaAMap(Empresa e, boolean detalleCompleto) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", e.getId());
        m.put("nombre", e.getNombre());
        m.put("cif", e.getCif());
        m.put("direccion", e.getDireccion());
        m.put("telefono", e.getTelefono());
        m.put("email", e.getEmailContacto());
        m.put("url_foto", e.getUrlFoto());
        if (detalleCompleto) {
            m.put("fechaAlta", e.getFechaAlta());
        }

        List<String> especialidadesStr = new ArrayList<>();
        if (e.getEspecialidades() != null) {
            for (var esp : e.getEspecialidades()) {
                especialidadesStr.add(esp.toString());
            }
        }
        m.put("especialidades", especialidadesStr);
        return m;
    }

    private void enriquecerConGerenteYValoraciones(int idEmpresa, Map<String, Object> m) {
        // Obtener foto del gerente
        try (Connection conn = ConexionDB.getConnection();
                PreparedStatement stmtG = conn.prepareStatement(
                        "SELECT u.url_foto FROM usuario u JOIN operario o ON u.id = o.id_usuario "
                                + "WHERE o.id_empresa = ? AND u.rol = 'GERENTE' LIMIT 1")) {
            stmtG.setInt(1, idEmpresa);
            try (ResultSet rsG = stmtG.executeQuery()) {
                if (rsG.next()) {
                    m.put("gerenteUrlFoto", rsG.getString("url_foto"));
                }
            }
        } catch (Exception ex) {
            System.err.println("Error enriqueciendo empresa con gerente: " + ex.getMessage());
        }

        // Obtener valoraciones
        try {
            TrabajoDAO trabajoDAO = new TrabajoDAOImpl();
            List<Trabajo> trabajosConValoracion = trabajoDAO.obtenerValoracionesPorEmpresa(idEmpresa);
            List<Map<String, Object>> valoraciones = new ArrayList<>();

            for (Trabajo t : trabajosConValoracion) {
                Map<String, Object> v = new HashMap<>();
                v.put("cliente", t.getCliente() != null ? t.getCliente().getNombreCompleto() : "Cliente Anónimo");
                v.put("puntos", t.getValoracion());
                v.put("comentario", t.getComentarioCliente());
                v.put("fecha", t.getFechaFinalizacion() != null ? t.getFechaFinalizacion().toString() : "Reciente");
                valoraciones.add(v);
            }
            m.put("valoraciones", valoraciones);
        } catch (Exception ex) {
            System.err.println("Error enriqueciendo empresa con valoraciones: " + ex.getMessage());
        }
    }
}

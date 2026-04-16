package com.fixfinder.red.procesadores;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fixfinder.data.ConexionDB;
import com.fixfinder.data.dao.EmpresaDAOImpl;
import com.fixfinder.data.dao.OperarioDAOImpl;
import com.fixfinder.data.dao.TrabajoDAOImpl;
import com.fixfinder.data.dao.UsuarioDAOImpl;
import com.fixfinder.data.interfaces.EmpresaDAO;
import com.fixfinder.data.interfaces.OperarioDAO;
import com.fixfinder.data.interfaces.TrabajoDAO;
import com.fixfinder.data.interfaces.UsuarioDAO;
import com.fixfinder.modelos.Empresa;
import com.fixfinder.modelos.Operario;
import com.fixfinder.modelos.Trabajo;
import com.fixfinder.modelos.Usuario;
import com.fixfinder.modelos.enums.CategoriaServicio;
import com.fixfinder.service.interfaz.OperarioService;
import com.fixfinder.service.interfaz.UsuarioService;
import com.fixfinder.service.impl.UsuarioServiceImpl;
import com.fixfinder.utilidades.ServiceException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProcesadorUsuarios {

    private final OperarioService operarioService;
    private final UsuarioService usuarioService;
    private final EmpresaDAO empresaDAO;
    private final OperarioDAO operarioDAO;
    private final ObjectMapper mapper;

    public ProcesadorUsuarios(OperarioService operarioService) {
        this.operarioService = operarioService;
        this.usuarioService = new UsuarioServiceImpl();
        this.empresaDAO = new EmpresaDAOImpl();
        this.operarioDAO = new OperarioDAOImpl();
        this.mapper = new ObjectMapper();
    }

    public void procesarListarOperarios(JsonNode datos, ObjectNode respuesta) {
        if (datos != null && datos.has("idEmpresa")) {
            try {
                int idEmpresa = datos.get("idEmpresa").asInt();
                System.out.println("[DEBUG-SERVER] Listando operarios para empresa ID: " + idEmpresa);

                List<Operario> lista = operarioService.listarPorEmpresa(idEmpresa);

                List<Map<String, Object>> listaSalida = new ArrayList<>();
                for (Operario op : lista) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", op.getId());
                    map.put("nombre", op.getNombreCompleto());
                    map.put("email", op.getEmail());
                    map.put("telefono", op.getTelefono());
                    map.put("dni", op.getDni());
                    map.put("especialidad", op.getEspecialidad().toString());
                    map.put("estaActivo", op.isEstaActivo());
                    map.put("url_foto", op.getUrlFoto());
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
                m.put("cif", e.getCif());
                m.put("direccion", e.getDireccion());
                m.put("telefono", e.getTelefono());
                m.put("email", e.getEmailContacto());
                m.put("url_foto", e.getUrlFoto());

                List<String> especialidadesStr = new ArrayList<>();
                if (e.getEspecialidades() != null) {
                    for (var esp : e.getEspecialidades()) {
                        especialidadesStr.add(esp.toString());
                    }
                }
                m.put("especialidades", especialidadesStr);
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

    public void procesarObtenerEmpresa(JsonNode datos, ObjectNode respuesta) {
        if (datos != null && datos.has("idEmpresa")) {
            try {
                int id = datos.get("idEmpresa").asInt();
                Empresa e = empresaDAO.obtenerPorId(id);
                if (e != null) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", e.getId());
                    m.put("nombre", e.getNombre());
                    m.put("cif", e.getCif());
                    m.put("direccion", e.getDireccion());
                    m.put("telefono", e.getTelefono());
                    m.put("email", e.getEmailContacto());
                    m.put("url_foto", e.getUrlFoto());
                    m.put("fechaAlta", e.getFechaAlta());

                    // Obtener foto del gerente con una consulta SQL directa
                    try (Connection conn = ConexionDB.getConnection();
                            PreparedStatement stmtG = conn.prepareStatement(
                                    "SELECT u.url_foto FROM usuario u JOIN operario o ON u.id = o.id_usuario "
                                            + "WHERE o.id_empresa = ? AND u.rol = 'GERENTE' LIMIT 1")) {
                        stmtG.setInt(1, id);
                        try (ResultSet rsG = stmtG.executeQuery()) {
                            if (rsG.next()) {
                                m.put("gerenteUrlFoto", rsG.getString("url_foto"));
                            }
                        }
                    } catch (Exception ex_g) {
                        System.err.println("Error buscando foto de gerente: " + ex_g.getMessage());
                    }

                    // Obtener valoraciones reales de trabajos FINALIZADOS mediante consulta
                    // optimizada
                    try {
                        TrabajoDAO trabajoDAO = new TrabajoDAOImpl();
                        List<Trabajo> trabajosConValoracion = trabajoDAO.obtenerValoracionesPorEmpresa(id);
                        List<Map<String, Object>> valoraciones = new ArrayList<>();

                        for (Trabajo t : trabajosConValoracion) {
                            Map<String, Object> v = new HashMap<>();
                            v.put("cliente",
                                    t.getCliente() != null ? t.getCliente().getNombreCompleto() : "Cliente Anónimo");
                            v.put("puntos", t.getValoracion());
                            v.put("comentario", t.getComentarioCliente());
                            v.put("fecha", t.getFechaFinalizacion() != null ? t.getFechaFinalizacion().toString()
                                    : "Reciente");
                            valoraciones.add(v);
                        }
                        m.put("valoraciones", valoraciones);
                    } catch (Exception ex_v) {
                        System.err.println("Error cargando valoraciones optimizadas: " + ex_v.getMessage());
                    }

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

    public void procesarModificarOperario(JsonNode datos, ObjectNode respuesta) {
        if (datos != null && (datos.has("id") || datos.has("idUsuario"))) {
            try {
                int id = datos.has("id") ? datos.get("id").asInt() : datos.get("idUsuario").asInt();

                Operario op = operarioDAO.obtenerPorId(id);
                if (op == null)
                    throw new ServiceException("Operario no existe");

                if (datos.has("nombre"))
                    op.setNombreCompleto(datos.get("nombre").asText());
                if (datos.has("dni"))
                    op.setDni(datos.get("dni").asText());
                if (datos.has("email"))
                    op.setEmail(datos.get("email").asText());
                if (datos.has("telefono")) {
                    String tel = datos.get("telefono").asText().replaceAll("[^0-9]", "");
                    op.setTelefono(tel);
                }
                if (datos.has("especialidad")) {
                    try {
                        op.setEspecialidad(CategoriaServicio.valueOf(datos.get("especialidad").asText()));
                    } catch (Exception ignored) {
                    }
                }
                if (datos.has("estaActivo"))
                    op.setEstaActivo(datos.get("estaActivo").asBoolean());

                operarioService.modificarOperario(op);

                respuesta.put("status", 200);
                respuesta.put("mensaje", "Operario modificado correctamente");
            } catch (ServiceException e) {
                respuesta.put("status", 400);
                respuesta.put("mensaje", e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
                respuesta.put("status", 500);
                respuesta.put("mensaje", "Error interno al modificar: " + e.getMessage());
            }
        } else {
            respuesta.put("status", 400);
            respuesta.put("mensaje", "Falta ID");
        }
    }

    public void procesarActualizarFotoPerfil(JsonNode datos, ObjectNode respuesta) {
        if (datos != null && datos.has("idUsuario") && datos.has("url_foto")) {
            try {
                int idUsuario = datos.get("idUsuario").asInt();
                String urlFoto = datos.get("url_foto").asText();

                UsuarioDAO usuarioDAO = new UsuarioDAOImpl();
                Usuario u = usuarioDAO.obtenerPorId(idUsuario);

                if (u == null)
                    throw new Exception("Usuario no encontrado");

                u.setUrlFoto(urlFoto);
                usuarioDAO.actualizar(u);

                respuesta.put("status", 200);
                respuesta.put("mensaje", "Foto de perfil actualizada correctamente");
            } catch (Exception e) {
                e.printStackTrace();
                respuesta.put("status", 500);
                respuesta.put("mensaje", "Error al actualizar foto: " + e.getMessage());
            }
        } else {
            respuesta.put("status", 400);
            respuesta.put("mensaje", "Datos incompletos para actualizar foto");
        }
    }

    public void procesarModificarUsuario(JsonNode datos, ObjectNode respuesta) {
        if (datos != null && datos.has("id")) {
            try {
                int id = datos.get("id").asInt();
                Usuario u = usuarioService.obtenerPorId(id);

                if (datos.has("nombre"))
                    u.setNombreCompleto(datos.get("nombre").asText());
                if (datos.has("email"))
                    u.setEmail(datos.get("email").asText());
                if (datos.has("telefono"))
                    u.setTelefono(datos.get("telefono").asText());
                if (datos.has("direccion"))
                    u.setDireccion(datos.get("direccion").asText());
                if (datos.has("url_foto"))
                    u.setUrlFoto(datos.get("url_foto").asText());

                usuarioService.modificarUsuario(u);

                respuesta.put("status", 200);
                respuesta.put("mensaje", "Datos actualizados correctamente");
            } catch (ServiceException e) {
                respuesta.put("status", 400);
                respuesta.put("mensaje", e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
                respuesta.put("status", 500);
                respuesta.put("mensaje", "Error interno al actualizar perfil");
            }
        } else {
            respuesta.put("status", 400);
            respuesta.put("mensaje", "Falta ID de usuario");
        }
    }
}

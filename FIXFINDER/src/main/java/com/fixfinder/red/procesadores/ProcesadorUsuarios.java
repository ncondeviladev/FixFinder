package com.fixfinder.red.procesadores;

import com.fixfinder.red.Broadcaster;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fixfinder.data.dao.OperarioDAOImpl;
import com.fixfinder.data.dao.UsuarioDAOImpl;
import com.fixfinder.data.interfaces.OperarioDAO;
import com.fixfinder.data.interfaces.UsuarioDAO;
import com.fixfinder.modelos.Operario;
import com.fixfinder.modelos.Usuario;
import com.fixfinder.modelos.enums.CategoriaServicio;
import com.fixfinder.service.interfaz.OperarioService;
import com.fixfinder.service.interfaz.UsuarioService;
import com.fixfinder.service.impl.UsuarioServiceImpl;
import com.fixfinder.utilidades.ServiceException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProcesadorUsuarios {

    private final OperarioService operarioService;
    private final UsuarioService usuarioService;
    private final OperarioDAO operarioDAO;
    private final ObjectMapper mapper;

    public ProcesadorUsuarios(OperarioService operarioService) {
        this.operarioService = operarioService;
        this.usuarioService = new UsuarioServiceImpl();
        this.operarioDAO = new OperarioDAOImpl();
        this.mapper = new ObjectMapper();
    }

    public void procesarListarOperarios(JsonNode datos, ObjectNode respuesta) {
        if (datos != null && datos.has("idEmpresa")) {
            try {
                int idEmpresa = datos.get("idEmpresa").asInt();

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

                // BROADCAST: Sincronizar cambios en el personal (Solo a su empresa)
                Broadcaster.getInstancia().difundirEventoOperario("MODIFICACION", id, op.getIdEmpresa(), "Datos de operario actualizados");
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

                // BROADCAST: Cambio silencioso de foto de perfil
                Broadcaster.getInstancia().difundirEventoUsuario("FOTO", idUsuario, "Foto de perfil actualizada", urlFoto, u.getNombreCompleto());
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
                
                // BROADCAST: Notificar cambio de nombre/datos silenciosamente
                Broadcaster.getInstancia().difundirEventoUsuario("DATOS", id, "Perfil actualizado", u.getUrlFoto(), u.getNombreCompleto());
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

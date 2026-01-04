package com.fixfinder.red.procesadores;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fixfinder.modelos.Cliente;
import com.fixfinder.modelos.Empresa;
import com.fixfinder.modelos.Operario;
import com.fixfinder.modelos.Usuario;
import com.fixfinder.modelos.enums.CategoriaServicio;
import com.fixfinder.modelos.enums.Rol;
import com.fixfinder.service.interfaz.EmpresaService;
import com.fixfinder.service.interfaz.UsuarioService;
import com.fixfinder.utilidades.ServiceException;
import java.util.ArrayList;

public class ProcesadorAutenticacion {

    private final UsuarioService usuarioService;
    private final EmpresaService empresaService;

    public ProcesadorAutenticacion(UsuarioService usuarioService, EmpresaService empresaService) {
        this.usuarioService = usuarioService;
        this.empresaService = empresaService;
    }

    public void procesarLogin(JsonNode datos, ObjectNode respuesta) {
        if (datos != null && datos.has("email") && datos.has("password")) {
            String email = datos.get("email").asText();
            String password = datos.get("password").asText();

            try {
                Usuario usuario = usuarioService.login(email, password);

                System.out.println("[LOGIN-DEBUG] PENDIENTE DE ENVIO: " + email + " (" + usuario.getRol() + ")");
                respuesta.put("status", 200);
                respuesta.put("mensaje", "Login correcto");

                ObjectNode datosUsuario = respuesta.putObject("datos");
                datosUsuario.put("id", usuario.getId());
                datosUsuario.put("nombreCompleto", usuario.getNombreCompleto());
                datosUsuario.put("email", usuario.getEmail());
                datosUsuario.put("rol", usuario.getRol().name());
                if (usuario.getFechaRegistro() != null) {
                    datosUsuario.put("fechaRegistro", usuario.getFechaRegistro().toString());
                }

                if (usuario instanceof Operario) {
                    datosUsuario.put("idEmpresa", ((Operario) usuario).getIdEmpresa());
                }
                System.out.println("[LOGIN-OK] " + email);

            } catch (ServiceException e) {
                if (e.getMessage().equals("Contraseña incorrecta")
                        || e.getMessage().equals("Usuario no encontrado")) {
                    System.out.println("[LOGIN-FALLIDO] " + email + ": " + e.getMessage());
                    respuesta.put("status", 401);
                    respuesta.put("mensaje", "Credenciales incorrectas");
                } else {
                    System.err.println("[ERROR-LOGIN] " + email + ": " + e.getMessage());
                    respuesta.put("status", 500);
                    respuesta.put("mensaje", "Error interno del servidor");
                }
            } catch (Exception e) {
                System.err.println("Error no controlado en login: " + e.getMessage());
                respuesta.put("status", 500);
                respuesta.put("mensaje", "Error interno del servidor");
            }
        } else {
            respuesta.put("status", 400);
            respuesta.put("mensaje", "Faltan datos de login");
        }
    }

    public void procesarRegistro(JsonNode datos, ObjectNode respuesta) {
        String tipo = "CLIENTE";
        if (datos != null && datos.has("tipo")) {
            tipo = datos.get("tipo").asText().toUpperCase();
        }

        if (tipo.equals("EMPRESA")) {
            if (datos != null) {
                try {
                    Empresa nuevaEmpresa = new Empresa();
                    nuevaEmpresa.setNombre(datos.has("nombreEmpresa") ? datos.get("nombreEmpresa").asText() : "");
                    nuevaEmpresa.setCif(datos.has("cif") ? datos.get("cif").asText() : "");
                    nuevaEmpresa.setEmailContacto(datos.has("emailEmpresa") ? datos.get("emailEmpresa").asText() : "");
                    nuevaEmpresa.setTelefono(datos.has("telefono") ? datos.get("telefono").asText() : "");
                    nuevaEmpresa.setDireccion(datos.has("direccion") ? datos.get("direccion").asText() : "");
                    nuevaEmpresa.setUrlFoto(datos.has("urlFoto") ? datos.get("urlFoto").asText() : "");
                    nuevaEmpresa.setEspecialidades(new ArrayList<>());

                    empresaService.registrarEmpresa(nuevaEmpresa);

                    Operario gerente = new Operario();
                    gerente.setNombreCompleto(datos.has("nombreGerente") ? datos.get("nombreGerente").asText() : "");
                    gerente.setEmail(datos.has("emailGerente") ? datos.get("emailGerente").asText() : "");
                    gerente.setPasswordHash(
                            datos.has("password") ? datos.get("password").asText()
                                    : (datos.has("passwordGerente") ? datos.get("passwordGerente").asText()
                                            : ""));
                    gerente.setDni(datos.has("dniGerente") ? datos.get("dniGerente").asText() : "");
                    gerente.setTelefono(
                            datos.has("telefonoGerente") ? datos.get("telefonoGerente").asText() : "");

                    gerente.setRol(Rol.GERENTE);
                    gerente.setIdEmpresa(nuevaEmpresa.getId());
                    gerente.setEstaActivo(true);
                    gerente.setEspecialidad(CategoriaServicio.ELECTRICIDAD);

                    usuarioService.registrarUsuario(gerente);

                    System.out.println("[REGISTRO-EMPRESA] " + nuevaEmpresa.getNombre() + " (Gerente: "
                            + gerente.getEmail() + ")");
                    respuesta.put("status", 200);
                    respuesta.put("mensaje", "Empresa y gerente registrado correctamente");

                } catch (Exception e) {
                    String errorMsg = e.getMessage()
                            + (e.getCause() != null ? " -> " + e.getCause().getMessage() : "");
                    System.err.println("[ERROR-REGISTRO-EMPRESA] " + errorMsg);
                    respuesta.put("status", 500);
                    respuesta.put("mensaje", "Error al registrar empresa y gerente");
                }
            } else {
                respuesta.put("status", 400);
                respuesta.put("mensaje", "Datos incompletos para REGISTRO");
            }

        } else if (tipo.equals("OPERARIO")) {
            if (datos != null) {
                try {
                    Operario nuevoOperario = new Operario();
                    nuevoOperario.setNombreCompleto(
                            datos.has("nombreOperario") ? datos.get("nombreOperario").asText() : "");
                    nuevoOperario.setDni(
                            datos.has("dniOperario") ? datos.get("dniOperario").asText() : "");
                    nuevoOperario.setEmail(
                            datos.has("emailOperario") ? datos.get("emailOperario").asText() : "");
                    nuevoOperario.setPasswordHash(
                            datos.has("passwordOperario") ? datos.get("passwordOperario").asText() : "");
                    nuevoOperario.setTelefono(
                            datos.has("telefonoOperario") ? datos.get("telefonoOperario").asText() : "");

                    if (datos.has("idEmpresa")) {
                        nuevoOperario.setIdEmpresa(datos.get("idEmpresa").asInt());
                    } else {
                        throw new ServiceException("Falta idEmpresa para crear operario");
                    }

                    String especialidadStr = datos.has("especialidad") ? datos.get("especialidad").asText()
                            : "ELECTRICIDAD";
                    try {
                        nuevoOperario.setEspecialidad(CategoriaServicio.valueOf(especialidadStr.toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        nuevoOperario.setEspecialidad(CategoriaServicio.ELECTRICIDAD);
                    }

                    nuevoOperario.setRol(Rol.OPERARIO);
                    // No usamos setEsGerente porque no existe, y por defecto será Rol.OPERARIO
                    nuevoOperario.setEstaActivo(true);

                    usuarioService.registrarUsuario(nuevoOperario);

                    System.out.println("[REGISTRO-OPERARIO] " + nuevoOperario.getEmail()
                            + " (Empresa ID: " + nuevoOperario.getIdEmpresa() + ")");
                    respuesta.put("status", 200);
                    respuesta.put("mensaje", "Operario registrado correctamente");
                } catch (Exception e) {
                    String errorMsg = e.getMessage();
                    System.err.println("Error registro operario: " + errorMsg);
                    respuesta.put("status", 500);
                    if (errorMsg != null && errorMsg.contains("Duplicate entry")) {
                        respuesta.put("mensaje", "El email o DNI ya está registrado");
                    } else {
                        respuesta.put("mensaje", "Error al registrar operario: " + errorMsg);
                    }
                }
            }

        } else if (tipo.equals("CLIENTE")) {
            if (datos != null) {
                try {
                    Cliente nuevoCliente = new Cliente();
                    nuevoCliente.setNombreCompleto(datos.has("nombre") ? datos.get("nombre").asText() : "");
                    if (datos.has("apellidos")) {
                        nuevoCliente.setNombreCompleto(
                                nuevoCliente.getNombreCompleto() + " " + datos.get("apellidos").asText());
                    }

                    nuevoCliente.setDni(datos.has("dni") ? datos.get("dni").asText() : "");
                    nuevoCliente.setEmail(datos.has("email") ? datos.get("email").asText() : "");
                    nuevoCliente.setPasswordHash(
                            datos.has("password") ? datos.get("password").asText() : "");
                    nuevoCliente.setTelefono(
                            datos.has("telefono") ? datos.get("telefono").asText() : "");
                    nuevoCliente.setDireccion(
                            datos.has("direccion") ? datos.get("direccion").asText() : "");

                    nuevoCliente.setRol(Rol.CLIENTE);
                    usuarioService.registrarUsuario(nuevoCliente);

                    System.out.println("[REGISTRO-CLIENTE] " + nuevoCliente.getEmail() + " (ID: "
                            + nuevoCliente.getId() + ")");
                    respuesta.put("status", 201);
                    respuesta.put("mensaje", "Cliente registrado OK. ID: " + nuevoCliente.getId());
                } catch (Exception e) {
                    String errorMsg = e.getMessage();
                    respuesta.put("status", 500);
                    if (errorMsg != null && errorMsg.contains("Duplicate entry")) {
                        respuesta.put("mensaje", "El email o DNI ya está registrado");
                    } else {
                        respuesta.put("mensaje", "Error al registrar cliente: " + errorMsg);
                    }
                }
            }
        } else {
            respuesta.put("status", 400);
            respuesta.put("mensaje", "Tipo de registro no válido");
        }
    }
}

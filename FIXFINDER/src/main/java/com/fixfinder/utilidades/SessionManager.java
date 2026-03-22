package com.fixfinder.utilidades;

/**
 * Singleton para gestionar la sesión del usuario logueado en el Dashboard.
 * Centraliza el ID, nombre, rol e id de empresa.
 */
public class SessionManager {

    private static SessionManager instance;

    private Integer usuarioId;
    private String nombre;
    private String rol;
    private Integer idEmpresa;

    private SessionManager() {}

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void setSession(int id, String nombre, String rol, Integer idEmpresa) {
        this.usuarioId = id;
        this.nombre = nombre;
        this.rol = rol;
        this.idEmpresa = idEmpresa;
    }

    public void clear() {
        this.usuarioId = null;
        this.nombre = null;
        this.rol = null;
        this.idEmpresa = null;
    }

    public boolean isLogged() {
        return usuarioId != null;
    }

    public Integer getUsuarioId() { return usuarioId; }
    public String getNombre() { return nombre; }
    public String getRol() { return rol; }
    public Integer getIdEmpresa() { return idEmpresa; }

    public boolean isGerente() {
        return "GERENTE".equalsIgnoreCase(rol);
    }

    public boolean isCliente() {
        return "CLIENTE".equalsIgnoreCase(rol);
    }
}

package com.fixfinder.modelos;

import com.fixfinder.modelos.enums.Rol;
import java.time.LocalDateTime;

/**
 * Clase base que representa a cualquier usuario del sistema.
 *
 * Esta clase es utilizada directamente para representar a:
 * - CLIENTES: Usuarios que solicitan servicios.
 * - ADMINS/GERENTES: Usuarios de gestión.
 *
 * Para los técnicos, se utiliza la subclase {@link Operario}.
 */
public class Usuario {
    protected int id;
    protected String nombreCompleto;
    protected String email;
    protected String passwordHash;
    /**
     * Rol del usuario en el sistema (ADMIN, GERENTE, OPERARIO, CLIENTE).
     */
    protected Rol rol;
    protected int idEmpresa;
    protected LocalDateTime fechaRegistro;
    protected String telefono;
    protected String direccion;
    protected String urlFoto;

    public Usuario() {
    }

    public Usuario(int id, String email, String passwordHash, Rol rol, int idEmpresa) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.rol = rol;
        this.idEmpresa = idEmpresa;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    public int getIdEmpresa() {
        return idEmpresa;
    }

    public void setIdEmpresa(int idEmpresa) {
        this.idEmpresa = idEmpresa;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getUrlFoto() {
        return urlFoto;
    }

    public void setUrlFoto(String urlFoto) {
        this.urlFoto = urlFoto;
    }
}

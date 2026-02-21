package com.fixfinder.modelos;

import com.fixfinder.modelos.enums.Rol;
import java.time.LocalDateTime;

/**
 * Clase base que representa a cualquier usuario del sistema.
 *
 * Contiene los atributos comunes: ID, credenciales, datos personales básicos.
 * Es extendida por {@link Operario} y {@link Cliente}.
 */
public abstract class Usuario {
    protected int id;
    protected String nombreCompleto;
    protected String email;
    protected String passwordHash;
    protected String urlFoto;
    protected LocalDateTime fechaRegistro;
    protected String telefono;
    protected String direccion;
    protected String dni;

    /**
     * Rol del usuario en el sistema (ADMIN, GERENTE, OPERARIO, CLIENTE).
     */
    protected Rol rol;

    public Usuario() {
    }

    // Constructor completo con todos los campos comunes
    protected Usuario(int id, String email, String passwordHash, Rol rol, String nombreCompleto, String dni) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.rol = rol;
        this.nombreCompleto = nombreCompleto;
        this.dni = dni;
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

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }
}

package com.fixfinder.modelos;

import com.fixfinder.modelos.enums.CategoriaServicio;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa a la entidad proveedora de servicios (Multi-tenant).
 *
 * En este sistema, los usuarios pertenecen a una empresa.
 * Esto permite que el software sea utilizado por múltiples compañías de
 * reparaciones
 * de forma aislada (SaaS).
 */
public class Empresa {
    private int id;
    private String nombre;
    private String cif;
    private String direccion;
    private String telefono;
    private String emailContacto;

    // Lista de servicios que ofrece la empresa (Multiservicio)
    private List<CategoriaServicio> especialidades = new ArrayList<>();

    public Empresa() {
    }

    public Empresa(int id, String nombre, String cif) {
        this.id = id;
        this.nombre = nombre;
        this.cif = cif;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCif() {
        return cif;
    }

    public void setCif(String cif) {
        this.cif = cif;
    }

    public List<CategoriaServicio> getEspecialidades() {
        return especialidades;
    }

    public void setEspecialidades(List<CategoriaServicio> especialidades) {
        this.especialidades = especialidades;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getEmailContacto() {
        return emailContacto;
    }

    public void setEmailContacto(String emailContacto) {
        this.emailContacto = emailContacto;
    }
}

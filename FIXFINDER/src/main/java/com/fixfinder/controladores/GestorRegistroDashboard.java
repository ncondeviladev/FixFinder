package com.fixfinder.controladores;

import java.io.IOException;

/**
 * Clase encargada de gestionar las peticiones de registro desde el Dashboard.
 */
public class GestorRegistroDashboard {

    private final DashboardController controller;

    public GestorRegistroDashboard(DashboardController controller) {
        this.controller = controller;
    }

    public void registrarEmpresa(String nombre, String cif, String email, String dir, 
                                 String nomGer, String emGer, String passGer, String dniGer) {
        try {
            controller.getServicioCliente().enviarRegistroEmpresa(
                    nombre, cif, email, dir, nomGer, emGer, passGer, dniGer,
                    null, null, null);
            controller.log("Enviada solicitud de REGISTRO EMPRESA");
        } catch (IOException e) {
            controller.log("Error en registro empresa: " + e.getMessage());
        }
    }

    public void registrarUsuario(boolean esOperario, String nombre, String dni, String email, 
                                 String pass, String tel, String dir, String idEmp) {
        try {
            controller.getServicioCliente().enviarRegistroUsuario(
                    esOperario, nombre, dni, email, pass, tel, dir, idEmp, "", null);
            controller.log("Enviada solicitud de REGISTRO USUARIO (" + (esOperario ? "OP" : "CLI") + ")");
        } catch (IOException e) {
            controller.log("Error en registro usuario: " + e.getMessage());
        }
    }
}

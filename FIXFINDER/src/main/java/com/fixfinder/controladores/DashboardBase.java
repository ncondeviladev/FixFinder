package com.fixfinder.controladores;

import com.fixfinder.ui.dashboard.TrabajoModelo;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

/**
 * Clase base para DashboardController que contiene todas las declaraciones @FXML.
 * Esto ayuda a reducir el tamaño del controlador principal.
 */
public abstract class DashboardBase {

    @FXML protected Button btnConectar;
    @FXML protected Button btnPing;
    @FXML protected Button btnAbrirSimulador;
    @FXML protected TextArea txtLog;
    @FXML protected TextField txtStatus;

    // Campos Registro Empresa
    @FXML protected TextField txtNombreEmpresa;
    @FXML protected TextField txtCif;
    @FXML protected TextField txtEmailEmpresa;
    @FXML protected TextField txtDireccionEmpresa;
    @FXML protected TextField txtNombreGerente;
    @FXML protected TextField txtEmailGerente;
    @FXML protected PasswordField txtPassGerente;
    @FXML protected TextField txtDniGerente;
    @FXML protected Button btnRegEmpresa;
    @FXML protected Label lblStatusEmpresa;

    // Campos Registro Usuario Generico (Cli/Op)
    @FXML protected RadioButton rbCliente;
    @FXML protected RadioButton rbOperario;
    @FXML protected TextField txtNombreUsuario;
    @FXML protected TextField txtDniUsuario;
    @FXML protected TextField txtEmailUsuario;
    @FXML protected PasswordField txtPassUsuario;
    @FXML protected TextField txtTelefonoUsuario;
    @FXML protected TextField txtDireccionUsuario;
    @FXML protected HBox boxIdEmpresa;
    @FXML protected TextField txtIdEmpresaOperario;
    @FXML protected Button btnRegUsuario;
    @FXML protected Label lblStatusUsuario;

    // Campos Login
    @FXML protected TextField txtLoginEmail;
    @FXML protected PasswordField txtLoginPassword;
    @FXML protected Button btnLogin;
    @FXML protected Button btnLogout;
    @FXML protected Label lblUsuarioLogueado;
    @FXML protected Label lblStatusLogin;

    // Campos Crear Trabajo
    @FXML protected TextField txtTituloTrabajo;
    @FXML protected TextArea txtDescripcionTrabajo;
    @FXML protected TextField txtDireccionTrabajo;
    @FXML protected ComboBox<String> comboCategoria;
    @FXML protected ComboBox<String> comboUrgencia;
    @FXML protected Button btnCrearTrabajo;
    @FXML protected Label lblStatusTrabajo;

    // CAMPOS TABLA TRABAJOS
    @FXML protected Button btnRefrescarTrabajos;
    @FXML protected TableView<TrabajoModelo> tablaTrabajos;
    @FXML protected TableColumn<TrabajoModelo, Integer> colIdTrabajo;
    @FXML protected TableColumn<TrabajoModelo, String> colTituloTrabajo;
    @FXML protected TableColumn<TrabajoModelo, String> colClienteTrabajo;
    @FXML protected TableColumn<TrabajoModelo, String> colOperarioTrabajo;
    @FXML protected TableColumn<TrabajoModelo, String> colEstadoTrabajo;
    @FXML protected TableColumn<TrabajoModelo, String> colFechaTrabajo;
}

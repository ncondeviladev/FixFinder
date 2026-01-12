package com.fixfinder.controladores;

import com.fasterxml.jackson.databind.JsonNode;
import com.fixfinder.cliente.ServicioCliente;
import com.fixfinder.cliente.RespuestaServidor;
import com.fixfinder.utilidades.ClienteException;
import java.io.IOException;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import java.util.List;
import java.util.ArrayList;

public class DashboardController {

    @FXML
    private Button btnConectar;
    @FXML
    private Button btnPing;
    @FXML
    private Button btnAbrirSimulador;
    @FXML
    private TextArea txtLog;
    @FXML
    private TextField txtStatus;

    // Campos Registro Empresa
    @FXML
    private TextField txtNombreEmpresa;
    @FXML
    private TextField txtCif;
    @FXML
    private TextField txtEmailEmpresa;
    @FXML
    private TextField txtDireccionEmpresa;
    @FXML
    private TextField txtNombreGerente;
    @FXML
    private TextField txtEmailGerente;
    @FXML
    private PasswordField txtPassGerente;
    @FXML
    private TextField txtDniGerente;
    @FXML
    private Button btnRegEmpresa;
    @FXML
    private Label lblStatusEmpresa;

    // Campos Registro Usuario Generico (Cli/Op)
    @FXML
    private RadioButton rbCliente;
    @FXML
    private RadioButton rbOperario;
    @FXML
    private TextField txtNombreUsuario;
    @FXML
    private TextField txtDniUsuario;
    @FXML
    private TextField txtEmailUsuario;
    @FXML
    private PasswordField txtPassUsuario;
    @FXML
    private TextField txtTelefonoUsuario;
    @FXML
    private TextField txtDireccionUsuario;

    @FXML
    private HBox boxIdEmpresa;
    @FXML
    private TextField txtIdEmpresaOperario;
    @FXML
    private Button btnRegUsuario;
    @FXML
    private Label lblStatusUsuario;

    // Campos Login
    @FXML
    private TextField txtLoginEmail;
    @FXML
    private PasswordField txtLoginPassword;
    @FXML
    private Button btnLogin;
    @FXML
    private Button btnLogout;
    @FXML
    private Label lblUsuarioLogueado;
    @FXML
    private Label lblStatusLogin;

    // Campos Crear Trabajo
    @FXML
    private TextField txtTituloTrabajo;
    @FXML
    private TextArea txtDescripcionTrabajo;
    @FXML
    private TextField txtDireccionTrabajo;
    @FXML
    private javafx.scene.control.ComboBox<String> comboCategoria;
    @FXML
    private javafx.scene.control.ComboBox<String> comboUrgencia;
    @FXML
    private Button btnCrearTrabajo;
    @FXML
    private Label lblStatusTrabajo;

    // CAMPOS TABLA TRABAJOS
    @FXML
    private Button btnRefrescarTrabajos;
    @FXML
    private TableView<TrabajoModelo> tablaTrabajos;
    @FXML
    private TableColumn<TrabajoModelo, Integer> colIdTrabajo;
    @FXML
    private TableColumn<TrabajoModelo, String> colTituloTrabajo;
    @FXML
    private TableColumn<TrabajoModelo, String> colClienteTrabajo;
    @FXML
    private TableColumn<TrabajoModelo, String> colOperarioTrabajo;
    @FXML
    private TableColumn<TrabajoModelo, String> colEstadoTrabajo;
    @FXML
    private TableColumn<TrabajoModelo, String> colFechaTrabajo;

    // Variables de estado para guardar la sesión en memoria
    private Integer usuarioLogueadoId = null;
    private String usuarioLogueadoNombre = null;
    private String usuarioLogueadoRol = null;
    private Integer usuarioLogueadoIdEmpresa = null;

    // Cache de operarios para asignación (solo Gerentes)
    private List<OperarioModelo> listaOperarios = new ArrayList<>();

    private ServicioCliente servicio;
    private final ToggleGroup tipoUsuarioGroup = new ToggleGroup();

    public void initialize() {
        servicio = new ServicioCliente();

        // Configurar RadioButtons
        if (rbCliente != null && rbOperario != null) {
            rbCliente.setToggleGroup(tipoUsuarioGroup);
            rbOperario.setToggleGroup(tipoUsuarioGroup);
        }

        // Inicializar ComboBox Categorias
        if (comboCategoria != null) {
            comboCategoria.getItems().addAll("FONTANERIA", "ELECTRICIDAD", "ALBAÑILERIA", "CARPINTERIA", "PINTURA",
                    "LIMPIEZA", "OTROS");
            comboCategoria.getSelectionModel().selectFirst();
        }

        // Inicializar ComboBox Urgencia (Visual)
        if (comboUrgencia != null) {
            comboUrgencia.getItems().addAll("Normal", "Prioridad", "Urgente");
            comboUrgencia.getSelectionModel().selectFirst();
        }

        // Configurar Tabla Trabajos
        if (tablaTrabajos != null) {
            colIdTrabajo.setCellValueFactory(new PropertyValueFactory<>("id"));
            colTituloTrabajo.setCellValueFactory(new PropertyValueFactory<>("titulo"));
            colClienteTrabajo.setCellValueFactory(new PropertyValueFactory<>("nombreCliente"));
            colOperarioTrabajo.setCellValueFactory(new PropertyValueFactory<>("nombreOperario"));
            colEstadoTrabajo.setCellValueFactory(new PropertyValueFactory<>("estado"));
            colFechaTrabajo.setCellValueFactory(new PropertyValueFactory<>("fecha"));

            // Evento Doble Clic para detalles
            tablaTrabajos.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && tablaTrabajos.getSelectionModel().getSelectedItem() != null) {
                    mostrarDetalleTrabajo(tablaTrabajos.getSelectionModel().getSelectedItem());
                }
            });
        }

        // Listener para habilitar/deshabilitar campo ID Empresa
        tipoUsuarioGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            boolean esOperario = rbOperario.isSelected();
            boxIdEmpresa.setDisable(!esOperario);
        });

        // Bloquear botones hasta conectar
        toggleBotones(true);
        btnConectar.setDisable(false);

        servicio.setOnMensajeRecibido(json -> {
            Platform.runLater(() -> {
                procesarRespuesta(json);
            });
        });

    }

    @FXML
    private void onConectarClick() {
        try {
            if (!servicio.isConectado()) {
                log("Conectando...");
                servicio.conectar("localhost", 5000);
                txtStatus.setText("CONECTADO");
                txtStatus.setStyle("-fx-text-fill: green;");
                btnConectar.setText("Desconectar");
                toggleBotones(false); // Habilitar
                log("Conexión establecida.");
            } else {
                servicio.desconectar();
                txtStatus.setText("DESCONECTADO");
                txtStatus.setStyle("-fx-text-fill: red;");
                btnConectar.setText("Conectar");
                toggleBotones(true); // Deshabilitar
                log("Desconectado.");
            }
        } catch (IOException e) {
            log("Error de conexión: " + e.getMessage());
        }
    }

    private void toggleBotones(boolean disable) {
        btnPing.setDisable(disable);
        btnRegEmpresa.setDisable(disable);
        btnRegUsuario.setDisable(disable);
    }

    @FXML
    private void onPingClick() {
        try {
            servicio.enviarPing();
            log("Enviado PING");
        } catch (IOException e) {
            log("Error enviando PING: " + e.getMessage());
        }
    }

    @FXML
    private void onAbrirSimuladorClick() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/fixfinder/vistas/VistaSimulador.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Simulador E2E - God Mode");
            stage.setScene(new Scene(root, 1000, 600));
            stage.show();
            log("Abriendo Simulador E2E...");
        } catch (IOException e) {
            log("Error abriendo simulador: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onLimpiarLogClick() {
        txtLog.clear();
    }

    @FXML
    private void onLoginClick() {
        lblStatusLogin.setText("Conectando...");
        lblStatusLogin.setStyle("-fx-text-fill: gray;");

        try {
            servicio.enviarLogin(txtLoginEmail.getText(), txtLoginPassword.getText());
            log("Enviado LOGIN");
        } catch (IOException e) {
            log("Error LOGIN: " + e.getMessage());
        }
    }

    @FXML
    private void onLogoutClick() {
        usuarioLogueadoId = null;
        usuarioLogueadoNombre = null;
        usuarioLogueadoNombre = null;
        usuarioLogueadoRol = null;
        usuarioLogueadoIdEmpresa = null;
        listaOperarios.clear();

        lblUsuarioLogueado.setText("Ninguna");
        lblUsuarioLogueado.setStyle("-fx-text-fill: #7f8c8d;");

        lblStatusLogin.setText("");
        btnLogin.setDisable(false);
        btnLogout.setDisable(true);

        // Deshabilitar Crear Trabajo
        btnCrearTrabajo.setDisable(true);
        lblStatusTrabajo.setText("Debes iniciar sesión para solicitar servicios");

        log("Sesión cerrada localmente.");
    }

    @FXML
    private void onCrearTrabajoClick() {
        if (usuarioLogueadoId == null) {
            log("Error: No hay usuario logueado.");
            return;
        }

        // Convertir selección visual a entero
        int nivelUrgencia = 1;
        String seleccion = comboUrgencia.getValue();
        if ("Prioridad".equals(seleccion))
            nivelUrgencia = 2;
        if ("Urgente".equals(seleccion))
            nivelUrgencia = 3;

        try {
            servicio.enviarCrearTrabajo(
                    usuarioLogueadoId,
                    txtTituloTrabajo.getText(),
                    txtDescripcionTrabajo.getText(),
                    txtDireccionTrabajo.getText(),
                    nivelUrgencia,
                    comboCategoria.getValue());
            log("Enviado CREAR_TRABAJO");
        } catch (IOException e) {
            log("Error CREAR_TRABAJO: " + e.getMessage());
        }
    }

    @FXML
    private void onRegistrarEmpresaClick() {
        try {
            servicio.enviarRegistroEmpresa(
                    txtNombreEmpresa.getText(),
                    txtCif.getText(),
                    txtEmailEmpresa.getText(),
                    txtDireccionEmpresa.getText(),
                    txtNombreGerente.getText(),
                    txtEmailGerente.getText(),
                    txtPassGerente.getText(),
                    txtDniGerente.getText());
            log("Enviado REGISTRO EMPRESA");
        } catch (IOException e) {
            log("Error REGISTRO: " + e.getMessage());
        }
    }

    @FXML
    private void onRegistrarUsuarioClick() {
        boolean esOperario = rbOperario.isSelected();
        try {
            servicio.enviarRegistroUsuario(
                    esOperario,
                    txtNombreUsuario.getText(),
                    txtDniUsuario.getText(),
                    txtEmailUsuario.getText(),
                    txtPassUsuario.getText(),
                    txtTelefonoUsuario.getText(),
                    txtDireccionUsuario.getText(),
                    txtIdEmpresaOperario.getText());
            log("Enviado REGISTRO USUARIO (" + (esOperario ? "OP" : "CLI") + ")");
        } catch (IOException e) {
            log("Error REGISTRO: " + e.getMessage());
        }
    }

    @FXML
    private void onRefrescarTrabajosClick() {
        if (usuarioLogueadoId == null) {
            log("Debes iniciar sesión para ver tus trabajos.");
            return;
        }

        // Configurar columnas según Rol
        boolean verCliente = !"CLIENTE".equalsIgnoreCase(usuarioLogueadoRol);
        boolean verOperario = "GERENTE".equalsIgnoreCase(usuarioLogueadoRol);

        colClienteTrabajo.setVisible(verCliente);
        colOperarioTrabajo.setVisible(verOperario);

        try {
            servicio.solicitarListaTrabajos(usuarioLogueadoId, usuarioLogueadoRol);
            log("Enviada solicitud de LISTAR_TRABAJOS...");
        } catch (IOException e) {
            log("Error al solicitar lista: " + e.getMessage());
        }
    }

    private void procesarRespuesta(String json) {
        try {
            // Usar el servicio para parsear la respuesta
            RespuestaServidor respuesta = servicio.interpretarRespuesta(json);

            int status = respuesta.getStatus();
            String mensaje = respuesta.getMensaje();
            JsonNode datos = respuesta.getDatos();

            // 1. GESTIÓN DE LOGIN
            if (respuesta.esExito()) {
                if (datos != null && datos.has("id") && datos.has("rol")) {
                    usuarioLogueadoId = datos.get("id").asInt();
                    usuarioLogueadoNombre = datos.has("nombreCompleto")
                            ? datos.get("nombreCompleto").asText()
                            : "Usuario Desconocido";
                    usuarioLogueadoRol = datos.get("rol").asText();
                    if (datos.has("idEmpresa")) {
                        usuarioLogueadoIdEmpresa = datos.get("idEmpresa").asInt();
                        // Si es gerente, pedir la lista de sus operarios de inmediato
                        if ("GERENTE".equalsIgnoreCase(usuarioLogueadoRol)) {
                            try {
                                servicio.solicitarListaOperarios(usuarioLogueadoIdEmpresa);
                            } catch (IOException e) {
                                log("Error solicitando operarios: " + e.getMessage());
                            }
                        }
                    } else {
                        usuarioLogueadoIdEmpresa = null;
                    }

                    lblUsuarioLogueado.setText(
                            usuarioLogueadoNombre + " (" + usuarioLogueadoRol + ") - ID: " + usuarioLogueadoId);
                    lblUsuarioLogueado.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");

                    lblStatusLogin.setText("Login Correcto");
                    lblStatusLogin.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");

                    // Activar Logout / Desactivar Login
                    btnLogin.setDisable(true);
                    btnLogout.setDisable(false);

                    // Habilitar Crear Trabajo si es CLIENTE
                    if ("CLIENTE".equalsIgnoreCase(usuarioLogueadoRol)) {
                        btnCrearTrabajo.setDisable(false);
                        lblStatusTrabajo.setText("Listo para solicitar servicios como " + usuarioLogueadoNombre);
                        lblStatusTrabajo.setStyle("-fx-text-fill: green;");
                    } else {
                        lblStatusTrabajo.setText(
                                "Solo los CLIENTES pueden solicitar servicios (Eres " + usuarioLogueadoRol + ")");
                        lblStatusTrabajo.setStyle("-fx-text-fill: orange;");
                    }

                    log("Sesión iniciada correctamente.");
                }
            } else if (status == 401 || mensaje.toLowerCase().contains("credenciales")
                    || mensaje.toLowerCase().contains("login")) {
                // Error específico de Login o Autenticación
                lblStatusLogin.setText(mensaje);
                lblStatusLogin.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                btnLogin.setDisable(false);
            }

            // GESTIÓN DE FEEDBACK GENÉRICO
            String colorStyle = respuesta.esExito() ? "-fx-text-fill: green;" : "-fx-text-fill: red;";

            if (mensaje.contains("Empresa")) {
                lblStatusEmpresa.setText(mensaje + (respuesta.esExito() ? " [OK]" : " [ERROR]"));
                lblStatusEmpresa.setStyle(colorStyle + " -fx-font-weight: bold;");
            }

            if (mensaje.contains("Operario") || mensaje.contains("Cliente") || mensaje.contains("usuario")) {
                lblStatusUsuario.setText(mensaje + (respuesta.esExito() ? " [OK]" : " [ERROR]"));
                lblStatusUsuario.setStyle(colorStyle + " -fx-font-weight: bold;");
            }

            // FEEDBACK CREAR TRABAJO
            if (mensaje.contains("Trabajo creado")) {
                lblStatusTrabajo.setText(mensaje);
                lblStatusTrabajo.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                // Limpiar campos
                txtTituloTrabajo.clear();
                txtDescripcionTrabajo.clear();
                txtDireccionTrabajo.clear();
            } else if (!respuesta.esExito() && usuarioLogueadoId != null && !mensaje.contains("login")
                    && !mensaje.contains("Empresa") && !mensaje.contains("Cliente")) {
                lblStatusTrabajo.setText("Error: " + mensaje);
                lblStatusTrabajo.setStyle("-fx-text-fill: red;");
            }

            // LOG GENERAL
            if (mensaje.contains("Listado obtenido")) {
                tablaTrabajos.getItems().clear();
                if (datos != null && datos.isArray()) {
                    for (JsonNode nodo : datos) {
                        tablaTrabajos.getItems().add(new TrabajoModelo(
                                nodo.get("id").asInt(),
                                nodo.get("titulo").asText(),
                                nodo.has("nombreCliente") ? nodo.get("nombreCliente").asText() : "",
                                nodo.has("nombreOperario") ? nodo.get("nombreOperario").asText() : "",
                                nodo.get("estado").asText(),
                                nodo.get("fecha").asText(),
                                nodo.has("descripcion") ? nodo.get("descripcion").asText() : "",
                                nodo.has("direccion") ? nodo.get("direccion").asText() : "",
                                nodo.has("categoria") ? nodo.get("categoria").asText() : ""));
                    }
                    log("Lista actualizada: " + datos.size() + " elementos.");
                }
            } else if (mensaje.contains("Lista de operarios obtenida")) {
                listaOperarios.clear();
                if (datos != null && datos.isArray()) {
                    for (JsonNode nodo : datos) {
                        listaOperarios.add(new OperarioModelo(
                                nodo.get("id").asInt(),
                                nodo.get("nombre").asText(),
                                nodo.get("especialidad").asText()));
                    }
                    log("Cache de operarios actualizada: " + listaOperarios.size());
                }
            } else if (mensaje.contains("Operario asignado") || mensaje.contains("desasignado")) {
                log("GESTIÓN ASIGNACIÓN: " + mensaje);
                // Refrescar la tabla
                onRefrescarTrabajosClick();
            } else if (!respuesta.esExito()) {
                log("Servidor reporta error (" + status + "): " + mensaje);
            }

        } catch (ClienteException e) {
            log("Error procesando respuesta: " + e.getMessage());
        }
    }

    private void log(String mensaje) {
        txtLog.appendText(mensaje + "\n");
    }

    private void mostrarDetalleTrabajo(TrabajoModelo t) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Detalle del Trabajo #" + t.getId());
        alert.setHeaderText(t.getTitulo());

        VBox content = new VBox(10);
        content.getChildren().add(new Label("Categoría: " + t.getCategoria()));
        content.getChildren().add(new Label("Estado: " + t.getEstado()));

        // Mostrar siempre Cliente y Dirección
        String nCliente = (t.getNombreCliente() == null || t.getNombreCliente().isEmpty()) ? "No especificado"
                : t.getNombreCliente();
        content.getChildren().add(new Label("Cliente: " + nCliente));

        String nDir = (t.getDireccion() == null || t.getDireccion().isEmpty()) ? "No especificada" : t.getDireccion();
        content.getChildren().add(new Label("Dirección: " + nDir));

        content.getChildren().add(new Label("Descripción:"));

        TextArea areaDesc = new TextArea(t.getDescripcion());
        areaDesc.setEditable(false);
        areaDesc.setWrapText(true);
        areaDesc.setPrefHeight(80);
        content.getChildren().add(areaDesc);

        String nOp = (t.getNombreOperario() == null || t.getNombreOperario().isEmpty()) ? "Pendiente de asignar"
                : t.getNombreOperario();
        content.getChildren().add(new Label("Operario Asignado: " + nOp));

        // Sección de Asignación (Solo para GERENTES y si el trabajo no está FINALIZADO)
        if ("GERENTE".equalsIgnoreCase(usuarioLogueadoRol) && !"FINALIZADO".equals(t.getEstado())) {

            content.getChildren().add(new Label("Gestión de Asignación:"));
            VBox boxGestion = new VBox(5);
            boxGestion.setStyle("-fx-border-color: #ccc; -fx-padding: 10; -fx-background-color: #f9f9f9;");

            // ASIGNAR / REASIGNAR
            HBox boxAsignar = new HBox(10);
            ComboBox<OperarioModelo> comboOps = new ComboBox<>();
            comboOps.setPromptText("Seleccionar Técnico");
            comboOps.getItems().addAll(listaOperarios);

            Button btnAsignar = new Button("Asignar / Cambiar");
            btnAsignar.setOnAction(e -> {
                OperarioModelo seleccionado = comboOps.getValue();
                if (seleccionado != null) {
                    try {
                        servicio.enviarAsignarOperario(t.getId(), seleccionado.getId(), usuarioLogueadoId);
                        log("Enviando asignación...");
                        alert.close();
                    } catch (IOException ex) {
                        log("Error al asignar: " + ex.getMessage());
                    }
                } else {
                    new Alert(Alert.AlertType.WARNING, "Selecciona un operario").showAndWait();
                }
            });
            boxAsignar.getChildren().addAll(comboOps, btnAsignar);
            boxGestion.getChildren().add(boxAsignar);

            // DESASIGNAR (Solo si hay operario real)
            boolean tieneOperario = (t.getNombreOperario() != null && !t.getNombreOperario().isEmpty()
                    && !t.getNombreOperario().equals("Sin asignar")
                    && !t.getNombreOperario().equals("Pendiente de asignar"));

            if (tieneOperario) {
                Button btnDesasignar = new Button("Desasignar Operario");
                btnDesasignar.setStyle("-fx-background-color: #ffcccc; -fx-text-fill: red;");
                btnDesasignar.setOnAction(e -> {
                    try {
                        // Enviamos -1 para desasignar
                        servicio.enviarAsignarOperario(t.getId(), -1, usuarioLogueadoId);
                        log("Enviando desasignación...");
                        alert.close();
                    } catch (IOException ex) {
                        log("Error al desasignar: " + ex.getMessage());
                    }
                });
                boxGestion.getChildren().add(btnDesasignar);
            }
            content.getChildren().add(boxGestion);
        }

        alert.getDialogPane().setContent(content);
        alert.showAndWait();
    }

    // Clase auxiliar para crear tabla de trabajos
    public static class TrabajoModelo {
        private final int id;
        private final String titulo;
        private final String nombreCliente;
        private final String nombreOperario;
        private final String estado;
        private final String fecha;
        // Datos extendidos (ocultos en tabla, visibles en detalle)
        private final String descripcion;
        private final String direccion;
        private final String categoria;

        public TrabajoModelo(int id, String titulo, String nombreCliente, String nombreOperario, String estado,
                String fecha, String descripcion, String direccion, String categoria) {
            this.id = id;
            this.titulo = titulo;
            this.nombreCliente = nombreCliente;
            this.nombreOperario = nombreOperario;
            this.estado = estado;
            this.fecha = fecha;
            this.descripcion = descripcion;
            this.direccion = direccion;
            this.categoria = categoria;
        }

        public int getId() {
            return id;
        }

        public String getTitulo() {
            return titulo;
        }

        public String getNombreCliente() {
            return nombreCliente;
        }

        public String getNombreOperario() {
            return nombreOperario;
        }

        public String getEstado() {
            return estado;
        }

        public String getFecha() {
            return fecha;
        }

        public String getDescripcion() {
            return descripcion;
        }

        public String getDireccion() {
            return direccion;
        }

        public String getCategoria() {
            return categoria;
        }
    }

    public static class OperarioModelo {
        private final int id;
        private final String nombre;
        private final String especialidad;

        public OperarioModelo(int id, String nombre, String especialidad) {
            this.id = id;
            this.nombre = nombre;
            this.especialidad = especialidad;
        }

        @Override
        public String toString() {
            return nombre + " (" + especialidad + ")";
        }

        public int getId() {
            return id;
        }
    }
}

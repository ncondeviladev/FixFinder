package com.fixfinder.controladores;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fixfinder.modelos.Trabajo;
import com.fixfinder.modelos.enums.EstadoTrabajo;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SimuladorController {

    @FXML
    private TableView<TrabajoModeloSimulador> tablaTrabajos;
    @FXML
    private TableColumn<TrabajoModeloSimulador, Integer> colId;
    @FXML
    private TableColumn<TrabajoModeloSimulador, String> colTitulo;
    @FXML
    private TableColumn<TrabajoModeloSimulador, String> colCliente;
    @FXML
    private TableColumn<TrabajoModeloSimulador, String> colEstado;
    @FXML
    private TableColumn<TrabajoModeloSimulador, String> colOperario;

    @FXML
    private Button btnCrearPresupuesto;
    @FXML
    private Button btnAceptarPresupuesto;
    @FXML
    private Button btnAsignarOperario;
    @FXML
    private Button btnFinalizarTrabajo;
    @FXML
    private Button btnGenerarFactura;
    @FXML
    private Button btnPagarFactura;

    @FXML
    private Label lblEstadoAccion;

    private final ObservableList<TrabajoModeloSimulador> listaTrabajos = FXCollections.observableArrayList();
    private final ObjectMapper mapper = new ObjectMapper();

    // Contexto de Usuario (Simulado, usaremos uno fijo o el logueado real si se
    // pasa)
    private int idUsuarioActual = 1;

    @FXML
    public void initialize() {
        configurarTabla();
        cargarTrabajos();

        // Listener de selección
        tablaTrabajos.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            actualizarBotones(newVal);
        });

        // Acciones
        btnCrearPresupuesto.setOnAction(e -> accionCrearPresupuesto());
        btnAceptarPresupuesto.setOnAction(e -> accionAceptarPresupuesto());
        btnAsignarOperario.setOnAction(e -> accionAsignarOperario());
        btnFinalizarTrabajo.setOnAction(e -> accionFinalizarTrabajo()); // Este requerirá implementar backend primero si
                                                                        // no está
        btnGenerarFactura.setOnAction(e -> accionGenerarFactura());
        btnPagarFactura.setOnAction(e -> accionPagarFactura());

        // El flujo se ha simplificado: "Finalizar Trabajo" ahora también genera
        // factura.
        btnGenerarFactura.setVisible(false);
        btnGenerarFactura.setManaged(false);
    }

    private void configurarTabla() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitulo.setCellValueFactory(new PropertyValueFactory<>("titulo"));
        colCliente.setCellValueFactory(new PropertyValueFactory<>("cliente"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        colOperario.setCellValueFactory(new PropertyValueFactory<>("operario"));

        tablaTrabajos.setItems(listaTrabajos);
    }

    private void cargarTrabajos() {
        // Guardar selección actual para restaurarla
        TrabajoModeloSimulador seleccionado = tablaTrabajos.getSelectionModel().getSelectedItem();
        int idSeleccionado = (seleccionado != null) ? seleccionado.getId() : -1;

        new Thread(() -> {
            try {
                ObjectNode request = mapper.createObjectNode();
                request.put("accion", "LISTAR_TRABAJOS");
                ObjectNode datos = request.putObject("datos");
                datos.put("rol", "ADMIN"); // Pedimos como ADMIN para ver todo
                datos.put("idUsuario", idUsuarioActual);

                JsonNode respuesta = enviarSolicitud(request);

                if (respuesta != null && respuesta.get("status").asInt() == 200) {
                    ArrayNode trabajosNode = (ArrayNode) respuesta.get("datos");
                    Platform.runLater(() -> {
                        listaTrabajos.clear();
                        for (JsonNode nodo : trabajosNode) {
                            listaTrabajos.add(new TrabajoModeloSimulador(nodo));
                        }

                        // Restaurar selección
                        if (idSeleccionado != -1) {
                            for (TrabajoModeloSimulador t : listaTrabajos) {
                                if (t.getId() == idSeleccionado) {
                                    tablaTrabajos.getSelectionModel().select(t);
                                    actualizarBotones(t); // Forzar actu botones
                                    break;
                                }
                            }
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void actualizarBotones(TrabajoModeloSimulador trabajo) {
        if (trabajo == null) {
            deshabilitarTodo();
            return;
        }

        String estado = trabajo.getEstado();
        boolean tieneOperario = !trabajo.getOperario().equals("Sin asignar");

        // Lógica de Estado
        // Permitir crear presupuestos en PENDIENTE o PRESUPUESTADO (varias empresas)
        btnCrearPresupuesto.setDisable(!estado.equals("PENDIENTE") && !estado.equals("PRESUPUESTADO"));

        // Permitir aceptar si ya hay presupuestos
        btnAceptarPresupuesto.setDisable(!estado.equals("PRESUPUESTADO") && !estado.equals("PENDIENTE"));

        // Se asigna operario después de aceptar presupuesto o para cambiarlo si ya está
        // asignado
        btnAsignarOperario.setDisable(!estado.equals("ACEPTADO") && !estado.equals("ASIGNADO"));

        // Se finaliza cuando está ASIGNADO
        btnFinalizarTrabajo.setDisable(!estado.equals("ASIGNADO"));

        // Se genera factura cuando está REALIZADO (Oculto, pero mantenemos lógica por
        // si acaso)
        btnGenerarFactura.setDisable(!estado.equals("REALIZADO"));

        // Se paga cuando está FINALIZADO
        btnPagarFactura.setDisable(!estado.equals("FINALIZADO"));

        // Si está PAGADO, todo deshabilitado
        if (estado.equals("PAGADO")) {
            deshabilitarTodo();
        }
    }

    private void deshabilitarTodo() {
        btnCrearPresupuesto.setDisable(true);
        btnAceptarPresupuesto.setDisable(true);
        btnAsignarOperario.setDisable(true);
        btnFinalizarTrabajo.setDisable(true);
        btnGenerarFactura.setDisable(true);
        btnPagarFactura.setDisable(true);
    }

    // --- Acciones ---

    private void accionCrearPresupuesto() {
        TrabajoModeloSimulador t = tablaTrabajos.getSelectionModel().getSelectedItem();
        if (t == null)
            return;

        // Listar Empresas
        ObjectNode request = mapper.createObjectNode();
        request.put("accion", "LISTAR_EMPRESAS");

        enviarSolicitudAsync(request, respuesta -> {
            if (respuesta != null && respuesta.get("status").asInt() == 200) {
                ArrayNode lista = (ArrayNode) respuesta.get("datos");
                if (lista.size() == 0) {
                    Platform.runLater(() -> mostrarMensaje("No hay empresas registradas en el sistema."));
                    return;
                }

                List<String> opciones = new ArrayList<>();
                for (JsonNode e : lista) {
                    opciones.add("ID: " + e.get("id").asInt() + " | " + e.get("nombre").asText());
                }

                Platform.runLater(() -> {
                    ChoiceDialog<String> dialog = new ChoiceDialog<>(opciones.get(0), opciones);
                    dialog.setTitle("Crear Presupuesto");
                    dialog.setHeaderText("Paso 1: Elija la empresa que emite el presupuesto:");
                    dialog.setContentText("Empresa:");

                    dialog.showAndWait().ifPresent(seleccion -> {
                        int idEmpresa = Integer.parseInt(seleccion.split(" ")[1]);

                        // Pedir Monto
                        TextInputDialog dialogMonto = new TextInputDialog("100.0");
                        dialogMonto.setTitle("Crear Presupuesto");
                        dialogMonto.setHeaderText("Paso 2: Monto del Presupuesto (€):");
                        dialogMonto.setContentText("Monto:");

                        dialogMonto.showAndWait().ifPresent(montoStr -> {
                            try {
                                double monto = Double.parseDouble(montoStr);
                                enviarAccion("CREAR_PRESUPUESTO", d -> {
                                    d.put("idTrabajo", t.getId());
                                    d.put("idEmpresa", idEmpresa);
                                    d.put("monto", monto);
                                });
                            } catch (NumberFormatException ex) {
                                mostrarMensaje("Monto inválido");
                            }
                        });
                    });
                });
            } else {
                Platform.runLater(() -> mostrarMensaje("Error listando empresas."));
            }
        });
    }

    private void accionAsignarOperario() {
        TrabajoModeloSimulador t = tablaTrabajos.getSelectionModel().getSelectedItem();
        if (t == null)
            return;

        // 1. Verificar Presupuesto ACEPTADO para saber empresa adjudicataria
        ObjectNode reqPres = mapper.createObjectNode();
        reqPres.put("accion", "LISTAR_PRESUPUESTOS");
        reqPres.putObject("datos").put("idTrabajo", t.getId());

        enviarSolicitudAsync(reqPres, resPres -> {
            if (resPres != null && resPres.get("status").asInt() == 200) {
                ArrayNode presupuestos = (ArrayNode) resPres.get("datos");
                int idEmpresaGanadora = -1;

                for (JsonNode p : presupuestos) {
                    if ("ACEPTADO".equals(p.get("estado").asText())) {
                        if (p.has("empresa")) {
                            idEmpresaGanadora = p.get("empresa").get("id").asInt();
                        }
                        break;
                    }
                }

                if (idEmpresaGanadora == -1) {
                    Platform.runLater(() -> mostrarMensaje(
                            "Este trabajo NO tiene un presupuesto ACEPTADO. No se puede asignar operario."));
                    return;
                }

                // 2. Listar Operarios de esa empresa
                final int idEmp = idEmpresaGanadora;
                ObjectNode reqOp = mapper.createObjectNode();
                reqOp.put("accion", "GET_OPERARIOS");
                reqOp.putObject("datos").put("idEmpresa", idEmp);

                enviarSolicitudAsync(reqOp, resOp -> {
                    if (resOp != null && resOp.get("status").asInt() == 200) {
                        ArrayNode operarios = (ArrayNode) resOp.get("datos");
                        if (operarios.size() == 0) {
                            Platform.runLater(() -> mostrarMensaje(
                                    "La empresa adjudicataria (ID " + idEmp + ") no tiene operarios registrados."));
                            return;
                        }

                        List<String> ops = new ArrayList<>();
                        for (JsonNode o : operarios) {
                            ops.add("ID: " + o.get("id").asInt() + " | " + o.get("nombre").asText() + " ["
                                    + o.get("especialidad").asText() + "]");
                        }

                        // Mostrar lista
                        Platform.runLater(() -> {
                            ChoiceDialog<String> dialog = new ChoiceDialog<>(ops.get(0), ops);
                            dialog.setTitle("Asignar Operario");
                            dialog.setHeaderText("Asignar operario de empresa adjudicataria (ID " + idEmp + "):");
                            dialog.setContentText("Operario:");

                            dialog.showAndWait().ifPresent(sel -> {
                                try {
                                    int idOp = Integer.parseInt(sel.split(" ")[1]);
                                    enviarAccion("ASIGNAR_OPERARIO", d -> {
                                        d.put("idTrabajo", t.getId());
                                        d.put("idOperario", idOp);
                                        d.put("idGerente", 1); // Simulado (ADMIN/GERENTE)
                                    });
                                } catch (Exception ex) {
                                    mostrarMensaje("Error ID operario");
                                }
                            });
                        });
                    } else {
                        Platform.runLater(() -> mostrarMensaje("Error listando operarios."));
                    }
                });

            } else {
                Platform.runLater(() -> mostrarMensaje("Error verificando presupuesto."));
            }
        });
    }

    private void accionAceptarPresupuesto() {
        TrabajoModeloSimulador t = tablaTrabajos.getSelectionModel().getSelectedItem();
        if (t == null)
            return;

        // 1. Obtener Presupuestos en segundo plano
        new Thread(() -> {
            try {
                ObjectNode request = mapper.createObjectNode();
                request.put("accion", "LISTAR_PRESUPUESTOS");
                request.putObject("datos").put("idTrabajo", t.getId());

                JsonNode respuesta = enviarSolicitud(request);

                Platform.runLater(() -> {
                    if (respuesta != null && respuesta.get("status").asInt() == 200 && respuesta.has("datos")) {
                        ArrayNode lista = (ArrayNode) respuesta.get("datos");
                        if (lista.size() == 0) {
                            mostrarMensaje("No hay presupuestos registrados para este trabajo.");
                            return;
                        }

                        List<String> opciones = new ArrayList<>();
                        for (JsonNode p : lista) {
                            // Solo mostramos pendientes para aceptar
                            if (!"PENDIENTE".equals(p.get("estado").asText()))
                                continue;

                            int idP = p.get("id").asInt();
                            double monto = p.get("monto").asDouble();
                            String empresa = "Desc.";
                            if (p.has("empresa") && !p.get("empresa").isNull()) {
                                empresa = p.get("empresa").has("nombreComercial")
                                        ? p.get("empresa").get("nombreComercial").asText()
                                        : "Empresa " + p.get("empresa").get("id").asInt();
                            }
                            opciones.add("ID: " + idP + " | " + empresa + " | " + monto + "€");
                        }

                        if (opciones.isEmpty()) {
                            mostrarMensaje("No hay presupuestos PENDIENTES (Ya aceptado o rechazados).");
                            return;
                        }

                        ChoiceDialog<String> dialog = new ChoiceDialog<>(opciones.get(0), opciones);
                        dialog.setTitle("Aceptar Presupuesto");
                        dialog.setHeaderText("Seleccione la oferta que desea ACEPTAR:");
                        dialog.setContentText("Oferta:");

                        dialog.showAndWait().ifPresent(seleccion -> {
                            // Parsear ID del string "ID: 123 | ..."
                            try {
                                String idStr = seleccion.split(" ")[1];
                                int idPresupuesto = Integer.parseInt(idStr);

                                enviarAccion("ACEPTAR_PRESUPUESTO", datos -> {
                                    datos.put("idPresupuesto", idPresupuesto);
                                });
                            } catch (Exception ex) {
                                mostrarMensaje("Error al parsear selección: " + ex.getMessage());
                            }
                        });

                    } else {
                        mostrarMensaje("Error obteniendo presupuestos: "
                                + (respuesta != null ? respuesta.get("mensaje").asText() : "Sin respuesta"));
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> mostrarMensaje("Error de red al buscar presupuestos."));
                e.printStackTrace();
            }
        }).start();
    }

    private void accionFinalizarTrabajo() {
        TrabajoModeloSimulador t = tablaTrabajos.getSelectionModel().getSelectedItem();
        if (t == null)
            return;

        enviarAccion("FINALIZAR_TRABAJO", datos -> {
            datos.put("idTrabajo", t.getId());
            datos.put("informe", "Finalizado desde Simulador E2E");
        });
    }

    private void accionGenerarFactura() {
        TrabajoModeloSimulador t = tablaTrabajos.getSelectionModel().getSelectedItem();

        new Thread(() -> {
            try {
                ObjectNode request = mapper.createObjectNode();
                request.put("accion", "GENERAR_FACTURA");
                request.putObject("datos").put("idTrabajo", t.getId());

                JsonNode respuesta = enviarSolicitud(request);

                Platform.runLater(() -> {
                    if (respuesta != null && respuesta.get("status").asInt() == 200) {
                        JsonNode datos = respuesta.get("datos");
                        int idFactura = datos.get("id").asInt();
                        double total = datos.get("total").asDouble();
                        mostrarMensaje("FACTURA GENERADA.\nID: " + idFactura + "\nTotal: " + total
                                + "€\n\nUse este ID para pagar.");
                        cargarTrabajos();
                    } else {
                        mostrarMensaje("Error generando factura: "
                                + (respuesta != null ? respuesta.get("mensaje").asText() : ""));
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void accionPagarFactura() {
        TrabajoModeloSimulador t = tablaTrabajos.getSelectionModel().getSelectedItem();
        enviarAccion("PAGAR_FACTURA", d -> d.put("idTrabajo", t.getId()));
    }

    // --- Helpers de Red ---

    private void enviarAccion(String accion, java.util.function.Consumer<ObjectNode> datosBuilder) {
        new Thread(() -> {
            try {
                ObjectNode request = mapper.createObjectNode();
                request.put("accion", accion);
                ObjectNode datos = request.putObject("datos");
                datosBuilder.accept(datos);

                JsonNode respuesta = enviarSolicitud(request);

                Platform.runLater(() -> {
                    if (respuesta != null) {
                        lblEstadoAccion.setText(
                                respuesta.has("mensaje") ? respuesta.get("mensaje").asText() : "Acción completada");
                        cargarTrabajos(); // Refrescar
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> lblEstadoAccion.setText("Error: " + e.getMessage()));
            }
        }).start();
    }

    private JsonNode enviarSolicitud(ObjectNode requestJson) {
        try (Socket socket = new Socket("localhost", 5000);
                DataInputStream entrada = new DataInputStream(socket.getInputStream());
                DataOutputStream salida = new DataOutputStream(socket.getOutputStream())) {

            salida.writeUTF(mapper.writeValueAsString(requestJson));
            String respuestaJson = entrada.readUTF();
            return mapper.readTree(respuestaJson);

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void enviarSolicitudAsync(ObjectNode request, java.util.function.Consumer<JsonNode> callback) {
        new Thread(() -> {
            JsonNode res = enviarSolicitud(request);
            callback.accept(res);
        }).start();
    }

    private void mostrarMensaje(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(msg);
        alert.show();
    }

    // Clase interna Modelo
    public static class TrabajoModeloSimulador {
        private final int id;
        private final SimpleStringProperty titulo;
        private final SimpleStringProperty cliente;
        private final SimpleStringProperty estado;
        private final SimpleStringProperty operario;
        private final boolean tienePresupuestoAceptado;

        public TrabajoModeloSimulador(JsonNode nodo) {
            this.id = nodo.get("id").asInt();
            this.titulo = new SimpleStringProperty(nodo.get("titulo").asText());
            this.estado = new SimpleStringProperty(nodo.get("estado").asText());
            this.tienePresupuestoAceptado = nodo.has("tienePresupuestoAceptado")
                    && nodo.get("tienePresupuestoAceptado").asBoolean();

            if (nodo.has("cliente") && !nodo.get("cliente").isNull()) {
                JsonNode c = nodo.get("cliente");
                this.cliente = new SimpleStringProperty(
                        c.has("nombre") ? c.get("nombre").asText() : "ID: " + c.get("id").asInt());
            } else {
                this.cliente = new SimpleStringProperty("Desconocido");
            }

            if (nodo.has("operarioAsignado") && !nodo.get("operarioAsignado").isNull()) {
                JsonNode o = nodo.get("operarioAsignado");
                this.operario = new SimpleStringProperty(
                        o.has("nombre") ? o.get("nombre").asText() : "ID: " + o.get("id").asInt());
            } else {
                this.operario = new SimpleStringProperty("Sin asignar");
            }
        }

        public int getId() {
            return id;
        }

        public String getTitulo() {
            return titulo.get();
        }

        public String getCliente() {
            return cliente.get();
        }

        public String getEstado() {
            return estado.get();
        }

        public String getOperario() {
            return operario.get();
        }

        public boolean isTienePresupuestoAceptado() {
            return tienePresupuestoAceptado;
        }
    }
}

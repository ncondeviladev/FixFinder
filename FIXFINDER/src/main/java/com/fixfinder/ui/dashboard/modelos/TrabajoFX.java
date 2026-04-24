package com.fixfinder.ui.dashboard.modelos;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;

/**
 * Modelo de datos reactivo (JavaFX Properties) para representar un Trabajo en
 * la UI del Dashboard.
 * Permite el binding directo con TableView y otros controles JavaFX.
 * Mantiene la lista de URLs de las incidencias subidas desde Firebase.
 */
public class TrabajoFX {

    /**
     * Crea una instancia de TrabajoFX a partir de un nodo JSON del servidor.
     * Centraliza la lógica de mapeo para evitar duplicidad en los controladores.
     * 
     * @param n                 Nodo JSON con datos del trabajo.
     * @param idEmpresaLogueada ID de la empresa para contextualizar flags de puja.
     * @return Instancia reactiva de TrabajoFX.
     */
    public static TrabajoFX fromNode(JsonNode n, int idEmpresaLogueada) {
        String estado = n.has("estado") ? n.get("estado").asText() : "";

        String cliente = "";
        String cliTelefono = "";
        String cliEmail = "";
        String cliUrlFoto = "";
        String cliDireccion = "";

        if (n.has("cliente") && !n.get("cliente").isNull()) {
            JsonNode c = n.get("cliente");
            cliente = c.has("nombre") ? c.get("nombre").asText()
                    : c.has("nombreCompleto") ? c.get("nombreCompleto").asText() : "";
            cliTelefono = c.has("telefono") ? c.get("telefono").asText() : "";
            cliEmail = c.has("email") ? c.get("email").asText() : "";
            cliDireccion = c.has("direccion") ? c.get("direccion").asText() : "";
            cliUrlFoto = c.has("url_foto") ? c.get("url_foto").asText()
                    : c.has("urlFoto") ? c.get("urlFoto").asText()
                            : c.has("foto") ? c.get("foto").asText() : "";
        }

        if (cliente.isBlank() && n.has("nombreCliente") && !n.get("nombreCliente").isNull()) {
            cliente = n.get("nombreCliente").asText();
            cliTelefono = n.has("telefonoCliente") ? n.get("telefonoCliente").asText() : "";
        }

        String operario = "";
        int idOp = -1;
        if (n.has("operarioAsignado") && !n.get("operarioAsignado").isNull()) {
            JsonNode op = n.get("operarioAsignado");
            operario = op.has("nombre") ? op.get("nombre").asText()
                    : op.has("nombreCompleto") ? op.get("nombreCompleto").asText() : "";
            idOp = op.has("id") ? op.get("id").asInt() : -1;
        }

        if (operario.isBlank() && n.has("nombreOperario") && !n.get("nombreOperario").isNull()) {
            operario = n.get("nombreOperario").asText();
        }

        TrabajoFX t = new TrabajoFX(
                n.get("id").asInt(),
                n.has("titulo") ? n.get("titulo").asText() : "",
                cliente,
                n.has("categoria") ? n.get("categoria").asText() : "OTROS",
                estado, operario,
                n.has("fecha") ? n.get("fecha").asText() : "",
                n.has("descripcion") ? n.get("descripcion").asText() : "",
                n.has("direccion") ? n.get("direccion").asText() : "",
                idOp, cliTelefono, cliEmail, cliDireccion,
                n.has("valoracion") ? n.get("valoracion").asInt() : 0,
                n.has("comentarioCliente") ? n.get("comentarioCliente").asText() : "");

        if (n.has("urls_fotos") && n.get("urls_fotos").isArray()) {
            List<String> fotos = new ArrayList<>();
            for (JsonNode urlNode : n.get("urls_fotos")) {
                fotos.add(urlNode.asText());
            }
            t.setUrlsFotos(fotos);
        }

        // Historial de presupuestos para lógica de visualización (Arrow/Re-bid)
        if (n.has("presupuestos") && n.get("presupuestos").isArray()) {
            for (JsonNode pNode : n.get("presupuestos")) {
                if (pNode.has("empresa") && pNode.get("empresa").has("id")) {
                    int idEmp = pNode.get("empresa").get("id").asInt();
                    String est = pNode.has("estado") ? pNode.get("estado").asText() : "PENDIENTE";
                    double monto = pNode.has("monto") ? pNode.get("monto").asDouble() : 0;
                    String notas = pNode.has("notas") ? pNode.get("notas").asText() : "";

                    t.agregarMetaPresupuesto(idEmp, est, monto, notas);
                }
            }
        }

        if (!cliUrlFoto.isBlank()) {
            t.setClienteUrlFoto(cliUrlFoto);
        }

        return t;
    }

    // Estructura ligera para metadatos de presupuesto
    public record MetaPresupuesto(int idEmpresa, String estado, double monto, String notas) {
    }

    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty titulo = new SimpleStringProperty();
    private final StringProperty cliente = new SimpleStringProperty();
    private final StringProperty categoria = new SimpleStringProperty();
    private final StringProperty estado = new SimpleStringProperty();
    private final StringProperty operario = new SimpleStringProperty();
    private final StringProperty fecha = new SimpleStringProperty();
    private final StringProperty descripcion = new SimpleStringProperty();
    private final StringProperty direccion = new SimpleStringProperty();
    private final IntegerProperty idOperario = new SimpleIntegerProperty(-1);

    private final StringProperty clienteTelefono = new SimpleStringProperty("");
    private final StringProperty clienteEmail = new SimpleStringProperty("");
    private final StringProperty clienteDireccion = new SimpleStringProperty("");
    private final IntegerProperty valoracion = new SimpleIntegerProperty(0);
    private final StringProperty comentarioCliente = new SimpleStringProperty("");
    private final StringProperty clienteUrlFoto = new SimpleStringProperty("");

    private final ObservableList<String> urlsFotos = FXCollections.observableArrayList();
    private final List<MetaPresupuesto> historialPresupuestos = new ArrayList<>();

    public TrabajoFX(int id, String titulo, String cliente, String categoria,
            String estado, String operario, String fecha,
            String descripcion, String direccion, int idOperario) {
        this.id.set(id);
        this.titulo.set(titulo);
        this.cliente.set(cliente);
        this.categoria.set(categoria != null ? categoria : "OTROS");
        this.estado.set(estado);
        this.operario.set(operario != null && !operario.isBlank() ? operario : "Sin asignar");
        this.fecha.set(fecha);
        this.descripcion.set(descripcion != null ? descripcion : "");
        this.direccion.set(direccion != null ? direccion : "");
        this.idOperario.set(idOperario);
    }

    public TrabajoFX(int id, String titulo, String cliente, String categoria,
            String estado, String operario, String fecha,
            String descripcion, String direccion, int idOperario,
            String clienteTelefono, String clienteEmail, String clienteDireccion,
            int valoracion, String comentarioCliente) {
        this(id, titulo, cliente, categoria, estado, operario, fecha, descripcion, direccion, idOperario);
        this.clienteTelefono.set(clienteTelefono != null ? clienteTelefono : "");
        this.clienteEmail.set(clienteEmail != null ? clienteEmail : "");
        this.clienteDireccion.set(clienteDireccion != null ? clienteDireccion : "");
        this.valoracion.set(valoracion);
        this.comentarioCliente.set(comentarioCliente != null ? comentarioCliente : "");
    }

    public int getId() {
        return id.get();
    }

    public IntegerProperty idProperty() {
        return id;
    }

    public String getTitulo() {
        return titulo.get();
    }

    public StringProperty tituloProperty() {
        return titulo;
    }

    public String getCliente() {
        return cliente.get();
    }

    public StringProperty clienteProperty() {
        return cliente;
    }

    public String getCategoria() {
        return categoria.get();
    }

    public StringProperty categoriaProperty() {
        return categoria;
    }

    public String getEstado() {
        return estado.get();
    }

    public StringProperty estadoProperty() {
        return estado;
    }

    public String getOperario() {
        return operario.get();
    }

    public StringProperty operarioProperty() {
        return operario;
    }

    public String getFecha() {
        return fecha.get();
    }

    public StringProperty fechaProperty() {
        return fecha;
    }

    public String getDescripcion() {
        return descripcion.get();
    }

    public String getDireccion() {
        return direccion.get();
    }

    public int getIdOperario() {
        return idOperario.get();
    }

    public String getClienteTelefono() {
        return clienteTelefono.get();
    }

    public String getClienteEmail() {
        return clienteEmail.get();
    }

    public String getClienteDireccion() {
        return clienteDireccion.get();
    }

    public StringProperty clienteDireccionProperty() {
        return clienteDireccion;
    }

    public int getValoracion() {
        return valoracion.get();
    }

    public IntegerProperty valoracionProperty() {
        return valoracion;
    }

    public String getComentarioCliente() {
        return comentarioCliente.get();
    }

    public StringProperty comentarioClienteProperty() {
        return comentarioCliente;
    }

    public String getClienteUrlFoto() {
        return clienteUrlFoto.get();
    }

    public void setClienteUrlFoto(String url) {
        this.clienteUrlFoto.set(url);
    }

    public ObservableList<String> getUrlsFotos() {
        return urlsFotos;
    }

    public void setUrlsFotos(java.util.List<String> fotos) {
        this.urlsFotos.setAll(fotos);
    }

    public void agregarMetaPresupuesto(int idEmp, String est, double monto, String notas) {
        this.historialPresupuestos.add(new MetaPresupuesto(idEmp, est, monto, notas));
    }

    /**
     * Indica si la empresa ya tiene un presupuesto activo (PENDIENTE o ACEPTADO).
     */
    public boolean haPresupuestado(int idEmp) {
        return historialPresupuestos.stream().anyMatch(p -> p.idEmpresa() == idEmp &&
                (p.estado().equalsIgnoreCase("PENDIENTE") || p.estado().equalsIgnoreCase("ACEPTADO")));
    }

    /**
     * Indica si el último presupuesto de esta empresa fue rechazado y no ha vuelto
     * a pujar.
     */
    public boolean fueRechazado(int idEmp) {
        // Un trabajo se considera rechazado para esta empresa si NO tiene presupuestos
        // pendientes
        // pero TIENE al menos uno rechazado.
        boolean tienePendiente = haPresupuestado(idEmp);
        boolean tieneRechazado = historialPresupuestos.stream()
                .anyMatch(p -> p.idEmpresa() == idEmp && p.estado().equalsIgnoreCase("RECHAZADO"));
        return !tienePendiente && tieneRechazado;
    }

    /**
     * Obtiene el último presupuesto (más reciente) de la empresa para mostrarlo en
     * el diálogo.
     */
    public MetaPresupuesto getMiUltimoPresupuesto(int idEmp) {
        return historialPresupuestos.stream()
                .filter(p -> p.idEmpresa() == idEmp)
                .reduce((first, second) -> second) // Quedarnos con el último añadido
                .orElse(null);
    }
}

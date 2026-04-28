package com.fixfinder.ui.dashboard.dialogos;

import com.fixfinder.ui.dashboard.modelos.*;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import java.util.List;
import java.util.Optional;

/**
 * Centro de Gestión de Incidencias (Diálogo Unificado).
 * Esta clase modular reemplaza a los antiguos diálogos de Detalle, Asignación y
 * Gestión.
 * Adapta su contenido dinámicamente según el estado del trabajo y los permisos.
 */
public class DialogoGestionIncidencia {

    /**
     * Resultado polimórfico del diálogo.
     * Puede contener datos de presupuesto o un ID de operario para asignación.
     */
    public record Resultado(Double monto, String notas, Integer idOperario) {
    }

    private final TrabajoFX trabajo;
    private final String cssPath;
    private final int idEmpresaLogueada;
    private final List<OperarioFX> operariosDisponibles;
    private final boolean permitirPresupuestar;

    public DialogoGestionIncidencia(TrabajoFX trabajo, String cssPath, int idEmpresaLogueada,
            List<OperarioFX> operarios, boolean permitirPresupuestar) {
        this.trabajo = trabajo;
        this.cssPath = cssPath;
        this.idEmpresaLogueada = idEmpresaLogueada;
        this.operariosDisponibles = operarios;
        this.permitirPresupuestar = permitirPresupuestar;
    }

    public Optional<Resultado> mostrar() {
        Dialog<Resultado> dialog = new Dialog<>();
        dialog.setResizable(true);
        String estado = trabajo.getEstado();

        // Título dinámico
        dialog.setTitle("Gestión de Incidencia — #" + trabajo.getId());
        dialog.setHeaderText(trabajo.getTitulo());

        if (cssPath != null) {
            dialog.getDialogPane().getStylesheets().add(cssPath);
        }
        dialog.getDialogPane().getStyleClass().add("panel-dialogo");

        VBox mainBox = new VBox(15);
        mainBox.setPadding(new Insets(10, 0, 10, 0));
        mainBox.setPrefWidth(550);

        // --- SECCIÓN 1: INFORMACIÓN BÁSICA ---
        GridPane grid = new GridPane();
        grid.setHgap(30);
        grid.setVgap(10);
        ColumnConstraints col = new ColumnConstraints();
        col.setPercentWidth(50);
        grid.getColumnConstraints().addAll(col, col);

        grid.add(crearFilaInfo("Cliente", trabajo.getCliente()), 0, 0);
        grid.add(crearFilaInfo("Categoría", trabajo.getCategoria()), 0, 1);
        grid.add(crearFilaInfo("Dirección",
                trabajo.getDireccion().isBlank() ? "Ver mapa en App" : trabajo.getDireccion()), 0, 2);

        grid.add(crearFilaInfo("Fecha Creación", formatFecha(trabajo.getFecha())), 1, 0);
        grid.add(crearFilaInfo("Estado Actual", estado), 1, 1);
        if (!trabajo.getClienteTelefono().isBlank()) {
            grid.add(crearFilaInfo("Contacto Cliente", trabajo.getClienteTelefono()), 1, 2);
        }
        mainBox.getChildren().add(grid);

        // --- SECCIÓN 2: DESCRIPCIÓN Y FOTOS ---
        VBox descBox = new VBox(10);
        Label lblDesc = new Label("Evolución de la Incidencia / Historial:");
        lblDesc.getStyleClass().add("etiqueta-modal");

        VBox contenedorBloques = new VBox(8);
        contenedorBloques.setPadding(new Insets(10));
        contenedorBloques.getStyleClass().add("contenedor-descripcion-modal");

        poblarBloquesDescripcion(contenedorBloques, trabajo.getDescripcion());

        ScrollPane scrollDesc = new ScrollPane(contenedorBloques);
        scrollDesc.setFitToWidth(true);
        scrollDesc.setPrefHeight(250);
        scrollDesc.setMinHeight(250);
        scrollDesc.getStyleClass().add("scroll-transparente"); // Clase CSS ya existente para scrollbars oscuros

        descBox.getChildren().addAll(lblDesc, scrollDesc);
        mainBox.getChildren().add(descBox);

        if (trabajo.getUrlsFotos() != null && !trabajo.getUrlsFotos().isEmpty()) {
            HBox hboxFotos = new HBox(10);
            for (String url : trabajo.getUrlsFotos()) {
                try {
                    ImageView iv = new ImageView(new Image(url, 100, 100, true, true, true));
                    iv.setFitWidth(100);
                    iv.setFitHeight(100);
                    iv.setCursor(Cursor.HAND);
                    iv.getStyleClass().add("foto-mini");
                    Rectangle clip = new Rectangle(100, 100);
                    clip.setArcWidth(12);
                    clip.setArcHeight(12);
                    iv.setClip(clip);
                    iv.setOnMouseClicked(e -> mostrarFotoGrande(url));
                    hboxFotos.getChildren().add(iv);
                } catch (Exception ignored) {
                }
            }
            if (!hboxFotos.getChildren().isEmpty()) {
                ScrollPane scrollFotos = new ScrollPane(hboxFotos);
                scrollFotos.getStyleClass().add("scroll-transparente");
                scrollFotos.setPrefHeight(120);
                mainBox.getChildren().add(new VBox(6, new Label("Evidencias visuales:"), scrollFotos));
            }
        }

        // --- SECCIÓN 3: ÁREA DE GESTIÓN (DInámica por estado) ---
        mainBox.getChildren().add(new Separator());

        // 3.1: Información de Presupuesto (Si existe)
        TrabajoFX.MetaPresupuesto miMeta = trabajo.getMiUltimoPresupuesto(idEmpresaLogueada);
        if (miMeta != null) {
            mainBox.getChildren().add(crearCajaPresupuesto(miMeta, estado));
        }

        // 3.2: Acciones dependiendo del flujo
        TextField txtMonto = new TextField();
        TextArea areaPropuesta = new TextArea();
        ComboBox<OperarioFX> comboOps = new ComboBox<>();

        if (permitirPresupuestar) {
            // Formulario para ofertar (Si es PENDIENTE o RECHAZADO)
            VBox formPpto = new VBox(10);
            Label lblPpto = new Label(miMeta != null ? "Actualizar Oferta (Re-puja):" : "Presentar Presupuesto:");
            lblPpto.getStyleClass().add("etiqueta-modal");

            areaPropuesta.setPromptText("Notas técnicas sobre la reparación...");
            areaPropuesta.setWrapText(true);
            areaPropuesta.setPrefHeight(80);
            areaPropuesta.getStyleClass().add("entrada-modal");

            // Permitir que el TAB pase al siguiente campo (txtMonto)
            areaPropuesta.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == javafx.scene.input.KeyCode.TAB) {
                    txtMonto.requestFocus();
                    event.consume();
                }
            });

            txtMonto.setPromptText("Importe en €");
            txtMonto.getStyleClass().add("entrada-modal");

            formPpto.getChildren().addAll(lblPpto, areaPropuesta, txtMonto);
            mainBox.getChildren().add(formPpto);

        } else if ("ACEPTADO".equals(estado) || "ASIGNADO".equals(estado)) {
            // Panel de Asignación de Técnico
            VBox assignBox = new VBox(10);
            Label lblAssign = new Label("Asignación de Técnico Responsable:");
            lblAssign.getStyleClass().add("etiqueta-modal");

            comboOps.setPromptText("Seleccionar operario...");
            comboOps.getItems().addAll(operariosDisponibles.stream().filter(OperarioFX::isActivo).toList());
            comboOps.getStyleClass().add("combo-modal");
            comboOps.setMaxWidth(Double.MAX_VALUE);

            if (trabajo.getIdOperario() > 0) {
                operariosDisponibles.stream()
                        .filter(o -> o.getId() == trabajo.getIdOperario())
                        .findFirst()
                        .ifPresent(comboOps::setValue);
            }

            assignBox.getChildren().addAll(lblAssign, comboOps);
            mainBox.getChildren().add(assignBox);

            if ("ASIGNADO".equals(estado)) {
                Label infoAsig = new Label("Técnico actual: " + trabajo.getOperario());
                infoAsig.getStyleClass().add("estado-disponible");
                assignBox.getChildren().add(0, infoAsig);
            }
        }

        // --- SECCIÓN 4: VALORACIÓN FINAL ---
        if (trabajo.getValoracion() > 0) {
            mainBox.getChildren().add(new Separator());
            mainBox.getChildren().add(crearCajaValoracion());
        }

        // Envolver en ScrollPane para que sea movible si hay mucho contenido
        ScrollPane scrollPane = new ScrollPane(mainBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(650); // Altura máxima recomendada
        scrollPane.getStyleClass().add("scroll-transparente"); // Reutilizar clase si existe

        dialog.getDialogPane().setContent(scrollPane);

        // --- BOTONES ---
        ButtonType btnOk = new ButtonType(
                permitirPresupuestar ? "Enviar Propuesta"
                        : ("ACEPTADO".equals(estado) || "ASIGNADO".equals(estado)) ? "Confirmar Asignación" : "Cerrar",
                ButtonBar.ButtonData.OK_DONE);

        dialog.getDialogPane().getButtonTypes().add(btnOk);
        if (permitirPresupuestar || "ACEPTADO".equals(estado) || "ASIGNADO".equals(estado)) {
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        }

        Button okBtn = (Button) dialog.getDialogPane().lookupButton(btnOk);
        okBtn.getStyleClass().add("btn-primario");

        Button cancelBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        if (cancelBtn != null) {
            cancelBtn.getStyleClass().add("btn-secundario");
        }

        // Convertidor de resultados
        dialog.setResultConverter(bt -> {
            if (bt == btnOk) {
                if (permitirPresupuestar) {
                    try {
                        double m = Double.parseDouble(txtMonto.getText().replace(",", "."));
                        return new Resultado(m, areaPropuesta.getText(), null);
                    } catch (Exception e) {
                        return null;
                    }
                } else if ("ACEPTADO".equals(estado) || "ASIGNADO".equals(estado)) {
                    OperarioFX sel = comboOps.getValue();
                    return new Resultado(null, null, sel != null ? sel.getId() : -1);
                }
            }
            return null;
        });

        return dialog.showAndWait();
    }

    private VBox crearCajaPresupuesto(TrabajoFX.MetaPresupuesto meta, String estado) {
        VBox box = new VBox(8);
        box.setPadding(new Insets(12));
        box.getStyleClass().add("caja-ppto-modal");
        box.setStyle("-fx-background-radius: 10; -fx-border-radius: 10;"); // Mantener radios para consistencia visual
                                                                           // inmediata

        boolean esRechazado = "RECHAZADO".equals(meta.estado());
        String ti = switch (estado) {
            case "PENDIENTE", "PRESUPUESTADO" -> esRechazado ? "⚠️ OFERTA ANTERIOR RECHAZADA" : "📩 OFERTA ENVIADA";
            case "ACEPTADO", "ASIGNADO" -> "✅ OFERTA SELECCIONADA";
            case "FINALIZADO" -> "💰 IMPORTE FINAL TRABAJO";
            default -> "📊 INFO PRESUPUESTO";
        };

        Label lblT = new Label(ti);
        lblT.getStyleClass().addAll("estado-ppto-modal",
                esRechazado ? "estado-ppto-modal-error" : "estado-ppto-modal-ok");

        Label lblM = new Label(String.format("%.2f €", meta.monto()));
        lblM.getStyleClass().add("monto-ppto-modal");

        box.getChildren().addAll(lblT, lblM);
        if (!meta.notas().isBlank()) {
            Label lblN = new Label(meta.notas());
            lblN.setWrapText(true);
            lblN.getStyleClass().add("texto-sin-asignar"); // Reutilizamos estilo cursiva tenue
            box.getChildren().add(lblN);
        }
        return box;
    }

    private VBox crearCajaValoracion() {
        VBox box = new VBox(5);
        String stars = "★".repeat(trabajo.getValoracion()) + "☆".repeat(5 - trabajo.getValoracion());
        Label lblS = new Label(stars);
        lblS.getStyleClass().add("estrellas-modal");
        Label lblC = new Label("\"" + trabajo.getComentarioCliente() + "\"");
        lblC.getStyleClass().add("texto-sin-asignar");
        lblC.setWrapText(true);
        box.getChildren().addAll(new Label("Valoración del cliente:"), lblS, lblC);
        return box;
    }

    private VBox crearFilaInfo(String etiqueta, String valor) {
        Label lbl = new Label(etiqueta);
        lbl.getStyleClass().add("etiqueta-modal");
        Label val = new Label(valor);
        val.getStyleClass().add("valor-modal");
        val.setWrapText(true);

        // Si es dirección, le damos estilo de link
        if (etiqueta.contains("Dirección")) {
            val.getStyleClass().add("enlace-mapa-modal");
            val.setOnMouseClicked(e -> {
                try {
                    String query = valor.replace(" ", "+");
                    String url = "https://www.google.com/maps/search/?api=1&query=" + query;
                    Runtime.getRuntime().exec("cmd /c start " + url.replace("&", "^&"));
                } catch (Exception ex) {
                    System.err.println("Error al abrir mapas: " + ex.getMessage());
                }
            });
        }

        return new VBox(2, lbl, val);
    }

    /**
     * Parsea el string de descripción para generar bloques visuales independientes.
     */
    private void poblarBloquesDescripcion(VBox contenedor, String descripcion) {
        if (descripcion == null || descripcion.trim().isEmpty())
            return;

        String[] marcadores = { "📝 CLIENTE:", "💰 GERENTE:", "🛠 OPERARIO:" };
        String descActual = descripcion.replace("==============================", ""); // Limpiar separadores planos

        for (int i = 0; i < marcadores.length; i++) {
            String marcador = marcadores[i];
            if (descActual.contains(marcador)) {
                int inicio = descActual.indexOf(marcador);

                // Buscar dónde termina este bloque (donde empiece el siguiente marcador)
                int fin = descActual.length();
                for (int j = i + 1; j < marcadores.length; j++) {
                    int posSiguienteMarcador = descActual.indexOf(marcadores[j]);
                    if (posSiguienteMarcador != -1 && posSiguienteMarcador < fin) {
                        fin = posSiguienteMarcador;
                    }
                }

                String contenidoBloque = descActual.substring(inicio + marcador.length(), fin).trim();
                if (!contenidoBloque.isEmpty() && !contenidoBloque.contains("(Sin ")) {
                    contenedor.getChildren().add(crearBloqueVisual(marcador, contenidoBloque));
                }
            }
        }

        // Fallback estética: si no se detectó ningún marcador oficial, mostrar el texto
        // bruto en un bloque estándar
        if (contenedor.getChildren().isEmpty() && !descripcion.trim().isEmpty()) {
            contenedor.getChildren().add(crearBloqueVisual("DESCRIPCIÓN:", descripcion.trim()));
        }
    }

    /**
     * Construye un bloque visual con estilo diferenciado según el rol.
     */
    private VBox crearBloqueVisual(String titulo, String contenido) {
        VBox bloque = new VBox(4);
        bloque.setPadding(new Insets(10));
        bloque.setMaxWidth(Double.MAX_VALUE);

        String extraClass = "bloque-desc-defecto";

        if (titulo.contains("CLIENTE")) {
            extraClass = "bloque-desc-cliente";
        } else if (titulo.contains("GERENTE")) {
            extraClass = "bloque-desc-gerente";
        } else if (titulo.contains("OPERARIO")) {
            extraClass = "bloque-desc-operario";
        }

        bloque.getStyleClass().addAll("bloque-desc", extraClass);

        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add("bloque-desc-titulo");

        Label lblContenido = new Label(contenido);
        lblContenido.setWrapText(true);
        lblContenido.getStyleClass().add("bloque-desc-contenido");

        bloque.getChildren().addAll(lblTitulo, lblContenido);
        return bloque;
    }

    private void mostrarFotoGrande(String url) {
        Dialog<Void> imgDialog = new Dialog<>();
        imgDialog.setTitle("Evidencia Visual");

        if (cssPath != null) {
            imgDialog.getDialogPane().getStylesheets().add(cssPath);
        }
        imgDialog.getDialogPane().getStyleClass().add("panel-dialogo");

        ImageView img = new ImageView(new Image(url));
        img.setFitWidth(800);
        img.setFitHeight(600);
        img.setPreserveRatio(true);
        imgDialog.getDialogPane().setContent(img);

        imgDialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        Button closeBtn = (Button) imgDialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        if (closeBtn != null) {
            closeBtn.getStyleClass().add("btn-secundario");
        }

        imgDialog.show();
    }

    private String formatFecha(String fechaRaw) {
        if (fechaRaw == null || fechaRaw.isBlank())
            return "Fecha no disp.";
        try {
            // 2026-04-17T12:04:16 -> 17/04/2026 12:04
            String[] partes = fechaRaw.split("T");
            String fecha = partes[0];
            String hora = partes.length > 1 ? partes[1].substring(0, 5) : "";

            String[] ymd = fecha.split("-");
            if (ymd.length == 3) {
                return String.format("%s/%s/%s %s", ymd[2], ymd[1], ymd[0], hora).trim();
            }
        } catch (Exception e) {
            return fechaRaw.replace("T", " ");
        }
        return fechaRaw.replace("T", " ");
    }
}

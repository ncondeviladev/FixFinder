package com.fixfinder.ui.dashboard.dialogos;

import com.fixfinder.ui.dashboard.modelos.TrabajoFX;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import java.util.Optional;

/**
 * Diálogo modular unificado para ver detalles de una incidencia y opcionalmente
 * crear un presupuesto.
 * Reúne la visualización de fotos, descripción y datos del cliente con el
 * formulario de presupuesto.
 */
public class DialogoDetalleIncidencia {

    private final TrabajoFX trabajo;
    private final String cssPath;
    private final boolean permitirPresupuestar;

    public record Resultado(double monto, String notas) {
    }

    public DialogoDetalleIncidencia(TrabajoFX trabajo, String cssPath,
            boolean permitirPresupuestar) {
        this.trabajo = trabajo;
        this.cssPath = cssPath;
        this.permitirPresupuestar = permitirPresupuestar;
    }

    public Optional<Resultado> mostrar() {
        Dialog<Resultado> dialog = new Dialog<>();
        dialog.setTitle(permitirPresupuestar ? "Gestión de Incidencia" : "Detalle de Incidencia");
        dialog.setHeaderText(trabajo.getTitulo());

        dialog.getDialogPane().getStylesheets().add(cssPath);
        dialog.getDialogPane().getStyleClass().add("dialog-pane");

        // Contenedor principal
        VBox mainBox = new VBox(15);
        mainBox.setPadding(new Insets(10, 0, 10, 0));
        mainBox.setPrefWidth(550);

        // --- SECCIÓN 1: INFO BÁSICA Y CLIENTE ---
        GridPane grid = new GridPane();
        grid.setHgap(30);
        grid.setVgap(10);

        // Column constraints for 50/50 split
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(col1, col2);

        grid.add(crearFilaInfo("Cliente", trabajo.getCliente()), 0, 0);
        grid.add(crearFilaInfo("Categoría", trabajo.getCategoria()), 0, 1);
        grid.add(crearFilaInfo("Dirección", trabajo.getDireccion().isBlank() ? "S/N" : trabajo.getDireccion()), 0, 2);

        grid.add(crearFilaInfo("Fecha", trabajo.getFecha()), 1, 0);
        grid.add(crearFilaInfo("Estado", trabajo.getEstado()), 1, 1);
        if (!trabajo.getClienteTelefono().isBlank()) {
            grid.add(crearFilaInfo("Contacto", trabajo.getClienteTelefono()), 1, 2);
        }

        mainBox.getChildren().add(grid);

        // --- SECCIÓN 2: DESCRIPCIÓN ---
        if (!trabajo.getDescripcion().isBlank() || permitirPresupuestar) {
            VBox descBox = new VBox(6);
            Label lblDesc = new Label(permitirPresupuestar ? "Hoja Informativa (Editable):" : "Descripción:");
            lblDesc.getStyleClass().add("modal-label");

            TextArea areaDesc = new TextArea(trabajo.getDescripcion());
            areaDesc.setWrapText(true);
            areaDesc.setPrefHeight(permitirPresupuestar ? 200 : 160);
            areaDesc.getStyleClass().add("modal-input");
            areaDesc.setEditable(permitirPresupuestar);
            if (!permitirPresupuestar) {
                areaDesc.setStyle("-fx-opacity: 0.8;");
            }

            descBox.getChildren().addAll(lblDesc, areaDesc);
            mainBox.getChildren().add(descBox);
        }

        // --- SECCIÓN 3: FOTOS ---
        if (trabajo.getUrlsFotos() != null && !trabajo.getUrlsFotos().isEmpty()) {
            VBox photosBox = new VBox(8);
            Label lblFotos = new Label("Fotos adjuntas:");
            lblFotos.getStyleClass().add("modal-label");

            HBox hboxFotos = new HBox(10);
            for (String url : trabajo.getUrlsFotos()) {
                ImageView iv = new ImageView();
                try {
                    Image img = new Image(url, 120, 120, true, true, true);
                    iv.setImage(img);
                    iv.setFitWidth(100);
                    iv.setFitHeight(100);
                    iv.setPreserveRatio(true);
                    iv.setCursor(Cursor.HAND);
                    iv.getStyleClass().add("photo-mini");

                    // Clip con esquinas redondeadas
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
                scrollFotos.getStyleClass().add("transparent-scroll");
                scrollFotos.setFitToHeight(true);
                scrollFotos.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                scrollFotos.setPrefHeight(120);
                photosBox.getChildren().addAll(lblFotos, scrollFotos);
                mainBox.getChildren().add(photosBox);
            }
        }

        // --- SECCIÓN 4: PRESUPUESTO (Si aplica) ---
        TextField txtMonto = new TextField();
        if (permitirPresupuestar) {
            Separator sep = new Separator();
            sep.setPadding(new Insets(5, 0, 5, 0));

            VBox budgetBox = new VBox(8);
            Label lblPpto = new Label("Ofertar Presupuesto (€):");
            lblPpto.getStyleClass().add("modal-label");

            txtMonto.setPromptText("Ej: 150.00");
            txtMonto.getStyleClass().add("modal-input");
            txtMonto.setMaxWidth(Double.MAX_VALUE);

            budgetBox.getChildren().addAll(lblPpto, txtMonto);
            mainBox.getChildren().addAll(sep, budgetBox);
        }

        // --- SECCIÓN 5: VALORACIÓN (Si ya está finalizado) ---
        if (trabajo.getValoracion() > 0) {
            VBox valBox = new VBox(5);
            valBox.setPadding(new Insets(10, 0, 0, 0));
            Label lblVal = new Label("Valoración del Cliente:");
            lblVal.getStyleClass().add("modal-label");

            HBox stars = new HBox(4);
            stars.setAlignment(Pos.CENTER_LEFT);
            for (int i = 0; i < 5; i++) {
                Label star = new Label("★");
                if (i < trabajo.getValoracion()) {
                    star.setStyle("-fx-text-fill: #FBBF24; -fx-font-size: 20px;");
                } else {
                    star.setStyle("-fx-text-fill: #334155; -fx-font-size: 20px;");
                }
                stars.getChildren().add(star);
            }

            Label lblComent = new Label("\"" + trabajo.getComentarioCliente() + "\"");
            lblComent.setStyle("-fx-text-fill: #94A3B8; -fx-font-style: italic; -fx-font-size: 13px;");
            lblComent.setWrapText(true);

            valBox.getChildren().addAll(lblVal, stars, lblComent);
            mainBox.getChildren().add(valBox);
        }

        dialog.getDialogPane().setContent(mainBox);

        // --- BOTONES ---
        ButtonType btnOk = new ButtonType(permitirPresupuestar ? "Enviar Presupuesto" : "Cerrar",
                ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancel = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);

        if (permitirPresupuestar) {
            dialog.getDialogPane().getButtonTypes().addAll(btnOk, btnCancel);
            Button botonOk = (Button) dialog.getDialogPane().lookupButton(btnOk);
            botonOk.getStyleClass().add("btn-primary");
            Button botonCancel = (Button) dialog.getDialogPane().lookupButton(btnCancel);
            botonCancel.getStyleClass().add("btn-secondary");
            botonOk.setDisable(true);

            txtMonto.textProperty().addListener((obs, old, val) -> {
                try {
                    double v = Double.parseDouble(val.replace(",", "."));
                    botonOk.setDisable(v <= 0);
                } catch (Exception ex) {
                    botonOk.setDisable(true);
                }
            });

            dialog.setResultConverter(bt -> {
                if (bt == btnOk) {
                    try {
                        double m = Double.parseDouble(txtMonto.getText().replace(",", "."));
                        TextArea area = (TextArea) ((VBox) mainBox.getChildren().stream()
                                .filter(n -> n instanceof VBox && !((VBox) n).getChildren().isEmpty()
                                        && ((VBox) n).getChildren().get(0) instanceof Label
                                        && ((Label) ((VBox) n).getChildren().get(0)).getText().contains("Hoja"))
                                .findFirst().orElse(null)).getChildren().get(1);
                        return new Resultado(m, area.getText());
                    } catch (Exception ex) {
                        return null;
                    }
                }
                return null;
            });
        } else {
            dialog.getDialogPane().getButtonTypes().add(btnOk);
            Button botonOk = (Button) dialog.getDialogPane().lookupButton(btnOk);
            botonOk.getStyleClass().add("btn-secondary");
        }

        return dialog.showAndWait();
    }

    private VBox crearFilaInfo(String etiqueta, String valor) {
        VBox v = new VBox(2);
        Label lbl = new Label(etiqueta);
        lbl.getStyleClass().add("modal-label");
        Label val = new Label(valor);
        val.getStyleClass().add("modal-value");
        val.setWrapText(true);
        v.getChildren().addAll(lbl, val);
        return v;
    }

    private void mostrarFotoGrande(String url) {
        Dialog<Void> imgDialog = new Dialog<>();
        imgDialog.setTitle("Foto ampliada");
        ImageView imgFull = new ImageView(new Image(url));
        imgFull.setFitWidth(800);
        imgFull.setFitHeight(600);
        imgFull.setPreserveRatio(true);
        imgDialog.getDialogPane().setContent(imgFull);
        imgDialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        imgDialog.getDialogPane().getStylesheets().add(cssPath);
        imgDialog.getDialogPane().getStyleClass().add("dialog-pane");
        Button btnClose = (Button) imgDialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        if (btnClose != null) btnClose.getStyleClass().add("btn-secondary");
        imgDialog.show();
    }
}

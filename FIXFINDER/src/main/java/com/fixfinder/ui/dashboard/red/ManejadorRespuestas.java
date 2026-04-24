package com.fixfinder.ui.dashboard.red;

import com.fasterxml.jackson.databind.JsonNode;
import com.fixfinder.ui.dashboard.modelos.OperarioFX;
import com.fixfinder.ui.dashboard.modelos.TrabajoFX;
import com.fixfinder.ui.dashboard.vistas.VistaDashboard;
import com.fixfinder.ui.dashboard.vistas.VistaEmpresa;
import com.fixfinder.ui.dashboard.utils.ToastUtils;
import javafx.collections.ObservableList;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Clase especializada en procesar e interpretar las respuestas JSON del
 * servidor.
 * Desacopla la lógica de red de la gestión de la interfaz de usuario,
 * cumpliendo
 * con el principio de responsabilidad única.
 */
public class ManejadorRespuestas {

    private final ObservableList<TrabajoFX> todosTrabajos;
    private final ObservableList<OperarioFX> listaOperarios;
    private final Map<String, Object> infoEmpresaActual;
    private final Consumer<String> alRegistrarActividad;
    private final Runnable alSolicitarRefresco;
    private final Consumer<String> alNavegarA;
    private final java.util.function.BiConsumer<String, String> alActualizarPerfilLateral; // (nombre, urlFoto)
    private final int idEmpresaLogueada;
    private final int idUsuarioLogueado;

    /**
     * Constructor del manejador de respuestas.
     * 
     * @param todosTrabajos        Lista observable de trabajos.
     * @param listaOperarios       Lista observable de operarios.
     * @param infoEmpresaActual    Mapa de datos de la empresa actual.
     * @param alRegistrarActividad Callback para registrar eventos en el historial.
     * @param alSolicitarRefresco  Callback para pedir una actualización de datos al
     *                             servidor.
     * @param alNavegarA           Callback para forzar navegación a una vista
     *                             específica.
     * @param idEmpresaLogueada    ID de la empresa del usuario en sesión.
     */
    public ManejadorRespuestas(ObservableList<TrabajoFX> todosTrabajos,
            ObservableList<OperarioFX> listaOperarios,
            Map<String, Object> infoEmpresaActual,
            Consumer<String> alRegistrarActividad,
            Runnable alSolicitarRefresco,
            Consumer<String> alNavegarA,
            java.util.function.BiConsumer<String, String> alActualizarPerfilLateral,
            int idEmpresaLogueada,
            int idUsuarioLogueado) {
        this.todosTrabajos = todosTrabajos;
        this.listaOperarios = listaOperarios;
        this.infoEmpresaActual = infoEmpresaActual;
        this.alRegistrarActividad = alRegistrarActividad;
        this.alSolicitarRefresco = alSolicitarRefresco;
        this.alNavegarA = alNavegarA;
        this.alActualizarPerfilLateral = alActualizarPerfilLateral;
        this.idEmpresaLogueada = idEmpresaLogueada;
        this.idUsuarioLogueado = idUsuarioLogueado;
    }

    /**
     * Procesa la carga lógica de una respuesta del servidor.
     * 
     * @param msg            Mensaje descriptivo de la respuesta.
     * @param datos          Nodo JSON con la información recibida.
     * @param status         Código de estado HTTP de la respuesta.
     * @param vistaDashboard Referencia a la vista para actualizar KPIs.
     * @param rootPane       Referencia al panel raíz para detectar la vista activa.
     */
    public void procesar(String accion, String msg, JsonNode datos, int status, VistaDashboard vistaDashboard,
            BorderPane rootPane) {
        System.out.println("📡 [MANEJADOR] Accion: " + accion + " | Status: " + status);

        if (accion == null) {
            // Fallback por si el servidor no envía acción (compatibilidad)
            if (msg != null && msg.toLowerCase().contains("listado"))
                accion = "LISTAR_TRABAJOS";
            else if (msg != null && msg.toLowerCase().contains("operarios"))
                accion = "GET_OPERARIOS";
            else if (msg != null && msg.toLowerCase().contains("empresa"))
                accion = "GET_EMPRESA";
            else
                return;
        }

        switch (accion) {
            case "LISTAR_TRABAJOS":
                if (datos != null && datos.isArray()) {
                    procesarListaTrabajos(datos, vistaDashboard);
                }
                break;

            case "GET_OPERARIOS":
                if (datos != null && datos.isArray()) {
                    procesarListaOperarios(datos);
                }
                break;

            case "GET_EMPRESA":
                if (datos != null && !datos.isArray()) {
                    actualizarEstadoEmpresa(datos, rootPane);
                }
                break;

            case "REGISTRO":
            case "ASIGNAR_OPERARIO":
            case "MODIFICAR_OPERARIO":
            case "MODIFICAR_EMPRESA":
            case "MODIFICAR_USUARIO":
            case "CREAR_PRESUPUESTO":
            case "ACEPTAR_PRESUPUESTO":
            case "RECHAZAR_PRESUPUESTO":
            case "CANCELAR_TRABAJO":
            case "ACTUALIZAR_FOTO_PERFIL":
                if (status < 400)
                    alSolicitarRefresco.run();
                else
                    alRegistrarActividad.accept("❌ " + msg);
                break;

            case "BROADCAST":
                procesarBroadcast(datos, rootPane);
                break;

            default:
                if (status >= 400)
                    alRegistrarActividad.accept("❌ " + msg);
                break;
        }
    }

    /**
     * Reacciona a notificaciones en tiempo real procedentes del Broadcaster central.
     * @param datos Información del evento (categoría, subtipo, id, etc).
     */
    private void procesarBroadcast(JsonNode datos, BorderPane rootPane) {
        if (datos == null) return;

        String categoria = datos.path("categoria").asText("SISTEMA");
        String subtipo = datos.path("subtipo").asText("");
        String info = datos.path("info").asText("");

        System.out.println("🔔 [NOTIFICATION] Recepción: " + categoria + " -> " + subtipo);

        // Intentar obtener el Stage para el Toast y las alertas de sistema
        Stage stage = null;
        if (rootPane != null && rootPane.getScene() != null) {
            stage = (Stage) rootPane.getScene().getWindow();
        }

        // Lógica de filtrado de aviso visual (Toast)
        int idEmpresaEvento = datos.path("idEmpresa").asInt(-1);
        int idClienteEvento = datos.path("idCliente").asInt(-1);
        
        boolean esParaMiEmpresa = (idEmpresaEvento == idEmpresaLogueada);
        boolean esParaMiUsuario = (idClienteEvento == idUsuarioLogueado);
        boolean esNuevoGlobal = "NUEVO".equals(subtipo);

        // 1. Mostrar Notificación Flotante (Toast) SOLO si soy el interesado o es algo nuevo global
        if (stage != null && (esParaMiEmpresa || esParaMiUsuario || esNuevoGlobal)) {
            final Stage finalStage = stage;
            String type = "INFO";
            if ("PRESUPUESTO".equals(categoria)) type = "SUCCESS";
            if ("TRABAJO".equals(categoria) && info.toUpperCase().contains("URGENTE")) type = "WARNING";
            
            ToastUtils.showToast(finalStage, info, type);
            
            // 2. Alertar si está minimizado o en segundo plano
            if (!finalStage.isFocused() && !"USUARIO".equals(categoria)) {
                finalStage.setIconified(false);
                finalStage.toFront();
            }
        }

        // 3. Lógica específica por categoría
        if ("USUARIO".equals(categoria)) {
            int idEvent = datos.path("idUsuario").asInt();
            if (idEvent == idUsuarioLogueado) {
                String nuevoNombre = datos.path("nombre").asText(null);
                String nuevaFoto = datos.path("url_foto").asText(null);
                if (alActualizarPerfilLateral != null) {
                    alActualizarPerfilLateral.accept(nuevoNombre, nuevaFoto);
                }
            }
            alSolicitarRefresco.run(); // Refrescamos para ver cambios de cualquier cliente/operario en las tablas
            return;
        }

        switch (categoria) {
            case "TRABAJO":
                alRegistrarActividad.accept("🔔 " + info);
                alSolicitarRefresco.run(); // Refrescamos tablas para ver la nueva incidencia o el cambio
                break;
            
            case "PRESUPUESTO":
                alRegistrarActividad.accept("💰 " + info);
                alSolicitarRefresco.run();
                break;

            case "VALORACION":
                alRegistrarActividad.accept("⭐ " + info);
                alSolicitarRefresco.run(); // Refrescar KPIs y panel de empresa
                break;

            case "EMPRESA":
                alRegistrarActividad.accept("🏢 " + info);
                alSolicitarRefresco.run(); // Refrescar datos corporativos
                break;

            case "OPERARIO":
                alRegistrarActividad.accept("🔧 " + info);
                alSolicitarRefresco.run(); // Refrescar tabla de personal
                break;

            case "SISTEMA":
                String msg = datos.path("mensaje").asText("Aviso de sistema");
                alRegistrarActividad.accept("📢 " + msg);
                break;
        }
    }

    /**
     * Mapea los datos JSON a la lista de trabajos y recalcula los indicadores
     * (KPIs).
     */
    private void procesarListaTrabajos(JsonNode datos, VistaDashboard vistaDashboard) {
        todosTrabajos.clear();
        int activos = 0, pendientes = 0, completados = 0, presupuestados = 0;

        for (JsonNode n : datos) {
            TrabajoFX t = TrabajoFX.fromNode(n, idEmpresaLogueada);
            todosTrabajos.add(t);

            String estado = t.getEstado();
            // Lógica de conteo para KPIs
            if (!"FINALIZADO".equals(estado) && !"REALIZADO".equals(estado) && !"CANCELADO".equals(estado))
                activos++;

            if (t.haPresupuestado(idEmpresaLogueada)) {
                presupuestados++;
            } else if ("PENDIENTE".equals(estado)) {
                pendientes++;
            }

            if ("REALIZADO".equals(estado) || "FINALIZADO".equals(estado))
                completados++;
        }

        // Ordenación institucional (Prioridad de estado + Antigüedad)
        todosTrabajos.sort((t1, t2) -> {
            int p1 = obtenerPesoEstado(t1.getEstado());
            int p2 = obtenerPesoEstado(t2.getEstado());
            if (p1 != p2)
                return Integer.compare(p1, p2);
            return Integer.compare(t2.getId(), t1.getId());
        });

        // Notificar a la vista si está inicializada
        if (vistaDashboard != null) {
            vistaDashboard.actualizarKpis(activos, pendientes, completados, presupuestados);
        }
    }

    /**
     * Fuerza un recálculo de los KPIs a partir de la lista actual de trabajos.
     * Útil cuando la vista se crea después de que los datos hayan llegado.
     */
    public void actualizarKpisManual(VistaDashboard vistaDashboard) {
        int activos = 0, pendientes = 0, completados = 0, presupuestados = 0;
        for (TrabajoFX t : todosTrabajos) {
            String estado = t.getEstado();
            if (!"FINALIZADO".equals(estado) && !"REALIZADO".equals(estado) && !"CANCELADO".equals(estado))
                activos++;
            if (t.haPresupuestado(idEmpresaLogueada)) {
                presupuestados++;
            } else if ("PENDIENTE".equals(estado)) {
                pendientes++;
            }
            if ("REALIZADO".equals(estado) || "FINALIZADO".equals(estado))
                completados++;
        }
        if (vistaDashboard != null) {
            vistaDashboard.actualizarKpis(activos, pendientes, completados, presupuestados);
        }
    }

    /**
     * Mapea los datos JSON a la lista de operarios de la empresa.
     */
    private void procesarListaOperarios(JsonNode datos) {
        listaOperarios.clear();
        for (JsonNode n : datos) {
            listaOperarios.add(OperarioFX.fromNode(n));
        }
    }

    /**
     * Sincroniza los datos de perfil de la empresa.
     */
    private void actualizarEstadoEmpresa(JsonNode datos, BorderPane rootPane) {
        infoEmpresaActual.clear();
        datos.fields().forEachRemaining(entry -> {
            if (entry.getValue().isArray() || entry.getValue().isObject()) {
                infoEmpresaActual.put(entry.getKey(), entry.getValue());
            } else {
                infoEmpresaActual.put(entry.getKey(), entry.getValue().asText());
            }
        });
        // Si el usuario está viendo la pantalla de empresa, forzamos recarga visual
        if (rootPane != null && rootPane.getCenter() instanceof VistaEmpresa) {
            alNavegarA.accept("empresa");
        }
    }

    /**
     * Define el orden de importancia de los estados en la tabla.
     */
    private int obtenerPesoEstado(String estado) {
        return switch (estado) {
            case "ASIGNADO", "ACEPTADO" -> 1;
            case "PRESUPUESTADO" -> 2;
            case "PENDIENTE" -> 3;
            case "REALIZADO", "FINALIZADO" -> 4;
            default -> 5;
        };
    }
}

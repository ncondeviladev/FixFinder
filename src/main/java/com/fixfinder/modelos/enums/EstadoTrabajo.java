package com.fixfinder.modelos.enums;

/**
 * Representa los posibles estados por los que pasa un trabajo.
 *
 * - PENDIENTE: Creado pero no asignado.
 * - ASIGNADO: Tiene operario pero no ha empezado.
 * - EN_PROCESO: El operario está trabajando en ello.
 * - FINALIZADO: Trabajo completado.
 * - CANCELADO: Trabajo anulado.
 */
public enum EstadoTrabajo {
    PENDIENTE,
    ASIGNADO,
    EN_PROCESO,
    FINALIZADO,
    CANCELADO
}

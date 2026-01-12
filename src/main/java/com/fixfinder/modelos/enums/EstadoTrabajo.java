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
    PENDIENTE, // Sin presupuestos
    PRESUPUESTADO, // Con presupuestos enviados
    ACEPTADO, // Presupuesto aceptado
    ASIGNADO, // Operario asignado
    REALIZADO, // Trabajo técnico terminado, pendiente de facturación
    FINALIZADO, // Cerrado y facturado, pendiente de pago
    PAGADO, // Ciclo completo, pagado por el cliente
    CANCELADO // Cancelado por cliente
}

package com.fixfinder.red.protocolo;

/**
 * Enum que define todas las acciones posibles que el cliente puede solicitar al
 * servidor
 * a través de Sockets.
 * 
 * Es fundamental para que el 'GestorCliente' sepa qué servicio invocar.
 */
public enum TipoAccion {
    // Autenticación
    LOGIN,
    LOGOUT,
    REGISTRO,

    // Gestión de Usuarios
    GET_PERFIL,
    UPDATE_PERFIL,

    // Gestión de Trabajos (Incidencias)
    CREAR_TRABAJO,
    GET_TRABAJOS, // Obtener lista (con filtros según rol)
    GET_TRABAJO_DETALLE,
    UPDATE_ESTADO_TRABAJO,
    ASIGNAR_OPERARIO, // Para gerentes/admin

    // Gestión de Operarios
    GET_OPERARIOS, // Para ver lista de técnicos disponibles
    UPDATE_POSICION, // GPS

    // Otros
    PING // Para verificar conexión
}

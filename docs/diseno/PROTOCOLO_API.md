# Protocolo de Comunicación JSON-Socket

Este documento define el estándar de comunicación entre el Servidor Java y los Clientes (Flutter, Kotlin, JavaFX).

## Estructura General

### Request (Cliente -> Servidor)
```json
{
  "action": "NOMBRE_ACCION",
  "token": "JWT_O_SESSION_ID",
  "data": {
    // Parámetros específicos de la acción
  }
}
```

### Response (Servidor -> Cliente)
```json
{
  "status": 200, // Códigos estilo HTTP: 200 OK, 401 Unauthorized, 500 Error
  "message": "Operación exitosa",
  "data": {
    // Objeto o Array de respuesta
  }
}
```

## Catálogo de Acciones

### 1. Autenticación

#### `LOGIN`
**Request:**
```json
{
  "action": "LOGIN",
  "data": {
    "email": "usuario@ejemplo.com",
    "password": "password123" // Se envía hasheada o en plano si hay SSL (para dev local: plano)
  }
}
```
**Response (Success):**
```json
{
  "status": 200,
  "data": {
    "token": "abc-123-xyz",
    "usuario": {
      "id": 1,
      "nombre": "Juan Pérez",
      "rol": "OPERARIO"
    }
  }
}
```

### 2. Gestión de Trabajos

#### `GET_JOBS` (Obtener trabajos)
**Request:**
```json
{
  "action": "GET_JOBS",
  "token": "...",
  "data": {
    "estado": "PENDIENTE" // Opcional, filtro
  }
}
```

#### `ASSIGN_JOB` (Asignar operario - Solo Gerente)
**Request:**
```json
{
  "action": "ASSIGN_JOB",
  "token": "...",
  "data": {
    "id_trabajo": 105,
    "id_operario": 2
  }
}
```

#### `UPDATE_JOB_STATUS` (Operario actualiza estado)
**Request:**
```json
{
  "action": "UPDATE_JOB_STATUS",
  "token": "...",
  "data": {
    "id_trabajo": 105,
    "nuevo_estado": "EN_PROCESO",
    "ubicacion": { "lat": 40.416, "lon": -3.703 }
  }
}
```

### 3. Facturación

#### `GENERATE_INVOICE`
**Request:**
```json
{
  "action": "GENERATE_INVOICE",
  "token": "...",
  "data": {
    "id_trabajo": 105
  }
}
```
**Response:**
```json
{
  "status": 200,
  "data": {
    "id_factura": 5001,
    "pdf_base64": "JVBERi0xLjQKJ..." // Archivo PDF codificado
  }
}
```

### 4. Notificaciones (Server Push)
El servidor puede enviar mensajes asíncronos al cliente sin petición previa (si el socket se mantiene abierto).

**Event:**
```json
{
  "type": "EVENT",
  "event": "NEW_JOB_ASSIGNED",
  "data": {
    "id_trabajo": 106,
    "titulo": "Fuga de agua"
  }
}
```

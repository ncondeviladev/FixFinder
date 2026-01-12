# Protocolo de Comunicación JSON-Socket

Este documento define el estándar de comunicación entre el Servidor Java y los Clientes (Flutter, Kotlin, JavaFX).

## Estructura General

### Request (Cliente -> Servidor)

```json
{
  "accion": "NOMBRE_ACCION",
  "token": "JWT_O_SESSION_ID",
  "datos": {
    // Parámetros específicos de la acción
  }
}
```

### Response (Servidor -> Cliente)

```json
{
  "status": 200, // Códigos estilo HTTP: 200 OK, 401 Unauthorized, 500 Error
  "mensaje": "Operación exitosa",
  "datos": {
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
  "accion": "LOGIN",
  "datos": {
    "email": "usuario@ejemplo.com",
    "password": "password123" // Se envía hasheada o en plano si hay SSL (para dev local: plano)
  }
}
```

**Response (Success):**

```json
{
  "status": 200,
  "datos": {
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
  "accion": "GET_JOBS",
  "token": "...",
  "datos": {
    "estado": "PENDIENTE" // Opcional, filtro
  }
}
```

#### `ASSIGN_JOB` (Asignar operario - Solo Gerente)

**Request:**

```json
{
  "accion": "ASSIGN_JOB",
  "token": "...",
  "datos": {
    "id_trabajo": 105,
    "id_operario": 2
  }
}
```

#### `UPDATE_JOB_STATUS` (Operario actualiza estado)

**Request:**

```json
{
  "accion": "UPDATE_JOB_STATUS",
  "token": "...",
  "datos": {
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
  "accion": "GENERATE_INVOICE",
  "token": "...",
  "datos": {
    "id_trabajo": 105
  }
}
```

**Response:**

```json
{
  "status": 200,
  "datos": {
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
  "datos": {
    "id_trabajo": 106,
    "titulo": "Fuga de agua"
  }
}
```

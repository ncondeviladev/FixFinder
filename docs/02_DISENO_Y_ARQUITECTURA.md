# 02. Diseño y Arquitectura - FIXFINDER

Este documento detalla la estructura técnica, el modelo de datos y el protocolo de comunicación del sistema.

---

## 1. Arquitectura del Sistema

El sistema sigue una arquitectura cliente-servidor basada en capas para asegurar el desacoplamiento:

### 📦 Responsabilidades por Paquete (Backend)

- **`modelos`**: POJOs puros (Entidades de negocio).
- **`dao`**: Aísla el código SQL. Incluye `ConexionDB` (Singleton) para optimizar recursos.
- **`servicios`**: Lógica de negocio pura. Orquestan las llamadas a los DAOs.
- **`red`**: Manejo de Sockets. `ServidorCentral` abre el puerto y `GestorConexion` gestiona cada hilo cliente.

### 🔄 Flujos Críticos

- **Asignación**: JavaFX -> Sockets -> Operario (Móvil).
- **Facturación**: Proceso en segundo plano (`Thread`) para no bloquear la UI de escritorio.

---

## 2. Modelo de Datos (Diagrama de Clases)

Representación de las entidades principales y sus relaciones:

![Diagrama de Clases](assets/diagramaDeClasesSimple.png)

---

## 3. Protocolo de Comunicación (JSON over Sockets)

Este documento define el estándar de comunicación entre el Servidor Java y los Clientes (Flutter, Kotlin, JavaFX).

### 3.1. Estructura General

#### Request (Cliente -> Servidor)

```json
{
  "accion": "NOMBRE_ACCION",
  "token": "JWT_O_SESSION_ID",
  "datos": {
    // Parámetros específicos de la acción
  }
}
```

#### Response (Servidor -> Cliente)

```json
{
  "status": 200, // Códigos estilo HTTP: 200 OK, 401 Unauthorized, 500 Error
  "mensaje": "Operación exitosa",
  "datos": {
    // Objeto o Array de respuesta
  }
}
```

### 3.2. Catálogo de Acciones

#### 1. Autenticación

##### `LOGIN`

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

#### 2. Gestión de Trabajos

##### `GET_JOBS` (Obtener trabajos)

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

##### `ASSIGN_JOB` (Asignar operario - Solo Gerente)

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

##### `UPDATE_JOB_STATUS` (Operario actualiza estado)

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

#### 3. Facturación

##### `GENERATE_INVOICE`

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

#### 4. Notificaciones (Server Push)

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

---

## 4. Esquema de Base de Datos (SQL)

```sql
-- Estructura simplificada del esquema actual
CREATE TABLE usuario (
    id INT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(100) UNIQUE,
    rol ENUM('ADMIN', 'GERENTE', 'OPERARIO', 'CLIENTE')
);

CREATE TABLE trabajo (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_cliente INT,
    id_operario INT,
    estado ENUM('PENDIENTE', 'PRESUPUESTADO', 'ACEPTADO', 'ASIGNADO', 'REALIZADO', 'FINALIZADO'),
    FOREIGN KEY (id_cliente) REFERENCES usuario(id)
);

CREATE TABLE foto_trabajo (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_trabajo INT,
    url_archivo TEXT, -- Soporta URLs largas de Firebase
    FOREIGN KEY (id_trabajo) REFERENCES trabajo(id)
);
```

_(Para ver el script completo de creación, consultar el archivo original: `assets/ESQUEMA_BD.sql`)_

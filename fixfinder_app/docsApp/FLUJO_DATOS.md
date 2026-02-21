# Mapa Mental del Flujo de Datos en FIXFINDER (Móvil)

Este diagrama muestra paso a paso qué ocurre cuando un usuario realiza una acción desde la App Móvil, cómo viaja la información al servidor y cómo vuelve.

## Leyenda de Colores/Capas

- **(VISTA)**: Lo que ve el usuario (App Móvil Flutter).
- **(RED)**: El socket TCP/IP por donde viajan los JSON.
- **(ROUTER)**: Quien recibe y distribuye el trabajo en el servidor (Java).
- **(CEREBRO)**: Quien piensa y aplica las normas de negocio.
- **(MEMORIA)**: Quien lee/escribe en la base de datos MySQL.

---

```mermaid
sequenceDiagram
    participant Usuario as 👤 Cliente
    participant Vista as 📱 App Móvil
    participant Red as 📡 SocketService
    participant Gestor as 🤵 GestorCliente (Server)
    participant Servicio as 🧠 ServicioTrabajos
    participant DAO as 💾 TrabajoDAO
    participant DB as 🗄️ MySQL

    Note over Usuario, DB: EJEMPLO: UN CLIENTE CREA UNA NUEVA INCIDENCIA

    Usuario->>Vista: 1. Rellena título, foto y pulsa "Enviar"

    rect rgb(200, 240, 255)
    Note right of Vista: CAPA CLIENTE (MÓVIL)
    Vista->>Red: 2. Construye JSON
    Note right of Red: { "action": "CREATE_INCIDENT", "data": { ... } }
    Red->>Gestor: 3. Envía JSON por Socket
    end

    rect rgb(255, 240, 200)
    Note right of Gestor: CAPA SERVIDOR (ROUTER)
    Gestor->>Gestor: 4. Recibe JSON, lee "CREATE_INCIDENT"
    Gestor->>Servicio: 5. Llama a crearTrabajo()
    end

    rect rgb(220, 255, 220)
    Note right of Servicio: CAPA SERVICIO (LÓGICA)
    Servicio->>Servicio: 6. Valida datos (¿Título vacío? ¿Usuario válido?)
    Servicio->>DAO: 7. "Todo OK. Guárdalo en BD."
    end

    rect rgb(255, 220, 220)
    Note right of DAO: CAPA DATOS (PERSISTENCIA)
    DAO->>DB: 8. INSERT INTO trabajos ...
    DB-->>DAO: 9. OK, ID generado: 204
    DAO-->>Servicio: 10. Return ID: 204
    end

    Servicio-->>Gestor: 11. Return Objeto Trabajo
    Gestor-->>Red: 12. Response JSON { status: 200, data: { id: 204 } }
    Red-->>Vista: 13. Decode JSON
    Vista->>Usuario: 14. Muestra: "✅ Incidencia enviada con éxito"
```

## Mapa de Responsabilidades (Quién hace qué)

### 1. 📱 App Móvil (Vista/Cliente)

- Captura datos del usuario.
- Valida campos básicos (email válido, campo no vacío) antes de enviar.
- **NO** toma decisiones de negocio.
- Mantiene el socket abierto.

### 2. 🤵 GestorCliente (Servidor Router)

- Recibe el String JSON crudo.
- Parsea a objeto Java.
- Decide a qué Servicio llamar según el `action`.

### 3. 🧠 Servicios (Lógica Java)

- Aplica reglas de negocio complejas.
- Orquesta operaciones (ej: guardar en BD + notificar a admin).
- Maneja transacciones.

### 4. 💾 DAOs

- Escribe y lee SQL puro.

# Mapa Mental del Flujo de Datos en FIXFINDER

Este diagrama muestra paso a paso qué ocurre cuando un usuario realiza una acción, quién es el responsable de cada tarea y cómo viaja la información.

## Leyenda de Colores/Capas

- **(VISTA)**: Lo que ve el usuario (JavaFX).
- **(RED)**: El cable/wifi por donde viajan los datos.
- **(ROUTER)**: Quien recibe y distribuye el trabajo en el servidor.
- **(CEREBRO)**: Quien piensa y aplica las normas de negocio.
- **(MEMORIA)**: Quien lee/escribe en la base de datos SQL.

---

```mermaid
sequenceDiagram
    participant Usuario as 👤 Usuario (Front)
    participant Vista as 🖥️ Controlador Vista
    participant Red as 📡 ClienteSocket
    participant Gestor as 🤵 GestorCliente (Router)
    participant Servicio as 🧠 Servicio (Cerebro)
    participant DAO as 💾 DAO (Memoria)
    participant DB as 🗄️ MySQL

    Note over Usuario, DB: EJEMPLO: UN JEFE QUIERE CREAR UN OPERARIO NUEVO

    Usuario->>Vista: 1. Rellena formulario (Nombre, DNI...) y pulsa "Guardar"

    rect rgb(200, 240, 255)
    Note right of Vista: CAPA CLIENTE (FRONTEND)
    Vista->>Red: 2. "Empaqueta" los datos en una Peticion
    Note right of Red: Peticion { Accion: ALTA_OPERARIO, Cuerpo: Operario(...) }
    Red->>Gestor: 3. Envía objeto por Internet (TCP)
    end

    rect rgb(255, 240, 200)
    Note right of Gestor: CAPA SERVIDOR (ROUTER)
    Gestor->>Gestor: 4. Recibe Petición y mira la Acción
    Gestor->>Servicio: 5. "¡ServicioEmpresa! Alguien quiere dar de alta un operario."
    end

    rect rgb(220, 255, 220)
    Note right of Servicio: CAPA SERVICIO (LÓGICA)
    Servicio->>Servicio: 6. Valida reglas:
    Note right of Servicio: - ¿El usuario que pide esto es JEFE? (Seguridad)
    Note right of Servicio: - ¿El DNI tiene formato correcto?
    Servicio->>DAO: 7. "Todo correcto. OperarioDAO, guárdalo."
    end

    rect rgb(255, 220, 220)
    Note right of DAO: CAPA DATOS (PERSISTENCIA)
    DAO->>DB: 8. INSERT INTO usuario...; INSERT INTO operario...;
    DB-->>DAO: 9. OK, ID generado: 55
    DAO-->>Servicio: 10. Guardado con éxito.
    end

    Servicio-->>Gestor: 11. Devuelve resultado (true).
    Gestor-->>Red: 12. Envía Respuesta { Exito: true, Mensaje: "Creado" }
    Red-->>Vista: 13. Desempaqueta respuesta.
    Vista->>Usuario: 14. Muestra popup: "✅ Operario creado correctamente"
```

## Mapa de Responsabilidades (Quién hace qué)

### 1. 🖥️ Controladores de Vista (Frontend)

Solo se preocupan de la **INTERFAZ**.

- Recoger texto de los inputs.
- Mostrar alertas de error.
- **NO** validan DNIs ni reglas complejas.
- **NO** saben SQL.

### 2. 🤵 GestorCliente (Router del Servidor)

Solo se preocupa de la **COMUNICACIÓN**.

- Lee el objeto del socket.
- `switch(accion)` para decidir a quién llamar.
- Encatcha errores generales (servidor caído, JSON mal formado).
- **NO** sabe si un DNI es válido o no. Solo pasa el paquete.

### 3. 🧠 Servicios (Lógica de Negocio)

Aquí está la **INTELIGENCIA**. Tendremos varios especialistas:

| Servicio               | Responsabilidad         | Acciones que maneja                                                                       |
| :--------------------- | :---------------------- | :---------------------------------------------------------------------------------------- |
| **`ServicioAuth`**     | Seguridad y Accesos     | `LOGIN`, `REGISTRO_CLIENTE`, `REGISTRO_EMPRESA`, `LOGOUT`                                 |
| **`ServicioEmpresa`**  | Gestión interna empresa | `CREAR_OPERARIO`, `LISTAR_OPERARIOS`, `BAJA_OPERARIO`                                     |
| **`ServicioTrabajos`** | Gestión de incidencias  | `CREAR_TRABAJO`, `LISTAR_TRABAJOS` (filtra por rol), `ASIGNAR_OPERARIO`, `CAMBIAR_ESTADO` |
| **`ServicioFacturas`** | Dinero y Documentos     | `GENERAR_PRESUPUESTO`, `ACEPTAR_PRESUPUESTO`, `GENERAR_FACTURA`                           |

### 4. 💾 DAOs (Acceso a Datos)

Solo se preocupan del **SQL**.

- `UsuarioDAO`: `SELECT`, `INSERT` usuarios.
- `TrabajoDAO`: `SELECT`, `INSERT` trabajos.
- **NO** validan permisos. Si les dices "borra", borran.

---

## Resumen de Acciones Necesarias (`TipoAccion`)

Para que el programa funcione completo, estas son las "órdenes" que el Router debe entender:

1.  **AUTH:**

    - `LOGIN`: Entrar.
    - `LOGOUT`: Salir.
    - `REGISTRO_CLIENTE`: Crear cuenta usuario final.
    - `REGISTRO_EMPRESA`: Crear cuenta empresa + admin.

2.  **TRABAJOS (El Core):**

    - `CREAR_INCIDENCIA`: Cliente sube avería.
    - `VER_INCIDENCIAS`: Empresa ve lista de trabajos pendientes (filtrado por su zona/especialidad).
    - `VER_MIS_TRABAJOS`: Cliente ve sus averías. Operario ve sus tareas.

3.  **GESTIÓN (Empresa):**

    - `CREAR_OPERARIO`: Jefe da de alta empleado.
    - `LISTAR_OPERARIOS`: Jefe ve su plantilla.
    - `ASIGNAR_TRABAJO`: Jefe vincula Trabajo <-> Operario.

4.  **FLUJO DE TRABAJO:**
    - `ENVIAR_PRESUPUESTO`: Empresa -> Cliente.
    - `ACEPTAR_PRESUPUESTO`: Cliente -> Empresa.
    - `FINALIZAR_TRABAJO`: Operario marca fin.

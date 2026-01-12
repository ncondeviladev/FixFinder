# Explicación Técnica y Flujos del Sistema

Este documento detalla la responsabilidad técnica de las clases principales y cómo interactúan en los flujos más importantes del sistema.

## 1. Resumen de Responsabilidades por Paquete

### 📦 `modelos` (Entidades)
Son POJOs (Plain Old Java Objects) puros.
*   **`Usuario` / `Operario`**: Representan a los actores. `Operario` extiende `Usuario` añadiendo datos específicos (latitud, especialidad).
*   **`Trabajo`**: La clase central. Vincula un `Cliente`, un `Operario` y un `Estado`.
*   **`Factura`**: Resultado final del proceso. Contiene los cálculos financieros.

### 🏦 `dao` (Acceso a Datos)
Aísla el código SQL del resto de la aplicación.
*   **`ConexionDB`**: Singleton. Garantiza que solo haya una conexión física a MySQL abierta, optimizando recursos.
*   **`TrabajoDAO`**: Contiene los `JOIN` complejos. Ejemplo: al pedir un trabajo, hace JOIN con `Usuario` para traer el nombre del cliente en una sola consulta.

### 🧠 `servicios` (Lógica y PSP)
El "cerebro" que no tiene interfaz gráfica.
*   **`ServidorTareas`**: El portero. Abre el `ServerSocket` (puerto 5000) y espera. Cuando llega alguien, le asigna un `ManejadorCliente`.
*   **`ManejadorCliente`**: Un `Thread` dedicado a un solo usuario conectado. Lee JSON, decide qué hacer y responde.
*   **`GestorAsignacion`**: Lógica pura. Decide si un operario es válido para un trabajo antes de asignarlo.
*   **`HiloFacturacion`**: Tarea pesada. Se ejecuta en segundo plano para no congelar la pantalla mientras genera PDFs.

### 🎨 `controladores` (JavaFX)
Manejan la interacción visual.
*   **`TareasController`**: Captura el evento "Drag & Drop" y llama al `GestorAsignacion`.
*   **`FacturacionController`**: Inicia el `HiloFacturacion` y escucha su progreso para actualizar una barra de carga.

---

## 2. Flujos Críticos del Sistema

### Flujo A: Asignación de un Trabajo (De JavaFX al Móvil)

Este flujo conecta la gestión (Escritorio) con el técnico (Móvil).

1.  **Inicio (JavaFX)**: El Gerente arrastra un `Trabajo` sobre un `Operario` en la vista `Tareas.fxml`.
2.  **Controlador**: `TareasController` captura el evento y llama a `GestorAsignacion.asignarTrabajo(trabajo, operario)`.
3.  **Lógica (Servicios)**:
    *   `GestorAsignacion` verifica que el operario esté `DISPONIBLE`.
    *   Llama a `TrabajoDAO.asignarOperario(...)` para actualizar la BD (UPDATE SQL).
4.  **Notificación (Sockets)**:
    *   Si la BD actualiza bien, `GestorAsignacion` pide al `ServidorTareas` que busque si ese Operario está conectado.
    *   Si está conectado, `ServidorTareas` busca su `ManejadorCliente` y le envía un JSON: `{ "type": "EVENT", "event": "NEW_JOB", ... }`.
5.  **Recepción (Móvil Kotlin)**: La App del Operario recibe el JSON, muestra una notificación push local y actualiza su lista de trabajos.

### Flujo B: Generación Masiva de Facturas (Hilos)

Este flujo demuestra el uso de concurrencia (PSP) para no bloquear la interfaz.

1.  **Inicio (JavaFX)**: El Gerente pulsa "Generar Facturas Pendientes" en `Facturacion.fxml`.
2.  **Controlador**: `FacturacionController` crea una instancia de `HiloFacturacion` (que implementa `Runnable` o `Task` de JavaFX).
3.  **Hilo en Segundo Plano**:
    *   El hilo arranca (`new Thread(hilo).start()`).
    *   Consulta `TrabajoDAO` para obtener trabajos finalizados sin facturar.
    *   **Bucle**: Para cada trabajo:
        1.  Calcula totales (Base + IVA).
        2.  Llama a `GeneradorPDF` (Utilidad) para crear el archivo físico en disco.
        3.  Llama a `FacturaDAO` para insertar el registro en BD.
        4.  **Actualización UI**: Usa `Platform.runLater(() -> progressBar.setProgress(...))` para que la barra avance suavemente en la pantalla del Gerente.
4.  **Fin**: Cuando termina, muestra una alerta "Proceso finalizado".

### Flujo C: Chat en Tiempo Real (Sockets)

1.  **Cliente (Flutter)**: Usuario envía mensaje "Ya llegué". Se envía JSON `{ "action": "CHAT", "msg": "Ya llegué" }` al Socket.
2.  **Servidor (Java)**:
    *   `ManejadorCliente` (del Usuario) recibe el mensaje.
    *   Guarda el mensaje en BD usando `ChatDAO` (persistencia).
    *   Identifica quién es el destinatario (el Operario asignado al trabajo).
    *   Busca el `ManejadorCliente` del Operario en la lista de conectados del `ServidorCentral`.
    *   Le reenvía el mensaje inmediatamente.

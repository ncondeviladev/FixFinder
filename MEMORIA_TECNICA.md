# Memoria Técnica - FIXFINDER

## 1. Introducción y Visión General
**FIXFINDER** es un sistema de gestión de reparaciones diseñado bajo una arquitectura **Cliente-Servidor**. Su propósito es permitir que múltiples usuarios (clientes de escritorio) interactúen simultáneamente con una base de datos centralizada para gestionar incidencias, reparaciones y usuarios.

### Tecnologías Clave
*   **Lenguaje:** Java (JDK 21).
*   **Interfaz Gráfica (Cliente):** JavaFX.
*   **Persistencia:** MySQL con JDBC y patrón DAO.
*   **Comunicación:** Sockets TCP/IP puros.
*   **Protocolo de Datos:** JSON (librería Jackson).

---

## 2. Arquitectura del Sistema

El sistema se divide en dos grandes bloques que se comunican a través de la red:

### 2.1. El Servidor (Backend)
Es el cerebro del sistema. No tiene interfaz gráfica.
*   **`ServidorCentral.java` (El Portero):** Es el punto de entrada. Escucha en el puerto `5000`. Su única misión es aceptar conexiones y controlar el aforo mediante un **Semáforo**.
*   **`GestorCliente.java` (El Camarero):** Es un hilo (`Thread`) dedicado. Cuando entra un cliente, el servidor crea un `GestorCliente` exclusivo para él. Este hilo vive mientras dure la conexión y se encarga de procesar las peticiones.
*   **`UsuarioDAO.java` (El Cocinero):** Es la capa de acceso a datos. El `GestorCliente` no toca la BD directamente; le pide al DAO que busque o guarde datos.

### 2.2. El Cliente (Frontend)
Es la aplicación que usa el usuario final.
*   **`AppEscritorio` / `Launcher`:** Punto de arranque. Usamos un `Launcher` intermedio para evitar problemas de carga de librerías JavaFX en el `.jar`.
*   **`DashboardController.java` (La Interfaz):** Controla los botones y textos de la pantalla.
*   **`ClienteSocket.java` (El Teléfono):** Es una clase auxiliar que maneja la complejidad de la red. Tiene un hilo secundario escuchando siempre para no congelar la pantalla cuando llegan mensajes.

---

## 3. Protocolo de Comunicación (JSON)

Hemos decidido usar **JSON** (texto) en lugar de serialización binaria de Java. Esto facilita la depuración y desacopla las versiones de cliente y servidor.

### 3.1. Estructura de los Mensajes
Hemos definido un "contrato" estándar para todos los mensajes:

**Petición (Cliente -> Servidor):**
```json
{
  "accion": "LOGIN",          // Verbo: Qué quiero hacer
  "datos": {                  // Payload: Información necesaria
    "email": "usuario@test.com",
    "pass": "secreto"
  }
}
```

**Respuesta (Servidor -> Cliente):**
```json
{
  "status": 200,              // Código de estado (200 OK, 400 Error)
  "mensaje": "Login correcto", // Mensaje legible para el usuario
  "datos": { ... }            // Datos solicitados (si los hay)
}
```

### 3.2. Serialización y Deserialización (`ObjectMapper`)
Usamos la clase `ObjectMapper` de la librería Jackson.
*   **Serializar (Java -> JSON):** `mapper.writeValueAsString(objeto)`. Convierte nuestros objetos en texto para enviarlos por el cable.
*   **Deserializar (JSON -> Java):** `mapper.readTree(jsonString)`. Convierte el texto recibido en un árbol de nodos (`JsonNode`) para que podamos leer los campos (`.get("accion")`).
*   **Optimización:** En el servidor, usamos una única instancia `static` de `ObjectMapper` compartida por todos los hilos, ya que es *Thread-Safe* y costosa de crear.

---

## 4. Decisiones Técnicas Importantes

### 4.1. Control de Concurrencia (Semáforos)
Utilizamos `java.util.concurrent.Semaphore` para limitar el número de clientes simultáneos (actualmente 10).

**Política de Saturación: Rechazo Inmediato**
*   Usamos `semaforo.tryAcquire()` en lugar de `acquire()`.
*   **Razón:** Si usáramos `acquire()` (bloqueante), el hilo principal del servidor se quedaría congelado esperando si llega el cliente número 11, impidiendo aceptar o rechazar a nadie más.
*   **Comportamiento:** Si el aforo está completo, el servidor cierra la conexión (`socket.close()`) inmediatamente.

### 4.2. Hilos y UI (JavaFX)
*   **Problema:** Las operaciones de red (`readUTF`) bloquean la ejecución. Si se hacen en el hilo principal de JavaFX, la ventana se congela ("No responde").
*   **Solución:** `ClienteSocket` crea un hilo secundario solo para escuchar.
*   **Vuelta a la UI:** Cuando llega un mensaje, el hilo secundario no puede tocar la pantalla directamente. Usa `Platform.runLater(() -> { ... })` para pedirle al hilo de JavaFX que actualice la interfaz de forma segura.

---

## 5. Flujo de Ejecución (Paso a Paso)

Ejemplo de un flujo de **LOGIN**:

1.  **Inicio:** El Usuario pulsa "Conectar". `ClienteSocket` abre el tubo (`Socket`) con el Servidor.
2.  **Aceptación:** `ServidorCentral` acepta, comprueba el semáforo, crea un `GestorCliente` y vuelve a dormir.
3.  **Envío:** El Usuario pulsa "Login". `ClienteSocket` crea el JSON `{"accion":"LOGIN"}` y lo envía (`writeUTF`).
4.  **Procesamiento:**
    *   `GestorCliente` recibe el texto.
    *   Lee la acción "LOGIN".
    *   Llama a `UsuarioDAO` para buscar en la BD.
    *   Verifica la contraseña.
    *   Genera un JSON de respuesta `{"status": 200}`.
    *   Lo envía de vuelta.
5.  **Recepción:**
    *   El hilo de escucha de `ClienteSocket` recibe el JSON.
    *   Ejecuta el *callback* del `DashboardController`.
    *   La pantalla se actualiza con "Bienvenido".

---

## 6. Estado Actual y Próximos Pasos
**Estado (a fecha 01/12/2025):**
*   ✅ Infraestructura Servidor-Cliente operativa.
*   ✅ Conexión por Sockets y transmisión de JSON validada.
*   ✅ Base de Datos y DAOs creados.
*   ⚠️ **Pendiente:** La lógica de Login en el servidor está simulada (siempre dice OK).

**Próximos Pasos:**
1.  Implementar `UsuarioDAO.obtenerPorEmail()`.
2.  Conectar `GestorCliente` con el DAO real para validar credenciales.
3.  Diseñar la interfaz gráfica final.

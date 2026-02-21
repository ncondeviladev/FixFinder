# Guía Funcional y Técnica de Desarrollo: FixFinder Mobile (Flutter)

Este documento es la referencia técnica definitiva para el desarrollo de la aplicación móvil de FixFinder. Su objetivo es asegurar que la comunicación con el servidor central y el flujo de negocio sean coherentes con la infraestructura actual.

---

## 🏗️ 1. Arquitectura de la Aplicación

La aplicación móvil será una herramienta híbrida (Flutter) con una lógica de **UI Condicional** basada en el rol del usuario:

- **Perfil CLIENTE:** Enfocado en la reporte de incidencias (fotos + formulario) y seguimiento de estados.
- **Perfil OPERARIO:** Enfocado en la gestión de trabajos asignados y cierre técnico de tareas.

### Arquitectura y Roles

La aplicación es un cliente único que adapta su funcionalidad según el rol obtenido tras el `LOGIN`:

**A. Perfil CLIENTE**

- **Función**: Solicitar reparaciones y gestionar presupuestos.
- **Flujo**: Crear Trabajo (con fotos) -> Recibir Notificación de Presupuesto -> Aceptar/Rechazar Presupuesto -> Realizar Pago (Tras finalización).

**B. Perfil OPERARIO**

- **Función**: Ejecutar los trabajos asignados en campo tras la aceptación del cliente.
- **Flujo**: Recibir Tarea Asignada -> Ver Detalles y Dirección -> Actualizar Estado -> Finalizar Trabajo (Informe técnico).

---

## 📡 2. Especificación de Comunicación (Socket TCP)

### A. Protocolo de Red

- **Puerto:** 5000 (TCP).
- **Formato de Datos:** JSON UTF-8.
- **Estructura del Mensaje:** Siempre debe contener las claves: `accion`, `datos`, y obligatoriamente `token` (excepto en LOGIN/REGISTRO).
- **Manejo de Longitud (`readUTF` de Java):**
  - El servidor utiliza `DataInputStream.readUTF()`.
  - **En Flutter:** No puedes enviar el JSON directamente como un String plano. Se debe usar un formato compatible con el estándar de Java que incluye 2 bytes de longitud al principio.
  - **Seguridad:** Tras el LOGIN, el servidor devuelve un **token UUID**. Este token debe almacenarse en el móvil (Secure Storage) y enviarse en la raíz de cada JSON posterior.

### B. Gestión de Saturación (Semáforos)

- **Límite:** 10 conexiones simultáneas.
- **Comportamiento:** Si el servidor está lleno, el socket se cerrará inmediatamente tras el `connect`.
- **Implementación en App:**
  1. Intentar conexión.
  2. Si se cierra bruscamente (`Connection reset`), mostrar: _"Servidor ocupado, reintentando..."_.
  3. Implementar un reintento automático (máximo 3 veces) antes de pedir intervención al usuario.

---

## 📸 3. Flujo Crítico: Gestión de Imágenes

Para evitar saturar la memoria del servidor y el ancho de banda del socket, se ha decidido utilizar un enfoque híbrido:

1.  **Carga Multimedia:** La App móvil sube las imágenes a **Firebase Storage** (Plan gratuito).
2.  **Referencia en Servidor:** Al crear el trabajo, la App envía un array de URLs correspondientes a los archivos subidos.
3.  **Registro Atómico:** La acción `CREAR_TRABAJO` en el servidor ya está preparada para recibir este array e insertar los links en la tabla `foto_trabajo` automáticamente vinculados al ID del trabajo.

### Paso a Paso Técnico

**Paso 1: Interfaz de Usuario (Flutter)**
El usuario completa el formulario: Título, Categoría, Descripción y selecciona hasta 3 fotos.

**Paso 2: Subida a la Nube (Firebase)**
Antes de hablar con el servidor Java, la App móvil sube las imágenes a **Firebase Storage**:

1.  Sube `foto1.jpg` -> Firebase devuelve `https://firebasestorage.../foto1.jpg`.
2.  Sube `foto2.jpg` -> Firebase devuelve `https://firebasestorage.../foto2.jpg`.
3.  La App guarda estas URLs en una lista.

**Paso 3: Envío del Socket (Mensaje Único)**
La App envía un **único mensaje JSON** al servidor Java para que todo sea atómico:

```json
{
  "accion": "CREAR_TRABAJO",
  "datos": {
    "idCliente": 12,
    "titulo": "Fuga caldera",
    "categoria": "FONTANERIA",
    "descripcion": "Gotea mucho por debajo",
    "direccion": "Calle Falsa 123",
    "urgencia": 2,
    "urls_fotos": [
      "https://firebasestorage.../foto1.jpg",
      "https://firebasestorage.../foto2.jpg"
    ]
  }
}
```

**Paso 4: Procesamiento en Servidor (Java)**

1.  El servidor crea el registro en la tabla `trabajo`.
2.  Obtiene el `id` generado.
3.  Recorre el array `urls_fotos` e inserta cada URL en la tabla `foto_trabajo`, vinculándolas al ID del trabajo recién creado.

---

## 🔄 4. Flujo de Trabajo (Business Logic)

### Login y Sesión

1.  **Login:** Enviar `email` y `password`.
2.  **Validación:** El servidor devuelve `status: 200`, el objeto `usuario` y el `token` UUID.
3.  **Persistencia Segura:** Guardar el `token` en **Flutter Secure Storage**. Guardar `idUsuario` y `rol` en `SharedPreferences`.
4.  **Uso:** En cada petición (ej: `LISTAR_TRABAJOS`), inyectar el token en la raíz del JSON.

### Cliente: Ciclo de Incidencia y Presupuesto

1.  **Reporte:** Crear trabajo con fotos (vía Firebase).
2.  **Negociación:**
    - El Gerente sube un presupuesto desde el escritorio.
    - El Cliente ve el presupuesto en su lista (Estado: `PRESUPUESTADO`).
    - El Cliente usa la acción `ACEPTAR_PRESUPUESTO` o lo rechaza.
3.  **Ejecución:** Una vez aceptado y asignado por el Gerente, el trabajo pasa a `ASIGNADO`.

### Operario: Ejecución de Tareas

1.  **Recepción:** Solo ve los trabajos en estado `ASIGNADO` que tengan su `idOperario`.
2.  **Cierre:** Acción `FINALIZAR_TRABAJO` al terminar la reparación.

---

## ✅ 5. Checklist para el Inicio del Proyecto Mobile

1.  [ ] **Configurar Firebase:** Crear proyecto y descargar `google-services.json`.
2.  [ ] **Servicio Socket:** Crear una clase `SocketService` singleton con `dart:io`.
3.  [ ] **Provider/Bloc:** Configurar la gestión de estados para Auth y Trabajos.
4.  [ ] **Validar Conexión:** Probar un simple `PING` contra el servidor en el puerto 5000.
5.  [ ] **Asegurar IPs:** En emuladores Android, usar la IP `10.0.2.2` para referenciar al `localhost` de la máquina de desarrollo.

---

**Nota Final:** El servidor Java y la base de datos ya han sido actualizados para soportar este flujo (URLs largas y array de fotos). No se requieren más cambios en el Backend para empezar.

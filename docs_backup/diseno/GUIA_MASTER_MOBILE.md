# Guía Funcional y Técnica de Desarrollo: FixFinder Mobile (Flutter)

Este documento es la referencia técnica definitiva para el desarrollo de la aplicación móvil de FixFinder. Su objetivo es asegurar que la comunicación con el servidor central y el flujo de negocio sean coherentes con la infraestructura actual.

---

## 🏗️ 1. Arquitectura de la Aplicación

La aplicación móvil será una herramienta híbrida (Flutter) con una lógica de **UI Condicional** basada en el rol del usuario:

- **Perfil CLIENTE:** Enfocado en la reporte de incidencias (fotos + formulario) y seguimiento de estados.
- **Perfil OPERARIO:** Enfocado en la gestión de trabajos asignados y cierre técnico de tareas.

---

## 📡 2. Especificación de Comunicación (Socket TCP)

### A. Protocolo de Red

- **Puerto:** 5000 (TCP).
- **Formato de Datos:** JSON UTF-8.
- **Estructura del Mensaje:** Siempre debe contener las claves: `accion`, `datos`, y opcionalmente `token`.
- **Manejo de Longitud (`readUTF` de Java):**
  - El servidor utiliza `DataInputStream.readUTF()`.
  - **En Flutter:** No puedes enviar el JSON directamente. Debes anteponer 2 bytes con la longitud del string (Big-endian) o buscar un paquete que emule `DataOutputStream.writeUTF()`.
  - **Lectura:** El servidor responderá con el mismo formato.

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

**Ejemplo de Payload:**

```json
{
  "accion": "CREAR_TRABAJO",
  "datos": {
    "idCliente": 1,
    "titulo": "Falla Eléctrica",
    "categoria": "ELECTRICIDAD",
    "descripcion": "Chispas en el cuadro",
    "urls_fotos": ["https://url1.com", "https://url2.com"]
  }
}
```

---

## 🔄 4. Flujo de Trabajo (Business Logic)

### Login y Persistencia

- Al hacer Login, el servidor devuelve un objeto `usuario` con su `rol`.
- **Persistencia:** Guardar el `rol`, `idUsuario` e `idEmpresa` (si es operario) en `SharedPreferences`.

### Cliente: Ciclo de Incidencia

1.  **Formulario:** Captura de datos básicos + selección de fotos.
2.  **Acción:** `CREAR_TRABAJO`.
3.  **Seguimiento:** Pantalla que refresca mediante la acción `LISTAR_TRABAJOS` filtrando por `idUsuario`.

### Operario: Gestión Técnica

1.  **Agenda:** Acción `LISTAR_TRABAJOS` con rol `OPERARIO` para ver sus tareas.
2.  **Cierre:** Acción `FINALIZAR_TRABAJO`. Requiere enviar un `informe` de texto.

---

## ✅ 5. Checklist para el Inicio del Proyecto Mobile

1.  [ ] **Configurar Firebase:** Crear proyecto y descargar `google-services.json`.
2.  [ ] **Servicio Socket:** Crear una clase `SocketService` singleton con `dart:io`.
3.  [ ] **Provider/Bloc:** Configurar la gestión de estados para Auth y Trabajos.
4.  [ ] **Validar Conexión:** Probar un simple `PING` contra el servidor en el puerto 5000.
5.  [ ] **Asegurar IPs:** En emuladores Android, usar la IP `10.0.2.2` para referenciar al `localhost` de la máquina de desarrollo.

---

**Nota Final:** El servidor Java y la base de datos ya han sido actualizados para soportar este flujo (URLs largas y array de fotos). No se requieren más cambios en el Backend para empezar.

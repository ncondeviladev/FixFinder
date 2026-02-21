# Guía Funcional: Aplicación Móvil FIXFINDER (Flutter)

Este documento es la referencia técnica definitiva para el desarrollo de la aplicación móvil, detallando el flujo exacto de trabajo y la gestión de archivos multimedia.

---

## 1. Arquitectura y Roles

La aplicación es un cliente único que adapta su funcionalidad según el rol obtenido tras el `LOGIN`:

### A. Perfil CLIENTE

- **Función**: Solicitar reparaciones y consultar su estado.
- **Flujo**: Crear Trabajo (con fotos) -> Ver Presupuestos -> Aceptar/Rechazar -> Confirmar Pago.

### B. Perfil OPERARIO

- **Función**: Ejecutar los trabajos asignados en campo.
- **Flujo**: Ver Agenda -> Actualizar Estado -> Finalizar Trabajo (Informe técnico).

---

## 2. Flujo de Creación de Incidencias (Con Imágenes) 📸

Este es el proceso crítico que integra Flutter, Firebase y el Servidor Java:

### Paso 1: Interfaz de Usuario (Flutter)

El usuario completa el formulario: Título, Categoría, Descripción y selecciona hasta 3 fotos.

### Paso 2: Subida a la Nube (Firebase)

Antes de hablar con el servidor Java, la App móvil sube las imágenes a **Firebase Storage**:

1.  Sube `foto1.jpg` -> Firebase devuelve `https://firebasestorage.../foto1.jpg`.
2.  Sube `foto2.jpg` -> Firebase devuelve `https://firebasestorage.../foto2.jpg`.
3.  La App guarda estas URLs en una lista.

### Paso 3: Envío del Socket (Mensaje Único)

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

### Paso 4: Procesamiento en Servidor (Java)

1.  El servidor crea el registro en la tabla `trabajo`.
2.  Obtiene el `id` generado.
3.  Recorre el array `urls_fotos` e inserta cada URL en la tabla `foto_trabajo`, vinculándolas al ID del trabajo recién creado.

---

## 3. Especificaciones del Protocolo de Red

- **Puerto**: 5000 (TCP).
- **Gestión de Hilos**: El servidor admite 10 conexiones simultáneas (Semaphore). Si se supera, el socket se cierra. La app debe gestionar el reintento.
- **Lectura/Escritura**: Se usa `readUTF()` / `writeUTF()` de Java.
- **Nota sobre Imágenes**: **NUNCA** enviar los bytes de la imagen por el socket. El servidor solo procesa el texto (URLs).

---

## 4. Cambios en la Base de Datos realizados

- **Tabla `foto_trabajo`**: Columna `url_archivo` cambiada de `VARCHAR(255)` a `TEXT` para soportar las URLs largas de Firebase.

---

## 5. Visualización en App de Escritorio

El administrador verá los trabajos y, si tienen fotos asociadas, JavaFX las descargará y mostrará mediante un `ImageView` cargando la URL directamente de internet.

---

**Próximo paso técnico**: Implementar en el servidor Java la lectura del array `urls_fotos` dentro de la acción `CREAR_TRABAJO`.

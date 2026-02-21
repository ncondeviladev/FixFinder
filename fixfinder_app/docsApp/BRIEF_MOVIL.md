# Resumen de Contexto para la App Móvil (FIXFINDER)

Aquí tienes el "Brief" técnico y funcional completo, extraído de toda tu documentación, listo para usar como contexto al crear el nuevo proyecto Flutter.

## 1. Visión General

FixFinder es una plataforma de gestión de reparaciones e incidencias. La aplicación móvil es la interfaz para dos actores clave: el Cliente (que solicita reparaciones) y el Operario (que las ejecuta).

**Backend:** Servidor Java Central con Base de Datos MySQL.
**Comunicación:** Sockets TCP/IP (JSON) persistentes. NO es una API REST.

## 2. Actores y Funcionalidades Móviles

| Actor           | Funcionalidades Clave                                                                                                                                                                                                                                                                                                                                                          |
| :-------------- | :----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 👤 **Cliente**  | • **Registro/Login:** Crear cuenta y acceder.<br>• **Crear Incidencia:** Título, descripción, urgencia y adjuntar fotos.<br>• **Gestión:** Ver estado de sus incidencias, historial.<br>• **Presupuestos:** Recibir notificación de presupuesto y Aceptar/Rechazar.<br>• **Cierre:** Validar el trabajo realizado por el operario para cerrar la incidencia y generar factura. |
| 🛠️ **Operario** | • **Gestión de Tareas:** Ver lista de trabajos asignados (Notificaciones Push/Socket).<br>• **Trabajo:** Marcar "En Proceso", reportar horas y materiales.<br>• **Ubicación:** Enviar geolocalización (para asignar trabajos por cercanía).<br>• **Chat:** Comunicación tiempo real con central/cliente (Opcional/Fase 2).                                                     |

## 3. Arquitectura y Comunicación (CRÍTICO)

La app móvil no usa HTTP. Debe mantener una conexión Socket TCP abierta con el servidor.

- **Host/Puerto:** 127.0.0.1 (o IP local del PC servidor) : 5000.
- **Protocolo:** Intercambio de mensajes JSON strings terminados en salto de línea.

**Estructura del Mensaje (Request):**

```json
{
  "action": "NOMBRE_ACCION",  // Ej: LOGIN, GET_JOBS, REPORT_INCIDENT
  "token": "JWT_TOKEN",       // Recibido tras el login
  "data": { ... }             // Datos específicos (ej: email, password, id_incidencia)
}
```

**Estructura del Mensaje (Response):**

```json
{
  "status": 200,             // 200 OK, 400 Error, etc.
  "message": "Texto...",
  "data": { ... }            // Objetos solicitados
}
```

## 4. Modelo de Datos (Simplificado para Móvil)

Debes replicar estos modelos en Dart:

- **Usuario:** id, email, password, rol (CLIENTE, OPERARIO), nombre.
- **Trabajo (Incidencia):**
  - id, titulo, descripcion.
  - estado: PENDIENTE, PRESUPUESTADO, ASIGNADA, EN_PROCESO, REALIZADA, CERRADA.
  - urgencia: BAJA, MEDIA, ALTA.
  - fotos (Lista de strings Base64 o URLs).
  - presupuesto (Double, nulo si no está presupuestado).
- **Tecnico (Extiende Usuario):** latitud, longitud, especialidad, disponible.
- **ItemFactura:** concepto, cantidad, precioUnitario.

## 5. Flujos Principales a Implementar

- **Auth Flow:** Login contra socket -> Guardar Token en SecureStorage -> Navegar a Home (Cliente u Operario según rol).
- **Report Flow (Cliente):** Formulario -> Tomar Foto -> Convertir a Base64 -> Enviar JSON CREATE_INCIDENT.
- **Job Flow (Operario):** Escuchar evento socket NEW_JOB_ASSIGNED -> Mostrar notificación local -> Actualizar lista -> Entrar en detalle -> Cambiar estado.

## 6. Pasos para iniciar el nuevo proyecto

- Crear carpeta fuera del repo actual.
- `flutter create fixfinder_mobile`.
- Dependencias recomendadas (`pubspec.yaml`):
  - `provider` o `flutter_bloc` (Gestión de estado).
  - `shared_preferences` / `flutter_secure_storage` (Guardar sesión).
  - `image_picker` (Cámara/Galería).
  - `intl` (Formatos de fecha/moneda).
- **Importante:** No necesitas `http` ni `dio` para la API principal, usarás Socket nativo de Dart (`dart:io`).

# Flujos Principales de Usuario Móvil - FIXFINDER

Este documento describe los "caminos felices" (Happy Paths) para los usuarios de la App Móvil: **Cliente** y **Operario**.

## 1. Solicitud de Servicio (Rol Cliente)

1.  **Registro/Login**: El usuario descarga la app, se registra (o loguea) y obtiene su token de sesión.
2.  **Crear Incidencia**:
    - Pulsa el botón "+".
    - Rellena título, descripción y selecciona urgencia.
    - (Opcional) Toma una foto con la cámara.
    - Envía la solicitud.
3.  **Confirmación**: Recibe una notificación en pantalla confirmando que la incidencia ha sido enviada al servidor.

## 2. Aceptación de Presupuesto (Rol Cliente)

_Prerrequisito: La empresa ha revisado la incidencia y enviado un presupuesto._

1.  **Notificación**: El cliente recibe una notificación push o ve un indicador en su lista de incidencias.
2.  **Revisión**: Abre la incidencia y ve el desglose del presupuesto.
3.  **Aceptación**: Pulsa "Aceptar Presupuesto".
4.  **Estado**: La incidencia cambia de estado y el cliente espera a que un técnico sea asignado.

## 3. Ejecución de Trabajo (Rol Operario)

_Prerrequisito: La empresa ha asignado la incidencia al operario._

1.  **Recepción**: El operario recibe notificación "Nueva Tarea Asignada".
2.  **Desplazamiento**: Abre la tarea, ve la dirección y los detalles.
3.  **Inicio**: Al llegar, puede marcar "En Proceso" (Opcional, futuro).
4.  **Trabajo**: Realiza la reparación física.
5.  **Reporte**:
    - Introduce materiales gastados y horas trabajadas en la app.
    - Pulsa "Finalizar Trabajo".
6.  **Cierre**: La tarea desaparece de "Pendientes" o pasa a "Finalizadas".

## 4. Validación Final (Rol Cliente)

1.  **Notificación**: El cliente recibe aviso de que el técnico ha terminado.
2.  **Validación**: Verifica la reparación.
3.  **Cierre**: Pulsa "Validar y Cerrar" en la app.
4.  **Factura**: El sistema genera el cobro automáticamente (el cliente puede descargar la factura si se implementa).

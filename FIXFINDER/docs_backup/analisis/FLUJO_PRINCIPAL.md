# Flujo Principal de Uso - FIXFINDER (Happy Path)

Este documento describe el ciclo de vida completo de un servicio en la aplicación, desde la configuración inicial hasta la facturación.

## 1. Configuración Inicial (App Escritorio - Rol Admin/Gerente)

1.  **Arranque**: Se inicia la aplicación de escritorio.
2.  **Registro de Empresa**: Se registra una nueva empresa proveedora de servicios (en esta fase simulamos una única empresa).
3.  **Login Empresa**: El gerente inicia sesión.
4.  **Gestión de Plantilla**: Registro de múltiples operarios con diferentes roles y categorías profesionales.

## 2. Solicitud de Servicio (App Móvil - Rol Cliente)

1.  **Registro/Login**: El usuario final se registra y accede a la app móvil.
2.  **Crear Incidencia**: Rellena un formulario con los datos del problema (descripción, urgencia, ubicación, etc.).
3.  **Subir Incidencia**: Se envía la solicitud al servidor.

## 3. Gestión y Presupuesto (App Escritorio - Rol Empresa)

1.  **Búsqueda de Incidencias**: La empresa visualiza un mapa o lista de incidencias cercanas.
2.  **Filtrado Inteligente**: Por defecto filtra por la categoría profesional de la empresa, pero permite cambiar filtros manualmente.
3.  **Evaluación**: Selecciona una incidencia pendiente creada por un cliente.
4.  **Envío de Presupuesto**: Evalúa el problema y envía una propuesta económica (presupuesto).

## 4. Aceptación (App Móvil - Rol Cliente)

1.  **Notificación**: El cliente recibe el presupuesto en su lista de incidencias.
2.  **Aceptación**: El cliente acepta el presupuesto propuesto.

## 5. Asignación y Ejecución (App Escritorio/Móvil - Rol Empresa/Operario)

1.  **Asignación (Escritorio)**: La empresa ve que el presupuesto ha sido aceptado y selecciona un operario disponible para el trabajo.
2.  **Notificación (Móvil Operario)**: Al operario le llega la orden de trabajo con toda la información.
3.  **Ejecución**: El operario se desplaza y realiza la reparación (simulado).
4.  **Finalización**: Tanto el Operario (en su app) como el Cliente confirman que el trabajo ha finalizado.

## 6. Cierre y Facturación

1.  **Factura**: La confirmación de finalización genera automáticamente la factura del servicio.

# 04. Gestión y Seguimiento - FIXFINDER

Este documento recopila el progreso del proyecto, las decisiones técnicas y el diario de desarrollo.

---

## 1. Estado Actual del Proyecto

### ✅ Backend Validado

1.  **Registro y Login**: Operativos para todos los roles.
2.  **Ciclo de Vida del Trabajo**: Implementado el flujo `PENDIENTE -> PRESUPUESTADO -> ACEPTADO -> ASIGNADO -> REALIZADO -> FINALIZADO`.
3.  **Persistencia**: Gestión de estados sincronizada con MySQL.

### 🗺️ Roadmap

- [x] **Fase 1-4**: Infraestructura, Lógica, Red y Gestión de Trabajos (Completado).
- [x] **Fase 5**: Simulador E2E (Completado).
- [ ] **Fase 6**: Aplicación Móvil Flutter (Próxima Prioridad).
- [ ] **Fase 7**: Aplicación Escritorio Profesional.

---

## 2. Diario de Sesiones (Bitácora)

### Sesión: 10/12/2025

#### Estado Inicial

- **Fase:** Refactorización de Capa de Datos y Pruebas de Integración.
- **Lo hecho:**
  - Se ha completado la refactorización de la base de datos para usar `ENUM` en lugar de tabla `Categoria`.
  - Se han actualizado `UsuarioDAO`, `OperarioDAO` y `TrabajoDAO` para soportar las nuevas estructuras.
- **Lo pendiente:** Verificar la consistencia de los DAOs y probar integraciones complejas (transacciones).

#### Decisiones Tomadas

1.  **Manejo de Conexiones:** Se ha detectado un bug crítico (`Operation not allowed after ResultSet closed`) causado por el uso de un Singleton de Conexión en llamadas DAO anidadas.
    - **Solución:** Se implementó el patrón de **Sobrecarga de Métodos DAOs** (`obtenerPorId(int, Connection)`), permitiendo pasar una conexión activa entre métodos para mantener la transacción abierta.
2.  **Testing Automático:** Se creó `PruebaIntegracion.java`, un script ejecutable que valida el ciclo completo de vida de los datos (CRUD) sin necesidad de interfaz gráfica, asegurando la robustez del backend.

#### Próximos Pasos (To-Do)

- [x] Validar capa de datos con `PruebaIntegracion` (COMPLETADO).
- [ ] Implementar la Capa de Servicios (`UsuarioService`, `TrabajoService`...) usando estos DAOs ya validados.
- [ ] Actualizar la documentación UML si es necesario para reflejar los cambios en DAOs.

---

### Sesión: 09/12/2025

#### Estado Inicial

- **Fase:** 2 (Autenticación Real).
- **Lo hecho:**
  - Backend: `ServidorCentral` (Sockets), `UsuarioDAO` (BD MySQL), `GestorCliente` (Lógica básica de login).
  - Protocolo: JSON definido.
  - Cliente: `ClienteSocket` operativo.
- **Lo pendiente:** Interfaz Gráfica (Login) y Refactorización de Lógica.

#### Decisiones Tomadas

1.  **Prioridad:** Se pospone la Interfaz Gráfica (JavaFX) para priorizar la **Lógica de Negocio** y asegurar que funciona en Terminal.
2.  **Arquitectura:** Se decide implementar una capa de **Servicios (Service Layer)** para desacoplar la lógica del servidor de la gestión de sockets.
    - Servidor: `com.fixfinder.servicios.negocio` (Ej: `UsuarioService`, `IncidenciaService`).
    - Cliente: `com.fixfinder.cliente.servicios` (Proxies para llamar al servidor).

#### Próximos Pasos (To-Do)

- [ ] Refactorizar la lógica de Auth del `GestorCliente` a `UsuarioService`.
- [ ] Implementar un menú de terminal en el Cliente para probar el Login sin GUI.

---

### Sesión Actual: Desarrollo Mobile y Fotos

- **Problema**: Limitación de 64KB en `readUTF()` para fotos.
- **Decisión**: Usar Firebase Storage para archivos y pasar solo la URL por el Socket.
- **Cambio**: Modificado `ProcesadorTrabajos` para recibir array de URLs.

### Sesión Actual: Refactorización Jerarquía Usuarios

- **Solución**: Se implementó `SchemaUpdater` para aplicar los cambios de BD sin borrar datos.

---

### Sesión Actual: Seguridad en Red (Propuesta)

- **Problema**: Las peticiones al servidor confían ciegamente en el `rol` enviado en el JSON del cliente, lo que podría permitir suplantación de identidad si se manipula el mensaje.
- **Decisión**: Implementar **Seguridad de Sesión en el GestorConexion**. El servidor guardará el objeto `Usuario` en el hilo de la conexión tras un login exitoso. Todas las peticiones posteriores ignorarán el rol del JSON y usarán el rol validado en la sesión del socket.
- **Estado**: Pendiente de implementación.

---

## 3. Registro de Decisiones Técnicas (ADR)

1.  **Manejo de Conexiones**: Uso de semáforos (límite 10) para control de concurrencia (requisito PSP).
2.  **Transacciones**: Patrón de sobrecarga de métodos en DAOs para pasar la `Connection` y evitar cierres prematuros de ResultSet.
3.  **Protocolo**: Se elige el idioma Español para las claves JSON (`accion`, `datos`, `mensaje`) para coincidir con el código fuente del servidor.
4.  **Seguridad de Sesión**: Se ha decidido que el `GestorConexion` mantenga el estado de autenticación (usuario logueado) para evitar que clientes malintencionados suplanten roles modificando el JSON de la petición.

---

## 4. Anexo: Estado de la Refactorización de Usuarios (Handoff)

**Fecha:** 17/12/2025
**Objetivo:** Refactorizar Jerarquía de Usuarios (Usuario -> Operario/Cliente)

### Estado Actual

Se ha completado la refactorización a nivel de código y diseño, pero falta aplicar los cambios en la base de datos y validar los tests.

#### ✅ Completado

1.  **Código**:
    - `Usuario` es ahora clase base abstracta.
    - `Operario` extiende `Usuario` (tiene `idEmpresa`).
    - `Cliente` extiende `Usuario` (INDEPENDIENTE, sin `idEmpresa`).
    - Actualizados Servicios (`UsuarioServiceImpl`, `TrabajoServiceImpl`, `PresupuestoServiceImpl`, `GestorCliente`) para manejar la lógica polimórfica.
    - Actualizados DAOs (`UsuarioDAOImpl`, `OperarioDAOImpl`, `ClienteDAOImpl`).
    - Actualizados Tests (`PruebaIntegracion`, `SimuladorDatos`, `BaseDatosTest`).
2.  **Base de Datos (Definición)**:
    - `ESQUEMA_BD.sql` actualizado (Tabla `cliente` creada, columnas movidas).
3.  **Herramientas**:
    - Creado `com.fixfinder.utilidades.SchemaUpdater` para aplicar el SQL.

#### ✅ Errores Solucionados

1.  **Fallo de Tests (`BaseDatosTest`)**:
    - **Solucionado**: Se corrigió el `TrabajoDAOImpl` para usar los nombres de columnas correctos (`ubicacion_lat`, `ubicacion_lon`) que coinciden con el `ESQUEMA_BD.sql`.
    - `BaseDatosTest` se ejecuta correctamente ahora.
2.  **Fallo al Actualizar Esquema**:
    - **Solucionado**: Se ejecutó `runSchemaUpdater` vía Gradle correctamente.
    - Se solucionó una discrepancia en `OperarioDAOImpl` con un alias de columna (`u.id` vs `id`).

### Pasos Siguientes (Para continuar)

Para retomar el trabajo, sigue estos pasos estrictos:

1.  **Ejecutar SchemaUpdater con Gradle**:
    - No usar `java` a pelo. Usar Gradle es la forma correcta de tener las dependencias (Driver MySQL).
    - Opción A: Ejecutar desde el IDE (Run `SchemaUpdater.main`).
    - Opción B: Crear tarea en `build.gradle` o usar `gradlew run` si se configura.

2.  **Verificar Base de Datos**:
    - Asegurarse de que las tablas `usuario`, `operario` y `cliente` tienen la estructura nueva (ver `ESQUEMA_BD.sql`).

3.  **Re-ejecutar Tests**:
    - Correr `BaseDatosTest` y `PruebaIntegracion`. Deberían pasar una vez la BD esté sincronizada.

---

### Sesión Actual: Desarrollo Mobile (Flutter) - Conectividad y UI Base

#### Estado Inicial

- **Fase:** Inicio de Aplicación Móvil.
- **Lo hecho:**
  - **Reparación de Entorno:** Solucionados problemas de sincronización en la nube (OneDrive/Subst) que bloqueaban Flutter.
  - **Servicio de Sockets:** Implementado `SocketService` con protocolo `readUTF` de Java (2-byte length header).
  - **Infraestructura:** Creados servicios de Autenticación, Modelos de datos (`Usuario`, `Trabajo`) y Proveedores de estado (`TrabajoProvider`).
  - **UI Premium:** Implementada `LoginPantalla` y `DashboardPantalla` con diseño adaptado a roles (Cliente/Operario).
  - **Validación:** Confirmada conexión exitosa entre la App (Windows Desktop) y el `ServidorCentral` Java usando los nuevos datos de prueba.
- **Lo pendiente:** Implementar flujos funcionales (Reporte de incidencias, gestión de estados por el operario, integración con Firebase).

#### Decisiones Tomadas

1.  **Nomenclatura:** Se ha adoptado el **Español** para toda la lógica interna, variables y nombres de widgets en Dart, siguiendo la petición del usuario para mantener la coherencia con el backend.
2.  **Arquitectura de Red:** La App utiliza el mismo puerto 5000 y formato JSON que el cliente de escritorio. Para el emulador, se usa la IP `10.0.2.2`; para escritorio, `127.0.0.1`.
3.  **Seguridad:** Almacenamiento seguro del token UUID en el dispositivo mediante `flutter_secure_storage`.

---

### Sesión Actual: Flujo de Negocio Completo y Resiliencia en Red

#### Estado Inicial

- **Fase:** Integración de Ciclo de Vida del Trabajo.
- **Lo hecho:**
  - **Corrección de Comunicación:** Resuelto bug crítico de bucle infinito en `SocketService._procesarBuffer` mediante el uso correcto de `takeBytes()` (consumo de buffer).
  - **Protocolo Sincronizado:** Ajustada la comunicación con el Backend Java (`LISTAR_TRABAJOS` en vez de `GET_JOBS` y envío obligatorio de `idCliente`).
  - **Resiliencia en Windows:** Implementado bypass para `MissingPluginException` en `shared_preferences` y `secure_storage`, permitiendo el desarrollo en memoria cuando el entorno de Windows bloquea los plugins.
  - **Ciclo de Vida:** Implementada la creación de incidencias, listado reactivo, visualización y aceptación de **Presupuestos**, y finalización de tareas por parte del operario.
  - **UX/UI:** Añadido diálogo de confirmación para cancelación de incidencias y actualización en tiempo real de estados usando `Consumer<TrabajoProvider>`.
- **Lo pendiente:** Estabilizar el entorno de compilación de Windows (Plugins) e iniciar la integración real de fotos con Firebase.

#### Decisiones Tomadas

1.  **Modelo de Datos Empático:** Se ha hecho el modelo `Trabajo.fromJson` más robusto para manejar campos nulos del servidor (como direcciones o fechas faltantes en listados masivos).
2.  **Gestión de Estado Centralizada:** Se ha movido toda la lógica de filtrado y actualización al `TrabajoProvider`, dejando las pantallas como vistas puramente reactivas que escuchan el `respuestas` stream del Socket.
3.  **Flujo Simplificado:** Se ha unificado la acción de actualización bajo `FINALIZAR_TRABAJO` para coincidir con la lógica del `ProcesadorTrabajos.java` del servidor.

#### Próximos Pasos (To-Do)

- [x] Sincronizar nombres de acciones con el servidor (COMPLETADO).
- [x] Implementar aceptación de presupuestos (COMPLETADO).
- [ ] Implementar selector de imágenes de galería/cámara.
- [ ] Probar el flujo E2E real: Cliente lanza incidencia -> Gerente presupuesta (Simulador) -> Cliente acepta -> Gerente asigna -> Operario finaliza.

---

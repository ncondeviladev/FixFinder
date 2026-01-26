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

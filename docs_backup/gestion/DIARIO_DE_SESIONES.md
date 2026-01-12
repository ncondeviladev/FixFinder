# Diario de Sesiones - FIXFINDER

Este documento sirve como bitácora para registrar el progreso diario, las decisiones tomadas y el estado en el que queda el proyecto entre sesiones de trabajo (PC1 <-> PC2).

## Sesión: 10/12/2025

### Estado Inicial

- **Fase:** Refactorización de Capa de Datos y Pruebas de Integración.
- **Lo hecho:**
  - Se ha completado la refactorización de la base de datos para usar `ENUM` en lugar de tabla `Categoria`.
  - Se han actualizado `UsuarioDAO`, `OperarioDAO` y `TrabajoDAO` para soportar las nuevas estructuras.
- **Lo pendiente:** Verificar la consistencia de los DAOs y probar integraciones complejas (transacciones).

### Decisiones Tomadas

1.  **Manejo de Conexiones:** Se ha detectado un bug crítico (`Operation not allowed after ResultSet closed`) causado por el uso de un Singleton de Conexión en llamadas DAO anidadas.
    - **Solución:** Se implementó el patrón de **Sobrecarga de Métodos DAOs** (`obtenerPorId(int, Connection)`), permitiendo pasar una conexión activa entre métodos para mantener la transacción abierta.
2.  **Testing Automático:** Se creó `PruebaIntegracion.java`, un script ejecutable que valida el ciclo completo de vida de los datos (CRUD) sin necesidad de interfaz gráfica, asegurando la robustez del backend.

### Próximos Pasos (To-Do)

- [x] Validar capa de datos con `PruebaIntegracion` (COMPLETADO).
- [ ] Implementar la Capa de Servicios (`UsuarioService`, `TrabajoService`...) usando estos DAOs ya validados.
- [ ] Actualizar la documentación UML si es necesario para reflejar los cambios en DAOs.

---

## Sesión: 09/12/2025

### Estado Inicial

- **Fase:** 2 (Autenticación Real).
- **Lo hecho:**
  - Backend: `ServidorCentral` (Sockets), `UsuarioDAO` (BD MySQL), `GestorCliente` (Lógica básica de login).
  - Protocolo: JSON definido.
  - Cliente: `ClienteSocket` operativo.
- **Lo pendiente:** Interfaz Gráfica (Login) y Refactorización de Lógica.

### Decisiones Tomadas

1.  **Prioridad:** Se pospone la Interfaz Gráfica (JavaFX) para priorizar la **Lógica de Negocio** y asegurar que funciona en Terminal.
2.  **Arquitectura:** Se decide implementar una capa de **Servicios (Service Layer)** para desacoplar la lógica del servidor de la gestión de sockets.
    - Servidor: `com.fixfinder.servicios.negocio` (Ej: `UsuarioService`, `IncidenciaService`).
    - Cliente: `com.fixfinder.cliente.servicios` (Proxies para llamar al servidor).

### Próximos Pasos (To-Do)

- [ ] Refactorizar la lógica de Auth del `GestorCliente` a `UsuarioService`.
- [ ] Implementar un menú de terminal en el Cliente para probar el Login sin GUI.

---

## Sesión: 10/12/2025

### Que se ha hecho

1.  **Revisión de Estado**: Se confirmó que la capa de persistencia está completa y falta la lógica de negocio.
2.  **Definición de Interfaces**: Se actualizaron las interfaces `UsuarioService`, `EmpresaService`, `TrabajoService`, `OperarioService` y `FacturaService` con los métodos necesarios.
3.  **Decisión de Arquitectura**: Se acordó usar `Integer` (wrapper) para los IDs en lugar de `Long` para mantener consistencia con los modelos actuales pero ganar seguridad ante nulos.
4.  **Definición del Flujo**: El usuario detalló el "Happy Path" completo del programa. Se ha documentado en `docs/gestion/FLUJO_PRINCIPAL.md`.

### Decisiones Importantes

- Mantener `int` (Integer) en IDs.
- Separar lógica en múltiples servicios especializados.
- El flujo incluye una fase explícita de **Presupuesto y Aceptación** antes de la asignación.

### Próximos Pasos

- Completar `PresupuestoService` que actualmente está vacío.
- Implementar la lógica de los servicios empezando por Usuario o Empresa.

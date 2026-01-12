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

### Sesión: Desarrollo Mobile y Fotos

- **Problema**: Limitación de 64KB en `readUTF()` para fotos.
- **Decisión**: Usar Firebase Storage para archivos y pasar solo la URL por el Socket.
- **Cambio**: Modificado `ProcesadorTrabajos` para recibir array de URLs.

### Sesión: Refactorización Jerarquía Usuarios

- **Cambio**: `Usuario` pasa a ser abstracto. Creación de sub-tablas `operario` y `cliente`.
- **Solución**: Se implementó `SchemaUpdater` para aplicar los cambios de BD sin borrar datos.

---

## 3. Registro de Decisiones Técnicas (ADR)

1.  **Manejo de Conexiones**: Uso de semáforos (límite 10) para control de concurrencia (requisito PSP).
2.  **Transacciones**: Patrón de sobrecarga de métodos en DAOs para pasar la `Connection` y evitar cierres prematuros de ResultSet.
3.  **Protocolo**: Se elige el idioma Español para las claves JSON (`accion`, `datos`, `mensaje`) para coincidir con el código fuente del servidor.

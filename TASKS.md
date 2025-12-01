# Planificación de Desarrollo - FIXFINDER

## Fase 1: Infraestructura Básica (✅ COMPLETADO)
- [x] Crear estructura del proyecto (Gradle, JavaFX).
- [x] Implementar Servidor Central con Sockets y Semáforos.
- [x] Implementar Cliente con hilos de escucha.
- [x] Definir protocolo JSON.
- [x] Crear Base de Datos y DAOs básicos (CRUD).
- [x] Crear Memoria Técnica inicial.

## Fase 2: Autenticación Real (🚧 EN PROGRESO)
Esta fase conecta el "esqueleto" con la "carne" (Base de Datos).
- [ ] **DAO:** Implementar `UsuarioDAO.obtenerPorEmail(String email)`.
- [ ] **Servidor:** Modificar `GestorCliente` para usar el DAO en el Login.
- [ ] **Seguridad:** Implementar verificación de hash de contraseñas (BCrypt o SHA-256 simple por ahora).
- [ ] **Cliente:** Crear una pantalla de Login real en JavaFX (separada del Dashboard).
- [ ] **Pruebas:** Verificar Login exitoso y fallido con usuarios de la BD.

## Fase 3: Gestión de Usuarios (Admin)
- [ ] **Protocolo:** Definir acciones JSON: `LISTAR_USUARIOS`, `CREAR_USUARIO`, `BORRAR_USUARIO`.
- [ ] **Servidor:** Implementar lógica en `GestorCliente` para estas acciones.
- [ ] **Cliente:** Crear tabla (TableView) en el Dashboard para ver usuarios.
- [ ] **Cliente:** Formularios para añadir/editar usuarios.

## Fase 4: Gestión de Incidencias (Core del Negocio)
- [ ] **BD:** Crear tabla `incidencias` y su DAO (`IncidenciaDAO`).
- [ ] **Protocolo:** Definir acciones: `CREAR_INCIDENCIA`, `LISTAR_MIS_INCIDENCIAS`, `ACTUALIZAR_ESTADO`.
- [ ] **Servidor:** Lógica de negocio (asignar técnico, cambiar estado).
- [ ] **Cliente:** Vistas diferenciadas según Rol (Cliente ve sus incidencias, Técnico ve las asignadas).

## Fase 5: Chat / Notificaciones (Opcional / Avanzado)
- [ ] Implementar envío de mensajes servidor -> cliente sin petición previa (Notificaciones).
- [ ] Chat simple sobre una incidencia.

## Fase 6: Pulido y Entrega
- [ ] Revisión de código y limpieza (Refactoring).
- [ ] Pruebas de carga (simular 10 clientes a la vez).
- [ ] Generación de Javadoc.
- [ ] Finalizar Memoria Técnica.

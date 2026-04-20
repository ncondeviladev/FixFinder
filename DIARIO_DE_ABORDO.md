PLAN_EVOLUCION_APP: Hoja de Ruta FixFinder

> **Archivo de sesión:** Este documento sirve como memoria de trabajo entre sesiones de desarrollo.
> Si un chat se pierde o se reinicia, leer este documento primero para recuperar el contexto completo.
> **⚠️ Nota:** La carpeta `DOCS/` **sí se sube a Git** pero el repositorio de GitHub debe mantenerse **privado** para que la Memoria del proyecto no sea pública.


---

## ESTADO ACTUAL DEL SISTEMA (07/03/2026)

### Arquitectura General

- **Backend:** Servidor Java puro con Sockets TCP en puerto `5000`. Sin Spring Boot.
  - Punto de entrada del servidor: `com.fixfinder.red.ServidorCentral`
  - Arranque: `.\gradlew.bat runServer` desde `C:\Users\ncond\Desktop\FF\FIXFINDER`
  - Gestión de conexiones: `GestorConexion.java` → tiene un switch con todas las acciones
  - Procesadores por entidad: `ProcesadorTrabajos.java`, `ProcesadorPresupuestos.java`, etc.
  - Tests: `.\gradlew.bat test` — usa JUnit 5, clase principal `ServiceTest.java`
  - **Protocolo de Comunicación:** 4 bytes de cabecera (longitud del mensaje) + payload JSON en bytes.
    - Java: `DataOutputStream.writeInt(len)` + `write(bytes)` / `DataInputStream.readInt()` + `readFully(bytes)`
    - Flutter: `socket.add(4 bytes big-endian + payload)` / lee 4 bytes cabecera + N bytes datos
    - **⚠️ El simulador `SimuladorController.java` usa también el protocolo de 4 bytes (ya actualizado)**
- **Base de datos:** MySQL en Docker. Contenedor: `FixFinderDb`. Root pass: `root`.
  - DB name: `fixfinder`
  - Acceso: `docker exec -i FixFinderDb mysql -u root -proot fixfinder -e "SQL;"`
  - Resetear datos de prueba: `.\gradlew.bat runSeeder`
- **App Móvil:** Flutter (Android). Carpeta: `C:\Users\ncond\Desktop\FF\fixfinder_app`
  - Arranque en emulador 1: `flutter run -d emulator-5554`
  - Arranque en emulador 2: `flutter run -d emulator-5556`
  - IP del servidor desde emulador: `10.0.2.2:5000`
  - Tests: `flutter test`
  - Estado del socket: singleton `SocketService`, reconecta automáticamente
- **App Escritorio (Windows/JavaFX):** `com.fixfinder.Launcher` → `AppEscritorio`
  - Arranque: `.\gradlew.bat runClient`
  - Para el panel maestro del Dashboard (tabla de trabajos) usar `.\gradlew.bat runDashboard` o acceder desde el menú de gerente/admin

### Usuarios de prueba en la BD (generados por `runSeeder`)

| Email                            | Contraseña  | Rol      | Tlf       | Dirección                    |
| -------------------------------- | ----------- | -------- | --------- | ---------------------------- |
| marta@gmail.com                  | password    | CLIENTE  | 600123456 | Calle Paz 5, 2ºA, Valencia   |
| juan@gmail.com                   | password    | CLIENTE  | 600234567 | Av. del Puerto 120, Valencia |
| elena@gmail.com                  | password    | CLIENTE  | 600345678 | Calle Xàtiva 22, Valencia    |
| gerente.a@levante.com            | password    | GERENTE  | 600123456 | Av. del Cid 45, Valencia     |
| (operarios generados por seeder) | password123 | OPERARIO | 666127582 | varía según operario         |

> ⚠️ IMPORTANTE: Los tests de JUnit (`ServiceTest`) generan usuarios temporales en la BD y pueden dejar telefono=NULL en usuarios existentes. Después de correr tests, ejecutar:
>
> ```sql
> UPDATE usuario SET telefono = '600123456' WHERE rol = 'CLIENTE' OR rol = 'OPERARIO';
> ```

---

## ESTADO DE CADA MÓDULO DEL BACKEND

### `TrabajoService` / `TrabajoServiceImpl`

Métodos implementados y funcionales:

- `solicitarReparacion(idCliente, titulo, categoria, descripcion, direccion, urgencia)` — Crea trabajo PENDIENTE
  - **⚠️ Nuevo (01/03):** Si `direccion` viene vacío, usa `cliente.getDireccion()` como fallback. Si tampoco tiene, pone "Sin dirección especificada"
- `cancelarTrabajo(idTrabajo, motivo)` — Pasa a CANCELADO. Solo si NO está ASIGNADO ni FINALIZADO
- `modificarTrabajo(idTrabajo, titulo, descripcion, direccion, categoria, urgencia)` — Solo si está PENDIENTE
- `finalizarTrabajo(idTrabajo, informe)` — Pasa a REALIZADO. Concatena informe al final de la descripción
- `valorarTrabajo(idTrabajo, valoracion, comentarioCliente)` — Solo si FINALIZADO/REALIZADO/PAGADO. Valoración 1-5 estrellas.
- `listarPorCliente()`, `listarPorOperario()`, enriquecimiento de DTOs en `procesarListarTrabajos`

### `ProcesadorTrabajos`

Acciones que maneja el switch en `GestorConexion`:

| Acción (String)       | Método procesador            |
| --------------------- | ---------------------------- |
| `CREAR_TRABAJO`       | `procesarCrearTrabajo`       |
| `LISTAR_TRABAJOS`     | `procesarListarTrabajos`     |
| `FINALIZAR_TRABAJO`   | `procesarCambiarEstado`      |
| `CANCELAR_TRABAJO`    | `procesarCancelarTrabajo`    |
| `MODIFICAR_TRABAJO`   | `procesarModificarTrabajo`   |
| `VALORAR_TRABAJO`     | `procesarValorarTrabajo`     |
| `ACEPTAR_PRESUPUESTO` | `procesarAceptarPresupuesto` |
| `LISTAR_PRESUPUESTOS` | `procesarListarPresupuestos` |

> ⚠️ IMPORTANTE sobre `procesarValorarTrabajo`: El mensaje de éxito en el JSON de respuesta es
> `"Valoracion guardada correctamente"` (SIN acento en la ó). El Completer en Flutter filtra por esta cadena.

### `ProcesadorTrabajos.procesarListarTrabajos` — Enriquecimiento del DTO

El JSON que envía el servidor al listar incluye (además de campos básicos):

- `id`, `titulo`, `descripcion`, `categoria`, `estado`, `fecha`
- **`direccion`** (String — dirección del trabajo. **Nuevo 01/03**: ya se incluye en la respuesta)
- `valoracion` (int 0-5), `comentarioCliente` (String o null), `fechaFinalizacion` (String ISO o null)
- `urls_fotos` (List<String>), `ubicacion` (objeto {lat, lon} o null)
- `cliente` (objeto completo con id, nombre, telefono, email, foto, direccion)
- `operarioAsignado` (objeto completo con id, nombre, telefono, email, foto)
- `presupuesto` (el presupuesto aceptado si existe), `tienePresupuestoAceptado` (boolean)

> ⚠️ **Bug corregido (01/03):** Antes el campo `direccion` del trabajo NO estaba en la respuesta LISTAR_TRABAJOS.
> Flutter caía en `json['direccionCliente']` y siempre mostraba la dirección del cliente, ignorando la dirección
> real de la incidencia. Ahora se incluye `map.put("direccion", t.getDireccion())` explícitamente.

---

## ESTADO DE CADA MÓDULO DE LA APP FLUTTER

### Estructura de carpetas

````
lib/
├── main.dart                          → Entrada, providers, rutas, tema
├── models/
├── models/
│   ├── trabajo.dart                   → Modelo Trabajo + enums EstadoTrabajo, CategoriaServicio
│   ├── usuario.dart                   → Modelo Usuario + enum Rol
│   ├── presupuesto.dart               → Modelo Presupuesto
│   └── empresa.dart                   → Modelo Empresa colaboradora
├── providers/
│   └── trabajo_provider.dart          → State management para trabajos
├── services/
│   ├── socket_service.dart            → Comunicación TCP con servidor Java (protocolo 4 bytes)
│   └── auth_service.dart             → Login, logout, persistencia token en SharedPreferences
├── screens/
│   ├── login_pantalla.dart
│   ├── dashboard_pantalla.dart
│   ├── detalle_trabajo_pantalla.dart  → REFACTORIZADA: delega en widgets separados
│   ├── crear_trabajo_pantalla.dart    → Crear y Modificar (modo dual)
│   └── perfil_pantalla.dart
└── widgets/
    ├── comunes/
    │   ├── dato_fila.dart
    │   └── estado_badge.dart
    ├── trabajos/
    │   ├── tarjeta_trabajo.dart
    │   ├── tarjeta_contacto.dart
    │   └── galeria_fotos.dart         → Preparada para URLs Firebase (Nuevo 08/03)
    └── detalle_trabajo/
        ├── detalle_info_card.dart
        ├── detalle_resumen_final.dart
        ├── detalle_seccion_presupuestos.dart
        └── dialogos_trabajo.dart      → Todos los AlertDialogs (borrar, finalizar, valorar)
```

---
## ✅ TAREAS COMPLETADAS: IMÃGENES (Actualizado 10/03)
## ✅ TAREAS COMPLETADAS: IMÁGENES (Actualizado 10/03)

Las siguientes tareas han sido implementadas y están listas para validación final:

1. **Fotos de Perfil en App (Cliente)**:
   - Implementado en `perfil_pantalla.dart` con `image_picker` y subida directa a Firebase Storage.
   - Sincronización con el servidor mediante `ServicioAutenticacion` y evento `ACTUALIZAR_FOTO_PERFIL`.
2. **Fotos de Perfil en Dashboard JavaFX**:
   - Implementada clase `FirebaseStorageUploader.java` (REST API) para subida de fotos desde escritorio.
   - Funcionalidad de cambio de foto añadida para **Gerente** (Panel Empresa) y **Operarios** (Panel Operarios).
   - Actualizada la lógica de `miniAvatar` para mostrar fotos reales desde URL con clips circulares en JavaFX.
3. **Soporte de Backend (Servidor Java)**:
   - Nuevo endpoint `ACTUALIZAR_FOTO_PERFIL` en `ProcesadorUsuarios`.
   - Consulta SQL optimizada para recuperar la foto del Gerente de forma aislada.

---

## PROTOCOLO DE PRUEBAS PARA LA PRÓXIMA SESIÓN

Para verificar que todo el sistema de imágenes es robusto, realizar los siguientes pasos en orden:

### 1. Prueba en App Móvil (Flujo Cliente)
- Iniciar sesión como **Cliente**.
- Ir a Perfil → Clic en el icono de la cámara (📸).
- Seleccionar una imagen de la galería.
- **Verificar:** El redondel del perfil debe actualizarse con la nueva foto.
- **Persistencia:** Cerrar sesión y volver a entrar; la foto debe seguir ahí (cargada desde URL).

### 2. Prueba en Dashboard (Flujo Gerente)
- Ir a la sección **Empresa**.
- Clic en el icono 📸 sobre el redondel del Gerente.
- Seleccionar un archivo del PC.
- **Verificar:** El redondel debe mostrar la foto tras la carga.

### 3. Prueba en Dashboard (Flujo Operario)
- Ir a la sección **Operarios**.
- En la tabla, pulsar el botón 📸 de un operario específico.
- Seleccionar foto.
- **Verificar:** La celda de "Nombre" del operario debe mostrar ahora su foto real en el avatar pequeño en lugar de las iniciales.

### 4. Prueba Cruzada
- Cambiar la foto de un operario en el Dashboard.
- Iniciar sesión con ese mismo operario en la App de Flutter.
- **Verificar:** En el perfil de la app, debe aparecer la foto que asignó el gerente.

---

---

### Providers

- **`TrabajoProvider`** (`lib/providers/trabajo_provider.dart`)
  - `obtenerTrabajos()` — Lista trabajos, excluye CANCELADOS, ordena por prioridad de estado
  - `crearTrabajo(datos)` — Envía `CREAR_TRABAJO`. No Completer, solo delay 800ms
  - `cancelarTrabajo(idTrabajo)` — Envía `CANCELAR_TRABAJO`, delay 800ms + llama `obtenerTrabajos()`
  - `modificarTrabajo(idTrabajo, datos)` — Envía `MODIFICAR_TRABAJO`, usa Completer que espera `"modificado"` en mensaje
  - `valorarTrabajo(idTrabajo, valoracion, comentario)` — Envía `VALORAR_TRABAJO`, usa Completer que espera `"Valoracion"` en mensaje
  - `actualizarEstadoTrabajo(idTrabajo, estado, informe?)` — Para FINALIZAR desde operario. delay 800ms + `obtenerTrabajos()`
  - `aceptarPresupuesto(idPresupuesto)` — delay 800ms + `obtenerTrabajos()`
  - `startPolling()` / `stopPolling()` — Refresco automático cada 15 segundos (evento push)

> ⚠️ TRUCO DEL COMPLETER para `modificar/valorar`: Los Completers filtran por palabras clave del `mensaje`
> de respuesta (NO por `status == 200`) para no capturar por accidente la respuesta de LISTAR que también
> devuelve 200 y llega de forma asíncrona.

### Pantallas — Comportamiento de Navegación (ACTUALIZADO 01/03)

**Patrón estándar para todas las acciones:**

1. La acción (finalizar, valorar, aceptar, borrar) llama al provider y espera el resultado.
2. Si `exito == true`, se hace **`Navigator.pop(context)`** simple (NO `popUntil`).
3. El dashboard tiene `.then((_) async { await Future.delayed(900ms); obtenerTrabajos(); })` en el `onTap`.
4. E delay de 900ms permite al servidor confirmar el cambio en BD antes de re-listar.

> ⚠️ **CAUSA DE CRASH HISTÓRICO:** Usando `popUntil(ModalRoute.withName('/dashboard'))` con rutas
> anónimas (`MaterialPageRoute`) el stack de navegación quedaba vacío → pantalla negra.
> **Nunca usar `popUntil` desde pantallas navegadas con `MaterialPageRoute` sin nombre.**

### Pantallas

- **`DashboardPantalla`** (`lib/screens/dashboard_pantalla.dart`)
  - Lista trabajos con `TarjetaTrabajo` (ordenados por prioridad)
  - **Botón Refresh** en AppBar (➤ `Icons.refresh`) para todos los roles
  - Pull-to-refresh con `RefreshIndicator`
  - Pantalla vacía mejorada: `CustomScrollView` con `AlwaysScrollableScrollPhysics` para que el pull-to-refresh funcione aunque no haya registros + botón "Actualizar" visible
  - Botón `+` flotante (solo CLIENTE)
  - `_tieneAccionPendiente`: Badge de acción solo si:
    - CLIENTE + PRESUPUESTADO → hay presupuesto por aceptar
    - CLIENTE + REALIZADO y `valoracion == 0` → pendiente valorar
    - CLIENTE + FINALIZADO y `valoracion == 0` → pendiente valorar
    - OPERARIO + ACEPTADO → hay trabajo por iniciar

- **`CrearTrabajoPantalla`** (`lib/screens/crear_trabajo_pantalla.dart`)
  - Parámetro opcional `trabajoAEditar: Trabajo?`
  - **Dirección opcional (nuevo 01/03):** Campo no obligatorio. Si se deja vacío, el servidor usa la dirección registrada del cliente. Hint text: "Si se deja vacío, se usa tu dirección registrada". Solo se envía el campo `direccion` en el JSON si el usuario escribió algo.
  - SnackBar verde/rojo con mensaje descriptivo

- **`DetalleTrabajoPantalla`** (`lib/screens/detalle_trabajo_pantalla.dart`)
  - **REFACTORIZADA en sesión anterior:** Delega el rendering en widgets separados de `widgets/detalle_trabajo/`
  - AppBar con `PopupMenuButton`: Modificar + Borrar (solo CLIENTE en estado PENDIENTE/PRESUPUESTADO)
  - Muestra `DetalleInfoCard` → información principal del trabajo
  - Si CLIENTE + PENDIENTE/PRESUPUESTADO → `DetalleSeccionPresupuestos`
  - Si OPERARIO + ASIGNADO/REALIZADO → botón verde "MARCAR COMO FINALIZADO"
  - Si FINALIZADO → `DetalleResumenFinal` (fecha, precio, valoración)
  - Si CLIENTE + FINALIZADO/REALIZADO + `valoracion == 0` → botón azul "VALORAR SERVICIO"

### Widgets

- `TarjetaTrabajo` — Tarjeta del dashboard, banner de acción pendiente, menú 3 puntos
- `TarjetaContacto` — Datos de contacto de cliente u operario
- `GaleriaFotos` — Tira horizontal de fotos, tap abre modal ampliado. **Preparada para URLs Firebase**
- `EstadoBadge` — Chip coloreado según estado
- `DatoFila` — Par Etiqueta: Valor simple
- `DetalleInfoCard` — Tarjeta principal de detalle (estado, categoría, descripción, contactos)
- `DetalleResumenFinal` — Tarjeta verde de cierre (precio, fecha, valoración)
- `DetalleSeccionPresupuestos` — Lista de presupuestos con botón Aceptar y diálogo de empresa
- `DialogosTrabajo` — Clase utilitaria con todos los AlertDialogs (borrar, finalizar, valorar)

---

SESIÓN 01/03/2026 — Cambios Detallados

### Objetivo de la sesión

Refactorización de código, limpieza de logs debug, añadir documentación a todas las clases, y corrección de múltiples bugs de funcionamiento en el flujo cliente-operario.

### Backend (Java) — Cambios

#### `ProcesadorTrabajos.java`

- **Bug fix crítico:** Añadido `map.put("direccion", t.getDireccion())` en `procesarListarTrabajos` (línea ~198). Antes esta clave nunca se incluía en la respuesta, por lo que Flutter siempre usaba la dirección del cliente en lugar de la del trabajo.

#### `TrabajoServiceImpl.java`

- **Nuevo comportamiento `solicitarReparacion`:** Si `direccion` viene vacío desde la app, el servidor usa `cliente.getDireccion()` como fallback. Si tampoco tiene, "Sin dirección especificada".

#### `SimuladorController.java`

- Actualizado al protocolo de 4 bytes (`writeInt` / `readInt`) para ser compatible con el servidor actualizado.

### App Flutter — Cambios

#### Limpieza de código

- Eliminados todos los `print()` y llamadas a `Logger` de: `trabajo_provider.dart`, `auth_service.dart`, `socket_service.dart`
- Reemplazados `Logger` por `debugPrint` solo en bloques `catch` críticos

#### Documentación

- Añadido comentario de cabecera en **todas** las clases del proyecto (2 líneas, estilo conciso):
  - `main.dart`, `login_pantalla.dart`, `dashboard_pantalla.dart`, `detalle_trabajo_pantalla.dart`, `crear_trabajo_pantalla.dart`, `perfil_pantalla.dart`
  - `socket_service.dart`, `auth_service.dart`, `trabajo_provider.dart`
  - `trabajo.dart`, `usuario.dart`, `presupuesto.dart`, `empresa.dart`
  - Todos los widgets en `widgets/comunes/`, `widgets/trabajos/`, `widgets/detalle_trabajo/`

#### `dashboard_pantalla.dart`

- Añadido botón `Icons.refresh` en AppBar (para todos los roles, sin condición)
- Pantalla vacía: cambiado de `Center(Text)` simple a `CustomScrollView + SliverFillRemaining` con `AlwaysScrollableScrollPhysics` para que el pull-to-refresh funcione incluso sin elementos. Incluye botón "Actualizar" visible.
- `.then()` en `onTap` ahora tiene `await Future.delayed(900ms)` antes de `obtenerTrabajos()` para dar tiempo al servidor a procesar el cambio en BD
- `_tieneAccionPendiente` refactorizado: ahora es explícito (if-return) en lugar de un `||` compuesto, y el estado REALIZADO solo activa el badge si `valoracion == 0`

#### `detalle_trabajo_pantalla.dart`

- **Todos los `Navigator.popUntil` eliminados** → reemplazados por `Navigator.pop(context)` simple
- Las llamadas a `obtenerTrabajos()` también se eliminaron de aquí (el dashboard las hace en `.then()`)
- `_handleBorrar`: limpiado (antes tenía `popUntil` que causaba crash de pantalla negra)
- `_aceptarPresupuesto`: idem
- `_finalizarTrabajo`: idem
- `_handleValorar`: en caso de error muestra SnackBar y hace `return` (no navega); en caso de éxito solo hace `pop`

#### `crear_trabajo_pantalla.dart`

- Campo `direccion` ya no tiene validador obligatorio
- Hint text: "Si se deja vacío, se usa tu dirección registrada"
- El campo `direccion` solo se incluye en el mapa `datos` si no está vacío (condicional `if` en el Map literal)

#### `trabajo_provider.dart`

- `cancelarTrabajo`: Añadido `obtenerTrabajos()` tras el delay de 800ms (antes solo retornaba `true` sin actualizar la lista)

### Bugs resueltos esta sesión

1. **Pantalla negra al finalizar/valorar/borrar** → Causa: `popUntil` con rutas anónimas vaciaba el stack. Fix: `Navigator.pop()` simple.
2. **Lista no se actualizaba después de acciones** → Causa: `obtenerTrabajos()` se llamaba antes del delay del servidor. Fix: Delay de 900ms en el `.then()` del dashboard.
3. **Dirección del trabajo siempre mostraba la del cliente** → Causa: Campo `direccion` ausente en JSON de LISTAR_TRABAJOS. Fix: `map.put("direccion", t.getDireccion())` en procesador Java.
4. **Cancelar desde detalle no actualizaba el dashboard** → Causa: `cancelarTrabajo` no llamaba a `obtenerTrabajos()`. Fix: añadido tras delay.
5. **Pantalla vacía del operario no permitía pull-to-refresh** → Fix: `CustomScrollView` con `AlwaysScrollableScrollPhysics`.
6. **Badge de "Valorar" persistía tras valorar** → Fix: condición `valoracion == 0` explícita en `_tieneAccionPendiente`.

---

## PRÓXIMAS FASES

### Fase 2: Fotos con Firebase Storage ⬜ SIGUIENTE

#### Plan de implementación:

**Firebase (setup):**

- [ ] Crear proyecto Firebase
- [ ] Añadir app Android al proyecto Firebase (google-services.json)
- [ ] Añadir dependencias en Flutter: `firebase_core`, `firebase_storage`, `image_picker`

**Flutter — Trabajos:**

- [ ] En `CrearTrabajoPantalla`: activar botón "Añadir foto" → `image_picker` → subir a Firebase Storage → recibir URL → añadir a `_urlsFotos`
- [ ] Enviar `urls_fotos` en el JSON al servidor ya que el campo existe en el mapa de datos
- [ ] `GaleriaFotos` ya está preparado → solo necesita URLs reales

**Flutter — Perfil de usuario:**

- [ ] En `PerfilPantalla`: añadir botón de editar foto → `image_picker` → subir a Firebase → actualizar `url_foto` del usuario en servidor
- [ ] Backend: nueva acción `ACTUALIZAR_PERFIL` o `SUBIR_FOTO_PERFIL` en `ProcesadorAutenticacion`
- [ ] Modelo `Usuario.urlFoto` ya existe → solo falta el flujo de subida

**Backend Java:**

- [ ] `FotoTrabajo` ya existe como clase. `FotoTrabajoDAO` ya existe y guarda en BD
- [ ] El servidor ya intenta cargar fotos en `procesarListarTrabajos` → solo falta recibir y guardar URLs al crear
- [ ] La acción `CREAR_TRABAJO` ya lee `urls_fotos` del JSON y llama a `fotoTrabajoDAO` → ya implementado

### Fase 3: Despliegue Local en Red (Móvil Físico) ⬜

**Objetivo:** Hacer funcionar la app en un móvil físico real dentro de la misma red WiFi.

- [ ] **SocketService:** Cambiar IP de `10.0.2.2` a la IP local de la máquina (ej: `192.168.1.X`)
  - Crear variable configurable o pantalla de configuración de IP
- [ ] **Firebase:** Ya funcionará con IP real (es HTTPS externo)
- [ ] **Servidor Java:** Asegurarse de que el firewall de Windows abre el puerto `5000`
  - PowerShell: `New-NetFirewallRule -DisplayName "FixFinder" -Direction Inbound -Protocol TCP -LocalPort 5000 -Action Allow`
- [ ] **App Escritorio (JavaFX dashboard):** Probado en red local (ya se conecta por socket a localhost)

### Fase 4: Despliegue en AWS EC2 ⬜

**Objetivo:** Servidor Java en la nube, app conectando a IP pública.

- [ ] Provisionar EC2 (Ubuntu 22.04 recomendado), instalar Java 21 y MySQL
- [ ] Copiar el JAR del servidor (`.\gradlew.bat jar`)
- [ ] Abrir puertos: `5000` (TCP) y `3306` (MySQL, solo acceso interno)
- [ ] Crear script de arranque automático con `systemd`
- [ ] **SocketService Flutter:** Parametrizar IP (leer de config) → apuntar a IP pública AWS
- [ ] Firebase Storage ya funciona con cualquier IP (es servicio externo)
- [ ] Probar flujo completo cliente → servidor AWS → BD RDS (o MySQL en EC2)

### Fase 5: Documentación y Defensa ⬜

---

SESIÓN 07/03/2026 — Refinado Gerencial y Comunicación

### Objetivo de la sesión

Refinar la visualización de la empresa (valoraciones reales), unificar la comunicación gerente-operario mediante la "Hoja Informativa" y solucionar errores críticos en la gestión de operarios del Dashboard.

### Backend (Java) — Cambios

#### `ProcesadorUsuarios.java`

- **Enriquecimiento de Empresa:** Al solicitar datos de empresa (`GET_EMPRESA`), el servidor ahora busca todos los trabajos `FINALIZADOS` vinculados a esa empresa y devuelve una lista de valoraciones reales (puntuación, cliente, comentario, fecha).
- **Limpieza de Referencias:** Eliminadas instanciaciones directas de `DataRepository` para favorecer la estabilidad de conexiones.

#### `OperarioDAOImpl.java`

- **Sincronización de ENUM SQL:** Se ha corregido la palabra mágica. El SQL usa `BAJA`, pero el código enviaba `INACTIVO`. Ahora se envía `BAJA` al desactivar (baja lógica).
- **Corrección de Mapeo:** Se asegura que al leer de la DB, cualquier estado distinto de `BAJA` se interprete como `estaActivo = true`.

#### `OperarioServiceImpl.java`

- **Pruebas Rápidas:** Se han comentado las validaciones de `matches()` para Email, DNI y Teléfono para permitir avanzar con datos de prueba no perfectos.
- **Sanitización:** Se ha añadido un `.replaceAll("[^0-9]", "")` al teléfono para evitar fallos por espacios o guiones.

#### `PresupuestoServiceImpl.java` & `ProcesadorTrabajos.java`

- **Eliminación de `notas`:** Se ha borrado el campo `notas` de la tabla `presupuesto` y de los objetos Java. Ya no se usa.

### App Escritorio (JavaFX) — Cambios

#### `VistaEmpresa.java`

- **Sección de Reseñas:** Implementada una lista visual que muestra las últimas valoraciones de los clientes con estrellas (⭐).
- **Fecha de Registro:** Corregida la visualización de la fecha de alta de la empresa (ya no sale "No disponible").

#### `DialogoCrearPresupuesto.java`

- **Hoja Informativa:** Ahora el área de texto de "Notas" actualiza directamente la `descripcion` del trabajo.
- **Plantilla Automática:** Si la descripción no está estructurada, el diálogo inserta una plantilla con cabeceras para `CLIENTE`, `GERENTE` y `OPERARIO`.

#### `DashboardPrincipalController.java`

- **Sincronización de Callbacks:** Los métodos `onPresupuestar` y similares ya no usan el parámetro `notas`, sino que gestionan la `nuevaDescripcion` del trabajo.

### Bugs resueltos esta sesión

1. **Error 500 al dar de baja operario:** Causa: Discrepancia entre "INACTIVO" e "BAJA" en el ENUM de MySQL. Fix: Sincronizado a "BAJA".
2. **Edición de operario fallaba por validación:** Causa: Teléfono con espacios o formato de email estricto. Fix: Comentadas validaciones y sanitizado teléfono.
3. **Valoraciones de empresa vacías:** Causa: No se estaban consultando los trabajos finalizados. Fix: Implementada búsqueda por empresa en el procesador.
4. **Desconexiones por "Connection Reset":** Causa: Demasiadas aperturas de `DataRepositoryImpl`. Fix: Refactorizado a uso de DAOs directos cuando es posible.

---

## PRÓXIMAS FASES

- [ ] Memoria técnica (arquitectura, decisiones de diseño, protocolo de comunicación)
- [ ] Diagrama de clases, diagrama de secuencia del flujo completo
- [ ] Presentación + ensayo

---

## 🛠️ COMANDOS DE REFERENCIA RÃPIDA

```powershell
# Arrancar el servidor
cd C:\Users\ncond\Desktop\FF\FIXFINDER
.\gradlew.bat runServer

# Arrancar dashboard JavaFX
.\gradlew.bat runClient

# Resetear BD con datos de prueba
.\gradlew.bat runSeeder

# Correr tests del backend
.\gradlew.bat test

# Arrancar app Flutter (dos emuladores)
cd C:\Users\ncond\Desktop\FF\fixfinder_app
flutter run -d emulator-5554
flutter run -d emulator-5556

# Hot reload (en consola de flutter run)
r   → hot reload
R   → hot restart (limpia estado)

# Correr tests Flutter
flutter test

# Abrir firewall para red local
New-NetFirewallRule -DisplayName "FixFinder" -Direction Inbound -Protocol TCP -LocalPort 5000 -Action Allow

# Consultar la BD
docker exec -i FixFinderDb mysql -u root -proot fixfinder -e "SELECT id,titulo,estado,valoracion,direccion FROM trabajo ORDER BY id DESC LIMIT 10;"

# Ver usuarios
docker exec -i FixFinderDb mysql -u root -proot fixfinder -e "SELECT id,email,rol,telefono,direccion FROM usuario;"

# Restaurar teléfonos si los tests los borran
docker exec -i FixFinderDb mysql -u root -proot fixfinder -e "UPDATE usuario SET telefono = '600123456' WHERE rol = 'CLIENTE' OR rol = 'OPERARIO';"
````

---

**Nota Final:** Trabajar siempre paso a paso. Antes de implementar una nueva funcionalidad,
leer este documento para no romper lo que ya funciona. El protocolo de 4 bytes y el patrón
`pop + then(delay + obtenerTrabajos)` son invariantes críticos del sistema.

---

## 🔧 SESIÓN 08/03/2026 — Auditoría de Calidad y Decisión de Ruta

### Objetivo de la sesión

Revisar el código completo, evaluar calidad, detectar clases problemáticas y preparar un plan quirúrgico de refactorización. En esta sesión **no se aplicaron los cambios** (por precaución, dado que el proyecto estaba en estado funcional). Se creó un checkpoint de Git antes de cualquier cambio.

### Estado del Repositorio

- **Rama actual:** `refactor`
- **Commit de punto de partida (pre-refactorización):** `ec6f1d3` — "pre refactor"
- **Comando para volver atrás si algo se rompe:**
  ```powershell
  git checkout ec6f1d3 -- .
  # o para descartar todos los cambios y volver al commit exacto:
  git reset --hard ec6f1d3
  ```

### Cambios aplicados ANTES de la auditoría (inicio de sesión)

Se realizaron estas modificaciones que YA ESTÃN en el commit `ec6f1d3`:

#### `ProcesadorTrabajos.java` — Refactorización parcial aplicada

- Método `mapearTrabajo(Trabajo t)` extraído como privado: centraliza la conversión de objeto Trabajo a `Map<String, Object>`. Antes se repetía inline en cada bloque del listado.
- Método `filtrarParaGerente(int idUsuario)` extraído como privado: encapsula la lógica de qué trabajos ve un gerente (PENDIENTE + PRESUPUESTADO + los de su empresa).
- **⚠️ ATENCIÓN:** La refactorización introdujo errores de compilación que se resolvieron durante la sesión. Los imports correctos son `com.fixfinder.modelos.enums.EstadoTrabajo` y `com.fixfinder.modelos.enums.EstadoPresupuesto`. La firma del servicio de cancelar es `cancelarTrabajo(Integer, String)` → siempre pasar motivo.

#### `DashboardPrincipalController.java`

- El método `solicitarTrabajos()` ahora también llama a `servicioCliente.enviarGetEmpresa(idEmpresa)` para refrescar los datos de la empresa sin necesidad de re-login.

#### `VistaDashboard.java`

- Animación añadida al botón `btnRefresh`: `RotateTransition` (360°, 0.5s) + `ScaleTransition` (1→0.85→1, 0.5s) en paralelo mediante `ParallelTransition`. Se ejecuta cada vez que se pulsa el botón.

#### `socket_service.dart` (Flutter)

- Añadido método `request(Map peticion, {String? accionEsperada, int timeoutSegundos})`: encapsula el patrón Completer + listen + timeout + cancel en un solo método reutilizable. Preparado para limpiar `TrabajoProvider`.

---

### ðŸ” Auditoría Completa de Calidad — Resultados

#### BACKEND JAVA

| Clase                                         | Tamaño    | Diagnóstico                                                                                                                                                                                                                   | Severidad   |
| --------------------------------------------- | --------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------- |
| `ProcesadorTrabajos.java`                     | ~280L     | ✅ Refactorizado. Mapeo único, filtrado encapsulado.                                                                                                                                                                          | ✅ Resuelto |
| `ProcesadorAutenticacion.java`                | 233L      | ⚠️ Método `procesarRegistro` mezcla 3 flujos (CLIENTE, OPERARIO, EMPRESA) en uno. Difícil de mantener.                                                                                                                        | ⚠️ Medio    |
| `ProcesadorUsuarios.java`                     | 209L      | ⚠️ Instancia DAOs directamente (`new EmpresaDAOImpl()`). Viola inversión de dependencias. La lógica de valoraciones de empresa (50L) debería estar en el Service, no en el Procesador.                                        | ⚠️ Medio    |
| `TrabajoServiceImpl.java`                     | 337L      | â„¹ï¸ `historialOperario` carga TODOS los trabajos y filtra en Java (no en SQL). Con muchos datos puede ser lento. La lógica de "parsear descripción por emojis" en `finalizarTrabajo` es frágil si alguien cambia la plantilla. | â„¹ï¸ Bajo     |
| `TrabajoDAOImpl.java`                         | 371L      | (ROJO) **N+1 Problem:** El método `cargarRelaciones` abre una nueva conexión SQL por cada trabajo de la lista para cargar cliente + operario + fotos. En 50 trabajos = 150 queries. Solución: JOIN en la query principal.         | (ROJO) Alto     |
| `GestorConexion.java`                         | 238L      | ✅ Bien diseñado. Delega. No tocar.                                                                                                                                                                                           | ✅ OK       |
| `ServidorCentral.java`                        | 110L      | ✅ Limpio. Semáforo de 10 conexiones.                                                                                                                                                                                         | ✅ OK       |
| `OperarioDAOImpl.java`, `EmpresaDAOImpl.java` | ~11KB c/u | ✅ Aceptables. Sin duplicación visible.                                                                                                                                                                                       | ✅ OK       |

#### DASHBOARD JAVAFX

| Clase                                 | Tamaño      | Diagnóstico                                                                                                                                        | Severidad |
| ------------------------------------- | ----------- | -------------------------------------------------------------------------------------------------------------------------------------------------- | --------- |
| `TablaIncidencias.java`               | 422L / 17KB | (ROJO) **GOD CLASS:** Controla tabla, 8 tipos de celdas, 3 diálogos de acción, filtros, tabs Y decoración de iconos. Si falla un método afecta a todo. | (ROJO) Alto   |
| `DashboardPrincipalController.java`   | ~331L       | ⚠️ Switch `procesarRespuesta` con ~20 casos. Funciona, pero en el límite de lo mantenible.                                                         | ⚠️ Medio  |
| `VistaDashboard.java`, `Sidebar.java` | <200L c/u   | ✅ Limpias.                                                                                                                                        | ✅ OK     |
| `TrabajoFX.java`, `OperarioFX.java`   | ~130L c/u   | ✅ JavaFX Properties correctas.                                                                                                                    | ✅ OK     |
| `DialogoNuevoOperario.java`           | 6KB         | â„¹ï¸ Grande pero cohesivo.                                                                                                                           | â„¹ï¸ Bajo   |

#### APP FLUTTER

| Clase                           | Tamaño      | Diagnóstico                                                                                                                                                                                                                                      | Severidad |
| ------------------------------- | ----------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | --------- |
| `trabajo_provider.dart`         | 365L / 11KB | (ROJO) **Boilerplate masivo:** Cada uno de los 8 métodos replica exactamente el mismo patrón `Completer + listen + send + timeout + cancel`. ~200L son código idéntico. El método `request()` ya existe en `socket_service.dart` para resolver esto. | (ROJO) Alto   |
| `dashboard_pantalla.dart`       | 228L        | ⚠️ Tiene lógica de negocio mezclada con UI: `_tieneAccionPendiente()` y `_obtenerIconoCategoria()` deberían estar en el modelo o en un helper.                                                                                                   | ⚠️ Medio  |
| `detalle_trabajo_pantalla.dart` | 269L        | ✅ Ya refactorizada (usa widgets separados).                                                                                                                                                                                                     | ✅ OK     |
| `tarjeta_trabajo.dart`          | 239L        | ✅ Bien encapsulada con animación propia.                                                                                                                                                                                                        | ✅ OK     |
| `socket_service.dart`           | ~200L       | ✅ Mejorado con `request()`.                                                                                                                                                                                                                     | ✅ OK     |

---

### 📋 Plan de Refactorización Quirúrgica (PENDIENTE DE EJECUTAR)

**Principio:** Cada bloque se compila y prueba ANTES de pasar al siguiente. Las firmas públicas de todos los métodos se respetan (sin breaking changes).

#### Bloque 1 — Flutter: Limpiar `trabajo_provider.dart`

- **Qué:** Migrar los 8 métodos al nuevo `_socket.request()`. Eliminar ~200L de boilerplate.
- **Riesgo:** Bajo. Solo cambia la implementación interna, no el contrato.
- **Estimación:** 365L → ~190L (−48%)
- **Mover `_obtenerIconoCategoria`** → helper estático en `models/trabajo.dart`
- **Mover `_tieneAccionPendiente`** → método de instancia en `Trabajo`

#### Bloque 2 — Java: Query JOIN en `TrabajoDAOImpl`

- **Qué:** Crear método `obtenerTodosConJoin()` con SQL que incluye `LEFT JOIN usuario` y `LEFT JOIN operario` para evitar las 150 queries al listar.
- **Riesgo:** Medio. Tocar el DAO más crítico del sistema.
- **El método `cargarRelaciones` actual se mantiene** para `obtenerPorId` (no es crítico en uso individual).

#### Bloque 3 — Java: Separar `ProcesadorAutenticacion.procesarRegistro`

- **Qué:** Extraer `registrarCliente()`, `registrarOperario()`, `registrarEmpresa()` como métodos privados. El método público queda como router de 3 líneas.
- **Riesgo:** Bajo. Sin cambio de firma pública.

#### Bloque 4 — JavaFX: Extraer helpers de `TablaIncidencias.java`

- **Qué:** Crear `UiHelper.java` con `miniAvatar()`, `crearLabel()`, `fila()`. Crear `CategoriaHelper.java` con `iconoCategoria()`.
- **Riesgo:** Bajo. Solo mover código, sin cambiar lógica.

---

### 🤔 DEBATE: ¿Refactorización primero o Firebase Fotos primero?

**Argumento para Firebase primero:**

- El proyecto ya está funcional y presentable para un `proyecto de clase`.
- Fotos es una funcionalidad visible que añade valor real al evaluador.
- La infraestructura ya está casi lista (`GaleriaFotos.dart` preparada, `FotoTrabajoDAO` existe, el servidor ya procesa `urls_fotos`).
- **Estimación:** 2-3 horas para un MVP funcional (seleccionar foto → subir → mostrar).

**Argumento para Refactorización primero:**

- El boilerplate en `TrabajoProvider` con Completers manuales es una fuente real de bugs silenciosos (memory leaks si el timeout falla).
- Si añadimos Firebase sobre código "sucio", el provider crecerá aún más.
- La God Class `TablaIncidencias` se volverá inmanejable si añadimos funcionalidad de fotos al dashboard.
- Con git y el commit de punto de partida, el riesgo de romper algo es mínimo.

**Recomendación del Agente:**

> Hacer primero el **Bloque 1** (Flutter `TrabajoProvider`) porque es el de menor riesgo, mayor impacto visible en limpieza y prepara el terreno para Firebase. Luego pasar a Firebase. Los Bloques 2, 3 y 4 (Java) se pueden hacer en una sesión separada cuando haya más tiempo.

---

## ✅ TAREAS COMPLETADAS: REFACTORIZACIÓN Y OPTIMIZACIÓN (Actualizado 15/03/2026)

Las siguientes mejoras estructurales y de rendimiento han sido implementadas:

1. **Limpieza Integral de Código Java**:
   - Eliminados todos los **imports inline (FQN)** en el proyecto backend y dashboard.

---


---


---

## ✅ SESIÓN 22/03/2026 — Refactorización final, documentación y commit

### Lo que se hizo en esta sesión

#### 🔨 Refactorización del código (Java)
- **`DashboardController`** reducido de 700+ líneas a **214 líneas**:
  - Creada `DashboardBase.java` — declaraciones @FXML y base UI.
  - Creada `GestorRegistroDashboard.java` — lógica de registro de empresa/usuario.
  - Creada `ManejadorRespuestaDashboard.java` — procesamiento de mensajes del servidor.
- **`SimuladorController`** reducido de 600+ líneas a **260 líneas**:
  - Creada `ClienteRedSimulador.java` (Singleton) — comunicación TCP del simulador.
  - Creada `ModeloTrabajoSimulador.java` — modelo de datos del simulador.
- **Validaciones restauradas** en `UsuarioServiceImpl.java` (email y teléfono estaban comentadas).
- **Tests 100% verdes** — todos los 10 tests de `ServiceTest.java` pasan.
- **Nomenclatura en castellano** en todos los nuevos componentes.
- **runSeeder** ejecutado correctamente para resetear la BD con datos de prueba.

#### 📄 Documentación
- **`DOCS/MEMORIA.md`** creada completa siguiendo la guía oficial del IES Maria Enríquez:
  - 9 secciones con todos los campos en blanco preparados para redactar.
  - **12 diagramas PNG** colocados en sus secciones correspondientes con referencias Mermaid.
  - Sección 5.1 incluye árbol de directorios + arquitectura AWS (EC2 + RDS + Firebase).
  - Sección 5.3 aclara explícitamente que **local = desarrollo/pruebas, AWS = despliegue final**.
  - Encabezados con jerarquía correcta para conversión a PDF.
  - Anexo D con tabla completa de todos los archivos de diagramas `.txt`.
- Diagramas verificados — entidad-relación confirmado correcto (herencia Usuario→Cliente/Operario, Empresa emite Presupuesto, no directamente Trabajo).

#### 🧹 Limpieza del proyecto
- Eliminados todos los archivos temporales de log y error: `build_error.txt`, `build_log.txt`, `test_error_log.txt`, `gradle_out.txt`, `flutter_log_f.txt`, etc.
- Eliminados ficheros Flutter obsoletos: `-nCon-Book.flutter-plugins-dependencies`, `test_conexion.dart`.

#### 📦 Git
- **Commit:** `b770ed3` — "Refactor clases grandes y debug, doc final y diagramas"
- **52 archivos cambiados**, 2105 inserciones (+), 369 borrados (-)
- **Push a `origin/main`** completado correctamente.

---

**Último Commit Git:** `b770ed3` — "Refactor clases grandes y debug, doc final y diagramas" (22/03/2026)

---

## ✅ SESIÓN 17/04/2026 — Identidad Visual y Estados Reactivos

### Lo que se hizo en esta sesión

#### 🚀 App Móvil: Unificación y Reactividad
- **Eliminación de Splash duplicado**: Se ha borrado `SplashPantalla.dart` eliminando el molesto "parpadeo" entre el splash del sistema y el de Flutter.
- **Implementación de AuthWrapper**: Nuevo componente que gestiona el auto-login manteniendo el splash nativo visible hasta que la app está lista.
- **UsuarioProvider**: Implementación de `ChangeNotifier` para que el perfil (foto, nombre, datos) se actualice en toda la app de forma instantánea al editar.
- **Native Splash Profesional**: Configuración de `flutter_native_splash` con logo y lema corporativo ajustado para evitar recortes en Android 12+.

#### 📊 Dashboard: Evolución Visual de Incidencias
- **Bloques Visuales de Seguimiento**: Sustitución del `TextArea` de descripción por un historial por bloques:
  - **CLIENTE (Azul Acero)**: El problema inicial.
  - **GERENTE (Ámbar)**: Notas de presupuesto.
  - **OPERARIO (Verde)**: Informe de cierre.
- **Limpieza de Nomenclatura**: Estandarización de todo el código nuevo al castellano.

#### 🧹 Mantenimiento
- **Limpieza de Assets**: Eliminación de imágenes de prueba y archivos de splash huérfanos.
- **Escalabilidad**: El código del Dashboard ahora permite añadir nuevos tipos de bloques (ej. fotos paso a paso) sin romper el diseño.

---

## 🎯 PRÓXIMOS PASOS (SIGUIENTE SESIÓN)

### Prioridad 1 — Cierre de UI/UX y Tiempo Real (Broadcaster)
- [ ] **Broadcaster (Backend)**: Implementar el patrón Observer en el servidor para notificar cambios de estado (Nuevos trabajos, ofertas aceptadas) a todos los clientes conectados.
- [ ] **Broadcaster (Apps)**: Conectar los widgets de Flutter y las tablas de JavaFX al flujo de eventos push para eliminar el polling de 15s.
- [x] Centralizar estilos hardcoded en `app_theme.dart` (Flutter) y `style.css` (Dashboard).

### Prioridad 2 — Despliegue AWS (Producción)
- [ ] **Levantar RDS MySQL:** Crear instancia `db.t3.micro`, configurar Security Group.
- [ ] **Migrar esquema:** ejecutar `SCHEMA.sql` en RDS.
- [ ] **Lanzar EC2:** Instancia `t3.micro` Ubuntu, instalar Docker + Java 21.

### Prioridad 3 — Memoria académica
- [ ] Redactar las secciones de texto de `DOCS/MEMORIA.md`.
- [ ] Insertar capturas de pantalla reales (App con el nuevo Splash y Dashboard con bloques).
- [ ] Exportar a PDF cuando esté lista.




---

SESIÓN 01/04/2026 — Limpieza de Arquitectura y Restauración de App

### Objetivo de la sesión
Resolver los bloqueos de compilación que impedían arrancar el sistema tras el último refactor y restaurar la funcionalidad básica de la App móvil que presentaba métodos ausentes.

### Backend y Dashboard (Java)
- **Limpieza de Código Legado:** Se han eliminado los archivos huérfanos del paquete `com.fixfinder.controladores` (`DashboardController`, `DashboardBase`, `GestorRegistroDashboard` y `ManejadorRespuestaDashboard`). Estos archivos causaban errores de símbolo no encontrado al intentar referenciar métodos que ya no existen en la nueva arquitectura modular.
- **Estado:** El proyecto Java ahora compila correctamente con `./gradlew compileJava`. 
- **Servidor:** Operativo y conectado a la base de datos (MySQL en Docker). Escuchando en el puerto 5000.

### App Móvil (Flutter)
- **Restauración de `AuthService.dart`:** Se ha re-implementado el método `registrar` que faltaba en el repositorio. Este método es esencial para que la pantalla de registro pueda enviar los datos al servidor.
- **Corrección de `SocketService.dart`:** Añadido el getter `isConectado` para facilitar la gestión de conexiones desde los servicios.
- **Sincronización de `JobApiService.dart`:** Corregido un error de sintaxis en el cierre de la clase que impedía que el compilador detectara correctamente el método `finalizeTrabajo`.
- **Análisis:** Tras los cambios, `flutter analyze` ya no reporta errores críticos de métodos no definidos en los servicios principales.

### Notas para la próxima sesión
- [ ] **Modificación de Perfil:** Implementar la edición de datos personales (teléfono, dirección, etc.) en `perfil_pantalla.dart`. Actualmente solo funciona el cambio de foto. Requiere crear una nueva acción en el servidor Java (ej: `ACTUALIZAR_DATOS_USUARIO`).
- [ ] **Validación de Registro:** Probar el flujo de registro en la App móvil para confirmar que el nuevo método `registrar` se comunica correctamente con el `ProcesadorAutenticacion` del servidor.
- [ ] **Validación de Finalización:** Verificar que un operario puede finalizar un trabajo sin errores de comunicación.
- [ ] **Continuar con AWS:** Una vez confirmada la estabilidad local, proceder con la configuración de la instancia EC2.

---

---

## [02/04/2026] - Sesión: Gran Unificación "Smart Main" y Blindaje de Infraestructura

Estado: VERIFICADO (PENDIENTE DE RE-AUDITORÍA DE SEGURIDAD AL ARRANCAR)

### Logros (Lujo de Detalle)

#### 1. Arquitectura Smart Main
Se ha eliminado la dependencia de ramas (Local vs AWS). Ahora el proyecto reside en un solo **MAIN** inteligente:
*   **Java Core (GlobalConfig.java):** Nueva clase maestra que centraliza el interruptor MODO_NUBE. Gestiona dinámicamente las URLs de JDBC para AWS RDS vs Docker Local y resuelve la IP del servidor de Sockets.
*   **Flutter Reactivo (.env):** Implementación de variables de entorno para que la App móvil resuelva su conexión de forma "Plug & Play" sin tocar código Dart.

#### 2. Indicadores de Estado
Implementación de telemetría visual en las pantallas de Login:
*   **Lógica de Colores:** 
    *   🔵 **Azul**: Modo LOCAL activo (Docker detectado).
    *   🔘 **Gris**: Intentando conectar (Cloud) o estado desconocido.
    *   🟢 **Verde**: Conexión exitosa con la instancia **EC2 de AWS**.
*   **Paquetes:** Reubicación de la lógica de test en com.fixfinder.TestPanel.
*   **Colisión de Nombres Solucionada:** El lanzador principal ahora es **LanzadorTestPanel.java**, evitando conflictos con clases del mismo nombre en subpaquetes.
*   **TestPanelController:** Actualizado para usar GlobalConfig.getServerIp(), permitiendo que el Tester de bajo nivel también funcione contra AWS.
*   **Simulador E2E:** Recuperado el simulador completo para realizar flujos de "Un Solo Hombre" (Gerente/Operario/Cliente a la vez).

#### 5. Optimización de Compilación
*   **Gradle Magic:** Añadida la tarea personalizada 
unTestPanel en uild.gradle que permite el lanzamiento limpio de las herramientas de test sin pasar por el modularismo estricto de JavaFX, resolviendo errores de visión de clases con las librerías de Firebase.

---

PRÓXIMA SESIÓN (URGENTE: AUDITORÍA Y GRADUACIÓN AWS)

### PRIORIDAD CRÍTICA: AUDITORÍA "RAYOS X"
Al retomar la sesión, **lo primero** es verificar de nuevo que el comando 'git restore .' no haya deshecho los cambios en ConexionDB, DbClean y los controladores de FXML. Comprobar línea a línea que el Smart Switch sigue intacto.

### 🧪 2. Certificación Local
* Lanzar el servidor y el dashboard en local.
* Confirmar semáforos **AZULES**.

### ☁️ 3. Despliegue en Caliente (AWS Grad)
* Switchear MODO_NUBE = true.
* Generar FIXFINDER.jar (updated version).
* Subir a EC2 vía SCP y reiniciar el servicio remoto.
* Confirmar semáforos **VERDES**.

### 📦 4. Generación de Entregables Release
* Build final de la APK release contra AWS.
* Empaquetado del Dashboard en instalador EXE (jpackage).

### 🧹 5. Deuda Técnica y Refactorización Pendiente (God Object a Trocear)
*   **Clase Afectada:** `TablaIncidencias.java` (Dashboard Desktop).
*   **Problema:** Violación flagrante del Principio de Responsabilidad Única (SRP). La clase es un "Objeto Dios" de casi 400 líneas.
*   **Diagnóstico:** Combina lógica de interfaz visual pura (VBox, controles) con lógica de negocio (filtrado estructurado interceptando eventos de botones), fábricas completas de celdas anónimas (`updateItem`) con HTML embebido, lógica generadora de avatares matemáticos y lanzamiento manual de diálogos modales.
*   **Propuesta de Arquitectura:** 
    1.  Extraer lógica de filtrado a un `FiltrosIncidenciaController`.
    2.  Extraer los Cell Factories a sus propias clases `.java` (ej. `AvatarCellFactory`, `EstadoBadgeFactory`).
    3.  Aislar los utilitarios visuales genéricos (`miniAvatar()`, `iconoCategoria()`) en un `UIComponentUtils.java` estático para darles reutilización en todo el proyecto.
    *(Requisito muy valioso para presentar en el apartado de "Mejoras Futuras" de la memoria).*

---

# TAREAS PENDIENTES, MEJORAS O PARA PRÓXIMAS VERSIONES

Esta sección centraliza la hoja de ruta técnica unificada, integrando deuda técnica, mejoras de UI y nuevas funcionalidades críticas de negocio.

## Nucleo de Negocio: Sistema de Presupuestos y Trabajo (REPLANTEO 07/04)

**Objetivo:** Implementar la segregación de responsabilidades y el ciclo de vida de rechazo/re-presupuesto.

- [x] **Logica de Rechazo de Presupuesto:**
  - Implementar acción `RECHAZAR_PRESUPUESTO` en el Backend/DAO.
  - El estado del trabajo debe volver a `PENDIENTE` automáticamente.
  - **Reset de Descripción:** Al rechazar, el campo `descripcion` del trabajo debe limpiarse de notas previas, volviendo a mostrar **únicamente el mensaje inicial del cliente**.
- [x] **Segregacion de Escritura (Gerente en Dashboard):**
  - El gerente ya no editará la descripción general. Se habilitará un `TextField` / `TextArea` exclusivo para su propuesta económica/técnica.
  - El servidor gestionará la visualización de este bloque de forma independiente al mensaje original del cliente.
- [x] **Visibilidad del Cliente (App Movil):**
  - Implementar **botón de "Rechazar Presupuesto"** en `detalle_trabajo_pantalla.dart`.
  - Mostrar claramente que el presupuesto fue rechazado y que la incidencia vuelve a esperar una propuesta técnica.

## App Móvil (Flutter)
- [x] **Gestión de Perfil:** Habilitar la modificación de datos personales (teléfono, dirección) en `perfil_pantalla.dart` como cliente.
- [x] **Registro de Usuarios:** Implementar pantalla y flujo de alta de nuevos clientes desde la App.
- [ ] **Actualización Real-Time (Broadcaster):** Implementar la escucha de eventos del Broadcaster para refrescar datos automáticamente sin polling.
- [x] **Refactor de Red:** Migrar los métodos del `TrabajoProvider` al sistema genérico `_socket.request()` para eliminar boilerplate masivo (~200 líneas).
- [x] **Tema de Colores:** Extraer todos los colores hardcodeados de `main.dart` a un fichero `lib/theme/app_theme.dart` (ThemeData) para centralizar el estilo de la App.
- [x] **Tema para Dashboard (CSS):** Centralizar estilos en un archivo `style.css` y eliminar el uso de `setStyle` en el código Java.
- [ ] **Actualización Real-Time (Broadcaster):** Implementar la escucha de eventos del Broadcaster en el Dashboard para refrescar la tabla de incidencias automáticamente.
- [x] **Ajuste de UI:** Calibrar el ancho de las columnas (especialmente "Estado") para evitar solapamientos.
- [ ] **Mejora de Descripción:** Trocear la descripción estructurada en bloques visuales independientes en `DialogoGestionIncidencia.java`.
- [ ] **Diagrama de Dashboard:** Crear diagrama de componentes específico para la arquitectura JavaFX modularizada.

## Backend y Deuda Técnica (Java)
- [ ] **Sistema Broadcaster:** Implementar la lógica en el servidor para notificar eventos en tiempo real a la App y al Dashboard.
- [x] **Optimización SQL (Problema N+1):** Refactorizar `cargarRelaciones()` en `TrabajoDAOImpl.java` para usar un único `LEFT JOIN` masivo.
- [x] **Refactor TablaIncidencias:** Desmontar la "God Class" `TablaIncidencias.java`. Separar factorías de celdas, filtrado y diálogos en clases independientes (SRP).
- [x] **Micro-refactor Autenticacion:** Trocear `procesarRegistro` en métodos privados segregados por rol.
- [x] **Gestion de Timeouts:** Asegurar que acciones como `VALORAR` o `CANCELAR` devuelvan siempre la clave `"mensaje"` en el JSON para evitar interrupciones de flujo en la App.

## Pruebas y QA
- [ ] **Terminar Suite de Tests (Post-Broadcaster):** Implementar por completo los tests unitarios y de integración a partir de los esqueletos preparados. Se pospone a después de la implementación del Broadcaster para asegurar que los tests cubran el flujo de tiempo real.

## Tareas Tras la Revisión del Tutor
- [ ] **Apaciguar la falta de Providers:** Crear un `usuario_provider.dart` ligero como envoltorio del perfil y documentar en la MEMORIA la desestimación técnica del "EmpresaProvider" por Arquitectura de Dominios.
- [ ] **Dividir Diagramas en la Memoria:** Sustituir en `MEMORIA.md` el diagrama global kilométrico por versiones troceadas (Flujo de Login, Trabajos, etc.) con sintaxis Mermaid para evitar el "efecto hormiga" en PDF.
- [x] **Renombrar Pruebas a Prototipos:** Cambiar la nomenclatura de la carpeta de "tests de UI" a "prototipos" en la memoria si fuese necesario para encajar en el glosario académico.
- [ ] **Generar Modelo E-R Visual:** Conectar MySQL Workbench y extraer diagrama Entidad-Relación explícito e incrustarlo en la documentación.
- [x] **Incorporar Funciones "Móviles" Nativas:** Integrar `url_launcher` para llamadas telefónicas desde la ficha de operarios y añadir integración de Mapas (GPS locales) para la dirección del trabajo.

---
_Bitácora técnica consolidada. El sistema está preparado para la implementación de la lógica de presupuestos segregados._



---

- Inyectada lógica de seguridad en `ClienteSocket.enviar()` mediante el keyword `synchronized`.

---

## ✅ SESIÓN 08/04/2026 — Refactor Estructural, IDs de Transacción y Multipresupuesto

### Lo que hemos conseguido hoy (Resumen Técnico)
- **Implementación de Transaction IDs (txid)**: Hemos blindado la comunicación Sockets. Cada paquete JSON incluye un `txid` único. El servidor devuelve el mismo ID, permitiendo asincronía perfecta.
- **Refactor de Red en Flutter**: Implementado el método maestro `request()` en `socket_service.dart`, eliminando más de 200 líneas de boilerplate.
- **Dismantling de la God Class (JavaFX)**: Segmentada `TablaIncidencias.java` en celdas independientes y gestores de diálogos.
- **Lógica de Multipresupuesto (1:N)**: Finalizada la persistencia de ofertas y la selección de presupuesto con inyección de notas en el trabajo.
- **Deadlock de BD Solucionado**: Refactorizado `ConexionDB` a **ThreadLocal<Connection>** para evitar colisiones entre hilos del servidor.

### Estado Final de la Sesión
- El servidor es ahora robusto y hilos-seguro.
- El Dashboard ya no se queda "congelado" tras ráfagas de mensajes (solucionado con el Lector Avaro).
- Inyectada lógica de seguridad en `ClienteSocket.enviar()` mediante el keyword `synchronized`.

---

## 🔧 SESIÓN 08/04/2026 (Tarde) — Gran Refactorización de Conexiones y Multipresupuesto
### Objetivo
Finalizar la lógica de multipresupuesto (1:N) y resolver bloqueos críticos en el servidor y el dashboard que impedían el envío de ofertas.

### Problemas Detectados y Soluciones Quirúrgicas
1. **Bloqueo del Servidor (Deadlock de BD):** 
   - **Causa:** `ConexionDB` usaba una conexión estática compartida. Al realizar múltiples consultas rápidas (refresco de trabajos + GET_EMPRESA + LISTAR_PRESUPUESTOS), los hilos se pisaban y bloqueaban el socket.
   - **Solución:** Refactorizado `ConexionDB` a un patrón **ThreadLocal<Connection>**. Ahora cada hilo del servidor tiene su propia conexión aislada. Se añadió `ConexionDB.cerrarConexion()` en el bloque `finally` de `GestorConexion` para limpieza total tras cada mensaje.

2. **Corrupción de Mensajes en Socket (Dashboard):**
   - **Causa:** `ClienteSocket.enviar()` no estaba sincronizado. Si el dashboard pedía refrescar datos mientras se enviaba un presupuesto, los JSON se mezclaban en el buffer, enviando basura al servidor.
   - **Solución:** Se marcó el método `enviar` como **synchronized**. Se añadieron logs de salida en consola para trazabilidad.

3. **Mismatch de Campos (notas vs nuevaDescripcion):**
   - **Causa:** El servidor esperaba la clave `"notas"` pero el cliente enviaba `"nuevaDescripcion"`.
   - **Solución:** Estandarización total al nombre de campo **`notas`** en el protocolo `CREAR_PRESUPUESTO`.

4. **Fallo Estructural de Base de Datos:**
   - **Causa:** La tabla `presupuesto` no tenía físicamente la columna `notas`.
   - **Solución:** Ejecutado `ALTER TABLE presupuesto ADD COLUMN notas TEXT;`. Actualizado `ESQUEMA_BD.sql` para persistencia.

5. **Lógica de Multipresupuesto (1:N):**
   - Implementado en `PresupuestoServiceImpl.aceptarPresupuesto`:
     - El presupuesto elegido pasa a `ACEPTADO`. El resto del mismo trabajo pasan a `RECHAZADO`.
     - Las notas del presupuesto aceptado se **inyectan** (con formato decorativo) al final de la descripción del trabajo.
     - El trabajo pasa a estado `ACEPTADO`.

### Estado Actual: LISTO PARA VALIDACIÓN
- El servidor es ahora robusto y hilos-seguro.
- El Dashboard ya no se queda "congelado" tras ráfagas de mensajes.
- Se ha verificado que `gerente.a@levante.com` está vinculado correctamente a la Empresa ID:2 en este entorno.

---

### 🏁 FINAL DE SESIÓN — DESCUBRIMIENTO CRÍTICO Y LECTOR AVARO
- **El Bloqueo (Deadlock TCP):** Tras añadir `List<Presupuesto>` y textos grandes (`notas`), la petición `LISTAR_TRABAJOS` mutó de pesar 5 KB a varios Megabytes. Al inyectar un JSON colosal por sockets con `salida.flush()`, si el Dashboard (JavaFX) tiene su hilo de red colapsado pintando interfaces, el Buffer TCP subyacente de Windows (~64KB) se atasca. El método `flush()` del Servidor se congela esperando a que el cliente libere la tubería, deteniendo la escucha global.
- **La Solución Próxima:** En la siguiente sesión vamos a implementar arquitectónicamente el **"Hilo Lector Avaro"** en el Dashboard y aislar las escrituras con un Pool de Ejecución (Asincronía) para evitar que el Servidor se quede colgado.

---

## ANEXO TECNICO PARA LA MEMORIA Y DEFENSA: HILOS, CONCURRENCIA Y EL "SINDROME DEL EMBUDO TCP"

*(Copia y pega o adapta estos conceptos para la sección de "Problemas Encontrados y Soluciones" de tu memoria académica, o úsalos en las preguntas del Tribunal).*

### ¿Por qué falló el Socket justo después de implementar el Multipresupuesto? (Causa Raíz)
Es lógico pensar que el problema de conexiones simultáneas no tiene relación directa con el Multipresupuesto, ya que el Dashboard lanzaba ráfagas desde antes. Lo que detonó el fallo tras el refactor fue **el tamaño exponencial del Payload JSON (Efecto Multiplicador en Red).**

1. **Antes del Multipresupuesto:** La petición `LISTAR_TRABAJOS` devolvía un listado plano. Sin arrays de presupuestos vinculados. El JSON era extremadamente ligero (unos pocos Kilobytes). Al ejecutarse `salida.flush()` en Java, ese diminuto JSON se engullía al instante por el *Buffer de Red TCP* del sistema operativo. El método terminaba en 1 milisegundo, y el hilo del servidor quedaba libre enseguida para el próximo bucle de lectura.
2. **Después del Multipresupuesto:** Para soportar que la App compare ofertas in-situ, tuvimos que "hidratar" cada objeto `Trabajo` con su colección lista de `List<Presupuesto>`. Peor aún, añadimos el campo de texto libre `notas` (la descripción técnica extensa). 
3. **La Explosión de Bytes:** Si un `LISTAR_TRABAJOS` trae 50 incidencias, y la mitad tiene 2 o 3 ofertas de diferentes empresas, cada una con un texto de 500 caracteres, un DTO en JSON puro pasa de pesar 5 KB a pesar varios Megabytes.
4. **El Bloqueo (TCP Window Full):** Al forzar un envío titánico por sockets (`salida.write` seguido de `flush()`), si el Dashboard (receptor) no está leyendo ese texto kilométrico sin interrupción y a suma velocidad, el Buffer subyacente TCP se empantana. Al llenarse, `flush()` sufre un bloqueo intrínseco de red. Se queda congelado ("Atascamiento") aguardando que el cliente vacíe milímetros de cauce. Al colgar el hilo con esta retención, el Servidor detiene toda monitorización de nuevas conexiones hasta purgar los Bytes de salida.

### Glosario de Concurrencia (¿Dónde usamos Multithreading en el proyecto?)
Este MVP de FixFinder destaca brutalmente de cara a tu tribunal por el uso nativo de sockets y la abstracción asíncrona robusta. Anota la enumeración explícita para la Memoria:

**EN EL SERVIDOR (Backend Java 21 - Non Spring):**
1. **El Hilo de Escucha (Dispatcher Listener):** Integrado en `ServidorCentral`. Su encasillamiento lógico es orbitar un bucle infinito sobre la operación bloqueante de IO `serverSocket.accept()`. Nace y muere esperando aperturas del Socket, con derivación reactiva al instante a un nuevo hilo Worker.
2. **Los Workers Concurrentes (`GestorConexion`):** Un pool/hilos instanciados individualmente. Al conectarse un gerente en PC, o cinco dispositivos Flutter, el Backend los abastece mediante cinco hilos vivos (Workers) que interpretan simultáneamente (Runnable) los JSON recibidos del canal InputStream.
3. **Aislamiento Semántico de BBDD (`ConexionDB` / `ThreadLocal`):** Mitigante de *Deadlocks*. Puesto que decenas de workers atacan a MySQL transaccionalmente, rehusamos inyectarles una única clase Java Connection estática (crasheos y traslapes JDBC). Se instrumentó `ThreadLocal<Connection>`, forzando así un "Scope" privado y un cauce SQL ajeno por cada hilo en transcurso.

**EN EL DASHBOARD (Cliente JavaFX - Arquitectura MVC):**
4. **Aislamiento del Hilo Gráfico (Platform Thread UI):** Todo SDK de Renderizado (sea SWING, FX, o QT) prohíbe intrusión forzada del CPU o llamadas de red prolongadas ("Freeze Frame"). Todas tus comunicaciones `solicitar...` subrogan a `CompletableFuture` (Future/Promesa de sub-hilo). Los desenlaces visuales terminan derivándose de nuevo a la UI únicamente envolviéndolos en `Platform.runLater(() -> {})`. 
5. **Semáforos Transversos:** Hemos programado sub-hilos "demonio" recurrentes atados con `sleep(ms)` intermedios que lanzan ping transparentes en las pantallas iniciales, detectando los virajes del entorno AWS a LOCAL.
6. **Cruce Estructural de Output `enviar()`:** Subsanado con `synchronized()`. Se obligan secuencias unitarias ante la coincidencia del hilo de UI y los hilos en backgound pretendiendo "incrustar" peticiones por el canal `DataOutputStream` idéntico. Un patrón Mutex elemental en Sockets.

**EN LA APP MÓVIL (Flutter / Dart):**
7. Dart maneja una filosofía asimétrica: usa **Isolates** y el Event Loop de asincronía (`async / await`). Opera virtualmente sobre Single-Thread sin necesidad empedernida de crear sub-hilos de sistema operativo para descargar Sockets, gestionándolo nativamente con multiplexación I/O no bloqueante, maximizando la eficiencia de batería e interfaz Android sin "saltos" (Jank/stutters).

### La Escalabilidad de Nuestra Solución (y por qué evitaremos que vuelva a pasar)
Es completamente lícito preguntarse: *¿Si el sistema peta ahora mismo con apenas unas peticiones de prueba, se colapsará el día de mañana si tenemos cientos de presupuestos reales?*

La respuesta es NO, porque no peta por "falta de potencia", sino por "falta de coordinación de tuberías".
Nuestra solución implementa un **"Hilo Lector Avaro"** combinado con Promesas Asíncronas (TxID). Esto separa radicalmente el acto de "Ingerir" datos de la red del acto de "Renderizar" la interfaz. 

Con esto, no importa cuán colosal sea el JSON resultante del Multipresupuesto: el Hilo Lector Avaro desvía los Bytes desde el buffer TCP de tu sistema operativo a la memoria de tu programa en mili-segundos, despejando la tubería (ventana TCP) de inmediato. El Servidor jamás detecta atascos y su `salida.flush()` empuja los datos sin trabas. Y si en el futuro se hablara de millones de registros, el proyecto está preparado estructuralmente para aplicar **Paginación** (ej: pedir 20 registros, que son instantáneos, y cargar más haciendo scroll).

---

## ✅ SESIÓN 15/04/2026 — Implementación de Perfil, Refactor de Red y Suite de Tests

### 🚀 Logros Técnicos de la Sesión

#### 1. Gestión de Perfil de Usuario (End-to-End)
- **Backend (Java)**: Implementada la acción `MODIFICAR_USUARIO` en `ProcesadorUsuarios.java` para persistir cambios en nombre, email, teléfono y dirección.
- **Frontend (Flutter)**: 
  - Habilitada la edición de datos personales en `perfil_pantalla.dart`.
  - Integración en `AuthService` mediante el método `actualizarPerfil()`, garantizando la sincronización con el servidor y la persistencia local en `SharedPreferences`.

#### 2. Refinamiento del Protocolo de Red
- **SocketService**: Se ha estandarizado el uso del método `request()` con sistema de **Ticket IDs** (`id_peticion`). Esto asegura que las respuestas asíncronas del servidor Java se vinculen correctamente con su petición original en Dart, eliminando colisiones de datos.

#### 3. Auditoría de Calidad y Documentación
- **Documentación Didáctica**: Se ha realizado un repaso exhaustivo a las **27 clases Dart** del proyecto. Se han añadido docstrings y comentarios técnicos detallados en Screens, Models, Providers y Services, dejando el código "Defense-Ready" para la presentación académica.
- **Limpieza del Diario**: Se ha realizado una limpieza quirúrgica de `DIARIO_DE_ABORDO.md`, corrigiendo artefactos de codificación UTF-8, consolidando encabezados duplicados y reparando el flujo cronológico.

#### 4. Suite de Pruebas y Validación (19 Archivos)
Se ha establecido una infraestructura de pruebas para garantizar la estabilidad antes del commit final:
- **Java (9 archivos)**: Tests de integración para la lógica 1:N de presupuestos (`MultiPresupuestoIntegracionTest`) y pruebas de carga/concurrencia.
- **Flutter (10 archivos)**: 
  - Tests funcionales de UI/Widgets (`crear_trabajo_test`, `galeria_fotos_test`, `tarjeta_contacto_test`).
  - Preparación de esqueletos (stubs) para la lógica de `AuthService`, `TrabajoProvider`, `Finalización de Trabajo`, `Valoración` y `Broadcaster`.

### 🏁 Estado Actual

---

## ✅ SESIÓN 20/04/2026 — Identidad Corporativa y Optimización UI

### Lo que se hizo en esta sesión

#### 💎 Dashboard: Identidad Corporativa Premium
- **Rediseño de VistaEmpresa**: Se ha sustituido el bloque independiente del gerente por una cabecera premium que integra:
    - **Logo Corporativo**: Tamaño 90x90 con carga asíncrona (`backgroundLoading = true`) y binding de progreso para evitar bloqueos de la interfaz de usuario.
    - **Información Unificada**: Nombre de la empresa destacado y responsable justo debajo, eliminando redundancias visuales y mejorando la jerarquía de información.
- **Limpieza de Emojis**: Se han corregido glifos invisibles y variantes de selector en el botón de edición ("⚙") que causaban errores de visualización en ciertos sistemas (símbolos rotos).
- **Estandarización de Botones**: Implementación y aplicación de nuevos estilos CSS (`.btn-transparente-naranja`, `.btn-solo-icono`, `.btn-secundario`) para asegurar una estética coordinada, botones circulares para cámaras y feedback visual en `hover`.

#### ⚡ Sincronización y Robustez de Red
- **Refresco Automático de Operarios**: Corregido el bug donde los nuevos operarios no aparecían tras el registro exitoso. Se ha actualizado el `ManejadorRespuestas` para disparar una sincronización total (`sincronizarTodo`) al recibir confirmación de acciones críticas: `REGISTRO`, `MODIFICAR_EMPRESA` y `MODIFICAR_USUARIO`.
- **Estandarización de Protocolo**: Validación de la interpretación de respuestas basada en códigos de estado tipo HTTP (`status < 400`), permitiendo una gestión de éxito genérica y robusta en el flujo de Sockets.

#### 🎨 Refinamiento Visual
- **Optimización de Estados**: El texto del estado "Disponible" se ha ajustado a un tono gris tenue (`-ff-tenue`) para evitar la fatiga visual, manteniendo el punto verde como indicador principal de actividad.
- **Consistencia en Diálogos**: El botón "Cancelar" en la configuración de la empresa ahora utiliza correctamente la clase `.btn-secundario`, eliminando estilos nativos del SO.

---

# 🚀 PLAN_EVOLUCION_APP: Hoja de Ruta FixFinder

> **Archivo de sesión:** Este documento sirve como memoria de trabajo entre sesiones de desarrollo.
> Si un chat se pierde o se reinicia, leer este documento primero para recuperar el contexto completo.
> **⚠️ Nota:** La carpeta `DOCS/` **sí se sube a Git** pero el repositorio de GitHub debe mantenerse **privado** para que la Memoria del proyecto no sea pública.


---

## 📋 ESTADO ACTUAL DEL SISTEMA (07/03/2026)

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
    - **⚠️ El simulador `SimuladorController.java` usa también el protocolo de 4 bytes (ya actualizado)**
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

> ⚠️ IMPORTANTE: Los tests de JUnit (`ServiceTest`) generan usuarios temporales en la BD y pueden dejar telefono=NULL en usuarios existentes. Después de correr tests, ejecutar:
>
> ```sql
> UPDATE usuario SET telefono = '600123456' WHERE rol = 'CLIENTE' OR rol = 'OPERARIO';
> ```

---

## 📦 ESTADO DE CADA MÓDULO DEL BACKEND

### `TrabajoService` / `TrabajoServiceImpl`

Métodos implementados y funcionales:

- `solicitarReparacion(idCliente, titulo, categoria, descripcion, direccion, urgencia)` — Crea trabajo PENDIENTE
  - **⚠️ Nuevo (01/03):** Si `direccion` viene vacío, usa `cliente.getDireccion()` como fallback. Si tampoco tiene, pone "Sin dirección especificada"
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

> ⚠️ IMPORTANTE sobre `procesarValorarTrabajo`: El mensaje de éxito en el JSON de respuesta es
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

> ⚠️ **Bug corregido (01/03):** Antes el campo `direccion` del trabajo NO estaba en la respuesta LISTAR_TRABAJOS.
> Flutter caía en `json['direccionCliente']` y siempre mostraba la dirección del cliente, ignorando la dirección
> real de la incidencia. Ahora se incluye `map.put("direccion", t.getDireccion())` explícitamente.

---

## 📱 ESTADO DE CADA MÓDULO DE LA APP FLUTTER

### Estructura de carpetas

````
lib/
├── main.dart                          ← Entrada, providers, rutas, tema
├── models/
│   ├── trabajo.dart                   ← Modelo Trabajo + enums EstadoTrabajo, CategoriaServicio
│   ├── usuario.dart                   ← Modelo Usuario + enum Rol
│   ├── presupuesto.dart               ← Modelo Presupuesto
│   └── empresa.dart                   ← Modelo Empresa colaboradora
├── providers/
│   └── trabajo_provider.dart          ← State management para trabajos
├── services/
│   ├── socket_service.dart            ← Comunicación TCP con servidor Java (protocolo 4 bytes)
│   └── auth_service.dart             ← Login, logout, persistencia token en SharedPreferences
├── screens/
│   ├── login_pantalla.dart
│   ├── dashboard_pantalla.dart
│   ├── detalle_trabajo_pantalla.dart  ← REFACTORIZADA: delega en widgets separados
│   ├── crear_trabajo_pantalla.dart    ← Crear y Modificar (modo dual)
│   └── perfil_pantalla.dart
└── widgets/
    ├── comunes/
    │   ├── dato_fila.dart
    │   └── estado_badge.dart
    ├── trabajos/
    │   ├── tarjeta_trabajo.dart
    │   ├── tarjeta_contacto.dart
    │   └── galeria_fotos.dart         ← Preparada para URLs Firebase (Nuevo 08/03)
    └── detalle_trabajo/
        ├── detalle_info_card.dart
        ├── detalle_resumen_final.dart
        ├── detalle_seccion_presupuestos.dart
        └── dialogos_trabajo.dart      ← Todos los AlertDialogs (borrar, finalizar, valorar)
```

---

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

## 🧪 PROTOCOLO DE PRUEBAS PARA LA PRÓXIMA SESIÓN

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

> ⚠️ TRUCO DEL COMPLETER para `modificar/valorar`: Los Completers filtran por palabras clave del `mensaje`
> de respuesta (NO por `status == 200`) para no capturar por accidente la respuesta de LISTAR que también
> devuelve 200 y llega de forma asíncrona.

### Pantallas — Comportamiento de Navegación (ACTUALIZADO 01/03)

**Patrón estándar para todas las acciones:**

1. La acción (finalizar, valorar, aceptar, borrar) llama al provider y espera el resultado.
2. Si `exito == true`, se hace **`Navigator.pop(context)`** simple (NO `popUntil`).
3. El dashboard tiene `.then((_) async { await Future.delayed(900ms); obtenerTrabajos(); })` en el `onTap`.
4. El delay de 900ms permite al servidor confirmar el cambio en BD antes de re-listar.

> ⚠️ **CAUSA DE CRASH HISTÓRICO:** Usando `popUntil(ModalRoute.withName('/dashboard'))` con rutas
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

## 🔧 SESIÓN 01/03/2026 — Cambios Detallados

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

## 🎯 PRÓXIMAS FASES

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

## 🔧 SESIÓN 07/03/2026 — Refinado Gerencial y Comunicación

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

- **Sección de Reseñas:** Implementada una lista visual que muestra las últimas valoraciones de los clientes con estrellas (⭐).
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

## 🎯 PRÓXIMAS FASES

- [ ] Memoria técnica (arquitectura, decisiones de diseño, protocolo de comunicación)
- [ ] Diagrama de clases, diagrama de secuencia del flujo completo
- [ ] Presentación + ensayo

---

## 🛠️ COMANDOS DE REFERENCIA RÁPIDA

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
r   ← hot reload
R   ← hot restart (limpia estado)

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

Se realizaron estas modificaciones que YA ESTÁN en el commit `ec6f1d3`:

#### `ProcesadorTrabajos.java` — Refactorización parcial aplicada

- Método `mapearTrabajo(Trabajo t)` extraído como privado: centraliza la conversión de objeto Trabajo a `Map<String, Object>`. Antes se repetía inline en cada bloque del listado.
- Método `filtrarParaGerente(int idUsuario)` extraído como privado: encapsula la lógica de qué trabajos ve un gerente (PENDIENTE + PRESUPUESTADO + los de su empresa).
- **⚠️ ATENCIÓN:** La refactorización introdujo errores de compilación que se resolvieron durante la sesión. Los imports correctos son `com.fixfinder.modelos.enums.EstadoTrabajo` y `com.fixfinder.modelos.enums.EstadoPresupuesto`. La firma del servicio de cancelar es `cancelarTrabajo(Integer, String)` → siempre pasar motivo.

#### `DashboardPrincipalController.java`

- El método `solicitarTrabajos()` ahora también llama a `servicioCliente.enviarGetEmpresa(idEmpresa)` para refrescar los datos de la empresa sin necesidad de re-login.

#### `VistaDashboard.java`

- Animación añadida al botón `btnRefresh`: `RotateTransition` (360°, 0.5s) + `ScaleTransition` (1→0.85→1, 0.5s) en paralelo mediante `ParallelTransition`. Se ejecuta cada vez que se pulsa el botón.

#### `socket_service.dart` (Flutter)

- Añadido método `request(Map peticion, {String? accionEsperada, int timeoutSegundos})`: encapsula el patrón Completer + listen + timeout + cancel en un solo método reutilizable. Preparado para limpiar `TrabajoProvider`.

---

### 🔍 Auditoría Completa de Calidad — Resultados

#### BACKEND JAVA

| Clase                                         | Tamaño    | Diagnóstico                                                                                                                                                                                                                   | Severidad   |
| --------------------------------------------- | --------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------- |
| `ProcesadorTrabajos.java`                     | ~280L     | ✅ Refactorizado. Mapeo único, filtrado encapsulado.                                                                                                                                                                          | ✅ Resuelto |
| `ProcesadorAutenticacion.java`                | 233L      | ⚠️ Método `procesarRegistro` mezcla 3 flujos (CLIENTE, OPERARIO, EMPRESA) en uno. Difícil de mantener.                                                                                                                        | ⚠️ Medio    |
| `ProcesadorUsuarios.java`                     | 209L      | ⚠️ Instancia DAOs directamente (`new EmpresaDAOImpl()`). Viola inversión de dependencias. La lógica de valoraciones de empresa (50L) debería estar en el Service, no en el Procesador.                                        | ⚠️ Medio    |
| `TrabajoServiceImpl.java`                     | 337L      | ℹ️ `historialOperario` carga TODOS los trabajos y filtra en Java (no en SQL). Con muchos datos puede ser lento. La lógica de "parsear descripción por emojis" en `finalizarTrabajo` es frágil si alguien cambia la plantilla. | ℹ️ Bajo     |
| `TrabajoDAOImpl.java`                         | 371L      | 🔴 **N+1 Problem:** El método `cargarRelaciones` abre una nueva conexión SQL por cada trabajo de la lista para cargar cliente + operario + fotos. En 50 trabajos = 150 queries. Solución: JOIN en la query principal.         | 🔴 Alto     |
| `GestorConexion.java`                         | 238L      | ✅ Bien diseñado. Delega. No tocar.                                                                                                                                                                                           | ✅ OK       |
| `ServidorCentral.java`                        | 110L      | ✅ Limpio. Semáforo de 10 conexiones.                                                                                                                                                                                         | ✅ OK       |
| `OperarioDAOImpl.java`, `EmpresaDAOImpl.java` | ~11KB c/u | ✅ Aceptables. Sin duplicación visible.                                                                                                                                                                                       | ✅ OK       |

#### DASHBOARD JAVAFX

| Clase                                 | Tamaño      | Diagnóstico                                                                                                                                        | Severidad |
| ------------------------------------- | ----------- | -------------------------------------------------------------------------------------------------------------------------------------------------- | --------- |
| `TablaIncidencias.java`               | 422L / 17KB | 🔴 **GOD CLASS:** Controla tabla, 8 tipos de celdas, 3 diálogos de acción, filtros, tabs Y decoración de iconos. Si falla un método afecta a todo. | 🔴 Alto   |
| `DashboardPrincipalController.java`   | ~331L       | ⚠️ Switch `procesarRespuesta` con ~20 casos. Funciona, pero en el límite de lo mantenible.                                                         | ⚠️ Medio  |
| `VistaDashboard.java`, `Sidebar.java` | <200L c/u   | ✅ Limpias.                                                                                                                                        | ✅ OK     |
| `TrabajoFX.java`, `OperarioFX.java`   | ~130L c/u   | ✅ JavaFX Properties correctas.                                                                                                                    | ✅ OK     |
| `DialogoNuevoOperario.java`           | 6KB         | ℹ️ Grande pero cohesivo.                                                                                                                           | ℹ️ Bajo   |

#### APP FLUTTER

| Clase                           | Tamaño      | Diagnóstico                                                                                                                                                                                                                                      | Severidad |
| ------------------------------- | ----------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | --------- |
| `trabajo_provider.dart`         | 365L / 11KB | 🔴 **Boilerplate masivo:** Cada uno de los 8 métodos replica exactamente el mismo patrón `Completer + listen + send + timeout + cancel`. ~200L son código idéntico. El método `request()` ya existe en `socket_service.dart` para resolver esto. | 🔴 Alto   |
| `dashboard_pantalla.dart`       | 228L        | ⚠️ Tiene lógica de negocio mezclada con UI: `_tieneAccionPendiente()` y `_obtenerIconoCategoria()` deberían estar en el modelo o en un helper.                                                                                                   | ⚠️ Medio  |
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
   - Estandarización de imports en la cabecera de todas las clases (`ProcesadorUsuarios`, `VistaDashboard`, `Dialogos`, etc.).
   - Corrección de errores de sintaxis y balanceo de llaves en `Sidebar.java`.

2. **Optimización de Rendimiento (Dashboard JavaFX)**:
   - **Carga Asíncrona de Imágenes**: En `VistaOperarios.java`, las fotos de perfil ahora se cargan en segundo plano (`backgroundLoading=true`), eliminando las congelaciones de la UI al navegar.
   - **Placeholders de Iniciales**: Implementado sistema de avatares con iniciales y colores de fondo que se muestran instantáneamente mientras la foto real se descarga.

3. **Mejora de UI Premium**:
   - **Panel de Valoraciones**: Rediseñado el sistema de estrellas en `DialogoDetalleIncidencia.java`. 
   - Corregido el escalado desigual de las estrellas y la lógica de activación (ahora se iluminan de izquierda a derecha correctamente).
   - Estética unificada con el panel de Empresa (colores `#FBBF24` vs `#334155`).

---

---

## 🚀 FASE 4: DESPLIEGUE REAL EN AWS (FREE TIER)

### 1. Seguridad y Control de Gastos (COMPLETADO)
- **Alertas de Capa Gratuita:** Activadas en la consola de AWS para recibir avisos por email en el correo de la cuenta. ✅
- **Presupuesto de Seguridad:** Creado presupuesto mensual de **1.00$** con alertas al **80%** de consumo para evitar sorpresas. ✅

### 2. Infraestructura de Datos (Pendiente)
- **AWS RDS (MySQL):** Crear una instancia `db.t3.micro` de MySQL para alojar los datos de forma persistente y profesional.

### 3. Servidor de Aplicaciones (Pendiente)
- **AWS EC2:** Lanzar una instancia `t3.micro` con Ubuntu Server.
- **Entorno:** Configurar Docker y Java para correr el Socket Server.
- **Firewall (Security Groups):** Apertura de los puertos necesarios (5000 para el servidor, 3306 para la BD).

### 4. Conectividad y Salto a Producción
- **Ajustes de Código:** Cambiar las IPs locales por el Endpoint de RDS (en el servidor) y la IP elástica de EC2 (en la App y Dashboard).
- **Validación:** Desplegar y probar la comunicación real entre App (móvil físico) -> EC2 -> RDS.

---

## 🧪 PROTOCOLO PARA LA PRÓXIMA SESIÓN

1. **Paso de Local a Red:** Cambiar IP en `socket_service.dart` a `192.168.0.13` y probar con el móvil físico conectado al mismo Wi-Fi.
2. **Preparación AWS:** Crear la instancia EC2 y configurar el entorno Docker/Java.
3. **Validación Final:** Probar que todas las fotos cargan correctamente desde URLs de Firebase tanto en el Dashboard como en la App móvil operando fuera del emulador.

---

## 🚀 MEJORAS DE ARQUITECTURA (PENDIENTES)

- [ ] **Implementar Escucha Directa (Push Notifications via Sockets):** Actualmente, algunos componentes requieren refresco manual o polling. Aprovechando que ya existe un servidor de sockets persistente, se debe implementar un sistema donde el servidor "empuje" las actualizaciones (`PUSH_UPDATE`) a los clientes interesados (App y Dashboard) inmediatamente cuando ocurra un cambio en la BD (ej: nuevo trabajo, cambio de estado, nuevo mensaje), eliminando la necesidad de actualizar manualmente.


## 📝 PRÓXIMOS PASOS (SESIÓN SIGUIENTE)

### 🧪 Fase A: Testing de Registros y Fotos (Finalización)
1.  **Commit de Seguridad:** Confirmar todos los cambios actuales de registros y fotos en Git.
2.  **Nueva Rama Git:** Crear rama `deploy/aws-production` para separar el trabajo de despliegue.
3.  **Testing Final Registro:** Probar registro de Clientes (Flutter) y Empresas/Operarios (JavaFX) con subida real a Firebase Storage.
4.  **Revisión Documentación:** Validar los diagramas de la carpeta `DOCS/` contra el código final.

### 🌩️ Fase B: Despliegue AWS (Producción)
1.  **Levantar RDS:** Crear la base de datos MySQL en Amazon.
2.  **Migración de Esquema:** Ejecutar scripts de creación de tablas en RDS.
3.  **Lanzar EC2:** Configurar el servidor de aplicaciones con Docker/Java.
4.  **Ajuste de IPs:** Actualizar las constantes de conexión en todo el proyecto.

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

## 🎯 PRÓXIMOS PASOS (SIGUIENTE SESIÓN)

### Prioridad 1 — Bugfix pendiente
- [ ] **Foto de perfil del cliente en la ficha del Dashboard** no se visualiza. Investigar `DialogoFichaCliente.java` y la carga de imagen desde URL de Firebase.

### Prioridad 2 — Despliegue AWS (Producción)
- [ ] **Levantar RDS MySQL:** Crear instancia `db.t3.micro`, configurar Security Group (puerto 3306 solo desde EC2).
- [ ] **Migrar esquema:** ejecutar `SCHEMA.sql` en RDS para crear todas las tablas.
- [ ] **Lanzar EC2:** Instancia `t3.micro` Ubuntu, instalar Docker + Java 21.
- [ ] **Dockerizar el servidor:** Crear `Dockerfile` para el servidor Java socket y hacer `docker build + run` en EC2.
- [ ] **Ajustar IPs en el código:**
  - `socket_service.dart` (Flutter) → IP elástica de EC2.
  - `ClienteSocket.java` (Dashboard) → IP elástica de EC2.
  - `application.properties` o config del servidor → endpoint RDS.
- [ ] **Validación final:** Probar App móvil en dispositivo físico real → EC2 → RDS.

### Prioridad 3 — Memoria académica
- [ ] Redactar las secciones de texto de `DOCS/MEMORIA.md` (campos `[Escribe aquí...]`).
- [ ] Insertar capturas de pantalla reales de la app y dashboard en la sección 5.4.
- [ ] Completar tabla de requerimientos funcionales/no funcionales (sección 3.1).
- [ ] Completar tabla de hitos del proyecto (sección 3.2).
- [ ] Añadir diagrama de Gantt.
- [ ] Exportar a PDF cuando esté lista.


Plantilla memoria: (ignora el valenciano debe ser en castellano)
IES Maria Enriquez Curs 2025-26
Guia del modul de Projecte Intermodular de 2DAM
Guia del modul de Projecte Intermodular de 2DAM...........................................................................1
Baremació del Projecte.................................................................................................................... 1
Avaluació:........................................................................................................................................ 1
Dates orientatives:............................................................................................................................2
Lliurament, exposició i defensa....................................................................................................... 2
Memòria...........................................................................................................................................2
Format de la memòria.................................................................................................................2
Continguts................................................................................................................................... 3
Baremació del Projecte
El projecte s’avaluara d’acord amb els seguents percentatges:
Mòdul – Part %
Memòria 15 %
Presentació 15 %
Accès a dades 14 %
DI 17 %
PMDM 13 %
PSP 7 %
SGE 13 %
Digitalització 3 %
Sostenibilitat 3 %
Avaluació:
• El professor de cada mòdul avaluarà els continguts i detalls tècnics dels seus respectius 
mòduls.
• Cada mòdul o part s’ha de superar almenys amb un 5.
• Els alumnes han de superar tots els RA, per tant si algun mòdul no es supera el mòdul de projecte estaria suspès.

---

## 🔧 SESIÓN 01/04/2026 — Limpieza de Arquitectura y Restauración de App

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

## 📋 HOJA DE RUTA CONSOLIDADA (ACTUALIZADO 02/04 - v1.2)

Este apartado detalla las tareas críticas para recuperar el progreso perdido y saltar a la arquitectura de tiempo real.

### 🔴 BLOQUE A: RESTAURACIÓN INTEGRAL (Prioridad Máxima)
*   **[ ] Recuperar Registro Multinivel:**
    *   **App Móvil:** Restaurar botón "Crear cuenta" en Login y vincular a `registro_pantalla.dart`.
    *   **PC Gerente:** Re-implementar el diálogo de "Registrar Nueva Empresa" (se usará la lógica existente en `ProcesadorAutenticacion.java`).
    *   **Ajuste de Protocolo:** Cambiar la acción del cliente de `REGISTRO_USUARIO` a `REGISTRO` para que sea 1:1 con el servidor.
*   **[ ] Edición de Perfil en App:**
    *   Implementar campos editables en `perfil_pantalla.dart` (Teléfono, Dirección, DNI).
    *   Crear la acción `ACTUALIZAR_DATOS_USUARIO` en el servidor (Java).

### 🟡 BLOQUE B: ARQUITECTURA DE TIEMPO REAL (PUSH)
*   **[ ] Notificaciones Instantáneas:**
    *   Sustituir el sistema de "actualización manual" (botones de refresco) y polling por eventos `PUSH_UPDATE` enviados por el servidor.
    *   **Lógica:** Cuando el operario finalice un trabajo, el servidor enviará un mensaje al socket del cliente afectado al instante. Esto justifica el uso de Sockets persistentes frente a una API REST.

### 🔵 BLOQUE C: SIMPLIFICACIÓN DE INTERFAZ (PC)
*   **[ ] Limpieza en Gestión de Operarios:**
    *   Eliminar la opción de "Subir foto" del diálogo de edición de operario. 
    *   **Razón:** Se mantiene la subida directa desde el botón de la tabla del panel principal para simplificar el flujo y evitar duplicidad de lógica. ✅

### 🟡 BLOQUE D: CORRECCIONES TÉCNICAS (GHOST BUGS v1.2)
*   **[ ] Corregir Respuestas del Servidor:**
    *   **Problema:** `VALORAR_TRABAJO` y `CANCELAR_TRABAJO` solo responden con `status: 200` pero sin `mensaje`.
    *   **Síntoma:** La App móvil (que usa Completers) se queda en estado de carga infinito hasta dar Timeout.
    *   **Acción:** Añadir `respuesta.put("mensaje", "...")` en `ProcesadorTrabajos.java` para que la App reciba las palabras clave necesarias.
*   **[ ] Ajuste Visual de Tabla:**
    *   Ampliar el ancho mínimo de la columna **Estado** en `TablaIncidencias.java` de 135 a 155 para mejorar la legibilidad de estados largos.

### 🌩️ ESTADO DEL DESPLIEGUE AWS
*   *Gestionado de forma independiente en rama de producción. Sin cambios requeridos en esta rama de desarrollo.*

---

IES Maria Enriquez - Proyecto Intermodular 2DAM - 2026

---

IES Maria Enriquez - Proyecto Intermodular 2DAM - 2026

---
• Els tribunals estaran compostos per 3-5 membres de l’equip docent que impartisca els 
mòduls associats.
IES Maria Enriquez Curs 2025-26
Dates orientatives:
• Fins el 22 de maig: seguiment (el seguiment de cada mòdul el farà el professor que 
l’imparteix. Es recomana que els alumnes consulten amb els professors dels mòduls dels 
quals tenen dubtes).
• 18 – 22 maig: acumulació hores PIM
• 25 – 29 maig: simulacres
• 29 de maig: lliurament dels projectes.
• 1 – 5 juny: Presentacions
• 15 – 18 juny: recuperacions
• 19 juny?: avaluació final
Lliurament, exposició i defensa.
El 29 de maig es lliuraran els projectes. El lliurament consistirà en un arxiu zip, amb tot el codi dels 
projectes, així com recursos associats, i una memòria en PDF.
Es tindran en compte altres recursos en altres formats: documentació en línia, manual de la 
aplicació, repositoris de codi, etc.
El dia de la presentació es lliurarà una còpia impressa i enquadernada de la memòria.
Pel que fa a l’exposició, l'alumnat disposarà d’un màxim de 15 minuts. per a l’exposició del 
projecte i de 15 minuts per a la seua demostració. 
Finalitzada la presentació, començarà el torn de preguntes (defensa) per part del tribunal amb una 
durada màxima de 15 minuts.
L’exposició i la defensa tenen caràcter públic.
Memòria
La memòria haurà de complir uns requisits mínims, i per tant obligatoris, pel que fa al format i al 
contingut.
Format de la memòria
• Document PDF
• De 40 a 60 pàgines *.
• Sense faltes ortogràfiques ni gramaticals.
• Font Liberation Serif 12 pt.
• Interlineat 1,5.
• Numeració de pàgina
• Capçalera i peu de pàgina.
• Coherència en la grandària i la posició de les captures de pantalla.
• El codi font i/o les ordres tindrà una font diferent.
IES Maria Enriquez Curs 2025-26
(*) La memòria pot contenir annexos que no es tindran en compte en aquesta xifra.
Continguts
Els continguts de la memòria són els establerts en el mòdul de projecte intermodular:
## 1. Introducció
- Presentació (i/o motivació) i objectiu del projecte.
- Factor diferenciador del projecte
- Anàlisis de la situació de partida
- Objectius a aconseguir amb el projecte
- Relació amb els continguts dels diferents mòduls
## 2. Presentació de les diverses tecnologies que es poden utilitzar per a la seua realització
### 2.1 Justificació de l’elecció de les tecnologies.
## 3. Anàlisi del projecte
### 3.1. Requeriments funcionals i no funcionals
- Requeriments funcionals
- Requeriments no funcionals
- analisi de costs i viabilitat del projecte
### 3.2. Temporalització del projecte
- Fites del projecte
- Diagrama de Gantt
### 3.3. Casos d’ús
- Diagrama de casos d’ús
- Descripció dels casos d’ús
### 3.4. Diagrama de classes inicial
- Diagrama de classes
- Descripció de les classes
- Diagrama entitat-relació (si escau)
## 3.5. Wireframes d’interfícies
IES Maria Enriquez Curs 2025-26
### 3.6. Altres diagrames i descripcions (si escau)
## 4. Disseny del projecte
### 4.1. Arquitectura del sistema
- Descripció de l’arquitectura
- Diagrama de l’arquitectura
- Diagrama de desplegament
### 4.2. Diagrama de classes definitiu
- Diagrama de classes
- Descripció de les classes
- Diagrama entitat-relació (si escau)
### 4.3. Disseny de la interfície d’usuari
- Mockups
- Diagrama de navegació
### 4.4. Altres diagrames i descripcions (si escau)
## 5. Implementació del projecte
- Estructura del projecte
- Descripció dels mòduls i components principals
- Desplegament de l’aplicació
- Captures de pantalla i exemples de codi (el codi es recomana ficar-ho als annexes)
## 6. Estudi dels resultats obtinguts
- Avaluació del projecte respecte als objectius inicials
- Problemes trobats i solucions aplicades
- Futures millores i ampliacions
## 7. Conclusions
- Relacions amb els continguts dels diferents mòduls
- Valoració personal del projecte
## 8. Bibliografia i recursos utilitzats
IES Maria Enriquez Curs 2025-26
## 9. Annexes
- Codi font complet del projecte
- Guia d’instal·lació i ús
- Documentació addicional
- Altres materials rellevants
Aquests continguts i estructura són orientatius i s’adaptaran a cada projecte.
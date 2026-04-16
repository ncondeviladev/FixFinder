PLAN_EVOLUCION_APP: Hoja de Ruta FixFinder

> **Archivo de sesi├│n:** Este documento sirve como memoria de trabajo entre sesiones de desarrollo.
> Si un chat se pierde o se reinicia, leer este documento primero para recuperar el contexto completo.
> **├ó┼í┬á├»┬©┬Å Nota:** La carpeta `DOCS/` **s├¡ se sube a Git** pero el repositorio de GitHub debe mantenerse **privado** para que la Memoria del proyecto no sea p├║blica.


---

## ESTADO ACTUAL DEL SISTEMA (07/03/2026)

### Arquitectura General

- **Backend:** Servidor Java puro con Sockets TCP en puerto `5000`. Sin Spring Boot.
  - Punto de entrada del servidor: `com.fixfinder.red.ServidorCentral`
  - Arranque: `.\gradlew.bat runServer` desde `C:\Users\ncond\Desktop\FF\FIXFINDER`
  - Gesti├│n de conexiones: `GestorConexion.java` ÔåÆ tiene un switch con todas las acciones
  - Procesadores por entidad: `ProcesadorTrabajos.java`, `ProcesadorPresupuestos.java`, etc.
  - Tests: `.\gradlew.bat test` ÔÇö usa JUnit 5, clase principal `ServiceTest.java`
  - **Protocolo de Comunicaci├│n:** 4 bytes de cabecera (longitud del mensaje) + payload JSON en bytes.
    - Java: `DataOutputStream.writeInt(len)` + `write(bytes)` / `DataInputStream.readInt()` + `readFully(bytes)`
    - Flutter: `socket.add(4 bytes big-endian + payload)` / lee 4 bytes cabecera + N bytes datos
    - **├ó┼í┬á├»┬©┬Å El simulador `SimuladorController.java` usa tambi├®n el protocolo de 4 bytes (ya actualizado)**
- **Base de datos:** MySQL en Docker. Contenedor: `FixFinderDb`. Root pass: `root`.
  - DB name: `fixfinder`
  - Acceso: `docker exec -i FixFinderDb mysql -u root -proot fixfinder -e "SQL;"`
  - Resetear datos de prueba: `.\gradlew.bat runSeeder`
- **App M├│vil:** Flutter (Android). Carpeta: `C:\Users\ncond\Desktop\FF\fixfinder_app`
  - Arranque en emulador 1: `flutter run -d emulator-5554`
  - Arranque en emulador 2: `flutter run -d emulator-5556`
  - IP del servidor desde emulador: `10.0.2.2:5000`
  - Tests: `flutter test`
  - Estado del socket: singleton `SocketService`, reconecta autom├íticamente
- **App Escritorio (Windows/JavaFX):** `com.fixfinder.Launcher` ÔåÆ `AppEscritorio`
  - Arranque: `.\gradlew.bat runClient`
  - Para el panel maestro del Dashboard (tabla de trabajos) usar `.\gradlew.bat runDashboard` o acceder desde el men├║ de gerente/admin

### Usuarios de prueba en la BD (generados por `runSeeder`)

| Email                            | Contrase├▒a  | Rol      | Tlf       | Direcci├│n                    |
| -------------------------------- | ----------- | -------- | --------- | ---------------------------- |
| marta@gmail.com                  | password    | CLIENTE  | 600123456 | Calle Paz 5, 2├é┬║A, Valencia   |
| juan@gmail.com                   | password    | CLIENTE  | 600234567 | Av. del Puerto 120, Valencia |
| elena@gmail.com                  | password    | CLIENTE  | 600345678 | Calle X├átiva 22, Valencia    |
| gerente.a@levante.com            | password    | GERENTE  | 600123456 | Av. del Cid 45, Valencia     |
| (operarios generados por seeder) | password123 | OPERARIO | 666127582 | var├¡a seg├║n operario         |

> ├ó┼í┬á├»┬©┬Å IMPORTANTE: Los tests de JUnit (`ServiceTest`) generan usuarios temporales en la BD y pueden dejar telefono=NULL en usuarios existentes. Despu├®s de correr tests, ejecutar:
>
> ```sql
> UPDATE usuario SET telefono = '600123456' WHERE rol = 'CLIENTE' OR rol = 'OPERARIO';
> ```

---

## ESTADO DE CADA M├ôDULO DEL BACKEND

### `TrabajoService` / `TrabajoServiceImpl`

M├®todos implementados y funcionales:

- `solicitarReparacion(idCliente, titulo, categoria, descripcion, direccion, urgencia)` ÔÇö Crea trabajo PENDIENTE
  - **├ó┼í┬á├»┬©┬Å Nuevo (01/03):** Si `direccion` viene vac├¡o, usa `cliente.getDireccion()` como fallback. Si tampoco tiene, pone "Sin direcci├│n especificada"
- `cancelarTrabajo(idTrabajo, motivo)` ÔÇö Pasa a CANCELADO. Solo si NO est├í ASIGNADO ni FINALIZADO
- `modificarTrabajo(idTrabajo, titulo, descripcion, direccion, categoria, urgencia)` ÔÇö Solo si est├í PENDIENTE
- `finalizarTrabajo(idTrabajo, informe)` ÔÇö Pasa a REALIZADO. Concatena informe al final de la descripci├│n
- `valorarTrabajo(idTrabajo, valoracion, comentarioCliente)` ÔÇö Solo si FINALIZADO/REALIZADO/PAGADO. Valoraci├│n 1-5 estrellas.
- `listarPorCliente()`, `listarPorOperario()`, enriquecimiento de DTOs en `procesarListarTrabajos`

### `ProcesadorTrabajos`

Acciones que maneja el switch en `GestorConexion`:

| Acci├│n (String)       | M├®todo procesador            |
| --------------------- | ---------------------------- |
| `CREAR_TRABAJO`       | `procesarCrearTrabajo`       |
| `LISTAR_TRABAJOS`     | `procesarListarTrabajos`     |
| `FINALIZAR_TRABAJO`   | `procesarCambiarEstado`      |
| `CANCELAR_TRABAJO`    | `procesarCancelarTrabajo`    |
| `MODIFICAR_TRABAJO`   | `procesarModificarTrabajo`   |
| `VALORAR_TRABAJO`     | `procesarValorarTrabajo`     |
| `ACEPTAR_PRESUPUESTO` | `procesarAceptarPresupuesto` |
| `LISTAR_PRESUPUESTOS` | `procesarListarPresupuestos` |

> ├ó┼í┬á├»┬©┬Å IMPORTANTE sobre `procesarValorarTrabajo`: El mensaje de ├®xito en el JSON de respuesta es
> `"Valoracion guardada correctamente"` (SIN acento en la ├│). El Completer en Flutter filtra por esta cadena.

### `ProcesadorTrabajos.procesarListarTrabajos` ÔÇö Enriquecimiento del DTO

El JSON que env├¡a el servidor al listar incluye (adem├ís de campos b├ísicos):

- `id`, `titulo`, `descripcion`, `categoria`, `estado`, `fecha`
- **`direccion`** (String ÔÇö direcci├│n del trabajo. **Nuevo 01/03**: ya se incluye en la respuesta)
- `valoracion` (int 0-5), `comentarioCliente` (String o null), `fechaFinalizacion` (String ISO o null)
- `urls_fotos` (List<String>), `ubicacion` (objeto {lat, lon} o null)
- `cliente` (objeto completo con id, nombre, telefono, email, foto, direccion)
- `operarioAsignado` (objeto completo con id, nombre, telefono, email, foto)
- `presupuesto` (el presupuesto aceptado si existe), `tienePresupuestoAceptado` (boolean)

> ├ó┼í┬á├»┬©┬Å **Bug corregido (01/03):** Antes el campo `direccion` del trabajo NO estaba en la respuesta LISTAR_TRABAJOS.
> Flutter ca├¡a en `json['direccionCliente']` y siempre mostraba la direcci├│n del cliente, ignorando la direcci├│n
> real de la incidencia. Ahora se incluye `map.put("direccion", t.getDireccion())` expl├¡citamente.

---

## ESTADO DE CADA M├ôDULO DE LA APP FLUTTER

### Estructura de carpetas

````
lib/
Ôö£ÔöÇÔöÇ main.dart                          ├óÔÇá┬É Entrada, providers, rutas, tema
Ôö£ÔöÇÔöÇ models/
Ôöé   Ôö£ÔöÇÔöÇ trabajo.dart                   ├óÔÇá┬É Modelo Trabajo + enums EstadoTrabajo, CategoriaServicio
Ôöé   Ôö£ÔöÇÔöÇ usuario.dart                   ├óÔÇá┬É Modelo Usuario + enum Rol
Ôöé   Ôö£ÔöÇÔöÇ presupuesto.dart               ├óÔÇá┬É Modelo Presupuesto
Ôöé   ÔööÔöÇÔöÇ empresa.dart                   ├óÔÇá┬É Modelo Empresa colaboradora
Ôö£ÔöÇÔöÇ providers/
Ôöé   ÔööÔöÇÔöÇ trabajo_provider.dart          ├óÔÇá┬É State management para trabajos
Ôö£ÔöÇÔöÇ services/
Ôöé   Ôö£ÔöÇÔöÇ socket_service.dart            ├óÔÇá┬É Comunicaci├│n TCP con servidor Java (protocolo 4 bytes)
Ôöé   ÔööÔöÇÔöÇ auth_service.dart             ├óÔÇá┬É Login, logout, persistencia token en SharedPreferences
Ôö£ÔöÇÔöÇ screens/
Ôöé   Ôö£ÔöÇÔöÇ login_pantalla.dart
Ôöé   Ôö£ÔöÇÔöÇ dashboard_pantalla.dart
Ôöé   Ôö£ÔöÇÔöÇ detalle_trabajo_pantalla.dart  ├óÔÇá┬É REFACTORIZADA: delega en widgets separados
Ôöé   Ôö£ÔöÇÔöÇ crear_trabajo_pantalla.dart    ├óÔÇá┬É Crear y Modificar (modo dual)
Ôöé   ÔööÔöÇÔöÇ perfil_pantalla.dart
ÔööÔöÇÔöÇ widgets/
    Ôö£ÔöÇÔöÇ comunes/
    Ôöé   Ôö£ÔöÇÔöÇ dato_fila.dart
    Ôöé   ÔööÔöÇÔöÇ estado_badge.dart
    Ôö£ÔöÇÔöÇ trabajos/
    Ôöé   Ôö£ÔöÇÔöÇ tarjeta_trabajo.dart
    Ôöé   Ôö£ÔöÇÔöÇ tarjeta_contacto.dart
    Ôöé   ÔööÔöÇÔöÇ galeria_fotos.dart         ├óÔÇá┬É Preparada para URLs Firebase (Nuevo 08/03)
    ÔööÔöÇÔöÇ detalle_trabajo/
        Ôö£ÔöÇÔöÇ detalle_info_card.dart
        Ôö£ÔöÇÔöÇ detalle_resumen_final.dart
        Ôö£ÔöÇÔöÇ detalle_seccion_presupuestos.dart
        ÔööÔöÇÔöÇ dialogos_trabajo.dart      ├óÔÇá┬É Todos los AlertDialogs (borrar, finalizar, valorar)
```

---

## Ô£à TAREAS COMPLETADAS: IM├â┬üGENES (Actualizado 10/03)

Las siguientes tareas han sido implementadas y est├ín listas para validaci├│n final:

1. **Fotos de Perfil en App (Cliente)**:
   - Implementado en `perfil_pantalla.dart` con `image_picker` y subida directa a Firebase Storage.
   - Sincronizaci├│n con el servidor mediante `ServicioAutenticacion` y evento `ACTUALIZAR_FOTO_PERFIL`.
2. **Fotos de Perfil en Dashboard JavaFX**:
   - Implementada clase `FirebaseStorageUploader.java` (REST API) para subida de fotos desde escritorio.
   - Funcionalidad de cambio de foto a├▒adida para **Gerente** (Panel Empresa) y **Operarios** (Panel Operarios).
   - Actualizada la l├│gica de `miniAvatar` para mostrar fotos reales desde URL con clips circulares en JavaFX.
3. **Soporte de Backend (Servidor Java)**:
   - Nuevo endpoint `ACTUALIZAR_FOTO_PERFIL` en `ProcesadorUsuarios`.
   - Consulta SQL optimizada para recuperar la foto del Gerente de forma aislada.

---

## PROTOCOLO DE PRUEBAS PARA LA PR├ôXIMA SESI├ôN

Para verificar que todo el sistema de im├ígenes es robusto, realizar los siguientes pasos en orden:

### 1. Prueba en App M├│vil (Flujo Cliente)
- Iniciar sesi├│n como **Cliente**.
- Ir a Perfil ÔåÆ Clic en el icono de la c├ímara (­ƒô©).
- Seleccionar una imagen de la galer├¡a.
- **Verificar:** El redondel del perfil debe actualizarse con la nueva foto.
- **Persistencia:** Cerrar sesi├│n y volver a entrar; la foto debe seguir ah├¡ (cargada desde URL).

### 2. Prueba en Dashboard (Flujo Gerente)
- Ir a la secci├│n **Empresa**.
- Clic en el icono ­ƒô© sobre el redondel del Gerente.
- Seleccionar un archivo del PC.
- **Verificar:** El redondel debe mostrar la foto tras la carga.

### 3. Prueba en Dashboard (Flujo Operario)
- Ir a la secci├│n **Operarios**.
- En la tabla, pulsar el bot├│n ­ƒô© de un operario espec├¡fico.
- Seleccionar foto.
- **Verificar:** La celda de "Nombre" del operario debe mostrar ahora su foto real en el avatar peque├▒o en lugar de las iniciales.

### 4. Prueba Cruzada
- Cambiar la foto de un operario en el Dashboard.
- Iniciar sesi├│n con ese mismo operario en la App de Flutter.
- **Verificar:** En el perfil de la app, debe aparecer la foto que asign├│ el gerente.

---

---

### Providers

- **`TrabajoProvider`** (`lib/providers/trabajo_provider.dart`)
  - `obtenerTrabajos()` ÔÇö Lista trabajos, excluye CANCELADOS, ordena por prioridad de estado
  - `crearTrabajo(datos)` ÔÇö Env├¡a `CREAR_TRABAJO`. No Completer, solo delay 800ms
  - `cancelarTrabajo(idTrabajo)` ÔÇö Env├¡a `CANCELAR_TRABAJO`, delay 800ms + llama `obtenerTrabajos()`
  - `modificarTrabajo(idTrabajo, datos)` ÔÇö Env├¡a `MODIFICAR_TRABAJO`, usa Completer que espera `"modificado"` en mensaje
  - `valorarTrabajo(idTrabajo, valoracion, comentario)` ÔÇö Env├¡a `VALORAR_TRABAJO`, usa Completer que espera `"Valoracion"` en mensaje
  - `actualizarEstadoTrabajo(idTrabajo, estado, informe?)` ÔÇö Para FINALIZAR desde operario. delay 800ms + `obtenerTrabajos()`
  - `aceptarPresupuesto(idPresupuesto)` ÔÇö delay 800ms + `obtenerTrabajos()`
  - `startPolling()` / `stopPolling()` ÔÇö Refresco autom├ítico cada 15 segundos (evento push)

> ├ó┼í┬á├»┬©┬Å TRUCO DEL COMPLETER para `modificar/valorar`: Los Completers filtran por palabras clave del `mensaje`
> de respuesta (NO por `status == 200`) para no capturar por accidente la respuesta de LISTAR que tambi├®n
> devuelve 200 y llega de forma as├¡ncrona.

### Pantallas ÔÇö Comportamiento de Navegaci├│n (ACTUALIZADO 01/03)

**Patr├│n est├índar para todas las acciones:**

1. La acci├│n (finalizar, valorar, aceptar, borrar) llama al provider y espera el resultado.
2. Si `exito == true`, se hace **`Navigator.pop(context)`** simple (NO `popUntil`).
3. El dashboard tiene `.then((_) async { await Future.delayed(900ms); obtenerTrabajos(); })` en el `onTap`.
4. El delay de 900ms permite al servidor confirmar el cambio en BD antes de re-listar.

> ├ó┼í┬á├»┬©┬Å **CAUSA DE CRASH HIST├ôRICO:** Usando `popUntil(ModalRoute.withName('/dashboard'))` con rutas
> an├│nimas (`MaterialPageRoute`) el stack de navegaci├│n quedaba vac├¡o ÔåÆ pantalla negra.
> **Nunca usar `popUntil` desde pantallas navegadas con `MaterialPageRoute` sin nombre.**

### Pantallas

- **`DashboardPantalla`** (`lib/screens/dashboard_pantalla.dart`)
  - Lista trabajos con `TarjetaTrabajo` (ordenados por prioridad)
  - **Bot├│n Refresh** en AppBar (Ô×ñ `Icons.refresh`) para todos los roles
  - Pull-to-refresh con `RefreshIndicator`
  - Pantalla vac├¡a mejorada: `CustomScrollView` con `AlwaysScrollableScrollPhysics` para que el pull-to-refresh funcione aunque no haya registros + bot├│n "Actualizar" visible
  - Bot├│n `+` flotante (solo CLIENTE)
  - `_tieneAccionPendiente`: Badge de acci├│n solo si:
    - CLIENTE + PRESUPUESTADO ÔåÆ hay presupuesto por aceptar
    - CLIENTE + REALIZADO y `valoracion == 0` ÔåÆ pendiente valorar
    - CLIENTE + FINALIZADO y `valoracion == 0` ÔåÆ pendiente valorar
    - OPERARIO + ACEPTADO ÔåÆ hay trabajo por iniciar

- **`CrearTrabajoPantalla`** (`lib/screens/crear_trabajo_pantalla.dart`)
  - Par├ímetro opcional `trabajoAEditar: Trabajo?`
  - **Direcci├│n opcional (nuevo 01/03):** Campo no obligatorio. Si se deja vac├¡o, el servidor usa la direcci├│n registrada del cliente. Hint text: "Si se deja vac├¡o, se usa tu direcci├│n registrada". Solo se env├¡a el campo `direccion` en el JSON si el usuario escribi├│ algo.
  - SnackBar verde/rojo con mensaje descriptivo

- **`DetalleTrabajoPantalla`** (`lib/screens/detalle_trabajo_pantalla.dart`)
  - **REFACTORIZADA en sesi├│n anterior:** Delega el rendering en widgets separados de `widgets/detalle_trabajo/`
  - AppBar con `PopupMenuButton`: Modificar + Borrar (solo CLIENTE en estado PENDIENTE/PRESUPUESTADO)
  - Muestra `DetalleInfoCard` ÔåÆ informaci├│n principal del trabajo
  - Si CLIENTE + PENDIENTE/PRESUPUESTADO ÔåÆ `DetalleSeccionPresupuestos`
  - Si OPERARIO + ASIGNADO/REALIZADO ÔåÆ bot├│n verde "MARCAR COMO FINALIZADO"
  - Si FINALIZADO ÔåÆ `DetalleResumenFinal` (fecha, precio, valoraci├│n)
  - Si CLIENTE + FINALIZADO/REALIZADO + `valoracion == 0` ÔåÆ bot├│n azul "VALORAR SERVICIO"

### Widgets

- `TarjetaTrabajo` ÔÇö Tarjeta del dashboard, banner de acci├│n pendiente, men├║ 3 puntos
- `TarjetaContacto` ÔÇö Datos de contacto de cliente u operario
- `GaleriaFotos` ÔÇö Tira horizontal de fotos, tap abre modal ampliado. **Preparada para URLs Firebase**
- `EstadoBadge` ÔÇö Chip coloreado seg├║n estado
- `DatoFila` ÔÇö Par Etiqueta: Valor simple
- `DetalleInfoCard` ÔÇö Tarjeta principal de detalle (estado, categor├¡a, descripci├│n, contactos)
- `DetalleResumenFinal` ÔÇö Tarjeta verde de cierre (precio, fecha, valoraci├│n)
- `DetalleSeccionPresupuestos` ÔÇö Lista de presupuestos con bot├│n Aceptar y di├ílogo de empresa
- `DialogosTrabajo` ÔÇö Clase utilitaria con todos los AlertDialogs (borrar, finalizar, valorar)

---

SESI├ôN 01/03/2026 ÔÇö Cambios Detallados

### Objetivo de la sesi├│n

Refactorizaci├│n de c├│digo, limpieza de logs debug, a├▒adir documentaci├│n a todas las clases, y correcci├│n de m├║ltiples bugs de funcionamiento en el flujo cliente-operario.

### Backend (Java) ÔÇö Cambios

#### `ProcesadorTrabajos.java`

- **Bug fix cr├¡tico:** A├▒adido `map.put("direccion", t.getDireccion())` en `procesarListarTrabajos` (l├¡nea ~198). Antes esta clave nunca se inclu├¡a en la respuesta, por lo que Flutter siempre usaba la direcci├│n del cliente en lugar de la del trabajo.

#### `TrabajoServiceImpl.java`

- **Nuevo comportamiento `solicitarReparacion`:** Si `direccion` viene vac├¡o desde la app, el servidor usa `cliente.getDireccion()` como fallback. Si tampoco tiene, "Sin direcci├│n especificada".

#### `SimuladorController.java`

- Actualizado al protocolo de 4 bytes (`writeInt` / `readInt`) para ser compatible con el servidor actualizado.

### App Flutter ÔÇö Cambios

#### Limpieza de c├│digo

- Eliminados todos los `print()` y llamadas a `Logger` de: `trabajo_provider.dart`, `auth_service.dart`, `socket_service.dart`
- Reemplazados `Logger` por `debugPrint` solo en bloques `catch` cr├¡ticos

#### Documentaci├│n

- A├▒adido comentario de cabecera en **todas** las clases del proyecto (2 l├¡neas, estilo conciso):
  - `main.dart`, `login_pantalla.dart`, `dashboard_pantalla.dart`, `detalle_trabajo_pantalla.dart`, `crear_trabajo_pantalla.dart`, `perfil_pantalla.dart`
  - `socket_service.dart`, `auth_service.dart`, `trabajo_provider.dart`
  - `trabajo.dart`, `usuario.dart`, `presupuesto.dart`, `empresa.dart`
  - Todos los widgets en `widgets/comunes/`, `widgets/trabajos/`, `widgets/detalle_trabajo/`

#### `dashboard_pantalla.dart`

- A├▒adido bot├│n `Icons.refresh` en AppBar (para todos los roles, sin condici├│n)
- Pantalla vac├¡a: cambiado de `Center(Text)` simple a `CustomScrollView + SliverFillRemaining` con `AlwaysScrollableScrollPhysics` para que el pull-to-refresh funcione incluso sin elementos. Incluye bot├│n "Actualizar" visible.
- `.then()` en `onTap` ahora tiene `await Future.delayed(900ms)` antes de `obtenerTrabajos()` para dar tiempo al servidor a procesar el cambio en BD
- `_tieneAccionPendiente` refactorizado: ahora es expl├¡cito (if-return) en lugar de un `||` compuesto, y el estado REALIZADO solo activa el badge si `valoracion == 0`

#### `detalle_trabajo_pantalla.dart`

- **Todos los `Navigator.popUntil` eliminados** ÔåÆ reemplazados por `Navigator.pop(context)` simple
- Las llamadas a `obtenerTrabajos()` tambi├®n se eliminaron de aqu├¡ (el dashboard las hace en `.then()`)
- `_handleBorrar`: limpiado (antes ten├¡a `popUntil` que causaba crash de pantalla negra)
- `_aceptarPresupuesto`: idem
- `_finalizarTrabajo`: idem
- `_handleValorar`: en caso de error muestra SnackBar y hace `return` (no navega); en caso de ├®xito solo hace `pop`

#### `crear_trabajo_pantalla.dart`

- Campo `direccion` ya no tiene validador obligatorio
- Hint text: "Si se deja vac├¡o, se usa tu direcci├│n registrada"
- El campo `direccion` solo se incluye en el mapa `datos` si no est├í vac├¡o (condicional `if` en el Map literal)

#### `trabajo_provider.dart`

- `cancelarTrabajo`: A├▒adido `obtenerTrabajos()` tras el delay de 800ms (antes solo retornaba `true` sin actualizar la lista)

### Bugs resueltos esta sesi├│n

1. **Pantalla negra al finalizar/valorar/borrar** ÔåÆ Causa: `popUntil` con rutas an├│nimas vaciaba el stack. Fix: `Navigator.pop()` simple.
2. **Lista no se actualizaba despu├®s de acciones** ÔåÆ Causa: `obtenerTrabajos()` se llamaba antes del delay del servidor. Fix: Delay de 900ms en el `.then()` del dashboard.
3. **Direcci├│n del trabajo siempre mostraba la del cliente** ÔåÆ Causa: Campo `direccion` ausente en JSON de LISTAR_TRABAJOS. Fix: `map.put("direccion", t.getDireccion())` en procesador Java.
4. **Cancelar desde detalle no actualizaba el dashboard** ÔåÆ Causa: `cancelarTrabajo` no llamaba a `obtenerTrabajos()`. Fix: a├▒adido tras delay.
5. **Pantalla vac├¡a del operario no permit├¡a pull-to-refresh** ÔåÆ Fix: `CustomScrollView` con `AlwaysScrollableScrollPhysics`.
6. **Badge de "Valorar" persist├¡a tras valorar** ÔåÆ Fix: condici├│n `valoracion == 0` expl├¡cita en `_tieneAccionPendiente`.

---

## PR├ôXIMAS FASES

### Fase 2: Fotos con Firebase Storage ├ó┬¼┼ô SIGUIENTE

#### Plan de implementaci├│n:

**Firebase (setup):**

- [ ] Crear proyecto Firebase
- [ ] A├▒adir app Android al proyecto Firebase (google-services.json)
- [ ] A├▒adir dependencias en Flutter: `firebase_core`, `firebase_storage`, `image_picker`

**Flutter ÔÇö Trabajos:**

- [ ] En `CrearTrabajoPantalla`: activar bot├│n "A├▒adir foto" ÔåÆ `image_picker` ÔåÆ subir a Firebase Storage ÔåÆ recibir URL ÔåÆ a├▒adir a `_urlsFotos`
- [ ] Enviar `urls_fotos` en el JSON al servidor ya que el campo existe en el mapa de datos
- [ ] `GaleriaFotos` ya est├í preparado ÔåÆ solo necesita URLs reales

**Flutter ÔÇö Perfil de usuario:**

- [ ] En `PerfilPantalla`: a├▒adir bot├│n de editar foto ÔåÆ `image_picker` ÔåÆ subir a Firebase ÔåÆ actualizar `url_foto` del usuario en servidor
- [ ] Backend: nueva acci├│n `ACTUALIZAR_PERFIL` o `SUBIR_FOTO_PERFIL` en `ProcesadorAutenticacion`
- [ ] Modelo `Usuario.urlFoto` ya existe ÔåÆ solo falta el flujo de subida

**Backend Java:**

- [ ] `FotoTrabajo` ya existe como clase. `FotoTrabajoDAO` ya existe y guarda en BD
- [ ] El servidor ya intenta cargar fotos en `procesarListarTrabajos` ÔåÆ solo falta recibir y guardar URLs al crear
- [ ] La acci├│n `CREAR_TRABAJO` ya lee `urls_fotos` del JSON y llama a `fotoTrabajoDAO` ÔåÆ ya implementado

### Fase 3: Despliegue Local en Red (M├│vil F├¡sico) ├ó┬¼┼ô

**Objetivo:** Hacer funcionar la app en un m├│vil f├¡sico real dentro de la misma red WiFi.

- [ ] **SocketService:** Cambiar IP de `10.0.2.2` a la IP local de la m├íquina (ej: `192.168.1.X`)
  - Crear variable configurable o pantalla de configuraci├│n de IP
- [ ] **Firebase:** Ya funcionar├í con IP real (es HTTPS externo)
- [ ] **Servidor Java:** Asegurarse de que el firewall de Windows abre el puerto `5000`
  - PowerShell: `New-NetFirewallRule -DisplayName "FixFinder" -Direction Inbound -Protocol TCP -LocalPort 5000 -Action Allow`
- [ ] **App Escritorio (JavaFX dashboard):** Probado en red local (ya se conecta por socket a localhost)

### Fase 4: Despliegue en AWS EC2 ├ó┬¼┼ô

**Objetivo:** Servidor Java en la nube, app conectando a IP p├║blica.

- [ ] Provisionar EC2 (Ubuntu 22.04 recomendado), instalar Java 21 y MySQL
- [ ] Copiar el JAR del servidor (`.\gradlew.bat jar`)
- [ ] Abrir puertos: `5000` (TCP) y `3306` (MySQL, solo acceso interno)
- [ ] Crear script de arranque autom├ítico con `systemd`
- [ ] **SocketService Flutter:** Parametrizar IP (leer de config) ÔåÆ apuntar a IP p├║blica AWS
- [ ] Firebase Storage ya funciona con cualquier IP (es servicio externo)
- [ ] Probar flujo completo cliente ÔåÆ servidor AWS ÔåÆ BD RDS (o MySQL en EC2)

### Fase 5: Documentaci├│n y Defensa ├ó┬¼┼ô

---

SESI├ôN 07/03/2026 ÔÇö Refinado Gerencial y Comunicaci├│n

### Objetivo de la sesi├│n

Refinar la visualizaci├│n de la empresa (valoraciones reales), unificar la comunicaci├│n gerente-operario mediante la "Hoja Informativa" y solucionar errores cr├¡ticos en la gesti├│n de operarios del Dashboard.

### Backend (Java) ÔÇö Cambios

#### `ProcesadorUsuarios.java`

- **Enriquecimiento de Empresa:** Al solicitar datos de empresa (`GET_EMPRESA`), el servidor ahora busca todos los trabajos `FINALIZADOS` vinculados a esa empresa y devuelve una lista de valoraciones reales (puntuaci├│n, cliente, comentario, fecha).
- **Limpieza de Referencias:** Eliminadas instanciaciones directas de `DataRepository` para favorecer la estabilidad de conexiones.

#### `OperarioDAOImpl.java`

- **Sincronizaci├│n de ENUM SQL:** Se ha corregido la palabra m├ígica. El SQL usa `BAJA`, pero el c├│digo enviaba `INACTIVO`. Ahora se env├¡a `BAJA` al desactivar (baja l├│gica).
- **Correcci├│n de Mapeo:** Se asegura que al leer de la DB, cualquier estado distinto de `BAJA` se interprete como `estaActivo = true`.

#### `OperarioServiceImpl.java`

- **Pruebas R├ípidas:** Se han comentado las validaciones de `matches()` para Email, DNI y Tel├®fono para permitir avanzar con datos de prueba no perfectos.
- **Sanitizaci├│n:** Se ha a├▒adido un `.replaceAll("[^0-9]", "")` al tel├®fono para evitar fallos por espacios o guiones.

#### `PresupuestoServiceImpl.java` & `ProcesadorTrabajos.java`

- **Eliminaci├│n de `notas`:** Se ha borrado el campo `notas` de la tabla `presupuesto` y de los objetos Java. Ya no se usa.

### App Escritorio (JavaFX) ÔÇö Cambios

#### `VistaEmpresa.java`

- **Secci├│n de Rese├▒as:** Implementada una lista visual que muestra las ├║ltimas valoraciones de los clientes con estrellas (├ó┬¡┬É).
- **Fecha de Registro:** Corregida la visualizaci├│n de la fecha de alta de la empresa (ya no sale "No disponible").

#### `DialogoCrearPresupuesto.java`

- **Hoja Informativa:** Ahora el ├írea de texto de "Notas" actualiza directamente la `descripcion` del trabajo.
- **Plantilla Autom├ítica:** Si la descripci├│n no est├í estructurada, el di├ílogo inserta una plantilla con cabeceras para `CLIENTE`, `GERENTE` y `OPERARIO`.

#### `DashboardPrincipalController.java`

- **Sincronizaci├│n de Callbacks:** Los m├®todos `onPresupuestar` y similares ya no usan el par├ímetro `notas`, sino que gestionan la `nuevaDescripcion` del trabajo.

### Bugs resueltos esta sesi├│n

1. **Error 500 al dar de baja operario:** Causa: Discrepancia entre "INACTIVO" e "BAJA" en el ENUM de MySQL. Fix: Sincronizado a "BAJA".
2. **Edici├│n de operario fallaba por validaci├│n:** Causa: Tel├®fono con espacios o formato de email estricto. Fix: Comentadas validaciones y sanitizado tel├®fono.
3. **Valoraciones de empresa vac├¡as:** Causa: No se estaban consultando los trabajos finalizados. Fix: Implementada b├║squeda por empresa en el procesador.
4. **Desconexiones por "Connection Reset":** Causa: Demasiadas aperturas de `DataRepositoryImpl`. Fix: Refactorizado a uso de DAOs directos cuando es posible.

---

## PR├ôXIMAS FASES

- [ ] Memoria t├®cnica (arquitectura, decisiones de dise├▒o, protocolo de comunicaci├│n)
- [ ] Diagrama de clases, diagrama de secuencia del flujo completo
- [ ] Presentaci├│n + ensayo

---

## ├░┼©ÔÇ║┬á├»┬©┬Å COMANDOS DE REFERENCIA R├â┬üPIDA

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
r   ├óÔÇá┬É hot reload
R   ├óÔÇá┬É hot restart (limpia estado)

# Correr tests Flutter
flutter test

# Abrir firewall para red local
New-NetFirewallRule -DisplayName "FixFinder" -Direction Inbound -Protocol TCP -LocalPort 5000 -Action Allow

# Consultar la BD
docker exec -i FixFinderDb mysql -u root -proot fixfinder -e "SELECT id,titulo,estado,valoracion,direccion FROM trabajo ORDER BY id DESC LIMIT 10;"

# Ver usuarios
docker exec -i FixFinderDb mysql -u root -proot fixfinder -e "SELECT id,email,rol,telefono,direccion FROM usuario;"

# Restaurar tel├®fonos si los tests los borran
docker exec -i FixFinderDb mysql -u root -proot fixfinder -e "UPDATE usuario SET telefono = '600123456' WHERE rol = 'CLIENTE' OR rol = 'OPERARIO';"
````

---

**Nota Final:** Trabajar siempre paso a paso. Antes de implementar una nueva funcionalidad,
leer este documento para no romper lo que ya funciona. El protocolo de 4 bytes y el patr├│n
`pop + then(delay + obtenerTrabajos)` son invariantes cr├¡ticos del sistema.

---

## ­ƒöº SESI├ôN 08/03/2026 ÔÇö Auditor├¡a de Calidad y Decisi├│n de Ruta

### Objetivo de la sesi├│n

Revisar el c├│digo completo, evaluar calidad, detectar clases problem├íticas y preparar un plan quir├║rgico de refactorizaci├│n. En esta sesi├│n **no se aplicaron los cambios** (por precauci├│n, dado que el proyecto estaba en estado funcional). Se cre├│ un checkpoint de Git antes de cualquier cambio.

### Estado del Repositorio

- **Rama actual:** `refactor`
- **Commit de punto de partida (pre-refactorizaci├│n):** `ec6f1d3` ÔÇö "pre refactor"
- **Comando para volver atr├ís si algo se rompe:**
  ```powershell
  git checkout ec6f1d3 -- .
  # o para descartar todos los cambios y volver al commit exacto:
  git reset --hard ec6f1d3
  ```

### Cambios aplicados ANTES de la auditor├¡a (inicio de sesi├│n)

Se realizaron estas modificaciones que YA EST├â┬üN en el commit `ec6f1d3`:

#### `ProcesadorTrabajos.java` ÔÇö Refactorizaci├│n parcial aplicada

- M├®todo `mapearTrabajo(Trabajo t)` extra├¡do como privado: centraliza la conversi├│n de objeto Trabajo a `Map<String, Object>`. Antes se repet├¡a inline en cada bloque del listado.
- M├®todo `filtrarParaGerente(int idUsuario)` extra├¡do como privado: encapsula la l├│gica de qu├® trabajos ve un gerente (PENDIENTE + PRESUPUESTADO + los de su empresa).
- **├ó┼í┬á├»┬©┬Å ATENCI├ôN:** La refactorizaci├│n introdujo errores de compilaci├│n que se resolvieron durante la sesi├│n. Los imports correctos son `com.fixfinder.modelos.enums.EstadoTrabajo` y `com.fixfinder.modelos.enums.EstadoPresupuesto`. La firma del servicio de cancelar es `cancelarTrabajo(Integer, String)` ÔåÆ siempre pasar motivo.

#### `DashboardPrincipalController.java`

- El m├®todo `solicitarTrabajos()` ahora tambi├®n llama a `servicioCliente.enviarGetEmpresa(idEmpresa)` para refrescar los datos de la empresa sin necesidad de re-login.

#### `VistaDashboard.java`

- Animaci├│n a├▒adida al bot├│n `btnRefresh`: `RotateTransition` (360├é┬░, 0.5s) + `ScaleTransition` (1ÔåÆ0.85ÔåÆ1, 0.5s) en paralelo mediante `ParallelTransition`. Se ejecuta cada vez que se pulsa el bot├│n.

#### `socket_service.dart` (Flutter)

- A├▒adido m├®todo `request(Map peticion, {String? accionEsperada, int timeoutSegundos})`: encapsula el patr├│n Completer + listen + timeout + cancel en un solo m├®todo reutilizable. Preparado para limpiar `TrabajoProvider`.

---

### ├░┼©ÔÇØ┬ì Auditor├¡a Completa de Calidad ÔÇö Resultados

#### BACKEND JAVA

| Clase                                         | Tama├▒o    | Diagn├│stico                                                                                                                                                                                                                   | Severidad   |
| --------------------------------------------- | --------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------- |
| `ProcesadorTrabajos.java`                     | ~280L     | Ô£à Refactorizado. Mapeo ├║nico, filtrado encapsulado.                                                                                                                                                                          | Ô£à Resuelto |
| `ProcesadorAutenticacion.java`                | 233L      | ├ó┼í┬á├»┬©┬Å M├®todo `procesarRegistro` mezcla 3 flujos (CLIENTE, OPERARIO, EMPRESA) en uno. Dif├¡cil de mantener.                                                                                                                        | ├ó┼í┬á├»┬©┬Å Medio    |
| `ProcesadorUsuarios.java`                     | 209L      | ├ó┼í┬á├»┬©┬Å Instancia DAOs directamente (`new EmpresaDAOImpl()`). Viola inversi├│n de dependencias. La l├│gica de valoraciones de empresa (50L) deber├¡a estar en el Service, no en el Procesador.                                        | ├ó┼í┬á├»┬©┬Å Medio    |
| `TrabajoServiceImpl.java`                     | 337L      | ├óÔÇ×┬╣├»┬©┬Å `historialOperario` carga TODOS los trabajos y filtra en Java (no en SQL). Con muchos datos puede ser lento. La l├│gica de "parsear descripci├│n por emojis" en `finalizarTrabajo` es fr├ígil si alguien cambia la plantilla. | ├óÔÇ×┬╣├»┬©┬Å Bajo     |
| `TrabajoDAOImpl.java`                         | 371L      | (ROJO) **N+1 Problem:** El m├®todo `cargarRelaciones` abre una nueva conexi├│n SQL por cada trabajo de la lista para cargar cliente + operario + fotos. En 50 trabajos = 150 queries. Soluci├│n: JOIN en la query principal.         | (ROJO) Alto     |
| `GestorConexion.java`                         | 238L      | Ô£à Bien dise├▒ado. Delega. No tocar.                                                                                                                                                                                           | Ô£à OK       |
| `ServidorCentral.java`                        | 110L      | Ô£à Limpio. Sem├íforo de 10 conexiones.                                                                                                                                                                                         | Ô£à OK       |
| `OperarioDAOImpl.java`, `EmpresaDAOImpl.java` | ~11KB c/u | Ô£à Aceptables. Sin duplicaci├│n visible.                                                                                                                                                                                       | Ô£à OK       |

#### DASHBOARD JAVAFX

| Clase                                 | Tama├▒o      | Diagn├│stico                                                                                                                                        | Severidad |
| ------------------------------------- | ----------- | -------------------------------------------------------------------------------------------------------------------------------------------------- | --------- |
| `TablaIncidencias.java`               | 422L / 17KB | (ROJO) **GOD CLASS:** Controla tabla, 8 tipos de celdas, 3 di├ílogos de acci├│n, filtros, tabs Y decoraci├│n de iconos. Si falla un m├®todo afecta a todo. | (ROJO) Alto   |
| `DashboardPrincipalController.java`   | ~331L       | ├ó┼í┬á├»┬©┬Å Switch `procesarRespuesta` con ~20 casos. Funciona, pero en el l├¡mite de lo mantenible.                                                         | ├ó┼í┬á├»┬©┬Å Medio  |
| `VistaDashboard.java`, `Sidebar.java` | <200L c/u   | Ô£à Limpias.                                                                                                                                        | Ô£à OK     |
| `TrabajoFX.java`, `OperarioFX.java`   | ~130L c/u   | Ô£à JavaFX Properties correctas.                                                                                                                    | Ô£à OK     |
| `DialogoNuevoOperario.java`           | 6KB         | ├óÔÇ×┬╣├»┬©┬Å Grande pero cohesivo.                                                                                                                           | ├óÔÇ×┬╣├»┬©┬Å Bajo   |

#### APP FLUTTER

| Clase                           | Tama├▒o      | Diagn├│stico                                                                                                                                                                                                                                      | Severidad |
| ------------------------------- | ----------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | --------- |
| `trabajo_provider.dart`         | 365L / 11KB | (ROJO) **Boilerplate masivo:** Cada uno de los 8 m├®todos replica exactamente el mismo patr├│n `Completer + listen + send + timeout + cancel`. ~200L son c├│digo id├®ntico. El m├®todo `request()` ya existe en `socket_service.dart` para resolver esto. | (ROJO) Alto   |
| `dashboard_pantalla.dart`       | 228L        | ├ó┼í┬á├»┬©┬Å Tiene l├│gica de negocio mezclada con UI: `_tieneAccionPendiente()` y `_obtenerIconoCategoria()` deber├¡an estar en el modelo o en un helper.                                                                                                   | ├ó┼í┬á├»┬©┬Å Medio  |
| `detalle_trabajo_pantalla.dart` | 269L        | Ô£à Ya refactorizada (usa widgets separados).                                                                                                                                                                                                     | Ô£à OK     |
| `tarjeta_trabajo.dart`          | 239L        | Ô£à Bien encapsulada con animaci├│n propia.                                                                                                                                                                                                        | Ô£à OK     |
| `socket_service.dart`           | ~200L       | Ô£à Mejorado con `request()`.                                                                                                                                                                                                                     | Ô£à OK     |

---

### ­ƒôï Plan de Refactorizaci├│n Quir├║rgica (PENDIENTE DE EJECUTAR)

**Principio:** Cada bloque se compila y prueba ANTES de pasar al siguiente. Las firmas p├║blicas de todos los m├®todos se respetan (sin breaking changes).

#### Bloque 1 ÔÇö Flutter: Limpiar `trabajo_provider.dart`

- **Qu├®:** Migrar los 8 m├®todos al nuevo `_socket.request()`. Eliminar ~200L de boilerplate.
- **Riesgo:** Bajo. Solo cambia la implementaci├│n interna, no el contrato.
- **Estimaci├│n:** 365L ÔåÆ ~190L (├ó╦åÔÇÖ48%)
- **Mover `_obtenerIconoCategoria`** ÔåÆ helper est├ítico en `models/trabajo.dart`
- **Mover `_tieneAccionPendiente`** ÔåÆ m├®todo de instancia en `Trabajo`

#### Bloque 2 ÔÇö Java: Query JOIN en `TrabajoDAOImpl`

- **Qu├®:** Crear m├®todo `obtenerTodosConJoin()` con SQL que incluye `LEFT JOIN usuario` y `LEFT JOIN operario` para evitar las 150 queries al listar.
- **Riesgo:** Medio. Tocar el DAO m├ís cr├¡tico del sistema.
- **El m├®todo `cargarRelaciones` actual se mantiene** para `obtenerPorId` (no es cr├¡tico en uso individual).

#### Bloque 3 ÔÇö Java: Separar `ProcesadorAutenticacion.procesarRegistro`

- **Qu├®:** Extraer `registrarCliente()`, `registrarOperario()`, `registrarEmpresa()` como m├®todos privados. El m├®todo p├║blico queda como router de 3 l├¡neas.
- **Riesgo:** Bajo. Sin cambio de firma p├║blica.

#### Bloque 4 ÔÇö JavaFX: Extraer helpers de `TablaIncidencias.java`

- **Qu├®:** Crear `UiHelper.java` con `miniAvatar()`, `crearLabel()`, `fila()`. Crear `CategoriaHelper.java` con `iconoCategoria()`.
- **Riesgo:** Bajo. Solo mover c├│digo, sin cambiar l├│gica.

---

### ├░┼©┬ñÔÇØ DEBATE: ├é┬┐Refactorizaci├│n primero o Firebase Fotos primero?

**Argumento para Firebase primero:**

- El proyecto ya est├í funcional y presentable para un `proyecto de clase`.
- Fotos es una funcionalidad visible que a├▒ade valor real al evaluador.
- La infraestructura ya est├í casi lista (`GaleriaFotos.dart` preparada, `FotoTrabajoDAO` existe, el servidor ya procesa `urls_fotos`).
- **Estimaci├│n:** 2-3 horas para un MVP funcional (seleccionar foto ÔåÆ subir ÔåÆ mostrar).

**Argumento para Refactorizaci├│n primero:**

- El boilerplate en `TrabajoProvider` con Completers manuales es una fuente real de bugs silenciosos (memory leaks si el timeout falla).
- Si a├▒adimos Firebase sobre c├│digo "sucio", el provider crecer├í a├║n m├ís.
- La God Class `TablaIncidencias` se volver├í inmanejable si a├▒adimos funcionalidad de fotos al dashboard.
- Con git y el commit de punto de partida, el riesgo de romper algo es m├¡nimo.

**Recomendaci├│n del Agente:**

> Hacer primero el **Bloque 1** (Flutter `TrabajoProvider`) porque es el de menor riesgo, mayor impacto visible en limpieza y prepara el terreno para Firebase. Luego pasar a Firebase. Los Bloques 2, 3 y 4 (Java) se pueden hacer en una sesi├│n separada cuando haya m├ís tiempo.

---

## Ô£à TAREAS COMPLETADAS: REFACTORIZACI├ôN Y OPTIMIZACI├ôN (Actualizado 15/03/2026)

Las siguientes mejoras estructurales y de rendimiento han sido implementadas:

1. **Limpieza Integral de C├│digo Java**:
   - Eliminados todos los **imports inline (FQN)** en el proyecto backend y dashboard.
   - Estandarizaci├│n de imports en la cabecera de todas las clases (`ProcesadorUsuarios`, `VistaDashboard`, `Dialogos`, etc.).
   - Correcci├│n de errores de sintaxis y balanceo de llaves en `Sidebar.java`.

2. **Optimizaci├│n de Rendimiento (Dashboard JavaFX)**:
   - **Carga As├¡ncrona de Im├ígenes**: En `VistaOperarios.java`, las fotos de perfil ahora se cargan en segundo plano (`backgroundLoading=true`), eliminando las congelaciones de la UI al navegar.
   - **Placeholders de Iniciales**: Implementado sistema de avatares con iniciales y colores de fondo que se muestran instant├íneamente mientras la foto real se descarga.

3. **Mejora de UI Premium**:
   - **Panel de Valoraciones**: Redise├▒ado el sistema de estrellas en `DialogoDetalleIncidencia.java`. 
   - Corregido el escalado desigual de las estrellas y la l├│gica de activaci├│n (ahora se iluminan de izquierda a derecha correctamente).
   - Est├®tica unificada con el panel de Empresa (colores `#FBBF24` vs `#334155`).

---

---

## Ô£à SESI├ôN 08/04/2026 ÔÇö Refactor Estructural, IDs de Transacci├│n y Estabilizaci├│n

### Lo que hemos conseguido hoy (Resumen T├®cnico)

#### ­ƒÜÇ 1. Refactor de Red y Protocolo (Capa de Transporte)
- **Implementaci├│n de Transaction IDs (`txid`)**: Hemos blindado la comunicaci├│n Sockets. Ahora cada paquete JSON incluye un `txid` ├║nico generado por el cliente. El servidor devuelve el mismo ID en la respuesta, lo que permite a la App y al Dashboard saber exactamente qu├® respuesta corresponde a qu├® petici├│n, eliminando colisiones as├¡ncronas.
- **Refactor de `socket_service.dart`**: Implementado el m├®todo maestro `request()`. Hemos eliminado m├ís de 200 l├¡neas de c├│digo repetitivo (boilerplate) en los providers de Flutter. Ahora las peticiones son at├│micas, tienen timeout integrado y son mucho m├ís seguras.
- **Estandarizaci├│n de Claves**: Unificado el campo de imagen a **`url_foto`** en todo el sistema (Servidor, Dashboard y App), terminando con los fallos de visualizaci├│n cruzada.

#### ­ƒøá´©Å 2. Modularizaci├│n del Dashboard (JavaFX)
- **Muerte a la God Class (`TablaIncidencias.java`)**: Hemos desmontado el "Objeto Dios". La l├│gica de la tabla ahora est├í segmentada:
    - **Celdas Independientes**: `CeldaOperario`, `CeldaEstado`, etc., tienen su propia clase, facilitando el mantenimiento.
    - **Gestores de Di├ílogos**: La l├│gica de abrir fichas y presupuestos se ha movido a controladores espec├¡ficos.
- **Estabilizaci├│n de Avatares**: Implementada carga as├¡ncrona robusta y recorte circular con efecto "cover" real. Las fichas de cliente y operario ahora lucen una UI premium y fluida.

#### ­ƒöÆ 3. Seguridad y UX
- **Confirmaci├│n de Salida**: A├▒adido di├ílogo de seguridad (S├¡/No) en el logout del Dashboard.
- **Limpieza de Tabla**: Eliminado ruido visual en la columna de operarios asignados, priorizando la legibilidad del texto sobre el avatar innecesario.

#### ­ƒÅù´©Å 4. Estabilizaci├│n del Entorno de Desarrollo
- **Fix de Infraestructura**: Configurado el uso de **Gradle 8.10 global** ante la corrupci├│n del wrapper local.
- **Recuperaci├│n de Emulador**: Aplicado Cold Boot y limpieza de procesos zombies (`qemu`, `java`) para restaurar la visibilidad de la App.

---

## ­ƒÄ» PR├ôXIMOS PASOS (URGENTE)

### Prioridad 1 ÔÇö L├│gica de Presupuesto (Pendiente)
- [ ] **Rechazar Presupuesto:** Implementar el flujo para que el cliente pueda rechazar ofertas y la incidencia vuelva a estado `PENDIENTE`.
- [ ] **Reset de Descripci├│n:** Al rechazar, limpiar notas previas de la descripci├│n t├®cnica para volver al mensaje original.

### Prioridad 2 ÔÇö Perfil y Datos
- [ ] **Modificar Datos Personales:** Habilitar edici├│n de tel├®fono y direcci├│n en la App (ahora solo funciona la foto).

### Prioridad 3 ÔÇö Memoria
- [ ] Redactar los campos de texto de `DOCS/MEMORIA.md` y volcar las capturas finales de hoy.

## ­ƒÜÇ FASE 4: DESPLIEGUE REAL EN AWS (FREE TIER)

### 1. Seguridad y Control de Gastos (COMPLETADO)
- **Alertas de Capa Gratuita:** Activadas en la consola de AWS para recibir avisos por email en el correo de la cuenta. Ô£à
- **Presupuesto de Seguridad:** Creado presupuesto mensual de **1.00$** con alertas al **80%** de consumo para evitar sorpresas. Ô£à

### 2. Infraestructura de Datos (Pendiente)
- **AWS RDS (MySQL):** Crear una instancia `db.t3.micro` de MySQL para alojar los datos de forma persistente y profesional.

### 3. Servidor de Aplicaciones (Pendiente)
- **AWS EC2:** Lanzar una instancia `t3.micro` con Ubuntu Server.
- **Entorno:** Configurar Docker y Java para correr el Socket Server.
- **Firewall (Security Groups):** Apertura de los puertos necesarios (5000 para el servidor, 3306 para la BD).

### 4. Conectividad y Salto a Producci├│n
- **Ajustes de C├│digo:** Cambiar las IPs locales por el Endpoint de RDS (en el servidor) y la IP el├ística de EC2 (en la App y Dashboard).
- **Validaci├│n:** Desplegar y probar la comunicaci├│n real entre App (m├│vil f├¡sico) -> EC2 -> RDS.

---

## ­ƒº¬ PROTOCOLO PARA LA PR├ôXIMA SESI├ôN

1. **Paso de Local a Red:** Cambiar IP en `socket_service.dart` a `192.168.0.13` y probar con el m├│vil f├¡sico conectado al mismo Wi-Fi.
2. **Preparaci├│n AWS:** Crear la instancia EC2 y configurar el entorno Docker/Java.
3. **Validaci├│n Final:** Probar que todas las fotos cargan correctamente desde URLs de Firebase tanto en el Dashboard como en la App m├│vil operando fuera del emulador.

---

## ­ƒÜÇ MEJORAS DE ARQUITECTURA (PENDIENTES)

- [ ] **Implementar Escucha Directa (Push Notifications via Sockets):** Actualmente, algunos componentes requieren refresco manual o polling. Aprovechando que ya existe un servidor de sockets persistente, se debe implementar un sistema donde el servidor "empuje" las actualizaciones (`PUSH_UPDATE`) a los clientes interesados (App y Dashboard) inmediatamente cuando ocurra un cambio en la BD (ej: nuevo trabajo, cambio de estado, nuevo mensaje), eliminando la necesidad de actualizar manualmente.


## ├░┼©ÔÇ£┬Ø PR├ôXIMOS PASOS (SESI├ôN SIGUIENTE)

### ­ƒº¬ Fase A: Testing de Registros y Fotos (Finalizaci├│n)
1.  **Commit de Seguridad:** Confirmar todos los cambios actuales de registros y fotos en Git.
2.  **Nueva Rama Git:** Crear rama `deploy/aws-production` para separar el trabajo de despliegue.
3.  **Testing Final Registro:** Probar registro de Clientes (Flutter) y Empresas/Operarios (JavaFX) con subida real a Firebase Storage.
4.  **Revisi├│n Documentaci├│n:** Validar los diagramas de la carpeta `DOCS/` contra el c├│digo final.

### ├░┼©┼Æ┬®├»┬©┬Å Fase B: Despliegue AWS (Producci├│n)
1.  **Levantar RDS:** Crear la base de datos MySQL en Amazon.
2.  **Migraci├│n de Esquema:** Ejecutar scripts de creaci├│n de tablas en RDS.
3.  **Lanzar EC2:** Configurar el servidor de aplicaciones con Docker/Java.
4.  **Ajuste de IPs:** Actualizar las constantes de conexi├│n en todo el proyecto.

---

## Ô£à SESI├ôN 22/03/2026 ÔÇö Refactorizaci├│n final, documentaci├│n y commit

### Lo que se hizo en esta sesi├│n

#### ├░┼©ÔÇØ┬¿ Refactorizaci├│n del c├│digo (Java)
- **`DashboardController`** reducido de 700+ l├¡neas a **214 l├¡neas**:
  - Creada `DashboardBase.java` ÔÇö declaraciones @FXML y base UI.
  - Creada `GestorRegistroDashboard.java` ÔÇö l├│gica de registro de empresa/usuario.
  - Creada `ManejadorRespuestaDashboard.java` ÔÇö procesamiento de mensajes del servidor.
- **`SimuladorController`** reducido de 600+ l├¡neas a **260 l├¡neas**:
  - Creada `ClienteRedSimulador.java` (Singleton) ÔÇö comunicaci├│n TCP del simulador.
  - Creada `ModeloTrabajoSimulador.java` ÔÇö modelo de datos del simulador.
- **Validaciones restauradas** en `UsuarioServiceImpl.java` (email y tel├®fono estaban comentadas).
- **Tests 100% verdes** ÔÇö todos los 10 tests de `ServiceTest.java` pasan.
- **Nomenclatura en castellano** en todos los nuevos componentes.
- **runSeeder** ejecutado correctamente para resetear la BD con datos de prueba.

#### ­ƒôä Documentaci├│n
- **`DOCS/MEMORIA.md`** creada completa siguiendo la gu├¡a oficial del IES Maria Enr├¡quez:
  - 9 secciones con todos los campos en blanco preparados para redactar.
  - **12 diagramas PNG** colocados en sus secciones correspondientes con referencias Mermaid.
  - Secci├│n 5.1 incluye ├írbol de directorios + arquitectura AWS (EC2 + RDS + Firebase).
  - Secci├│n 5.3 aclara expl├¡citamente que **local = desarrollo/pruebas, AWS = despliegue final**.
  - Encabezados con jerarqu├¡a correcta para conversi├│n a PDF.
  - Anexo D con tabla completa de todos los archivos de diagramas `.txt`.
- Diagramas verificados ÔÇö entidad-relaci├│n confirmado correcto (herencia UsuarioÔåÆCliente/Operario, Empresa emite Presupuesto, no directamente Trabajo).

#### ­ƒº╣ Limpieza del proyecto
- Eliminados todos los archivos temporales de log y error: `build_error.txt`, `build_log.txt`, `test_error_log.txt`, `gradle_out.txt`, `flutter_log_f.txt`, etc.
- Eliminados ficheros Flutter obsoletos: `-nCon-Book.flutter-plugins-dependencies`, `test_conexion.dart`.

#### ­ƒôª Git
- **Commit:** `b770ed3` ÔÇö "Refactor clases grandes y debug, doc final y diagramas"
- **52 archivos cambiados**, 2105 inserciones (+), 369 borrados (-)
- **Push a `origin/main`** completado correctamente.

---

**├Ültimo Commit Git:** `b770ed3` ÔÇö "Refactor clases grandes y debug, doc final y diagramas" (22/03/2026)

---

## ­ƒÄ» PR├ôXIMOS PASOS (SIGUIENTE SESI├ôN)

### Prioridad 1 ÔÇö Bugfix pendiente (COMPLETADO 08/04)
- [x] **Foto de perfil del cliente en la ficha del Dashboard:** ┬íResuelto! Corregida carga as├¡ncrona y mapeo de campo JSON.


### Prioridad 2 ÔÇö Despliegue AWS (Producci├│n)
- [ ] **Levantar RDS MySQL:** Crear instancia `db.t3.micro`, configurar Security Group (puerto 3306 solo desde EC2).
- [ ] **Migrar esquema:** ejecutar `SCHEMA.sql` en RDS para crear todas las tablas.
- [ ] **Lanzar EC2:** Instancia `t3.micro` Ubuntu, instalar Docker + Java 21.
- [ ] **Dockerizar el servidor:** Crear `Dockerfile` para el servidor Java socket y hacer `docker build + run` en EC2.
- [ ] **Ajustar IPs en el c├│digo:**
  - `socket_service.dart` (Flutter) ÔåÆ IP el├ística de EC2.
  - `ClienteSocket.java` (Dashboard) ÔåÆ IP el├ística de EC2.
  - `application.properties` o config del servidor ÔåÆ endpoint RDS.
- [ ] **Validaci├│n final:** Probar App m├│vil en dispositivo f├¡sico real ÔåÆ EC2 ÔåÆ RDS.

### Prioridad 3 ÔÇö Memoria acad├®mica
- [ ] Redactar las secciones de texto de `DOCS/MEMORIA.md` (campos `[Escribe aqu├¡...]`).
- [ ] Insertar capturas de pantalla reales de la app y dashboard en la secci├│n 5.4.
- [ ] Completar tabla de requerimientos funcionales/no funcionales (secci├│n 3.1).
- [ ] Completar tabla de hitos del proyecto (secci├│n 3.2).
- [ ] A├▒adir diagrama de Gantt.
- [ ] Exportar a PDF cuando est├® lista.




---

SESI├ôN 01/04/2026 ÔÇö Limpieza de Arquitectura y Restauraci├│n de App

### Objetivo de la sesi├│n
Resolver los bloqueos de compilaci├│n que imped├¡an arrancar el sistema tras el ├║ltimo refactor y restaurar la funcionalidad b├ísica de la App m├│vil que presentaba m├®todos ausentes.

### Backend y Dashboard (Java)
- **Limpieza de C├│digo Legado:** Se han eliminado los archivos hu├®rfanos del paquete `com.fixfinder.controladores` (`DashboardController`, `DashboardBase`, `GestorRegistroDashboard` y `ManejadorRespuestaDashboard`). Estos archivos causaban errores de s├¡mbolo no encontrado al intentar referenciar m├®todos que ya no existen en la nueva arquitectura modular.
- **Estado:** El proyecto Java ahora compila correctamente con `./gradlew compileJava`. 
- **Servidor:** Operativo y conectado a la base de datos (MySQL en Docker). Escuchando en el puerto 5000.

### App M├│vil (Flutter)
- **Restauraci├│n de `AuthService.dart`:** Se ha re-implementado el m├®todo `registrar` que faltaba en el repositorio. Este m├®todo es esencial para que la pantalla de registro pueda enviar los datos al servidor.
- **Correcci├│n de `SocketService.dart`:** A├▒adido el getter `isConectado` para facilitar la gesti├│n de conexiones desde los servicios.
- **Sincronizaci├│n de `JobApiService.dart`:** Corregido un error de sintaxis en el cierre de la clase que imped├¡a que el compilador detectara correctamente el m├®todo `finalizeTrabajo`.
- **An├ílisis:** Tras los cambios, `flutter analyze` ya no reporta errores cr├¡ticos de m├®todos no definidos en los servicios principales.

### Notas para la pr├│xima sesi├│n
- [ ] **Modificaci├│n de Perfil:** Implementar la edici├│n de datos personales (tel├®fono, direcci├│n, etc.) en `perfil_pantalla.dart`. Actualmente solo funciona el cambio de foto. Requiere crear una nueva acci├│n en el servidor Java (ej: `ACTUALIZAR_DATOS_USUARIO`).
- [ ] **Validaci├│n de Registro:** Probar el flujo de registro en la App m├│vil para confirmar que el nuevo m├®todo `registrar` se comunica correctamente con el `ProcesadorAutenticacion` del servidor.
- [ ] **Validaci├│n de Finalizaci├│n:** Verificar que un operario puede finalizar un trabajo sin errores de comunicaci├│n.
- [ ] **Continuar con AWS:** Una vez confirmada la estabilidad local, proceder con la configuraci├│n de la instancia EC2.

---


---

SESI├ôN 02/04/2026 ÔÇö Arquitectura "Smart Main", Sem├íforos y God Mode

### Objetivo de la sesi├│n
Consolidar el proyecto en un ├║nico c├│digo fuente capaz de operar en **Local (Docker)** y **Nube (AWS)** mediante un interruptor l├│gico, mejorando la UX con indicadores de estado y restaurando herramientas de test.

### 1. Arquitectura "Smart Switch"
Se ha eliminado la duplicidad de ramas para despliegue:
- **Centralizaci├│n (Java):** Se utiliza `GlobalConfig.java` como ├║nica fuente de verdad para IPs, puertos y credenciales RDS. El booleano `MODO_NUBE` propaga el cambio a todo el sistema (Servidor, Dashboard y Herramientas).
- **Reactividad (Flutter):** El archivo `.env` ahora distingue entre `ENVIRONMENT=LOCAL` y `ENVIRONMENT=NUBE`, inyectando la IP de la instancia EC2 o `10.0.2.2` seg├║n corresponda.

### 2. Sem├íforos de Conexi├│n (Indicadores de Estado)
Implementaci├│n de un sistema de feedback visual en tiempo real:
- **L├│gica de Colores:** L├│gica de Colores: Azul (Local), Gris (Iniciando AWS), Verde (Conexi├│n AWS Exitosa).
- **Asincron├¡a:**
  - En **JavaFX (Dashboard)**, se utiliza un `Thread` independiente con un timeout de 2s para no bloquear el inicio de la App mientras se hace el "ping" al socket.
  - En **Flutter (App)**, se implement├│ un `ping()` as├¡ncrono en `initState` que actualiza el estado del widget mediante un `ValueNotifier` o `setState`.

### 3. Recuperaci├│n y Modernizaci├│n de Herramientas (God Mode)
Se han rescatado del historial de Git (`c828544`) las herramientas de simulaci├│n borradas accidentalmente:
- **`TestPanel`**: Panel de control de bajo nivel para depurar el protocolo de red.
- **`Simulador E2E (God Mode)`**: Herramienta para simular flujo completo (Presupuesto -> Asignaci├│n -> Factura -> Pago) sin necesidad de m├║ltiples dispositivos.
- **Mejora:** Se han movido a la carpeta `com.fixfinder.TestPanel` y se han refactorizado para usar `GlobalConfig`. Ahora el "God Mode" tambi├®n funciona contra AWS.

### 4. Seguridad en el Sembrado (DbClean)
El seeder de la base de datos ahora es "entorno-consciente":
- Detecta si est├í en modo Nube. Si es as├¡, pide confirmaci├│n/advierte y limpia Firebase Storage.
- Si est├í en modo Local, omite la limpieza de la nube para proteger los archivos reales de producci├│n.

---


## ?? Comandos ├Ütiles para el Cierre de Proyecto

### ?? Generar EXE (Dashboard)
./gradlew jpackage (Esto generar├í el instalador en la carpeta uild/jpackage)

### ?? Generar APK (App M├│vil)
lutter build apk --release (Asegurarse de que .env est├® en MODO_NUBE)

### ?? Despliegue en AWS (EC2)
scp -i ffk.pem FIXFINDER/build/libs/FIXFINDER.jar ec2-user@15.217.56.66:~/


---

---

## [02/04/2026] - Sesi├│n: Gran Unificaci├│n "Smart Main" y Blindaje de Infraestructura

Estado: VERIFICADO (PENDIENTE DE RE-AUDITOR├ìA DE SEGURIDAD AL ARRANCAR)

### Logros (Lujo de Detalle)

#### 1. Arquitectura Smart Main
Se ha eliminado la dependencia de ramas (Local vs AWS). Ahora el proyecto reside en un solo **MAIN** inteligente:
*   **Java Core (GlobalConfig.java):** Nueva clase maestra que centraliza el interruptor MODO_NUBE. Gestiona din├ímicamente las URLs de JDBC para AWS RDS vs Docker Local y resuelve la IP del servidor de Sockets.
*   **Flutter Reactivo (.env):** Implementaci├│n de variables de entorno para que la App m├│vil resuelva su conexi├│n de forma "Plug & Play" sin tocar c├│digo Dart.

#### 2. Indicadores de Estado
Implementaci├│n de telemetr├¡a visual en las pantallas de Login:
*   **Logica de Colores:** 
    *   (AZUL) **Azul**: Modo LOCAL activo (Docker detectado).
    *   (GRIS) **Gris**: Intentando conectar (Cloud) o estado desconocido.
    *   (VERDE) **Verde**: Conexion exitosa con la instancia **EC2 de AWS**.
*   **JavaFX (AppDashboardPrincipal.java):** Refactorizacion del layout usando un StackPane para inyectar un indicardor circular en la esquina superior derecha. Implementacion de hilo asincono con timeout de 2s para el ping de red.
*   **Flutter (login_pantalla.dart):** Creacion del widget _ConnectionStatusDot y un Timer.periodic de 10 segundos que monitoriza el estado del servidor en segundo plano mediante el metodo ping() del SocketService.

#### 3. Blindaje de Datos
*   **DbClean.java (Seeder Seguro):** Se ha modificado el limpiador de base de datos para que sea "consciente" del entorno. Si detecta el modo NUBE, activa un prompt interactivo (Scanner(System.in)) que exige confirmar por consola antes de borrar la RDS de AWS.
*   **Protecci├│n de Firebase:** L├│gica de protecci├│n en limpiarFirebaseStorage() para evitar el borrado accidental del bucket en la nube durante pruebas locales.

#### 4. Restauraci├│n del God Mode
Recuperaci├│n total de las herramientas de simulaci├│n de bajo nivel:
*   **Paquetes:** Reubicaci├│n de la l├│gica de test en com.fixfinder.TestPanel.
*   **Colisi├│n de Nombres Solucionada:** El lanzador principal ahora es **LanzadorTestPanel.java**, evitando conflictos con clases del mismo nombre en subpaquetes.
*   **TestPanelController:** Actualizado para usar GlobalConfig.getServerIp(), permitiendo que el Tester de bajo nivel tambi├®n funcione contra AWS.
*   **Simulador E2E:** Recuperado el simulador completo para realizar flujos de "Un Solo Hombre" (Gerente/Operario/Cliente a la vez).

#### 5. Optimizaci├│n de Compilaci├│n
*   **Gradle Magic:** A├▒adida la tarea personalizada 
unTestPanel en uild.gradle que permite el lanzamiento limpio de las herramientas de test sin pasar por el modularismo estricto de JavaFX, resolviendo errores de visi├│n de clases con las librer├¡as de Firebase.

---

PR├ôXIMA SESI├ôN (URGENTE: AUDITOR├ìA Y GRADUACI├ôN AWS)

### PRIORIDAD CR├ìTICA: AUDITOR├ìA "RAYOS X"
Al retomar la sesi├│n, **lo primero** es verificar de nuevo que el comando 'git restore .' no haya deshecho los cambios en ConexionDB, DbClean y los controladores de FXML. Comprobar l├¡nea a l├¡nea que el Smart Switch sigue intacto.

### ­ƒº¬ 2. Certificaci├│n Local
* Lanzar el servidor y el dashboard en local.
* Confirmar sem├íforos **AZULES (AZUL)**.

### ├ó╦£ ├»┬©  3. Despliegue en Caliente (AWS Grad)
* Switchear MODO_NUBE = true.
* Generar FIXFINDER.jar (updated version).
* Subir a EC2 v├¡a SCP y reiniciar el servicio remoto.
* Confirmar sem├íforos **VERDES (VERDE)**.

### ­ƒôª 4. Generaci├│n de Entregables Release
* Build final de la APK release contra AWS.
* Empaquetado del Dashboard en instalador EXE (jpackage).

### ­ƒº╣ 5. Deuda T├®cnica y Refactorizaci├│n Pendiente (God Object a Trocear)
*   **Clase Afectada:** `TablaIncidencias.java` (Dashboard Desktop).
*   **Problema:** Violaci├│n flagrante del Principio de Responsabilidad ├Ünica (SRP). La clase es un "Objeto Dios" de casi 400 l├¡neas.
*   **Diagn├│stico:** Combina l├│gica de interfaz visual pura (VBox, controles) con l├│gica de negocio (filtrado estructurado interceptando eventos de botones), f├íbricas completas de celdas an├│nimas (`updateItem`) con HTML embebido, l├│gica generadora de avatares matem├íticos y lanzamiento manual de di├ílogos modales.
*   **Propuesta de Arquitectura:** 
    1.  Extraer l├│gica de filtrado a un `FiltrosIncidenciaController`.
    2.  Extraer los Cell Factories a sus propias clases `.java` (ej. `AvatarCellFactory`, `EstadoBadgeFactory`).
    3.  Aislar los utilitarios visuales gen├®ricos (`miniAvatar()`, `iconoCategoria()`) en un `UIComponentUtils.java` est├ítico para darles reutilizaci├│n en todo el proyecto.
    *(Requisito muy valioso para presentar en el apartado de "Mejoras Futuras" de la memoria).*

---

---

# TAREAS PENDIENTES, MEJORAS O PARA PROXIMAS VERSIONES

Esta secci├│n centraliza la hoja de ruta t├®cnica unificada, integrando deuda t├®cnica, mejoras de UI y nuevas funcionalidades cr├¡ticas de negocio.

## Nucleo de Negocio: Sistema de Presupuestos y Trabajo (REPLANTEO 07/04)

**Objetivo:** Implementar la segregaci├│n de responsabilidades y el ciclo de vida de rechazo/re-presupuesto.

- [x] **Logica de Rechazo de Presupuesto:**
  - Implementar acci├│n `RECHAZAR_PRESUPUESTO` en el Backend/DAO.
  - El estado del trabajo debe volver a `PENDIENTE` autom├íticamente.
  - **Reset de Descripci├│n:** Al rechazar, el campo `descripcion` del trabajo debe limpiarse de notas previas, volviendo a mostrar **├║nicamente el mensaje inicial del cliente**.
- [x] **Segregacion de Escritura (Gerente en Dashboard):**
  - El gerente ya no editar├í la descripci├│n general. Se habilitar├í un `TextField` / `TextArea` exclusivo para su propuesta econ├│mica/t├®cnica.
  - El servidor gestionar├í la visualizaci├│n de este bloque de forma independiente al mensaje original del cliente.
- [x] **Visibilidad del Cliente (App Movil):**
  - Implementar **bot├│n de "Rechazar Presupuesto"** en `detalle_trabajo_pantalla.dart`.
  - Mostrar claramente que el presupuesto fue rechazado y que la incidencia vuelve a esperar una propuesta t├®cnica.

## App Movil (Flutter)
- [ ] **Gesti├│n de Perfil:** Habilitar edici├│n de datos personales (tel├®fono, direcci├│n) en `perfil_pantalla.dart`. Requiere la implementaci├│n de la nueva acci├│n `ACTUALIZAR_DATOS_PERSONALES` en el servidor.
- [ ] **Registro de Usuarios:** Implementar pantalla y flujo de alta de nuevos clientes/operarios desde la App (`registro_pantalla.dart`).
- [ ] **Actualizaci├│n Real-Time (Push Real):** Reemplazar el polling de 15s por una escucha reactiva de eventos v├¡a socket.
    - **T├®cnica:** El `SocketService` debe exponer un `Stream` de eventos globales que el `TrabajoProvider` escuchar├í para a├▒adir/actualizar elementos sin recargar la vista.
- [x] **Refactor de Red:** Migrar los metodos del TrabajoProvider al sistema generico _socket.request() para eliminar boilerplate masivo (~200 lineas).
- [ ] **Tema de Colores (Centralizaci├│n):** Extraer todos los colores hardcodeados a `lib/theme/app_theme.dart` usando `ThemeData`. 
    - **T├®cnica:** Definir un `colorScheme` unificado y usar `Theme.of(context)` en todos los widgets para permitir cambios globales de dise├▒o.
- [ ] **Documentaci├│n Explicativa:** A├▒adir comentarios did├ícticos l├¡nea a l├¡nea en todas las clases (`lib/`), explicando el flujo de datos y la l├│gica t├®cnica para la defensa del proyecto.

## Dashboard JavaFX
- [x] **Bug Visual Foto Cliente:** Corregir carga de la foto de perfil en la ficha de detalle del cliente (DialogoFichaCliente.java).
- [ ] **Registro de Empresas:** Implementar el formulario de alta y la validaci├│n visual para nuevos gerentes/empresas desde el lanzador (la l├│gica de BCrypt ya est├í operativa en el backend).
- [ ] **Tematizaci├│n (CSS Centralizado):** Crear `src/main/resources/css/style.css` para unificar el dise├▒o (Dark Mode / Glassmorphism) y eliminar los `setStyle()` del c├│digo Java.
- [x] **Ajuste de UI:** Calibrar el ancho de las columnas (especialmente "Estado") para evitar solapamientos.
- [ ] **Diagrama de Dashboard:** Crear diagrama de componentes espec├¡fico para la arquitectura JavaFX modularizada.

- [ ] **Sistema de Broadcaster (Push):** Implementar una arquitectura de **Observer** en el servidor para notificar eventos en tiempo real.
    - **T├®cnica:** Clase `SesionManager` que mantenga un `Set<GestorConexion>` de hilos activos. Al procesar un cambio (nuevo trabajo/presupuesto), iterar y emitir el JSON de aviso a los clientes interesados.
- [x] **Optimizacion SQL (Problema N+1):** Refactorizar cargarRelaciones() en TrabajoDAOImpl.java para usar un unico LEFT JOIN masivo.
- [x] **Refactor TablaIncidencias:** Desmontar la "God Class" TablaIncidencias.java. Separar factorias de celdas, filtrado y dialogos en clases independientes (SRP).
- [x] **Micro-refactor Autenticacion:** Trocear procesarRegistro en metodos privados segregados por rol.
- [x] **Gestion de Timeouts:** Asegurar que acciones como VALORAR o CANCELAR devuelvan siempre la clave "mensaje" en el JSON.

---
_Bit├ícora t├®cnica consolidada. El sistema est├í preparado para la implementaci├│n de la l├│gica de presupuestos segregados._

# ESTRATEGIA DE DEFENSA ACADEMICA (PARA MEMORIA/EXAMEN)

**Punto Clave:** ┬┐Por qu├® usar un Protocolo Binario Propio sobre Sockets TCP en vez de una API REST HTTP?

1. **Eficiencia de Transmisi├│n (Ligereza):**
   - **HTTP:** Env├¡a entre 500 y 1000 bytes de cabeceras (headers) innecesarias por cada petici├│n (cookies, user-agent, etc.).
   - **FixFinder (TCP):** Protocolo `[4 bytes cabecera][Cuerpo JSON]`. Solo env├¡a los metadatos estrictamente necesarios. Es una arquitectura **"Low Overhead"**.

2. **Comunicaci├│n Bidireccional Real (Full-Duplex):**
   - A diferencia de HTTP (Request-Response), nuestro servidor puede "empujar" datos a la App sin que esta pregunte. Es el fundamento para las **notificaciones push instant├íneas**.

3. **Ejemplos Visuales del Flujo de Datos:**
   - **Nivel de Aplicaci├│n:** `{ "accion": "LOGIN", ... }` (Mapa de Dart).
   - **Nivel de Serializaci├│n:** `"{ "accion": "LOGIN", ... }"` (String JSON).
   - **Nivel de Red:** `[ 0, 0, 0, 56, 123, 34, 97, 99... ]` (Bytes brutos).
     - *Los primeros 4 bytes:* La longitud del mensaje.
     - *El resto:* El contenido del JSON codificado en UTF-8.

4. **Nivel de Ingenier├¡a:** Demuestra que el desarrollador entiende el modelo de referencia OSI (Capas de Red y Transporte) y no solo sabe consumir librer├¡as de alto nivel. Es un sistema m├ís complejo de implementar pero mucho m├ís optimizado.

5. **Codificaci├│n Binaria (Big-Endian y Potencias de 256):**
   - Para enviar la longitud, troceamos el n├║mero en 4 bytes. 
   - **Analog├¡a de Cajas (Base 256):** Como en un byte solo cabe del 0-255, cada posici├│n a la izquierda vale 256 veces m├ís que la anterior, igual que en decimal cada posici├│n vale 10 veces m├ís.
     - Byte 4: Unidades (x1)
     - Byte 3: Grupos de 256 (x256)
     - Byte 2: Grupos de 65.536 (x65.536)
     - Byte 1: Grupos de 16,7 millones (x16,7M)
   - Esto permite que con solo 4 bytes se pueda representar un n├║mero de longitud de hasta 4GB, garantizando que el servidor (Java) y la app (Flutter) siempre sepan exactamente cu├íntos datos deben leer para evitar la fragmentaci├│n de paquetes en TCP.

---



---

## ­ƒöº SESI├ôN 08/04/2026 (Tarde) ÔÇö Gran Refactorizaci├│n de Conexiones y Multipresupuesto
### Objetivo
Finalizar la l├│gica de multipresupuesto (1:N) y resolver bloqueos cr├¡ticos en el servidor y el dashboard que imped├¡an el env├¡o de ofertas.

### Problemas Detectados y Soluciones Quir├║rgicas
1. **Bloqueo del Servidor (Deadlock de BD):** 
   - **Causa:** `ConexionDB` usaba una conexi├│n est├ítica compartida. Al realizar m├║ltiples consultas r├ípidas (refresco de trabajos + GET_EMPRESA + LISTAR_PRESUPUESTOS), los hilos se pisaban y bloqueaban el socket.
   - **Soluci├│n:** Refactorizado `ConexionDB` a un patr├│n **ThreadLocal<Connection>**. Ahora cada hilo del servidor tiene su propia conexi├│n aislada. Se a├▒adi├│ `ConexionDB.cerrarConexion()` en el bloque `finally` de `GestorConexion` para limpieza total tras cada mensaje.

2. **Corrupci├│n de Mensajes en Socket (Dashboard):**
   - **Causa:** `ClienteSocket.enviar()` no estaba sincronizado. Si el dashboard ped├¡a refrescar datos mientras se enviaba un presupuesto, los JSON se mezclaban en el buffer, enviando basura al servidor.
   - **Soluci├│n:** Se marc├│ el m├®todo `enviar` como **synchronized**. Se a├▒adieron logs de salida en consola para trazabilidad.

3. **Mismatch de Campos (notas vs nuevaDescripcion):**
   - **Causa:** El servidor esperaba la clave `"notas"` pero el cliente enviaba `"nuevaDescripcion"`.
   - **Soluci├│n:** Estandarizaci├│n total al nombre de campo **`notas`** en el protocolo `CREAR_PRESUPUESTO`.

4. **Fallo Estructural de Base de Datos:**
   - **Causa:** La tabla `presupuesto` no ten├¡a f├¡sicamente la columna `notas`.
   - **Soluci├│n:** Ejecutado `ALTER TABLE presupuesto ADD COLUMN notas TEXT;`. Actualizado `ESQUEMA_BD.sql` para persistencia.

5. **L├│gica de Multipresupuesto (1:N):**
   - Implementado en `PresupuestoServiceImpl.aceptarPresupuesto`:
     - El presupuesto elegido pasa a `ACEPTADO`. El resto del mismo trabajo pasan a `RECHAZADO`.
     - Las notas del presupuesto aceptado se **inyectan** (con formato decorativo) al final de la descripci├│n del trabajo.
     - El trabajo pasa a estado `ACEPTADO`.

### Estado Actual: LISTO PARA VALIDACI├ôN
- El servidor es ahora robusto y hilos-seguro.
- El Dashboard ya no se queda "congelado" tras r├ífagas de mensajes.
- Se ha verificado que `gerente.a@levante.com` est├í vinculado correctamente a la Empresa ID:2 en este entorno.

---

## ANEXO TECNICO PARA LA MEMORIA Y DEFENSA: HILOS, CONCURRENCIA Y EL "SINDROME DEL EMBUDO TCP"

*(Copia y pega o adapta estos conceptos para la secci├│n de "Problemas Encontrados y Soluciones" de tu memoria acad├®mica, o ├║salos en las preguntas del Tribunal).*

### ┬┐Por qu├® fall├│ el Socket justo despu├®s de implementar el Multipresupuesto? (Causa Ra├¡z)
Es l├│gico pensar que el problema de conexiones simult├íneas no tiene relaci├│n directa con el Multipresupuesto, ya que el Dashboard lanzaba r├ífagas desde antes. Lo que deton├│ el fallo tras el refactor fue **el tama├▒o exponencial del Payload JSON (Efecto Multiplicador en Red).**

1. **Antes del Multipresupuesto:** La petici├│n `LISTAR_TRABAJOS` devolv├¡a un listado plano. Sin arrays de presupuestos vinculados. El JSON era extremadamente ligero (unos pocos Kilobytes). Al ejecutarse `salida.flush()` en Java, ese diminuto JSON se engull├¡a al instante por el *Buffer de Red TCP* del sistema operativo. El m├®todo terminaba en 1 milisegundo, y el hilo del servidor quedaba libre enseguida para el pr├│ximo bucle de lectura.
2. **Despu├®s del Multipresupuesto:** Para soportar que la App compare ofertas in-situ, tuvimos que "hidratar" cada objeto `Trabajo` con su colecci├│n lista de `List<Presupuesto>`. Peor a├║n, a├▒adimos el campo de texto libre `notas` (la descripci├│n t├®cnica extensa). 
3. **La Explosi├│n de Bytes:** Si un `LISTAR_TRABAJOS` trae 50 incidencias, y la mitad tiene 2 o 3 ofertas de diferentes empresas, cada una con un texto de 500 caracteres, un DTO en JSON puro pasa de pesar 5 KB a pesar varios Megabytes.
4. **El Bloqueo (TCP Window Full):** Al forzar un env├¡o tit├ínico por sockets (`salida.write` seguido de `flush()`), si el Dashboard (receptor) no est├í leyendo ese texto kilom├®trico sin interrupci├│n y a suma velocidad, el Buffer subyacente TCP se empantana. Al llenarse, `flush()` sufre un bloqueo intr├¡nseco de red. Se queda congelado ("Atascamiento") aguardando que el cliente vac├¡e mil├¡metros de cauce. Al colgar el hilo con esta retenci├│n, el Servidor detiene toda monitorizaci├│n de nuevas conexiones hasta purgar los Bytes de salida.

### Glosario de Concurrencia (┬┐D├│nde usamos Multithreading en el proyecto?)
Este MVP de FixFinder destaca brutalmente de cara a tu tribunal por el uso nativo de sockets y la abstracci├│n as├¡ncrona robusta. Anota la enumeraci├│n expl├¡cita para la Memoria:

**EN EL SERVIDOR (Backend Java 21 - Non Spring):**
1. **El Hilo de Escucha (Dispatcher Listener):** Integrado en `ServidorCentral`. Su encasillamiento l├│gico es orbitar un bucle infinito sobre la operaci├│n bloqueante de IO `serverSocket.accept()`. Nace y muere esperando aperturas del Socket, con derivaci├│n reactiva al instante a un nuevo hilo Worker.
2. **Los Workers Concurrentes (`GestorConexion`):** Un pool/hilos instanciados individualmente. Al conectarse un gerente en PC, o cinco dispositivos Flutter, el Backend los abastece mediante cinco hilos vivos (Workers) que interpretan simult├íneamente (Runnable) los JSON recibidos del canal InputStream.
3. **Aislamiento Sem├íntico de BBDD (`ConexionDB` / `ThreadLocal`):** Mitigante de *Deadlocks*. Puesto que decenas de workers atacan a MySQL transaccionalmente, rehusamos inyectarles una ├║nica clase Java Connection est├ítica (crasheos y traslapes JDBC). Se instrument├│ `ThreadLocal<Connection>`, forzando as├¡ un "Scope" privado y un cauce SQL ajeno por cada hilo en transcurso.

**EN EL DASHBOARD (Cliente JavaFX - Arquitectura MVC):**
4. **Aislamiento del Hilo Gr├ífico (Platform Thread UI):** Todo SDK de Renderizado (sea SWING, FX, o QT) proh├¡be intrusi├│n forzada del CPU o llamadas de red prolongadas ("Freeze Frame"). Todas tus comunicaciones `solicitar...` subrogan a `CompletableFuture` (Future/Promesa de sub-hilo). Los desenlaces visuales terminan deriv├índose de nuevo a la UI ├║nicamente envolvi├®ndolos en `Platform.runLater(() -> {})`. 
5. **Sem├íforos Transversos:** Hemos programado sub-hilos "demonio" recurrentes atados con `sleep(ms)` intermedios que lanzan ping transparentes en las pantallas iniciales, detectando los virajes del entorno AWS a LOCAL.
6. **Cruce Estructural de Output `enviar()`:** Subsanado con `synchronized()`. Se obligan secuencias unitarias ante la coincidencia del hilo de UI y los hilos en backgound pretendiendo "incrustar" peticiones por el canal `DataOutputStream` id├®ntico. Un patr├│n Mutex elemental en Sockets.

**EN LA APP M├ôVIL (Flutter / Dart):**
7. Dart maneja una filosof├¡a asim├®trica: usa **Isolates** y el Event Loop de asincron├¡a (`async / await`). Opera virtualmente sobre Single-Thread sin necesidad empedernida de crear sub-hilos de sistema operativo para descargar Sockets, gestion├índolo nativamente con multiplexaci├│n I/O no bloqueante, maximizando la eficiencia de bater├¡a e interfaz Android sin "saltos" (Jank/stutters).

### La Escalabilidad de Nuestra Soluci├│n (y por qu├® evitaremos que vuelva a pasar)
Es completamente l├¡cito preguntarse: *┬┐Si el sistema peta ahora mismo con apenas unas peticiones de prueba, se colapsar├í el d├¡a de ma├▒ana si tenemos cientos de presupuestos reales?*

La respuesta es NO, porque no peta por "falta de potencia", sino por "falta de coordinaci├│n de tuber├¡as".
Nuestra soluci├│n implementa un **"Hilo Lector Avaro"** combinado con Promesas As├¡ncronas (TxID). Esto separa radicalmente el acto de "Ingerir" datos de la red del acto de "Renderizar" la interfaz. 

Con esto, no importa cu├ín colosal sea el JSON resultante del Multipresupuesto: el Hilo Lector Avaro desv├¡a los Bytes desde el buffer TCP de tu sistema operativo a la memoria de tu programa en mili-segundos, despejando la tuber├¡a (ventana TCP) de inmediato. El Servidor jam├ís detecta atascos y su `salida.flush()` empuja los datos sin trabas. Y si en el futuro se hablara de millones de registros, el proyecto est├í preparado estructuralmente para aplicar **Paginaci├│n** (ej: pedir 20 registros, que son instant├íneos, y cargar m├ís haciendo scroll).



## [02/04/2026] - Sesi├│n: Gran Unificaci├│n "Smart Main" y Blindaje de Infraestructura

Estado: VERIFICADO (PENDIENTE DE RE-AUDITOR├ìA DE SEGURIDAD AL ARRANCAR)

### Logros (Lujo de Detalle)

#### 1. Arquitectura Smart Main
Se ha eliminado la dependencia de ramas (Local vs AWS). Ahora el proyecto reside en un solo **MAIN** inteligente:
*   **Java Core (GlobalConfig.java):** Nueva clase maestra que centraliza el interruptor MODO_NUBE. Gestiona din├ímicamente las URLs de JDBC para AWS RDS vs Docker Local y resuelve la IP del servidor de Sockets.
*   **Flutter Reactivo (.env):** Implementaci├│n de variables de entorno para que la App m├│vil resuelva su conexi├│n de forma "Plug & Play" sin tocar c├│digo Dart.

#### 2. Indicadores de Estado
Implementaci├│n de telemetr├¡a visual en las pantallas de Login:
*   **L├│gica de Colores:** 
    *   (AZUL) **Azul**: Modo LOCAL activo (Docker detectado).
    *   ├░┼©ÔÇØ╦£ **Gris**: Intentando conectar (Cloud) o estado desconocido.
    *   (VERDE) **Verde**: Conexi├│n exitosa con la instancia **EC2 de AWS**.
*   **JavaFX (AppDashboardPrincipal.java):** Refactorizaci├│n del layout usando un StackPane para inyectar un indicardor circular en la esquina superior derecha. Implementaci├│n de hilo as├¡ncrono con timeout de 2s para el ping de red.
*   **Flutter (login_pantalla.dart):** Creaci├│n del widget _ConnectionStatusDot y un Timer.periodic de 10 segundos que monitoriza el estado del servidor en segundo plano mediante el m├®todo ping() del SocketService.

#### 3. Blindaje de Datos
*   **DbClean.java (Seeder Seguro):** Se ha modificado el limpiador de base de datos para que sea "consciente" del entorno. Si detecta el modo NUBE, activa un prompt interactivo (Scanner(System.in)) que exige confirmar por consola antes de borrar la RDS de AWS.
*   **Protecci├│n de Firebase:** L├│gica de protecci├│n en limpiarFirebaseStorage() para evitar el borrado accidental del bucket en la nube durante pruebas locales.

#### 4. Restauraci├│n del God Mode
Recuperaci├│n total de las herramientas de simulaci├│n de bajo nivel:
*   **Paquetes:** Reubicaci├│n de la l├│gica de test en com.fixfinder.TestPanel.
*   **Colisi├│n de Nombres Solucionada:** El lanzador principal ahora es **LanzadorTestPanel.java**, evitando conflictos con clases del mismo nombre en subpaquetes.
*   **TestPanelController:** Actualizado para usar GlobalConfig.getServerIp(), permitiendo que el Tester de bajo nivel tambi├®n funcione contra AWS.
*   **Simulador E2E:** Recuperado el simulador completo para realizar flujos de "Un Solo Hombre" (Gerente/Operario/Cliente a la vez).

#### 5. Optimizaci├│n de Compilaci├│n
*   **Gradle Magic:** A├▒adida la tarea personalizada 
unTestPanel en  uild.gradle que permite el lanzamiento limpio de las herramientas de test sin pasar por el modularismo estricto de JavaFX, resolviendo errores de visi├│n de clases con las librer├¡as de Firebase.

---

PR├ôXIMA SESI├ôN (URGENTE: AUDITOR├ìA Y GRADUACI├ôN AWS)

### PRIORIDAD CR├ìTICA: AUDITOR├ìA "RAYOS X"
Al retomar la sesi├│n, **lo primero** es verificar de nuevo que el comando 'git restore .' no haya deshecho los cambios en ConexionDB, DbClean y los controladores de FXML. Comprobar l├¡nea a l├¡nea que el Smart Switch sigue intacto.

### ­ƒº¬ 2. Certificaci├│n Local
* Lanzar el servidor y el dashboard en local.
* Confirmar sem├íforos **AZULES (AZUL)**.

### ├ó╦£ ├»┬©  3. Despliegue en Caliente (AWS Grad)
* Switchear MODO_NUBE = true.
* Generar FIXFINDER.jar (updated version).
* Subir a EC2 v├¡a SCP y reiniciar el servicio remoto.
* Confirmar sem├íforos **VERDES (VERDE)**.

### ­ƒôª 4. Generaci├│n de Entregables Release
* Build final de la APK release contra AWS.
* Empaquetado del Dashboard en instalador EXE (jpackage).

### ­ƒº╣ 5. Deuda T├®cnica y Refactorizaci├│n Pendiente (God Object a Trocear)
*   **Clase Afectada:** `TablaIncidencias.java` (Dashboard Desktop).
*   **Problema:** Violaci├│n flagrante del Principio de Responsabilidad ├Ünica (SRP). La clase es un "Objeto Dios" de casi 400 l├¡neas.
*   **Diagn├│stico:** Combina l├│gica de interfaz visual pura (VBox, controles) con l├│gica de negocio (filtrado estructurado interceptando eventos de botones), f├íbricas completas de celdas an├│nimas (`updateItem`) con HTML embebido, l├│gica generadora de avatares matem├íticos y lanzamiento manual de di├ílogos modales.
*   **Propuesta de Arquitectura:** 
    1.  Extraer l├│gica de filtrado a un `FiltrosIncidenciaController`.
    2.  Extraer los Cell Factories a sus propias clases `.java` (ej. `AvatarCellFactory`, `EstadoBadgeFactory`).
    3.  Aislar los utilitarios visuales gen├®ricos (`miniAvatar()`, `iconoCategoria()`) en un `UIComponentUtils.java` est├ítico para darles reutilizaci├│n en todo el proyecto.
    *(Requisito muy valioso para presentar en el apartado de "Mejoras Futuras" de la memoria).*

---

---

# TAREAS PENDIENTES, MEJORAS O PARA PR├ôXIMAS VERSIONES

Esta secci├│n centraliza la hoja de ruta t├®cnica unificada, integrando deuda t├®cnica, mejoras de UI y nuevas funcionalidades cr├¡ticas de negocio.

## Nucleo de Negocio: Sistema de Presupuestos y Trabajo (REPLANTEO 07/04)

**Objetivo:** Implementar la segregaci├│n de responsabilidades y el ciclo de vida de rechazo/re-presupuesto.

- [x] **Logica de Rechazo de Presupuesto:**
  - Implementar acci├│n `RECHAZAR_PRESUPUESTO` en el Backend/DAO.
  - El estado del trabajo debe volver a `PENDIENTE` autom├íticamente.
  - **Reset de Descripci├│n:** Al rechazar, el campo `descripcion` del trabajo debe limpiarse de notas previas, volviendo a mostrar **├║nicamente el mensaje inicial del cliente**.
- [x] **Segregacion de Escritura (Gerente en Dashboard):**
  - El gerente ya no editar├í la descripci├│n general. Se habilitar├í un `TextField` / `TextArea` exclusivo para su propuesta econ├│mica/t├®cnica.
  - El servidor gestionar├í la visualizaci├│n de este bloque de forma independiente al mensaje original del cliente.
- [x] **Visibilidad del Cliente (App Movil):**
  - Implementar **bot├│n de "Rechazar Presupuesto"** en `detalle_trabajo_pantalla.dart`.
  - Mostrar claramente que el presupuesto fue rechazado y que la incidencia vuelve a esperar una propuesta t├®cnica.

## App M├│vil (Flutter)
- [ ] **Gesti├│n de Perfil:** Habilitar la modificaci├│n de datos personales (tel├®fono, direcci├│n) en `perfil_pantalla.dart` como cliente.
- [ ] **Registro de Usuarios:** Implementar pantalla y flujo de alta de nuevos clientes desde la App.
- [ ] **Actualizaci├│n Real-Time (Broadcaster):** Implementar la escucha de eventos del Broadcaster para refrescar datos autom├íticamente sin polling.
- [x] **Refactor de Red:** Migrar los m├®todos del `TrabajoProvider` al sistema gen├®rico `_socket.request()` para eliminar boilerplate masivo (~200 l├¡neas).
- [ ] **Tema de Colores:** Extraer todos los colores hardcodeados de `main.dart` a un fichero `lib/theme/app_theme.dart` (ThemeData) para centralizar el estilo de la App.
- [ ] **Documentaci├│n Explicativa:** A├▒adir comentarios did├ícticos l├¡nea a l├¡nea en todas las clases (`lib/`), explicando el flujo de datos y la l├│gica t├®cnica para la defensa del proyecto. **REQUISITO:** Realizarlo con extremo cuidado, asegurando la integridad total de la l├│gica funcional (no eliminar m├®todos de soporte como `_sanitizarURL` ni bloques visuales de los `AppBar`).

## Dashboard JavaFX
- [x] **Bug Visual Foto Cliente:** Corregir carga de la foto de perfil en la ficha de detalle del cliente (`DialogoFichaCliente.java`).
- [ ] **Registro de Empresas:** Formulario de alta para nuevas empresas (con su correspondiente operario gerente) desde el lanzador del Dashboard.
- [ ] **Tema para Dashboard (CSS):** Centralizar estilos en un archivo `style.css` y eliminar el uso de `setStyle` en el c├│digo Java.
- [ ] **Actualizaci├│n Real-Time (Broadcaster):** Implementar la escucha de eventos del Broadcaster en el Dashboard para refrescar la tabla de incidencias autom├íticamente.
- [x] **Ajuste de UI:** Calibrar el ancho de las columnas (especialmente "Estado") para evitar solapamientos.
- [ ] **Diagrama de Dashboard:** Crear diagrama de componentes espec├¡fico para la arquitectura JavaFX modularizada.

## Backend y Deuda T├®cnica (Java)
- [ ] **Sistema Broadcaster:** Implementar la l├│gica en el servidor para notificar eventos en tiempo real a la App y al Dashboard.
- [x] **Optimizaci├│n SQL (Problema N+1):** Refactorizar `cargarRelaciones()` en `TrabajoDAOImpl.java` para usar un ├║nico `LEFT JOIN` masivo.
- [x] **Refactor TablaIncidencias:** Desmontar la "God Class" `TablaIncidencias.java`. Separar factor├¡as de celdas, filtrado y di├ílogos en clases independientes (SRP).
- [x] **Micro-refactor Autenticacion:** Trocear `procesarRegistro` en m├®todos privados segregados por rol.
- [x] **Gestion de Timeouts:** Asegurar que acciones como `VALORAR` o `CANCELAR` devuelvan siempre la clave `"mensaje"` en el JSON para evitar interrupciones de flujo en la App.

---
_Bit├ícora t├®cnica consolidada. El sistema est├í preparado para la implementaci├│n de la l├│gica de presupuestos segregados._

# ESTRATEGIA DE DEFENSA ACAD├ëMICA (PARA MEMORIA/EXAMEN)

**Punto Clave:** ┬┐Por qu├® usar un Protocolo Binario Propio sobre Sockets TCP en vez de una API REST HTTP?

1. **Eficiencia de Transmisi├│n (Ligereza):**
   - **HTTP:** Env├¡a entre 500 y 1000 bytes de cabeceras (headers) innecesarias por cada petici├│n (cookies, user-agent, etc.).
   - **FixFinder (TCP):** Protocolo `[4 bytes cabecera][Cuerpo JSON]`. Solo env├¡a los metadatos estrictamente necesarios. Es una arquitectura **"Low Overhead"**.

2. **Comunicaci├│n Bidireccional Real (Full-Duplex):**
   - A diferencia de HTTP (Request-Response), nuestro servidor puede "empujar" datos a la App sin que esta pregunte. Es el fundamento para las **notificaciones push instant├íneas**.

3. **Ejemplos Visuales del Flujo de Datos:**
   - **Nivel de Aplicaci├│n:** `{ "accion": "LOGIN", ... }` (Mapa de Dart).
   - **Nivel de Serializaci├│n:** `"{ "accion": "LOGIN", ... }"` (String JSON).
   - **Nivel de Red:** `[ 0, 0, 0, 56, 123, 34, 97, 99... ]` (Bytes brutos).
     - *Los primeros 4 bytes:* La longitud del mensaje.
     - *El resto:* El contenido del JSON codificado en UTF-8.

4. **Nivel de Ingenier├¡a:** Demuestra que el desarrollador entiende el modelo de referencia OSI (Capas de Red y Transporte) y no solo sabe consumir librer├¡as de alto nivel. Es un sistema m├ís complejo de implementar pero mucho m├ís optimizado.

5. **Codificaci├│n Binaria (Big-Endian y Potencias de 256):**
   - Para enviar la longitud, troceamos el n├║mero en 4 bytes. 
   - **Analog├¡a de Cajas (Base 256):** Como en un byte solo cabe del 0-255, cada posici├│n a la izquierda vale 256 veces m├ís que la anterior, igual que en decimal cada posici├│n vale 10 veces m├ís.
     - Byte 4: Unidades (x1)
     - Byte 3: Grupos de 256 (x256)
     - Byte 2: Grupos de 65.536 (x65.536)
     - Byte 1: Grupos de 16,7 millones (x16,7M)
   - Esto permite que con solo 4 bytes se pueda representar un n├║mero de longitud de hasta 4GB, garantizando que el servidor (Java) y la app (Flutter) siempre sepan exactamente cu├íntos datos deben leer para evitar la fragmentaci├│n de paquetes en TCP.

---



---

## ­ƒöº SESI├ôN 08/04/2026 (Tarde) ÔÇö Gran Refactorizaci├│n de Conexiones y Multipresupuesto
### Objetivo
Finalizar la l├│gica de multipresupuesto (1:N) y resolver bloqueos cr├¡ticos en el servidor y el dashboard que imped├¡an el env├¡o de ofertas.

### Problemas Detectados y Soluciones Quir├║rgicas
1. **Bloqueo del Servidor (Deadlock de BD):** 
   - **Causa:** `ConexionDB` usaba una conexi├│n est├ítica compartida. Al realizar m├║ltiples consultas r├ípidas (refresco de trabajos + GET_EMPRESA + LISTAR_PRESUPUESTOS), los hilos se pisaban y bloqueaban el socket.
   - **Soluci├│n:** Refactorizado `ConexionDB` a un patr├│n **ThreadLocal<Connection>**. Ahora cada hilo del servidor tiene su propia conexi├│n aislada. Se a├▒adi├│ `ConexionDB.cerrarConexion()` en el bloque `finally` de `GestorConexion` para limpieza total tras cada mensaje.

2. **Corrupci├│n de Mensajes en Socket (Dashboard):**
   - **Causa:** `ClienteSocket.enviar()` no estaba sincronizado. Si el dashboard ped├¡a refrescar datos mientras se enviaba un presupuesto, los JSON se mezclaban en el buffer, enviando basura al servidor.
   - **Soluci├│n:** Se marc├│ el m├®todo `enviar` como **synchronized**. Se a├▒adieron logs de salida en consola para trazabilidad.

3. **Mismatch de Campos (notas vs nuevaDescripcion):**
   - **Causa:** El servidor esperaba la clave `"notas"` pero el cliente enviaba `"nuevaDescripcion"`.
   - **Soluci├│n:** Estandarizaci├│n total al nombre de campo **`notas`** en el protocolo `CREAR_PRESUPUESTO`.

4. **Fallo Estructural de Base de Datos:**
   - **Causa:** La tabla `presupuesto` no ten├¡a f├¡sicamente la columna `notas`.
   - **Soluci├│n:** Ejecutado `ALTER TABLE presupuesto ADD COLUMN notas TEXT;`. Actualizado `ESQUEMA_BD.sql` para persistencia.

5. **L├│gica de Multipresupuesto (1:N):**
   - Implementado en `PresupuestoServiceImpl.aceptarPresupuesto`:
     - El presupuesto elegido pasa a `ACEPTADO`. El resto del mismo trabajo pasan a `RECHAZADO`.
     - Las notas del presupuesto aceptado se **inyectan** (con formato decorativo) al final de la descripci├│n del trabajo.
     - El trabajo pasa a estado `ACEPTADO`.

### Estado Actual: LISTO PARA VALIDACI├ôN
- El servidor es ahora robusto y hilos-seguro.
- El Dashboard ya no se queda "congelado" tras r├ífagas de mensajes.
- Se ha verificado que `gerente.a@levante.com` est├í vinculado correctamente a la Empresa ID:2 en este entorno.

---

### ­ƒÅü FINAL DE SESI├ôN ÔÇö DESCUBRIMIENTO CR├ìTICO
- **Diagn├│stico Final:** El servidor se queda bloqueado en la fase de `salida.flush()` tras enviar respuestas grandes (como la lista de operarios). Al estar el buffer de red lleno y el cliente enviando m├ís peticiones sin haber procesado del todo las anteriores, se produce un "atascamiento" en el socket. El servidor no puede volver al inicio del bucle para leer el `CREAR_PRESUPUESTO` porque sigue intentando vaciar la respuesta anterior.
- **Pr├│ximos Pasos (Ma├▒ana):** 
  1. Optimizar los procesadores para no enviar datos innecesarios en r├ífaga.
  2. Implementar un sistema de confirmaci├│n de lectura en el Dashboard.
  3. Revisar el sistema de hilos del `ClienteSocket` para asegurar m├íxima fluidez en la recepci├│n.

---

## ANEXO TECNICO PARA LA MEMORIA Y DEFENSA: HILOS, CONCURRENCIA Y EL "SINDROME DEL EMBUDO TCP"

*(Copia y pega o adapta estos conceptos para la secci├│n de "Problemas Encontrados y Soluciones" de tu memoria acad├®mica, o ├║salos en las preguntas del Tribunal).*

### ┬┐Por qu├® fall├│ el Socket justo despu├®s de implementar el Multipresupuesto? (Causa Ra├¡z)
Es l├│gico pensar que el problema de conexiones simult├íneas no tiene relaci├│n directa con el Multipresupuesto, ya que el Dashboard lanzaba r├ífagas desde antes. Lo que deton├│ el fallo tras el refactor fue **el tama├▒o exponencial del Payload JSON (Efecto Multiplicador en Red).**

1. **Antes del Multipresupuesto:** La petici├│n `LISTAR_TRABAJOS` devolv├¡a un listado plano. Sin arrays de presupuestos vinculados. El JSON era extremadamente ligero (unos pocos Kilobytes). Al ejecutarse `salida.flush()` en Java, ese diminuto JSON se engull├¡a al instante por el *Buffer de Red TCP* del sistema operativo. El m├®todo terminaba en 1 milisegundo, y el hilo del servidor quedaba libre enseguida para el pr├│ximo bucle de lectura.
2. **Despu├®s del Multipresupuesto:** Para soportar que la App compare ofertas in-situ, tuvimos que "hidratar" cada objeto `Trabajo` con su colecci├│n lista de `List<Presupuesto>`. Peor a├║n, a├▒adimos el campo de texto libre `notas` (la descripci├│n t├®cnica extensa). 
3. **La Explosi├│n de Bytes:** Si un `LISTAR_TRABAJOS` trae 50 incidencias, y la mitad tiene 2 o 3 ofertas de diferentes empresas, cada una con un texto de 500 caracteres, un DTO en JSON puro pasa de pesar 5 KB a pesar varios Megabytes.
4. **El Bloqueo (TCP Window Full):** Al forzar un env├¡o tit├ínico por sockets (`salida.write` seguido de `flush()`), si el Dashboard (receptor) no est├í leyendo ese texto kilom├®trico sin interrupci├│n y a suma velocidad, el Buffer subyacente TCP se empantana. Al llenarse, `flush()` sufre un bloqueo intr├¡nseco de red. Se queda congelado ("Atascamiento") aguardando que el cliente vac├¡e mil├¡metros de cauce. Al colgar el hilo con esta retenci├│n, el Servidor detiene toda monitorizaci├│n de nuevas conexiones hasta purgar los Bytes de salida.

### Glosario de Concurrencia (┬┐D├│nde usamos Multithreading en el proyecto?)
Este MVP de FixFinder destaca brutalmente de cara a tu tribunal por el uso nativo de sockets y la abstracci├│n as├¡ncrona robusta. Anota la enumeraci├│n expl├¡cita para la Memoria:

**EN EL SERVIDOR (Backend Java 21 - Non Spring):**
1. **El Hilo de Escucha (Dispatcher Listener):** Integrado en `ServidorCentral`. Su encasillamiento l├│gico es orbitar un bucle infinito sobre la operaci├│n bloqueante de IO `serverSocket.accept()`. Nace y muere esperando aperturas del Socket, con derivaci├│n reactiva al instante a un nuevo hilo Worker.
2. **Los Workers Concurrentes (`GestorConexion`):** Un pool/hilos instanciados individualmente. Al conectarse un gerente en PC, o cinco dispositivos Flutter, el Backend los abastece mediante cinco hilos vivos (Workers) que interpretan simult├íneamente (Runnable) los JSON recibidos del canal InputStream.
3. **Aislamiento Sem├íntico de BBDD (`ConexionDB` / `ThreadLocal`):** Mitigante de *Deadlocks*. Puesto que decenas de workers atacan a MySQL transaccionalmente, rehusamos inyectarles una ├║nica clase Java Connection est├ítica (crasheos y traslapes JDBC). Se instrument├│ `ThreadLocal<Connection>`, forzando as├¡ un "Scope" privado y un cauce SQL ajeno por cada hilo en transcurso.

**EN EL DASHBOARD (Cliente JavaFX - Arquitectura MVC):**
4. **Aislamiento del Hilo Gr├ífico (Platform Thread UI):** Todo SDK de Renderizado (sea SWING, FX, o QT) proh├¡be intrusi├│n forzada del CPU o llamadas de red prolongadas ("Freeze Frame"). Todas tus comunicaciones `solicitar...` subrogan a `CompletableFuture` (Future/Promesa de sub-hilo). Los desenlaces visuales terminan deriv├índose de nuevo a la UI ├║nicamente envolvi├®ndolos en `Platform.runLater(() -> {})`. 
5. **Sem├íforos Transversos:** Hemos programado sub-hilos "demonio" recurrentes atados con `sleep(ms)` intermedios que lanzan ping transparentes en las pantallas iniciales, detectando los virajes del entorno AWS a LOCAL.
6. **Cruce Estructural de Output `enviar()`:** Subsanado con `synchronized()`. Se obligan secuencias unitarias ante la coincidencia del hilo de UI y los hilos en backgound pretendiendo "incrustar" peticiones por el canal `DataOutputStream` id├®ntico. Un patr├│n Mutex elemental en Sockets.

**EN LA APP M├ôVIL (Flutter / Dart):**
7. Dart maneja una filosof├¡a asim├®trica: usa **Isolates** y el Event Loop de asincron├¡a (`async / await`). Opera virtualmente sobre Single-Thread sin necesidad empedernida de crear sub-hilos de sistema operativo para descargar Sockets, gestion├índolo nativamente con multiplexaci├│n I/O no bloqueante, maximizando la eficiencia de bater├¡a e interfaz Android sin "saltos" (Jank/stutters).

### La Escalabilidad de Nuestra Soluci├│n (y por qu├® evitaremos que vuelva a pasar)
Es completamente l├¡cito preguntarse: *┬┐Si el sistema peta ahora mismo con apenas unas peticiones de prueba, se colapsar├í el d├¡a de ma├▒ana si tenemos cientos de presupuestos reales?*

La respuesta es NO, porque no peta por "falta de potencia", sino por "falta de coordinaci├│n de tuber├¡as".
Nuestra soluci├│n implementa un **"Hilo Lector Avaro"** combinado con Promesas As├¡ncronas (TxID). Esto separa radicalmente el acto de "Ingerir" datos de la red del acto de "Renderizar" la interfaz. 

Con esto, no importa cu├ín colosal sea el JSON resultante del Multipresupuesto: el Hilo Lector Avaro desv├¡a los Bytes desde el buffer TCP de tu sistema operativo a la memoria de tu programa en mili-segundos, despejando la tuber├¡a (ventana TCP) de inmediato. El Servidor jam├ís detecta atascos y su `salida.flush()` empuja los datos sin trabas. Y si en el futuro se hablara de millones de registros, el proyecto est├í preparado estructuralmente para aplicar **Paginaci├│n** (ej: pedir 20 registros, que son instant├íneos, y cargar m├ís haciendo scroll).


---

## ­ƒöº SESI├ôN 08/04/2026 (Tarde) ÔÇö Gran Refactorizaci├│n de Conexiones y Multipresupuesto
### Objetivo
Finalizar la l├│gica de multipresupuesto (1:N) y resolver bloqueos cr├¡ticos en el servidor y el dashboard que imped├¡an el env├¡o de ofertas.

### Problemas Detectados y Soluciones Quir├║rgicas
1. **Bloqueo del Servidor (Deadlock de BD):** 
   - **Causa:** `ConexionDB` usaba una conexi├│n est├ítica compartida. Al realizar m├║ltiples consultas r├ípidas (refresco de trabajos + GET_EMPRESA + LISTAR_PRESUPUESTOS), los hilos se pisaban y bloqueaban el socket.
   - **Soluci├│n:** Refactorizado `ConexionDB` a un patr├│n **ThreadLocal<Connection>**. Ahora cada hilo del servidor tiene su propia conexi├│n aislada. Se a├▒adi├│ `ConexionDB.cerrarConexion()` en el bloque `finally` de `GestorConexion` para limpieza total tras cada mensaje.

2. **Corrupci├│n de Mensajes en Socket (Dashboard):**
   - **Causa:** `ClienteSocket.enviar()` no estaba sincronizado. Si el dashboard ped├¡a refrescar datos mientras se enviaba un presupuesto, los JSON se mezclaban en el buffer, enviando basura al servidor.
   - **Soluci├│n:** Se marc├│ el m├®todo `enviar` como **synchronized**. Se a├▒adieron logs de salida en consola para trazabilidad.

3. **Mismatch de Campos (notas vs nuevaDescripcion):**
   - **Causa:** El servidor esperaba la clave `"notas"` pero el cliente enviaba `"nuevaDescripcion"`.
   - **Soluci├│n:** Estandarizaci├│n total al nombre de campo **`notas`** en el protocolo `CREAR_PRESUPUESTO`.

4. **Fallo Estructural de Base de Datos:**
   - **Causa:** La tabla `presupuesto` no ten├¡a f├¡sicamente la columna `notas`.
   - **Soluci├│n:** Ejecutado `ALTER TABLE presupuesto ADD COLUMN notas TEXT;`. Actualizado `ESQUEMA_BD.sql` para persistencia.

5. **L├│gica de Multipresupuesto (1:N):**
   - Implementado en `PresupuestoServiceImpl.aceptarPresupuesto`:
     - El presupuesto elegido pasa a `ACEPTADO`. El resto del mismo trabajo pasan a `RECHAZADO`.
     - Las notas del presupuesto aceptado se **inyectan** (con formato decorativo) al final de la descripci├│n del trabajo.
     - El trabajo pasa a estado `ACEPTADO`.

### Estado Actual: LISTO PARA VALIDACI├ôN
- El servidor es ahora robusto y hilos-seguro.
- El Dashboard ya no se queda "congelado" tras r├ífagas de mensajes.

---

### ­ƒÅü FINAL DE SESI├ôN ÔÇö DESCUBRIMIENTO CR├ìTICO Y LECTOR AVARO
- **El Bloqueo (Deadlock TCP):** Tras a├▒adir `List<Presupuesto>` y textos grandes (`notas`), la petici├│n `LISTAR_TRABAJOS` mut├│ de pesar 5 KB a varios Megabytes. Al inyectar un JSON colosal por sockets con `salida.flush()`, si el Dashboard (JavaFX) tiene su hilo de red colapsado pintando interfaces, el Buffer TCP subyacente de Windows (~64KB) se atasca. El m├®todo `flush()` del Servidor se congela esperando a que el cliente libere la tuber├¡a, deteniendo la escucha global.
- **La Soluci├│n Pr├│xima:** En la siguiente sesi├│n vamos a implementar arquitect├│nicamente el **"Hilo Lector Avaro"** en el Dashboard y aislar las escrituras con un Pool de Ejecuci├│n (Asincron├¡a) para evitar que el Servidor se quede colgado.

---

## ANEXO TECNICO PARA LA MEMORIA Y DEFENSA: HILOS, CONCURRENCIA Y EL "SINDROME DEL EMBUDO TCP"

*(Copia y pega o adapta estos conceptos para la secci├│n de "Problemas Encontrados y Soluciones" de tu memoria acad├®mica, o ├║salos en las preguntas del Tribunal).*

### ┬┐Por qu├® fall├│ el Socket justo despu├®s de implementar el Multipresupuesto? (Causa Ra├¡z)
Es l├│gico pensar que el problema de conexiones simult├íneas no tiene relaci├│n directa con el Multipresupuesto, ya que el Dashboard lanzaba r├ífagas desde antes. Lo que deton├│ el fallo tras el refactor fue **el tama├▒o exponencial del Payload JSON (Efecto Multiplicador en Red).**

1. **Antes del Multipresupuesto:** La petici├│n `LISTAR_TRABAJOS` devolv├¡a un listado plano. Sin arrays de presupuestos vinculados. El JSON era extremadamente ligero (unos pocos Kilobytes). Al ejecutarse `salida.flush()` en Java, ese diminuto JSON se engull├¡a al instante por el *Buffer de Red TCP* del sistema operativo. El m├®todo terminaba en 1 milisegundo, y el hilo del servidor quedaba libre enseguida para el pr├│ximo bucle de lectura.
2. **Despu├®s del Multipresupuesto:** Para soportar que la App compare ofertas in-situ, tuvimos que "hidratar" cada objeto `Trabajo` con su colecci├│n lista de `List<Presupuesto>`. Peor a├║n, a├▒adimos el campo de texto libre `notas` (la descripci├│n t├®cnica extensa). 
3. **La Explosi├│n de Bytes:** Si un `LISTAR_TRABAJOS` trae 50 incidencias, y la mitad tiene 2 o 3 ofertas de diferentes empresas, cada una con un texto de 500 caracteres, un DTO en JSON puro pasa de pesar 5 KB a pesar varios Megabytes.
4. **El Bloqueo (TCP Window Full):** Al forzar un env├¡o tit├ínico por sockets (`salida.write` seguido de `flush()`), si el Dashboard (receptor) no est├í leyendo ese texto kilom├®trico sin interrupci├│n y a suma velocidad, el Buffer subyacente TCP se empantana. Al llenarse, `flush()` sufre un bloqueo intr├¡nseco de red. Se queda congelado ("Atascamiento") aguardando que el cliente vac├¡e mil├¡metros de cauce. Al colgar el hilo con esta retenci├│n, el Servidor detiene toda monitorizaci├│n de nuevas conexiones hasta purgar los Bytes de salida.

### Glosario de Concurrencia (┬┐D├│nde usamos Multithreading en el proyecto?)
Este MVP de FixFinder destaca brutalmente de cara a tu tribunal por el uso nativo de sockets y la abstracci├│n as├¡ncrona robusta. Anota la enumeraci├│n expl├¡cita para la Memoria:

**EN EL SERVIDOR (Backend Java 21 - Non Spring):**
1. **El Hilo de Escucha (Dispatcher Listener):** Integrado en `ServidorCentral`. Su encasillamiento l├│gico es orbitar un bucle infinito sobre la operaci├│n bloqueante de IO `serverSocket.accept()`. Nace y muere esperando aperturas del Socket, con derivaci├│n reactiva al instante a un nuevo hilo Worker.
2. **Los Workers Concurrentes (`GestorConexion`):** Un pool/hilos instanciados individualmente. Al conectarse un gerente en PC, o cinco dispositivos Flutter, el Backend los abastece mediante cinco hilos vivos (Workers) que interpretan simult├íneamente (Runnable) los JSON recibidos del canal InputStream.
3. **Aislamiento Sem├íntico de BBDD (`ConexionDB` / `ThreadLocal`):** Mitigante de *Deadlocks*. Puesto que decenas de workers atacan a MySQL transaccionalmente, rehusamos inyectarles una ├║nica clase Java Connection est├ítica (crasheos y traslapes JDBC). Se instrument├│ `ThreadLocal<Connection>`, forzando as├¡ un "Scope" privado y un cauce SQL ajeno por cada hilo en transcurso.

**EN EL DASHBOARD (Cliente JavaFX - Arquitectura MVC):**
4. **Aislamiento del Hilo Gr├ífico (Platform Thread UI):** Todo SDK de Renderizado (sea SWING, FX, o QT) proh├¡be intrusi├│n forzada del CPU o llamadas de red prolongadas ("Freeze Frame"). Todas tus comunicaciones `solicitar...` subrogan a `CompletableFuture` (Future/Promesa de sub-hilo). Los desenlaces visuales terminan deriv├índose de nuevo a la UI ├║nicamente envolvi├®ndolos en `Platform.runLater(() -> {})`. 
5. **Sem├íforos Transversos:** Hemos programado sub-hilos "demonio" recurrentes atados con `sleep(ms)` intermedios que lanzan ping transparentes en las pantallas iniciales, detectando los virajes del entorno AWS a LOCAL.
6. **Cruce Estructural de Output `enviar()`:** Subsanado con `synchronized()`. Se obligan secuencias unitarias ante la coincidencia del hilo de UI y los hilos en backgound pretendiendo "incrustar" peticiones por el canal `DataOutputStream` id├®ntico. Un patr├│n Mutex elemental en Sockets.

**EN LA APP M├ôVIL (Flutter / Dart):**
7. Dart maneja una filosof├¡a asim├®trica: usa **Isolates** y el Event Loop de asincron├¡a (`async / await`). Opera virtualmente sobre Single-Thread sin necesidad empedernida de crear sub-hilos de sistema operativo para descargar Sockets, gestion├índolo nativamente con multiplexaci├│n I/O no bloqueante, maximizando la eficiencia de bater├¡a e interfaz Android sin "saltos" (Jank/stutters).

### La Escalabilidad de Nuestra Soluci├│n (y por qu├® evitaremos que vuelva a pasar)
Es completamente l├¡cito preguntarse: *┬┐Si el sistema peta ahora mismo con apenas unas peticiones de prueba, se colapsar├í el d├¡a de ma├▒ana si tenemos cientos de presupuestos reales?*

La respuesta es NO, porque no peta por "falta de potencia", sino por "falta de coordinaci├│n de tuber├¡as".
Nuestra soluci├│n implementa un **"Hilo Lector Avaro"** combinado con Promesas As├¡ncronas (TxID). Esto separa radicalmente el acto de "Ingerir" datos de la red del acto de "Renderizar" la interfaz. 

Con esto, no importa cu├ín colosal sea el JSON resultante del Multipresupuesto: el Hilo Lector Avaro desv├¡a los Bytes desde el buffer TCP de tu sistema operativo a la memoria de tu programa en mili-segundos, despejando la tuber├¡a (ventana TCP) de inmediato. El Servidor jam├ís detecta atascos y su `salida.flush()` empuja los datos sin trabas. Y si en el futuro se hablara de millones de registros, el proyecto est├í preparado estructuralmente para aplicar **Paginaci├│n** (ej: pedir 20 registros, que son instant├íneos, y cargar m├ís haciendo scroll).


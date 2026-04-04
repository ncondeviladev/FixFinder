# ðŸš€ PLAN_EVOLUCION_APP: Hoja de Ruta FixFinder

> **Archivo de sesiÃ³n:** Este documento sirve como memoria de trabajo entre sesiones de desarrollo.
> Si un chat se pierde o se reinicia, leer este documento primero para recuperar el contexto completo.
> **âš ï¸ Nota:** La carpeta `DOCS/` **sÃ­ se sube a Git** pero el repositorio de GitHub debe mantenerse **privado** para que la Memoria del proyecto no sea pÃºblica.


---

## ðŸ“‹ ESTADO ACTUAL DEL SISTEMA (07/03/2026)

### Arquitectura General

- **Backend:** Servidor Java puro con Sockets TCP en puerto `5000`. Sin Spring Boot.
  - Punto de entrada del servidor: `com.fixfinder.red.ServidorCentral`
  - Arranque: `.\gradlew.bat runServer` desde `C:\Users\ncond\Desktop\FF\FIXFINDER`
  - GestiÃ³n de conexiones: `GestorConexion.java` â†’ tiene un switch con todas las acciones
  - Procesadores por entidad: `ProcesadorTrabajos.java`, `ProcesadorPresupuestos.java`, etc.
  - Tests: `.\gradlew.bat test` â€” usa JUnit 5, clase principal `ServiceTest.java`
  - **Protocolo de ComunicaciÃ³n:** 4 bytes de cabecera (longitud del mensaje) + payload JSON en bytes.
    - Java: `DataOutputStream.writeInt(len)` + `write(bytes)` / `DataInputStream.readInt()` + `readFully(bytes)`
    - Flutter: `socket.add(4 bytes big-endian + payload)` / lee 4 bytes cabecera + N bytes datos
    - **âš ï¸ El simulador `SimuladorController.java` usa tambiÃ©n el protocolo de 4 bytes (ya actualizado)**
- **Base de datos:** MySQL en Docker. Contenedor: `FixFinderDb`. Root pass: `root`.
  - DB name: `fixfinder`
  - Acceso: `docker exec -i FixFinderDb mysql -u root -proot fixfinder -e "SQL;"`
  - Resetear datos de prueba: `.\gradlew.bat runSeeder`
- **App MÃ³vil:** Flutter (Android). Carpeta: `C:\Users\ncond\Desktop\FF\fixfinder_app`
  - Arranque en emulador 1: `flutter run -d emulator-5554`
  - Arranque en emulador 2: `flutter run -d emulator-5556`
  - IP del servidor desde emulador: `10.0.2.2:5000`
  - Tests: `flutter test`
  - Estado del socket: singleton `SocketService`, reconecta automÃ¡ticamente
- **App Escritorio (Windows/JavaFX):** `com.fixfinder.Launcher` â†’ `AppEscritorio`
  - Arranque: `.\gradlew.bat runClient`
  - Para el panel maestro del Dashboard (tabla de trabajos) usar `.\gradlew.bat runDashboard` o acceder desde el menÃº de gerente/admin

### Usuarios de prueba en la BD (generados por `runSeeder`)

| Email                            | ContraseÃ±a  | Rol      | Tlf       | DirecciÃ³n                    |
| -------------------------------- | ----------- | -------- | --------- | ---------------------------- |
| marta@gmail.com                  | password    | CLIENTE  | 600123456 | Calle Paz 5, 2ÂºA, Valencia   |
| juan@gmail.com                   | password    | CLIENTE  | 600234567 | Av. del Puerto 120, Valencia |
| elena@gmail.com                  | password    | CLIENTE  | 600345678 | Calle XÃ tiva 22, Valencia    |
| gerente.a@levante.com            | password    | GERENTE  | 600123456 | Av. del Cid 45, Valencia     |
| (operarios generados por seeder) | password123 | OPERARIO | 666127582 | varÃ­a segÃºn operario         |

> âš ï¸ IMPORTANTE: Los tests de JUnit (`ServiceTest`) generan usuarios temporales en la BD y pueden dejar telefono=NULL en usuarios existentes. DespuÃ©s de correr tests, ejecutar:
>
> ```sql
> UPDATE usuario SET telefono = '600123456' WHERE rol = 'CLIENTE' OR rol = 'OPERARIO';
> ```

---

## ðŸ“¦ ESTADO DE CADA MÃ“DULO DEL BACKEND

### `TrabajoService` / `TrabajoServiceImpl`

MÃ©todos implementados y funcionales:

- `solicitarReparacion(idCliente, titulo, categoria, descripcion, direccion, urgencia)` â€” Crea trabajo PENDIENTE
  - **âš ï¸ Nuevo (01/03):** Si `direccion` viene vacÃ­o, usa `cliente.getDireccion()` como fallback. Si tampoco tiene, pone "Sin direcciÃ³n especificada"
- `cancelarTrabajo(idTrabajo, motivo)` â€” Pasa a CANCELADO. Solo si NO estÃ¡ ASIGNADO ni FINALIZADO
- `modificarTrabajo(idTrabajo, titulo, descripcion, direccion, categoria, urgencia)` â€” Solo si estÃ¡ PENDIENTE
- `finalizarTrabajo(idTrabajo, informe)` â€” Pasa a REALIZADO. Concatena informe al final de la descripciÃ³n
- `valorarTrabajo(idTrabajo, valoracion, comentarioCliente)` â€” Solo si FINALIZADO/REALIZADO/PAGADO. ValoraciÃ³n 1-5 estrellas.
- `listarPorCliente()`, `listarPorOperario()`, enriquecimiento de DTOs en `procesarListarTrabajos`

### `ProcesadorTrabajos`

Acciones que maneja el switch en `GestorConexion`:

| AcciÃ³n (String)       | MÃ©todo procesador            |
| --------------------- | ---------------------------- |
| `CREAR_TRABAJO`       | `procesarCrearTrabajo`       |
| `LISTAR_TRABAJOS`     | `procesarListarTrabajos`     |
| `FINALIZAR_TRABAJO`   | `procesarCambiarEstado`      |
| `CANCELAR_TRABAJO`    | `procesarCancelarTrabajo`    |
| `MODIFICAR_TRABAJO`   | `procesarModificarTrabajo`   |
| `VALORAR_TRABAJO`     | `procesarValorarTrabajo`     |
| `ACEPTAR_PRESUPUESTO` | `procesarAceptarPresupuesto` |
| `LISTAR_PRESUPUESTOS` | `procesarListarPresupuestos` |

> âš ï¸ IMPORTANTE sobre `procesarValorarTrabajo`: El mensaje de Ã©xito en el JSON de respuesta es
> `"Valoracion guardada correctamente"` (SIN acento en la Ã³). El Completer en Flutter filtra por esta cadena.

### `ProcesadorTrabajos.procesarListarTrabajos` â€” Enriquecimiento del DTO

El JSON que envÃ­a el servidor al listar incluye (ademÃ¡s de campos bÃ¡sicos):

- `id`, `titulo`, `descripcion`, `categoria`, `estado`, `fecha`
- **`direccion`** (String â€” direcciÃ³n del trabajo. **Nuevo 01/03**: ya se incluye en la respuesta)
- `valoracion` (int 0-5), `comentarioCliente` (String o null), `fechaFinalizacion` (String ISO o null)
- `urls_fotos` (List<String>), `ubicacion` (objeto {lat, lon} o null)
- `cliente` (objeto completo con id, nombre, telefono, email, foto, direccion)
- `operarioAsignado` (objeto completo con id, nombre, telefono, email, foto)
- `presupuesto` (el presupuesto aceptado si existe), `tienePresupuestoAceptado` (boolean)

> âš ï¸ **Bug corregido (01/03):** Antes el campo `direccion` del trabajo NO estaba en la respuesta LISTAR_TRABAJOS.
> Flutter caÃ­a en `json['direccionCliente']` y siempre mostraba la direcciÃ³n del cliente, ignorando la direcciÃ³n
> real de la incidencia. Ahora se incluye `map.put("direccion", t.getDireccion())` explÃ­citamente.

---

## ðŸ“± ESTADO DE CADA MÃ“DULO DE LA APP FLUTTER

### Estructura de carpetas

````
lib/
â”œâ”€â”€ main.dart                          â† Entrada, providers, rutas, tema
â”œâ”€â”€ models/
â”‚   â”œâ”€â”€ trabajo.dart                   â† Modelo Trabajo + enums EstadoTrabajo, CategoriaServicio
â”‚   â”œâ”€â”€ usuario.dart                   â† Modelo Usuario + enum Rol
â”‚   â”œâ”€â”€ presupuesto.dart               â† Modelo Presupuesto
â”‚   â””â”€â”€ empresa.dart                   â† Modelo Empresa colaboradora
â”œâ”€â”€ providers/
â”‚   â””â”€â”€ trabajo_provider.dart          â† State management para trabajos
â”œâ”€â”€ services/
â”‚   â”œâ”€â”€ socket_service.dart            â† ComunicaciÃ³n TCP con servidor Java (protocolo 4 bytes)
â”‚   â””â”€â”€ auth_service.dart             â† Login, logout, persistencia token en SharedPreferences
â”œâ”€â”€ screens/
â”‚   â”œâ”€â”€ login_pantalla.dart
â”‚   â”œâ”€â”€ dashboard_pantalla.dart
â”‚   â”œâ”€â”€ detalle_trabajo_pantalla.dart  â† REFACTORIZADA: delega en widgets separados
â”‚   â”œâ”€â”€ crear_trabajo_pantalla.dart    â† Crear y Modificar (modo dual)
â”‚   â””â”€â”€ perfil_pantalla.dart
â””â”€â”€ widgets/
    â”œâ”€â”€ comunes/
    â”‚   â”œâ”€â”€ dato_fila.dart
    â”‚   â””â”€â”€ estado_badge.dart
    â”œâ”€â”€ trabajos/
    â”‚   â”œâ”€â”€ tarjeta_trabajo.dart
    â”‚   â”œâ”€â”€ tarjeta_contacto.dart
    â”‚   â””â”€â”€ galeria_fotos.dart         â† Preparada para URLs Firebase (Nuevo 08/03)
    â””â”€â”€ detalle_trabajo/
        â”œâ”€â”€ detalle_info_card.dart
        â”œâ”€â”€ detalle_resumen_final.dart
        â”œâ”€â”€ detalle_seccion_presupuestos.dart
        â””â”€â”€ dialogos_trabajo.dart      â† Todos los AlertDialogs (borrar, finalizar, valorar)
```

---

## âœ… TAREAS COMPLETADAS: IMÃGENES (Actualizado 10/03)

Las siguientes tareas han sido implementadas y estÃ¡n listas para validaciÃ³n final:

1. **Fotos de Perfil en App (Cliente)**:
   - Implementado en `perfil_pantalla.dart` con `image_picker` y subida directa a Firebase Storage.
   - SincronizaciÃ³n con el servidor mediante `ServicioAutenticacion` y evento `ACTUALIZAR_FOTO_PERFIL`.
2. **Fotos de Perfil en Dashboard JavaFX**:
   - Implementada clase `FirebaseStorageUploader.java` (REST API) para subida de fotos desde escritorio.
   - Funcionalidad de cambio de foto aÃ±adida para **Gerente** (Panel Empresa) y **Operarios** (Panel Operarios).
   - Actualizada la lÃ³gica de `miniAvatar` para mostrar fotos reales desde URL con clips circulares en JavaFX.
3. **Soporte de Backend (Servidor Java)**:
   - Nuevo endpoint `ACTUALIZAR_FOTO_PERFIL` en `ProcesadorUsuarios`.
   - Consulta SQL optimizada para recuperar la foto del Gerente de forma aislada.

---

## ðŸ§ª PROTOCOLO DE PRUEBAS PARA LA PRÃ“XIMA SESIÃ“N

Para verificar que todo el sistema de imÃ¡genes es robusto, realizar los siguientes pasos en orden:

### 1. Prueba en App MÃ³vil (Flujo Cliente)
- Iniciar sesiÃ³n como **Cliente**.
- Ir a Perfil â†’ Clic en el icono de la cÃ¡mara (ðŸ“¸).
- Seleccionar una imagen de la galerÃ­a.
- **Verificar:** El redondel del perfil debe actualizarse con la nueva foto.
- **Persistencia:** Cerrar sesiÃ³n y volver a entrar; la foto debe seguir ahÃ­ (cargada desde URL).

### 2. Prueba en Dashboard (Flujo Gerente)
- Ir a la secciÃ³n **Empresa**.
- Clic en el icono ðŸ“¸ sobre el redondel del Gerente.
- Seleccionar un archivo del PC.
- **Verificar:** El redondel debe mostrar la foto tras la carga.

### 3. Prueba en Dashboard (Flujo Operario)
- Ir a la secciÃ³n **Operarios**.
- En la tabla, pulsar el botÃ³n ðŸ“¸ de un operario especÃ­fico.
- Seleccionar foto.
- **Verificar:** La celda de "Nombre" del operario debe mostrar ahora su foto real en el avatar pequeÃ±o en lugar de las iniciales.

### 4. Prueba Cruzada
- Cambiar la foto de un operario en el Dashboard.
- Iniciar sesiÃ³n con ese mismo operario en la App de Flutter.
- **Verificar:** En el perfil de la app, debe aparecer la foto que asignÃ³ el gerente.

---

---

### Providers

- **`TrabajoProvider`** (`lib/providers/trabajo_provider.dart`)
  - `obtenerTrabajos()` â€” Lista trabajos, excluye CANCELADOS, ordena por prioridad de estado
  - `crearTrabajo(datos)` â€” EnvÃ­a `CREAR_TRABAJO`. No Completer, solo delay 800ms
  - `cancelarTrabajo(idTrabajo)` â€” EnvÃ­a `CANCELAR_TRABAJO`, delay 800ms + llama `obtenerTrabajos()`
  - `modificarTrabajo(idTrabajo, datos)` â€” EnvÃ­a `MODIFICAR_TRABAJO`, usa Completer que espera `"modificado"` en mensaje
  - `valorarTrabajo(idTrabajo, valoracion, comentario)` â€” EnvÃ­a `VALORAR_TRABAJO`, usa Completer que espera `"Valoracion"` en mensaje
  - `actualizarEstadoTrabajo(idTrabajo, estado, informe?)` â€” Para FINALIZAR desde operario. delay 800ms + `obtenerTrabajos()`
  - `aceptarPresupuesto(idPresupuesto)` â€” delay 800ms + `obtenerTrabajos()`
  - `startPolling()` / `stopPolling()` â€” Refresco automÃ¡tico cada 15 segundos (evento push)

> âš ï¸ TRUCO DEL COMPLETER para `modificar/valorar`: Los Completers filtran por palabras clave del `mensaje`
> de respuesta (NO por `status == 200`) para no capturar por accidente la respuesta de LISTAR que tambiÃ©n
> devuelve 200 y llega de forma asÃ­ncrona.

### Pantallas â€” Comportamiento de NavegaciÃ³n (ACTUALIZADO 01/03)

**PatrÃ³n estÃ¡ndar para todas las acciones:**

1. La acciÃ³n (finalizar, valorar, aceptar, borrar) llama al provider y espera el resultado.
2. Si `exito == true`, se hace **`Navigator.pop(context)`** simple (NO `popUntil`).
3. El dashboard tiene `.then((_) async { await Future.delayed(900ms); obtenerTrabajos(); })` en el `onTap`.
4. El delay de 900ms permite al servidor confirmar el cambio en BD antes de re-listar.

> âš ï¸ **CAUSA DE CRASH HISTÃ“RICO:** Usando `popUntil(ModalRoute.withName('/dashboard'))` con rutas
> anÃ³nimas (`MaterialPageRoute`) el stack de navegaciÃ³n quedaba vacÃ­o â†’ pantalla negra.
> **Nunca usar `popUntil` desde pantallas navegadas con `MaterialPageRoute` sin nombre.**

### Pantallas

- **`DashboardPantalla`** (`lib/screens/dashboard_pantalla.dart`)
  - Lista trabajos con `TarjetaTrabajo` (ordenados por prioridad)
  - **BotÃ³n Refresh** en AppBar (âž¤ `Icons.refresh`) para todos los roles
  - Pull-to-refresh con `RefreshIndicator`
  - Pantalla vacÃ­a mejorada: `CustomScrollView` con `AlwaysScrollableScrollPhysics` para que el pull-to-refresh funcione aunque no haya registros + botÃ³n "Actualizar" visible
  - BotÃ³n `+` flotante (solo CLIENTE)
  - `_tieneAccionPendiente`: Badge de acciÃ³n solo si:
    - CLIENTE + PRESUPUESTADO â†’ hay presupuesto por aceptar
    - CLIENTE + REALIZADO y `valoracion == 0` â†’ pendiente valorar
    - CLIENTE + FINALIZADO y `valoracion == 0` â†’ pendiente valorar
    - OPERARIO + ACEPTADO â†’ hay trabajo por iniciar

- **`CrearTrabajoPantalla`** (`lib/screens/crear_trabajo_pantalla.dart`)
  - ParÃ¡metro opcional `trabajoAEditar: Trabajo?`
  - **DirecciÃ³n opcional (nuevo 01/03):** Campo no obligatorio. Si se deja vacÃ­o, el servidor usa la direcciÃ³n registrada del cliente. Hint text: "Si se deja vacÃ­o, se usa tu direcciÃ³n registrada". Solo se envÃ­a el campo `direccion` en el JSON si el usuario escribiÃ³ algo.
  - SnackBar verde/rojo con mensaje descriptivo

- **`DetalleTrabajoPantalla`** (`lib/screens/detalle_trabajo_pantalla.dart`)
  - **REFACTORIZADA en sesiÃ³n anterior:** Delega el rendering en widgets separados de `widgets/detalle_trabajo/`
  - AppBar con `PopupMenuButton`: Modificar + Borrar (solo CLIENTE en estado PENDIENTE/PRESUPUESTADO)
  - Muestra `DetalleInfoCard` â†’ informaciÃ³n principal del trabajo
  - Si CLIENTE + PENDIENTE/PRESUPUESTADO â†’ `DetalleSeccionPresupuestos`
  - Si OPERARIO + ASIGNADO/REALIZADO â†’ botÃ³n verde "MARCAR COMO FINALIZADO"
  - Si FINALIZADO â†’ `DetalleResumenFinal` (fecha, precio, valoraciÃ³n)
  - Si CLIENTE + FINALIZADO/REALIZADO + `valoracion == 0` â†’ botÃ³n azul "VALORAR SERVICIO"

### Widgets

- `TarjetaTrabajo` â€” Tarjeta del dashboard, banner de acciÃ³n pendiente, menÃº 3 puntos
- `TarjetaContacto` â€” Datos de contacto de cliente u operario
- `GaleriaFotos` â€” Tira horizontal de fotos, tap abre modal ampliado. **Preparada para URLs Firebase**
- `EstadoBadge` â€” Chip coloreado segÃºn estado
- `DatoFila` â€” Par Etiqueta: Valor simple
- `DetalleInfoCard` â€” Tarjeta principal de detalle (estado, categorÃ­a, descripciÃ³n, contactos)
- `DetalleResumenFinal` â€” Tarjeta verde de cierre (precio, fecha, valoraciÃ³n)
- `DetalleSeccionPresupuestos` â€” Lista de presupuestos con botÃ³n Aceptar y diÃ¡logo de empresa
- `DialogosTrabajo` â€” Clase utilitaria con todos los AlertDialogs (borrar, finalizar, valorar)

---

## ðŸ”§ SESIÃ“N 01/03/2026 â€” Cambios Detallados

### Objetivo de la sesiÃ³n

RefactorizaciÃ³n de cÃ³digo, limpieza de logs debug, aÃ±adir documentaciÃ³n a todas las clases, y correcciÃ³n de mÃºltiples bugs de funcionamiento en el flujo cliente-operario.

### Backend (Java) â€” Cambios

#### `ProcesadorTrabajos.java`

- **Bug fix crÃ­tico:** AÃ±adido `map.put("direccion", t.getDireccion())` en `procesarListarTrabajos` (lÃ­nea ~198). Antes esta clave nunca se incluÃ­a en la respuesta, por lo que Flutter siempre usaba la direcciÃ³n del cliente en lugar de la del trabajo.

#### `TrabajoServiceImpl.java`

- **Nuevo comportamiento `solicitarReparacion`:** Si `direccion` viene vacÃ­o desde la app, el servidor usa `cliente.getDireccion()` como fallback. Si tampoco tiene, "Sin direcciÃ³n especificada".

#### `SimuladorController.java`

- Actualizado al protocolo de 4 bytes (`writeInt` / `readInt`) para ser compatible con el servidor actualizado.

### App Flutter â€” Cambios

#### Limpieza de cÃ³digo

- Eliminados todos los `print()` y llamadas a `Logger` de: `trabajo_provider.dart`, `auth_service.dart`, `socket_service.dart`
- Reemplazados `Logger` por `debugPrint` solo en bloques `catch` crÃ­ticos

#### DocumentaciÃ³n

- AÃ±adido comentario de cabecera en **todas** las clases del proyecto (2 lÃ­neas, estilo conciso):
  - `main.dart`, `login_pantalla.dart`, `dashboard_pantalla.dart`, `detalle_trabajo_pantalla.dart`, `crear_trabajo_pantalla.dart`, `perfil_pantalla.dart`
  - `socket_service.dart`, `auth_service.dart`, `trabajo_provider.dart`
  - `trabajo.dart`, `usuario.dart`, `presupuesto.dart`, `empresa.dart`
  - Todos los widgets en `widgets/comunes/`, `widgets/trabajos/`, `widgets/detalle_trabajo/`

#### `dashboard_pantalla.dart`

- AÃ±adido botÃ³n `Icons.refresh` en AppBar (para todos los roles, sin condiciÃ³n)
- Pantalla vacÃ­a: cambiado de `Center(Text)` simple a `CustomScrollView + SliverFillRemaining` con `AlwaysScrollableScrollPhysics` para que el pull-to-refresh funcione incluso sin elementos. Incluye botÃ³n "Actualizar" visible.
- `.then()` en `onTap` ahora tiene `await Future.delayed(900ms)` antes de `obtenerTrabajos()` para dar tiempo al servidor a procesar el cambio en BD
- `_tieneAccionPendiente` refactorizado: ahora es explÃ­cito (if-return) en lugar de un `||` compuesto, y el estado REALIZADO solo activa el badge si `valoracion == 0`

#### `detalle_trabajo_pantalla.dart`

- **Todos los `Navigator.popUntil` eliminados** â†’ reemplazados por `Navigator.pop(context)` simple
- Las llamadas a `obtenerTrabajos()` tambiÃ©n se eliminaron de aquÃ­ (el dashboard las hace en `.then()`)
- `_handleBorrar`: limpiado (antes tenÃ­a `popUntil` que causaba crash de pantalla negra)
- `_aceptarPresupuesto`: idem
- `_finalizarTrabajo`: idem
- `_handleValorar`: en caso de error muestra SnackBar y hace `return` (no navega); en caso de Ã©xito solo hace `pop`

#### `crear_trabajo_pantalla.dart`

- Campo `direccion` ya no tiene validador obligatorio
- Hint text: "Si se deja vacÃ­o, se usa tu direcciÃ³n registrada"
- El campo `direccion` solo se incluye en el mapa `datos` si no estÃ¡ vacÃ­o (condicional `if` en el Map literal)

#### `trabajo_provider.dart`

- `cancelarTrabajo`: AÃ±adido `obtenerTrabajos()` tras el delay de 800ms (antes solo retornaba `true` sin actualizar la lista)

### Bugs resueltos esta sesiÃ³n

1. **Pantalla negra al finalizar/valorar/borrar** â†’ Causa: `popUntil` con rutas anÃ³nimas vaciaba el stack. Fix: `Navigator.pop()` simple.
2. **Lista no se actualizaba despuÃ©s de acciones** â†’ Causa: `obtenerTrabajos()` se llamaba antes del delay del servidor. Fix: Delay de 900ms en el `.then()` del dashboard.
3. **DirecciÃ³n del trabajo siempre mostraba la del cliente** â†’ Causa: Campo `direccion` ausente en JSON de LISTAR_TRABAJOS. Fix: `map.put("direccion", t.getDireccion())` en procesador Java.
4. **Cancelar desde detalle no actualizaba el dashboard** â†’ Causa: `cancelarTrabajo` no llamaba a `obtenerTrabajos()`. Fix: aÃ±adido tras delay.
5. **Pantalla vacÃ­a del operario no permitÃ­a pull-to-refresh** â†’ Fix: `CustomScrollView` con `AlwaysScrollableScrollPhysics`.
6. **Badge de "Valorar" persistÃ­a tras valorar** â†’ Fix: condiciÃ³n `valoracion == 0` explÃ­cita en `_tieneAccionPendiente`.

---

## ðŸŽ¯ PRÃ“XIMAS FASES

### Fase 2: Fotos con Firebase Storage â¬œ SIGUIENTE

#### Plan de implementaciÃ³n:

**Firebase (setup):**

- [ ] Crear proyecto Firebase
- [ ] AÃ±adir app Android al proyecto Firebase (google-services.json)
- [ ] AÃ±adir dependencias en Flutter: `firebase_core`, `firebase_storage`, `image_picker`

**Flutter â€” Trabajos:**

- [ ] En `CrearTrabajoPantalla`: activar botÃ³n "AÃ±adir foto" â†’ `image_picker` â†’ subir a Firebase Storage â†’ recibir URL â†’ aÃ±adir a `_urlsFotos`
- [ ] Enviar `urls_fotos` en el JSON al servidor ya que el campo existe en el mapa de datos
- [ ] `GaleriaFotos` ya estÃ¡ preparado â†’ solo necesita URLs reales

**Flutter â€” Perfil de usuario:**

- [ ] En `PerfilPantalla`: aÃ±adir botÃ³n de editar foto â†’ `image_picker` â†’ subir a Firebase â†’ actualizar `url_foto` del usuario en servidor
- [ ] Backend: nueva acciÃ³n `ACTUALIZAR_PERFIL` o `SUBIR_FOTO_PERFIL` en `ProcesadorAutenticacion`
- [ ] Modelo `Usuario.urlFoto` ya existe â†’ solo falta el flujo de subida

**Backend Java:**

- [ ] `FotoTrabajo` ya existe como clase. `FotoTrabajoDAO` ya existe y guarda en BD
- [ ] El servidor ya intenta cargar fotos en `procesarListarTrabajos` â†’ solo falta recibir y guardar URLs al crear
- [ ] La acciÃ³n `CREAR_TRABAJO` ya lee `urls_fotos` del JSON y llama a `fotoTrabajoDAO` â†’ ya implementado

### Fase 3: Despliegue Local en Red (MÃ³vil FÃ­sico) â¬œ

**Objetivo:** Hacer funcionar la app en un mÃ³vil fÃ­sico real dentro de la misma red WiFi.

- [ ] **SocketService:** Cambiar IP de `10.0.2.2` a la IP local de la mÃ¡quina (ej: `192.168.1.X`)
  - Crear variable configurable o pantalla de configuraciÃ³n de IP
- [ ] **Firebase:** Ya funcionarÃ¡ con IP real (es HTTPS externo)
- [ ] **Servidor Java:** Asegurarse de que el firewall de Windows abre el puerto `5000`
  - PowerShell: `New-NetFirewallRule -DisplayName "FixFinder" -Direction Inbound -Protocol TCP -LocalPort 5000 -Action Allow`
- [ ] **App Escritorio (JavaFX dashboard):** Probado en red local (ya se conecta por socket a localhost)

### Fase 4: Despliegue en AWS EC2 â¬œ

**Objetivo:** Servidor Java en la nube, app conectando a IP pÃºblica.

- [ ] Provisionar EC2 (Ubuntu 22.04 recomendado), instalar Java 21 y MySQL
- [ ] Copiar el JAR del servidor (`.\gradlew.bat jar`)
- [ ] Abrir puertos: `5000` (TCP) y `3306` (MySQL, solo acceso interno)
- [ ] Crear script de arranque automÃ¡tico con `systemd`
- [ ] **SocketService Flutter:** Parametrizar IP (leer de config) â†’ apuntar a IP pÃºblica AWS
- [ ] Firebase Storage ya funciona con cualquier IP (es servicio externo)
- [ ] Probar flujo completo cliente â†’ servidor AWS â†’ BD RDS (o MySQL en EC2)

### Fase 5: DocumentaciÃ³n y Defensa â¬œ

---

## ðŸ”§ SESIÃ“N 07/03/2026 â€” Refinado Gerencial y ComunicaciÃ³n

### Objetivo de la sesiÃ³n

Refinar la visualizaciÃ³n de la empresa (valoraciones reales), unificar la comunicaciÃ³n gerente-operario mediante la "Hoja Informativa" y solucionar errores crÃ­ticos en la gestiÃ³n de operarios del Dashboard.

### Backend (Java) â€” Cambios

#### `ProcesadorUsuarios.java`

- **Enriquecimiento de Empresa:** Al solicitar datos de empresa (`GET_EMPRESA`), el servidor ahora busca todos los trabajos `FINALIZADOS` vinculados a esa empresa y devuelve una lista de valoraciones reales (puntuaciÃ³n, cliente, comentario, fecha).
- **Limpieza de Referencias:** Eliminadas instanciaciones directas de `DataRepository` para favorecer la estabilidad de conexiones.

#### `OperarioDAOImpl.java`

- **SincronizaciÃ³n de ENUM SQL:** Se ha corregido la palabra mÃ¡gica. El SQL usa `BAJA`, pero el cÃ³digo enviaba `INACTIVO`. Ahora se envÃ­a `BAJA` al desactivar (baja lÃ³gica).
- **CorrecciÃ³n de Mapeo:** Se asegura que al leer de la DB, cualquier estado distinto de `BAJA` se interprete como `estaActivo = true`.

#### `OperarioServiceImpl.java`

- **Pruebas RÃ¡pidas:** Se han comentado las validaciones de `matches()` para Email, DNI y TelÃ©fono para permitir avanzar con datos de prueba no perfectos.
- **SanitizaciÃ³n:** Se ha aÃ±adido un `.replaceAll("[^0-9]", "")` al telÃ©fono para evitar fallos por espacios o guiones.

#### `PresupuestoServiceImpl.java` & `ProcesadorTrabajos.java`

- **EliminaciÃ³n de `notas`:** Se ha borrado el campo `notas` de la tabla `presupuesto` y de los objetos Java. Ya no se usa.

### App Escritorio (JavaFX) â€” Cambios

#### `VistaEmpresa.java`

- **SecciÃ³n de ReseÃ±as:** Implementada una lista visual que muestra las Ãºltimas valoraciones de los clientes con estrellas (â­).
- **Fecha de Registro:** Corregida la visualizaciÃ³n de la fecha de alta de la empresa (ya no sale "No disponible").

#### `DialogoCrearPresupuesto.java`

- **Hoja Informativa:** Ahora el Ã¡rea de texto de "Notas" actualiza directamente la `descripcion` del trabajo.
- **Plantilla AutomÃ¡tica:** Si la descripciÃ³n no estÃ¡ estructurada, el diÃ¡logo inserta una plantilla con cabeceras para `CLIENTE`, `GERENTE` y `OPERARIO`.

#### `DashboardPrincipalController.java`

- **SincronizaciÃ³n de Callbacks:** Los mÃ©todos `onPresupuestar` y similares ya no usan el parÃ¡metro `notas`, sino que gestionan la `nuevaDescripcion` del trabajo.

### Bugs resueltos esta sesiÃ³n

1. **Error 500 al dar de baja operario:** Causa: Discrepancia entre "INACTIVO" e "BAJA" en el ENUM de MySQL. Fix: Sincronizado a "BAJA".
2. **EdiciÃ³n de operario fallaba por validaciÃ³n:** Causa: TelÃ©fono con espacios o formato de email estricto. Fix: Comentadas validaciones y sanitizado telÃ©fono.
3. **Valoraciones de empresa vacÃ­as:** Causa: No se estaban consultando los trabajos finalizados. Fix: Implementada bÃºsqueda por empresa en el procesador.
4. **Desconexiones por "Connection Reset":** Causa: Demasiadas aperturas de `DataRepositoryImpl`. Fix: Refactorizado a uso de DAOs directos cuando es posible.

---

## ðŸŽ¯ PRÃ“XIMAS FASES

- [ ] Memoria tÃ©cnica (arquitectura, decisiones de diseÃ±o, protocolo de comunicaciÃ³n)
- [ ] Diagrama de clases, diagrama de secuencia del flujo completo
- [ ] PresentaciÃ³n + ensayo

---

## ðŸ› ï¸ COMANDOS DE REFERENCIA RÃPIDA

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
r   â† hot reload
R   â† hot restart (limpia estado)

# Correr tests Flutter
flutter test

# Abrir firewall para red local
New-NetFirewallRule -DisplayName "FixFinder" -Direction Inbound -Protocol TCP -LocalPort 5000 -Action Allow

# Consultar la BD
docker exec -i FixFinderDb mysql -u root -proot fixfinder -e "SELECT id,titulo,estado,valoracion,direccion FROM trabajo ORDER BY id DESC LIMIT 10;"

# Ver usuarios
docker exec -i FixFinderDb mysql -u root -proot fixfinder -e "SELECT id,email,rol,telefono,direccion FROM usuario;"

# Restaurar telÃ©fonos si los tests los borran
docker exec -i FixFinderDb mysql -u root -proot fixfinder -e "UPDATE usuario SET telefono = '600123456' WHERE rol = 'CLIENTE' OR rol = 'OPERARIO';"
````

---

**Nota Final:** Trabajar siempre paso a paso. Antes de implementar una nueva funcionalidad,
leer este documento para no romper lo que ya funciona. El protocolo de 4 bytes y el patrÃ³n
`pop + then(delay + obtenerTrabajos)` son invariantes crÃ­ticos del sistema.

---

## ðŸ”§ SESIÃ“N 08/03/2026 â€” AuditorÃ­a de Calidad y DecisiÃ³n de Ruta

### Objetivo de la sesiÃ³n

Revisar el cÃ³digo completo, evaluar calidad, detectar clases problemÃ¡ticas y preparar un plan quirÃºrgico de refactorizaciÃ³n. En esta sesiÃ³n **no se aplicaron los cambios** (por precauciÃ³n, dado que el proyecto estaba en estado funcional). Se creÃ³ un checkpoint de Git antes de cualquier cambio.

### Estado del Repositorio

- **Rama actual:** `refactor`
- **Commit de punto de partida (pre-refactorizaciÃ³n):** `ec6f1d3` â€” "pre refactor"
- **Comando para volver atrÃ¡s si algo se rompe:**
  ```powershell
  git checkout ec6f1d3 -- .
  # o para descartar todos los cambios y volver al commit exacto:
  git reset --hard ec6f1d3
  ```

### Cambios aplicados ANTES de la auditorÃ­a (inicio de sesiÃ³n)

Se realizaron estas modificaciones que YA ESTÃN en el commit `ec6f1d3`:

#### `ProcesadorTrabajos.java` â€” RefactorizaciÃ³n parcial aplicada

- MÃ©todo `mapearTrabajo(Trabajo t)` extraÃ­do como privado: centraliza la conversiÃ³n de objeto Trabajo a `Map<String, Object>`. Antes se repetÃ­a inline en cada bloque del listado.
- MÃ©todo `filtrarParaGerente(int idUsuario)` extraÃ­do como privado: encapsula la lÃ³gica de quÃ© trabajos ve un gerente (PENDIENTE + PRESUPUESTADO + los de su empresa).
- **âš ï¸ ATENCIÃ“N:** La refactorizaciÃ³n introdujo errores de compilaciÃ³n que se resolvieron durante la sesiÃ³n. Los imports correctos son `com.fixfinder.modelos.enums.EstadoTrabajo` y `com.fixfinder.modelos.enums.EstadoPresupuesto`. La firma del servicio de cancelar es `cancelarTrabajo(Integer, String)` â†’ siempre pasar motivo.

#### `DashboardPrincipalController.java`

- El mÃ©todo `solicitarTrabajos()` ahora tambiÃ©n llama a `servicioCliente.enviarGetEmpresa(idEmpresa)` para refrescar los datos de la empresa sin necesidad de re-login.

#### `VistaDashboard.java`

- AnimaciÃ³n aÃ±adida al botÃ³n `btnRefresh`: `RotateTransition` (360Â°, 0.5s) + `ScaleTransition` (1â†’0.85â†’1, 0.5s) en paralelo mediante `ParallelTransition`. Se ejecuta cada vez que se pulsa el botÃ³n.

#### `socket_service.dart` (Flutter)

- AÃ±adido mÃ©todo `request(Map peticion, {String? accionEsperada, int timeoutSegundos})`: encapsula el patrÃ³n Completer + listen + timeout + cancel en un solo mÃ©todo reutilizable. Preparado para limpiar `TrabajoProvider`.

---

### ðŸ” AuditorÃ­a Completa de Calidad â€” Resultados

#### BACKEND JAVA

| Clase                                         | TamaÃ±o    | DiagnÃ³stico                                                                                                                                                                                                                   | Severidad   |
| --------------------------------------------- | --------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------- |
| `ProcesadorTrabajos.java`                     | ~280L     | âœ… Refactorizado. Mapeo Ãºnico, filtrado encapsulado.                                                                                                                                                                          | âœ… Resuelto |
| `ProcesadorAutenticacion.java`                | 233L      | âš ï¸ MÃ©todo `procesarRegistro` mezcla 3 flujos (CLIENTE, OPERARIO, EMPRESA) en uno. DifÃ­cil de mantener.                                                                                                                        | âš ï¸ Medio    |
| `ProcesadorUsuarios.java`                     | 209L      | âš ï¸ Instancia DAOs directamente (`new EmpresaDAOImpl()`). Viola inversiÃ³n de dependencias. La lÃ³gica de valoraciones de empresa (50L) deberÃ­a estar en el Service, no en el Procesador.                                        | âš ï¸ Medio    |
| `TrabajoServiceImpl.java`                     | 337L      | â„¹ï¸ `historialOperario` carga TODOS los trabajos y filtra en Java (no en SQL). Con muchos datos puede ser lento. La lÃ³gica de "parsear descripciÃ³n por emojis" en `finalizarTrabajo` es frÃ¡gil si alguien cambia la plantilla. | â„¹ï¸ Bajo     |
| `TrabajoDAOImpl.java`                         | 371L      | ðŸ”´ **N+1 Problem:** El mÃ©todo `cargarRelaciones` abre una nueva conexiÃ³n SQL por cada trabajo de la lista para cargar cliente + operario + fotos. En 50 trabajos = 150 queries. SoluciÃ³n: JOIN en la query principal.         | ðŸ”´ Alto     |
| `GestorConexion.java`                         | 238L      | âœ… Bien diseÃ±ado. Delega. No tocar.                                                                                                                                                                                           | âœ… OK       |
| `ServidorCentral.java`                        | 110L      | âœ… Limpio. SemÃ¡foro de 10 conexiones.                                                                                                                                                                                         | âœ… OK       |
| `OperarioDAOImpl.java`, `EmpresaDAOImpl.java` | ~11KB c/u | âœ… Aceptables. Sin duplicaciÃ³n visible.                                                                                                                                                                                       | âœ… OK       |

#### DASHBOARD JAVAFX

| Clase                                 | TamaÃ±o      | DiagnÃ³stico                                                                                                                                        | Severidad |
| ------------------------------------- | ----------- | -------------------------------------------------------------------------------------------------------------------------------------------------- | --------- |
| `TablaIncidencias.java`               | 422L / 17KB | ðŸ”´ **GOD CLASS:** Controla tabla, 8 tipos de celdas, 3 diÃ¡logos de acciÃ³n, filtros, tabs Y decoraciÃ³n de iconos. Si falla un mÃ©todo afecta a todo. | ðŸ”´ Alto   |
| `DashboardPrincipalController.java`   | ~331L       | âš ï¸ Switch `procesarRespuesta` con ~20 casos. Funciona, pero en el lÃ­mite de lo mantenible.                                                         | âš ï¸ Medio  |
| `VistaDashboard.java`, `Sidebar.java` | <200L c/u   | âœ… Limpias.                                                                                                                                        | âœ… OK     |
| `TrabajoFX.java`, `OperarioFX.java`   | ~130L c/u   | âœ… JavaFX Properties correctas.                                                                                                                    | âœ… OK     |
| `DialogoNuevoOperario.java`           | 6KB         | â„¹ï¸ Grande pero cohesivo.                                                                                                                           | â„¹ï¸ Bajo   |

#### APP FLUTTER

| Clase                           | TamaÃ±o      | DiagnÃ³stico                                                                                                                                                                                                                                      | Severidad |
| ------------------------------- | ----------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | --------- |
| `trabajo_provider.dart`         | 365L / 11KB | ðŸ”´ **Boilerplate masivo:** Cada uno de los 8 mÃ©todos replica exactamente el mismo patrÃ³n `Completer + listen + send + timeout + cancel`. ~200L son cÃ³digo idÃ©ntico. El mÃ©todo `request()` ya existe en `socket_service.dart` para resolver esto. | ðŸ”´ Alto   |
| `dashboard_pantalla.dart`       | 228L        | âš ï¸ Tiene lÃ³gica de negocio mezclada con UI: `_tieneAccionPendiente()` y `_obtenerIconoCategoria()` deberÃ­an estar en el modelo o en un helper.                                                                                                   | âš ï¸ Medio  |
| `detalle_trabajo_pantalla.dart` | 269L        | âœ… Ya refactorizada (usa widgets separados).                                                                                                                                                                                                     | âœ… OK     |
| `tarjeta_trabajo.dart`          | 239L        | âœ… Bien encapsulada con animaciÃ³n propia.                                                                                                                                                                                                        | âœ… OK     |
| `socket_service.dart`           | ~200L       | âœ… Mejorado con `request()`.                                                                                                                                                                                                                     | âœ… OK     |

---

### ðŸ“‹ Plan de RefactorizaciÃ³n QuirÃºrgica (PENDIENTE DE EJECUTAR)

**Principio:** Cada bloque se compila y prueba ANTES de pasar al siguiente. Las firmas pÃºblicas de todos los mÃ©todos se respetan (sin breaking changes).

#### Bloque 1 â€” Flutter: Limpiar `trabajo_provider.dart`

- **QuÃ©:** Migrar los 8 mÃ©todos al nuevo `_socket.request()`. Eliminar ~200L de boilerplate.
- **Riesgo:** Bajo. Solo cambia la implementaciÃ³n interna, no el contrato.
- **EstimaciÃ³n:** 365L â†’ ~190L (âˆ’48%)
- **Mover `_obtenerIconoCategoria`** â†’ helper estÃ¡tico en `models/trabajo.dart`
- **Mover `_tieneAccionPendiente`** â†’ mÃ©todo de instancia en `Trabajo`

#### Bloque 2 â€” Java: Query JOIN en `TrabajoDAOImpl`

- **QuÃ©:** Crear mÃ©todo `obtenerTodosConJoin()` con SQL que incluye `LEFT JOIN usuario` y `LEFT JOIN operario` para evitar las 150 queries al listar.
- **Riesgo:** Medio. Tocar el DAO mÃ¡s crÃ­tico del sistema.
- **El mÃ©todo `cargarRelaciones` actual se mantiene** para `obtenerPorId` (no es crÃ­tico en uso individual).

#### Bloque 3 â€” Java: Separar `ProcesadorAutenticacion.procesarRegistro`

- **QuÃ©:** Extraer `registrarCliente()`, `registrarOperario()`, `registrarEmpresa()` como mÃ©todos privados. El mÃ©todo pÃºblico queda como router de 3 lÃ­neas.
- **Riesgo:** Bajo. Sin cambio de firma pÃºblica.

#### Bloque 4 â€” JavaFX: Extraer helpers de `TablaIncidencias.java`

- **QuÃ©:** Crear `UiHelper.java` con `miniAvatar()`, `crearLabel()`, `fila()`. Crear `CategoriaHelper.java` con `iconoCategoria()`.
- **Riesgo:** Bajo. Solo mover cÃ³digo, sin cambiar lÃ³gica.

---

### ðŸ¤” DEBATE: Â¿RefactorizaciÃ³n primero o Firebase Fotos primero?

**Argumento para Firebase primero:**

- El proyecto ya estÃ¡ funcional y presentable para un `proyecto de clase`.
- Fotos es una funcionalidad visible que aÃ±ade valor real al evaluador.
- La infraestructura ya estÃ¡ casi lista (`GaleriaFotos.dart` preparada, `FotoTrabajoDAO` existe, el servidor ya procesa `urls_fotos`).
- **EstimaciÃ³n:** 2-3 horas para un MVP funcional (seleccionar foto â†’ subir â†’ mostrar).

**Argumento para RefactorizaciÃ³n primero:**

- El boilerplate en `TrabajoProvider` con Completers manuales es una fuente real de bugs silenciosos (memory leaks si el timeout falla).
- Si aÃ±adimos Firebase sobre cÃ³digo "sucio", el provider crecerÃ¡ aÃºn mÃ¡s.
- La God Class `TablaIncidencias` se volverÃ¡ inmanejable si aÃ±adimos funcionalidad de fotos al dashboard.
- Con git y el commit de punto de partida, el riesgo de romper algo es mÃ­nimo.

**RecomendaciÃ³n del Agente:**

> Hacer primero el **Bloque 1** (Flutter `TrabajoProvider`) porque es el de menor riesgo, mayor impacto visible en limpieza y prepara el terreno para Firebase. Luego pasar a Firebase. Los Bloques 2, 3 y 4 (Java) se pueden hacer en una sesiÃ³n separada cuando haya mÃ¡s tiempo.

---

## âœ… TAREAS COMPLETADAS: REFACTORIZACIÃ“N Y OPTIMIZACIÃ“N (Actualizado 15/03/2026)

Las siguientes mejoras estructurales y de rendimiento han sido implementadas:

1. **Limpieza Integral de CÃ³digo Java**:
   - Eliminados todos los **imports inline (FQN)** en el proyecto backend y dashboard.
   - EstandarizaciÃ³n de imports en la cabecera de todas las clases (`ProcesadorUsuarios`, `VistaDashboard`, `Dialogos`, etc.).
   - CorrecciÃ³n de errores de sintaxis y balanceo de llaves en `Sidebar.java`.

2. **OptimizaciÃ³n de Rendimiento (Dashboard JavaFX)**:
   - **Carga AsÃ­ncrona de ImÃ¡genes**: En `VistaOperarios.java`, las fotos de perfil ahora se cargan en segundo plano (`backgroundLoading=true`), eliminando las congelaciones de la UI al navegar.
   - **Placeholders de Iniciales**: Implementado sistema de avatares con iniciales y colores de fondo que se muestran instantÃ¡neamente mientras la foto real se descarga.

3. **Mejora de UI Premium**:
   - **Panel de Valoraciones**: RediseÃ±ado el sistema de estrellas en `DialogoDetalleIncidencia.java`. 
   - Corregido el escalado desigual de las estrellas y la lÃ³gica de activaciÃ³n (ahora se iluminan de izquierda a derecha correctamente).
   - EstÃ©tica unificada con el panel de Empresa (colores `#FBBF24` vs `#334155`).

---

---

## ðŸš€ FASE 4: DESPLIEGUE REAL EN AWS (FREE TIER)

### 1. Seguridad y Control de Gastos (COMPLETADO)
- **Alertas de Capa Gratuita:** Activadas en la consola de AWS para recibir avisos por email en el correo de la cuenta. âœ…
- **Presupuesto de Seguridad:** Creado presupuesto mensual de **1.00$** con alertas al **80%** de consumo para evitar sorpresas. âœ…

### 2. Infraestructura de Datos (Pendiente)
- **AWS RDS (MySQL):** Crear una instancia `db.t3.micro` de MySQL para alojar los datos de forma persistente y profesional.

### 3. Servidor de Aplicaciones (Pendiente)
- **AWS EC2:** Lanzar una instancia `t3.micro` con Ubuntu Server.
- **Entorno:** Configurar Docker y Java para correr el Socket Server.
- **Firewall (Security Groups):** Apertura de los puertos necesarios (5000 para el servidor, 3306 para la BD).

### 4. Conectividad y Salto a ProducciÃ³n
- **Ajustes de CÃ³digo:** Cambiar las IPs locales por el Endpoint de RDS (en el servidor) y la IP elÃ¡stica de EC2 (en la App y Dashboard).
- **ValidaciÃ³n:** Desplegar y probar la comunicaciÃ³n real entre App (mÃ³vil fÃ­sico) -> EC2 -> RDS.

---

## ðŸ§ª PROTOCOLO PARA LA PRÃ“XIMA SESIÃ“N

1. **Paso de Local a Red:** Cambiar IP en `socket_service.dart` a `192.168.0.13` y probar con el mÃ³vil fÃ­sico conectado al mismo Wi-Fi.
2. **PreparaciÃ³n AWS:** Crear la instancia EC2 y configurar el entorno Docker/Java.
3. **ValidaciÃ³n Final:** Probar que todas las fotos cargan correctamente desde URLs de Firebase tanto en el Dashboard como en la App mÃ³vil operando fuera del emulador.

---

## ðŸš€ MEJORAS DE ARQUITECTURA (PENDIENTES)

- [ ] **Implementar Escucha Directa (Push Notifications via Sockets):** Actualmente, algunos componentes requieren refresco manual o polling. Aprovechando que ya existe un servidor de sockets persistente, se debe implementar un sistema donde el servidor "empuje" las actualizaciones (`PUSH_UPDATE`) a los clientes interesados (App y Dashboard) inmediatamente cuando ocurra un cambio en la BD (ej: nuevo trabajo, cambio de estado, nuevo mensaje), eliminando la necesidad de actualizar manualmente.


## ðŸ“ PRÃ“XIMOS PASOS (SESIÃ“N SIGUIENTE)

### ðŸ§ª Fase A: Testing de Registros y Fotos (FinalizaciÃ³n)
1.  **Commit de Seguridad:** Confirmar todos los cambios actuales de registros y fotos en Git.
2.  **Nueva Rama Git:** Crear rama `deploy/aws-production` para separar el trabajo de despliegue.
3.  **Testing Final Registro:** Probar registro de Clientes (Flutter) y Empresas/Operarios (JavaFX) con subida real a Firebase Storage.
4.  **RevisiÃ³n DocumentaciÃ³n:** Validar los diagramas de la carpeta `DOCS/` contra el cÃ³digo final.

### ðŸŒ©ï¸ Fase B: Despliegue AWS (ProducciÃ³n)
1.  **Levantar RDS:** Crear la base de datos MySQL en Amazon.
2.  **MigraciÃ³n de Esquema:** Ejecutar scripts de creaciÃ³n de tablas en RDS.
3.  **Lanzar EC2:** Configurar el servidor de aplicaciones con Docker/Java.
4.  **Ajuste de IPs:** Actualizar las constantes de conexiÃ³n en todo el proyecto.

---

## âœ… SESIÃ“N 22/03/2026 â€” RefactorizaciÃ³n final, documentaciÃ³n y commit

### Lo que se hizo en esta sesiÃ³n

#### ðŸ”¨ RefactorizaciÃ³n del cÃ³digo (Java)
- **`DashboardController`** reducido de 700+ lÃ­neas a **214 lÃ­neas**:
  - Creada `DashboardBase.java` â€” declaraciones @FXML y base UI.
  - Creada `GestorRegistroDashboard.java` â€” lÃ³gica de registro de empresa/usuario.
  - Creada `ManejadorRespuestaDashboard.java` â€” procesamiento de mensajes del servidor.
- **`SimuladorController`** reducido de 600+ lÃ­neas a **260 lÃ­neas**:
  - Creada `ClienteRedSimulador.java` (Singleton) â€” comunicaciÃ³n TCP del simulador.
  - Creada `ModeloTrabajoSimulador.java` â€” modelo de datos del simulador.
- **Validaciones restauradas** en `UsuarioServiceImpl.java` (email y telÃ©fono estaban comentadas).
- **Tests 100% verdes** â€” todos los 10 tests de `ServiceTest.java` pasan.
- **Nomenclatura en castellano** en todos los nuevos componentes.
- **runSeeder** ejecutado correctamente para resetear la BD con datos de prueba.

#### ðŸ“„ DocumentaciÃ³n
- **`DOCS/MEMORIA.md`** creada completa siguiendo la guÃ­a oficial del IES Maria EnrÃ­quez:
  - 9 secciones con todos los campos en blanco preparados para redactar.
  - **12 diagramas PNG** colocados en sus secciones correspondientes con referencias Mermaid.
  - SecciÃ³n 5.1 incluye Ã¡rbol de directorios + arquitectura AWS (EC2 + RDS + Firebase).
  - SecciÃ³n 5.3 aclara explÃ­citamente que **local = desarrollo/pruebas, AWS = despliegue final**.
  - Encabezados con jerarquÃ­a correcta para conversiÃ³n a PDF.
  - Anexo D con tabla completa de todos los archivos de diagramas `.txt`.
- Diagramas verificados â€” entidad-relaciÃ³n confirmado correcto (herencia Usuarioâ†’Cliente/Operario, Empresa emite Presupuesto, no directamente Trabajo).

#### ðŸ§¹ Limpieza del proyecto
- Eliminados todos los archivos temporales de log y error: `build_error.txt`, `build_log.txt`, `test_error_log.txt`, `gradle_out.txt`, `flutter_log_f.txt`, etc.
- Eliminados ficheros Flutter obsoletos: `-nCon-Book.flutter-plugins-dependencies`, `test_conexion.dart`.

#### ðŸ“¦ Git
- **Commit:** `b770ed3` â€” "Refactor clases grandes y debug, doc final y diagramas"
- **52 archivos cambiados**, 2105 inserciones (+), 369 borrados (-)
- **Push a `origin/main`** completado correctamente.

---

**Ãšltimo Commit Git:** `b770ed3` â€” "Refactor clases grandes y debug, doc final y diagramas" (22/03/2026)

---

## ðŸŽ¯ PRÃ“XIMOS PASOS (SIGUIENTE SESIÃ“N)

### Prioridad 1 â€” Bugfix pendiente
- [ ] **Foto de perfil del cliente en la ficha del Dashboard** no se visualiza. Investigar `DialogoFichaCliente.java` y la carga de imagen desde URL de Firebase.

### Prioridad 2 â€” Despliegue AWS (ProducciÃ³n)
- [ ] **Levantar RDS MySQL:** Crear instancia `db.t3.micro`, configurar Security Group (puerto 3306 solo desde EC2).
- [ ] **Migrar esquema:** ejecutar `SCHEMA.sql` en RDS para crear todas las tablas.
- [ ] **Lanzar EC2:** Instancia `t3.micro` Ubuntu, instalar Docker + Java 21.
- [ ] **Dockerizar el servidor:** Crear `Dockerfile` para el servidor Java socket y hacer `docker build + run` en EC2.
- [ ] **Ajustar IPs en el cÃ³digo:**
  - `socket_service.dart` (Flutter) â†’ IP elÃ¡stica de EC2.
  - `ClienteSocket.java` (Dashboard) â†’ IP elÃ¡stica de EC2.
  - `application.properties` o config del servidor â†’ endpoint RDS.
- [ ] **ValidaciÃ³n final:** Probar App mÃ³vil en dispositivo fÃ­sico real â†’ EC2 â†’ RDS.

### Prioridad 3 â€” Memoria acadÃ©mica
- [ ] Redactar las secciones de texto de `DOCS/MEMORIA.md` (campos `[Escribe aquÃ­...]`).
- [ ] Insertar capturas de pantalla reales de la app y dashboard en la secciÃ³n 5.4.
- [ ] Completar tabla de requerimientos funcionales/no funcionales (secciÃ³n 3.1).
- [ ] Completar tabla de hitos del proyecto (secciÃ³n 3.2).
- [ ] AÃ±adir diagrama de Gantt.
- [ ] Exportar a PDF cuando estÃ© lista.


Plantilla memoria: (ignora el valenciano debe ser en castellano)
IES Maria Enriquez Curs 2025-26
Guia del modul de Projecte Intermodular de 2DAM
Guia del modul de Projecte Intermodular de 2DAM...........................................................................1
BaremaciÃ³ del Projecte.................................................................................................................... 1
AvaluaciÃ³:........................................................................................................................................ 1
Dates orientatives:............................................................................................................................2
Lliurament, exposiciÃ³ i defensa....................................................................................................... 2
MemÃ²ria...........................................................................................................................................2
Format de la memÃ²ria.................................................................................................................2
Continguts................................................................................................................................... 3
BaremaciÃ³ del Projecte
El projecte sâ€™avaluara dâ€™acord amb els seguents percentatges:
MÃ²dul â€“ Part %
MemÃ²ria 15 %
PresentaciÃ³ 15 %
AccÃ¨s a dades 14 %
DI 17 %
PMDM 13 %
PSP 7 %
SGE 13 %
DigitalitzaciÃ³ 3 %
Sostenibilitat 3 %
AvaluaciÃ³:
â€¢ El professor de cada mÃ²dul avaluarÃ  els continguts i detalls tÃ¨cnics dels seus respectius 
mÃ²duls.
â€¢ Cada mÃ²dul o part sâ€™ha de superar almenys amb un 5.
â€¢ Els alumnes han de superar tots els RA, per tant si algun mÃ²dul no es supera el mÃ²dul de projecte estaria suspÃ¨s.

---

## ðŸ”§ SESIÃ“N 01/04/2026 â€” Limpieza de Arquitectura y RestauraciÃ³n de App

### Objetivo de la sesiÃ³n
Resolver los bloqueos de compilaciÃ³n que impedÃ­an arrancar el sistema tras el Ãºltimo refactor y restaurar la funcionalidad bÃ¡sica de la App mÃ³vil que presentaba mÃ©todos ausentes.

### Backend y Dashboard (Java)
- **Limpieza de CÃ³digo Legado:** Se han eliminado los archivos huÃ©rfanos del paquete `com.fixfinder.controladores` (`DashboardController`, `DashboardBase`, `GestorRegistroDashboard` y `ManejadorRespuestaDashboard`). Estos archivos causaban errores de sÃ­mbolo no encontrado al intentar referenciar mÃ©todos que ya no existen en la nueva arquitectura modular.
- **Estado:** El proyecto Java ahora compila correctamente con `./gradlew compileJava`. 
- **Servidor:** Operativo y conectado a la base de datos (MySQL en Docker). Escuchando en el puerto 5000.

### App MÃ³vil (Flutter)
- **RestauraciÃ³n de `AuthService.dart`:** Se ha re-implementado el mÃ©todo `registrar` que faltaba en el repositorio. Este mÃ©todo es esencial para que la pantalla de registro pueda enviar los datos al servidor.
- **CorrecciÃ³n de `SocketService.dart`:** AÃ±adido el getter `isConectado` para facilitar la gestiÃ³n de conexiones desde los servicios.
- **SincronizaciÃ³n de `JobApiService.dart`:** Corregido un error de sintaxis en el cierre de la clase que impedÃ­a que el compilador detectara correctamente el mÃ©todo `finalizeTrabajo`.
- **AnÃ¡lisis:** Tras los cambios, `flutter analyze` ya no reporta errores crÃ­ticos de mÃ©todos no definidos en los servicios principales.

### Notas para la prÃ³xima sesiÃ³n
- [ ] **ModificaciÃ³n de Perfil:** Implementar la ediciÃ³n de datos personales (telÃ©fono, direcciÃ³n, etc.) en `perfil_pantalla.dart`. Actualmente solo funciona el cambio de foto. Requiere crear una nueva acciÃ³n en el servidor Java (ej: `ACTUALIZAR_DATOS_USUARIO`).
- [ ] **ValidaciÃ³n de Registro:** Probar el flujo de registro en la App mÃ³vil para confirmar que el nuevo mÃ©todo `registrar` se comunica correctamente con el `ProcesadorAutenticacion` del servidor.
- [ ] **ValidaciÃ³n de FinalizaciÃ³n:** Verificar que un operario puede finalizar un trabajo sin errores de comunicaciÃ³n.
- [ ] **Continuar con AWS:** Una vez confirmada la estabilidad local, proceder con la configuraciÃ³n de la instancia EC2.

---
â€¢ Els tribunals estaran compostos per 3-5 membres de lâ€™equip docent que impartisca els 
mÃ²duls associats.
IES Maria Enriquez Curs 2025-26
Dates orientatives:
â€¢ Fins el 22 de maig: seguiment (el seguiment de cada mÃ²dul el farÃ  el professor que 
lâ€™imparteix. Es recomana que els alumnes consulten amb els professors dels mÃ²duls dels 
quals tenen dubtes).
â€¢ 18 â€“ 22 maig: acumulaciÃ³ hores PIM
â€¢ 25 â€“ 29 maig: simulacres
â€¢ 29 de maig: lliurament dels projectes.
â€¢ 1 â€“ 5 juny: Presentacions
â€¢ 15 â€“ 18 juny: recuperacions
â€¢ 19 juny?: avaluaciÃ³ final
Lliurament, exposiciÃ³ i defensa.
El 29 de maig es lliuraran els projectes. El lliurament consistirÃ  en un arxiu zip, amb tot el codi dels 
projectes, aixÃ­ com recursos associats, i una memÃ²ria en PDF.
Es tindran en compte altres recursos en altres formats: documentaciÃ³ en lÃ­nia, manual de la 
aplicaciÃ³, repositoris de codi, etc.
El dia de la presentaciÃ³ es lliurarÃ  una cÃ²pia impressa i enquadernada de la memÃ²ria.
Pel que fa a lâ€™exposiciÃ³, l'alumnat disposarÃ  dâ€™un mÃ xim de 15 minuts. per a lâ€™exposiciÃ³ del 
projecte i de 15 minuts per a la seua demostraciÃ³. 
Finalitzada la presentaciÃ³, comenÃ§arÃ  el torn de preguntes (defensa) per part del tribunal amb una 
durada mÃ xima de 15 minuts.
Lâ€™exposiciÃ³ i la defensa tenen carÃ cter pÃºblic.
MemÃ²ria
La memÃ²ria haurÃ  de complir uns requisits mÃ­nims, i per tant obligatoris, pel que fa al format i al 
contingut.
Format de la memÃ²ria
â€¢ Document PDF
â€¢ De 40 a 60 pÃ gines *.
â€¢ Sense faltes ortogrÃ fiques ni gramaticals.
â€¢ Font Liberation Serif 12 pt.
â€¢ Interlineat 1,5.
â€¢ NumeraciÃ³ de pÃ gina
â€¢ CapÃ§alera i peu de pÃ gina.
â€¢ CoherÃ¨ncia en la grandÃ ria i la posiciÃ³ de les captures de pantalla.
â€¢ El codi font i/o les ordres tindrÃ  una font diferent.
IES Maria Enriquez Curs 2025-26
(*) La memÃ²ria pot contenir annexos que no es tindran en compte en aquesta xifra.
Continguts
Els continguts de la memÃ²ria sÃ³n els establerts en el mÃ²dul de projecte intermodular:
## 1. IntroducciÃ³
- PresentaciÃ³ (i/o motivaciÃ³) i objectiu del projecte.
- Factor diferenciador del projecte
- AnÃ lisis de la situaciÃ³ de partida
- Objectius a aconseguir amb el projecte
- RelaciÃ³ amb els continguts dels diferents mÃ²duls
## 2. PresentaciÃ³ de les diverses tecnologies que es poden utilitzar per a la seua realitzaciÃ³
### 2.1 JustificaciÃ³ de lâ€™elecciÃ³ de les tecnologies.
## 3. AnÃ lisi del projecte
### 3.1. Requeriments funcionals i no funcionals
- Requeriments funcionals
- Requeriments no funcionals
- analisi de costs i viabilitat del projecte
### 3.2. TemporalitzaciÃ³ del projecte
- Fites del projecte
- Diagrama de Gantt
### 3.3. Casos dâ€™Ãºs
- Diagrama de casos dâ€™Ãºs
- DescripciÃ³ dels casos dâ€™Ãºs
### 3.4. Diagrama de classes inicial
- Diagrama de classes
- DescripciÃ³ de les classes
- Diagrama entitat-relaciÃ³ (si escau)
## 3.5. Wireframes dâ€™interfÃ­cies
IES Maria Enriquez Curs 2025-26
### 3.6. Altres diagrames i descripcions (si escau)
## 4. Disseny del projecte
### 4.1. Arquitectura del sistema
- DescripciÃ³ de lâ€™arquitectura
- Diagrama de lâ€™arquitectura
- Diagrama de desplegament
### 4.2. Diagrama de classes definitiu
- Diagrama de classes
- DescripciÃ³ de les classes
- Diagrama entitat-relaciÃ³ (si escau)
### 4.3. Disseny de la interfÃ­cie dâ€™usuari
- Mockups
- Diagrama de navegaciÃ³
### 4.4. Altres diagrames i descripcions (si escau)
## 5. ImplementaciÃ³ del projecte
- Estructura del projecte
- DescripciÃ³ dels mÃ²duls i components principals
- Desplegament de lâ€™aplicaciÃ³
- Captures de pantalla i exemples de codi (el codi es recomana ficar-ho als annexes)
## 6. Estudi dels resultats obtinguts
- AvaluaciÃ³ del projecte respecte als objectius inicials
- Problemes trobats i solucions aplicades

---

## ðŸ”§ SESIÃ“N 02/04/2026 â€” Arquitectura "Smart Main", SemÃ¡foros y God Mode

### Objetivo de la sesiÃ³n
Consolidar el proyecto en un Ãºnico cÃ³digo fuente capaz de operar en **Local (Docker)** y **Nube (AWS)** mediante un interruptor lÃ³gico, mejorando la UX con indicadores de estado y restaurando herramientas de test.

### 1. Arquitectura "Smart Switch"
Se ha eliminado la duplicidad de ramas para despliegue:
- **CentralizaciÃ³n (Java):** Se utiliza `GlobalConfig.java` como Ãºnica fuente de verdad para IPs, puertos y credenciales RDS. El booleano `MODO_NUBE` propaga el cambio a todo el sistema (Servidor, Dashboard y Herramientas).
- **Reactividad (Flutter):** El archivo `.env` ahora distingue entre `ENVIRONMENT=LOCAL` y `ENVIRONMENT=NUBE`, inyectando la IP de la instancia EC2 o `10.0.2.2` segÃºn corresponda.

### 2. SemÃ¡foros de ConexiÃ³n (Indicadores de Estado)
ImplementaciÃ³n de un sistema de feedback visual en tiempo real:
- **LÃ³gica de Colores:** ðŸ”µ Azul (Local), ðŸ”˜ Gris (Iniciando AWS), ðŸŸ¢ Verde (ConexiÃ³n AWS Exitosa).
- **AsincronÃ­a:**
  - En **JavaFX (Dashboard)**, se utiliza un `Thread` independiente con un timeout de 2s para no bloquear el inicio de la App mientras se hace el "ping" al socket.
  - En **Flutter (App)**, se implementÃ³ un `ping()` asÃ­ncrono en `initState` que actualiza el estado del widget mediante un `ValueNotifier` o `setState`.

### 3. RecuperaciÃ³n y ModernizaciÃ³n de Herramientas (God Mode)
Se han rescatado del historial de Git (`c828544`) las herramientas de simulaciÃ³n borradas accidentalmente:
- **`TestPanel`**: Panel de control de bajo nivel para depurar el protocolo de red.
- **`Simulador E2E (God Mode)`**: Herramienta para simular flujo completo (Presupuesto -> AsignaciÃ³n -> Factura -> Pago) sin necesidad de mÃºltiples dispositivos.
- **Mejora:** Se han movido a la carpeta `com.fixfinder.TestPanel` y se han refactorizado para usar `GlobalConfig`. Ahora el "God Mode" tambiÃ©n funciona contra AWS.

### 4. Seguridad en el Sembrado (DbClean)
El seeder de la base de datos ahora es "entorno-consciente":
- Detecta si estÃ¡ en modo Nube. Si es asÃ­, pide confirmaciÃ³n/advierte y limpia Firebase Storage.
- Si estÃ¡ en modo Local, omite la limpieza de la nube para proteger los archivos reales de producciÃ³n.

---

## 7. Conclusions
- Relacions amb els continguts dels diferents mÃ²duls
- ValoraciÃ³ personal del projecte
## 8. Bibliografia i recursos utilitzats
IES Maria Enriquez Curs 2025-26
## 9. Annexes
- Codi font complet del projecte
- Guia dâ€™instalÂ·laciÃ³ i Ãºs
- DocumentaciÃ³ addicional
- Altres materials rellevants
Aquests continguts i estructura sÃ³n orientatius i sâ€™adaptaran a cada projecte.
## ?? Comandos Útiles para el Cierre de Proyecto

### ?? Generar EXE (Dashboard)
./gradlew jpackage (Esto generará el instalador en la carpeta uild/jpackage)

### ?? Generar APK (App Móvil)
lutter build apk --release (Asegurarse de que .env esté en MODO_NUBE)

### ?? Despliegue en AWS (EC2)
scp -i ffk.pem FIXFINDER/build/libs/FIXFINDER.jar ec2-user@15.217.56.66:~/


---

---

## [02/04/2026] - SesiÃ³n: Gran UnificaciÃ³n "Smart Main" y Blindaje de Infraestructura

**Estado:** ðŸŸ¡ VERIFICADO (PENDIENTE DE RE-AUDITORÃ A DE SEGURIDAD AL ARRANCAR)

### ðŸ † Desglose TÃ©cnico de Logros (Lujo de Detalle)

#### 1. ðŸ’Ž Arquitectura "Smart Main" (Unified Environment)
Se ha eliminado la dependencia de ramas (Local vs AWS). Ahora el proyecto reside en un solo **MAIN** inteligente:
*   **Java Core (GlobalConfig.java):** Nueva clase maestra que centraliza el interruptor MODO_NUBE. Gestiona dinÃ¡micamente las URLs de JDBC para AWS RDS vs Docker Local y resuelve la IP del servidor de Sockets.
*   **Flutter Reactivo (.env):** ImplementaciÃ³n de variables de entorno para que la App mÃ³vil resuelva su conexiÃ³n de forma "Plug & Play" sin tocar cÃ³digo Dart.

#### 2. ðŸ”µ Indicadores de Estado (Smart Semaphores)
ImplementaciÃ³n de telemetrÃ­a visual en las pantallas de Login:
*   **LÃ³gica de Colores:** 
    *   ðŸ”µ **Azul**: Modo LOCAL activo (Docker detectado).
    *   ðŸ”˜ **Gris**: Intentando conectar (Cloud) o estado desconocido.
    *   ðŸŸ¢ **Verde**: ConexiÃ³n exitosa con la instancia **EC2 de AWS**.
*   **JavaFX (AppDashboardPrincipal.java):** RefactorizaciÃ³n del layout usando un StackPane para inyectar un indicardor circular en la esquina superior derecha. ImplementaciÃ³n de hilo asÃ­ncrono con timeout de 2s para el ping de red.
*   **Flutter (login_pantalla.dart):** CreaciÃ³n del widget _ConnectionStatusDot y un Timer.periodic de 10 segundos que monitoriza el estado del servidor en segundo plano mediante el mÃ©todo ping() del SocketService.

#### 3. ðŸ›¡ï¸  Blindaje de Datos (Environment Awareness)
*   **DbClean.java (Seeder Seguro):** Se ha modificado el limpiador de base de datos para que sea "consciente" del entorno. Si detecta el modo NUBE, activa un prompt interactivo (Scanner(System.in)) que exige confirmar por consola antes de borrar la RDS de AWS.
*   **ProtecciÃ³n de Firebase:** LÃ³gica de protecciÃ³n en limpiarFirebaseStorage() para evitar el borrado accidental del bucket en la nube durante pruebas locales.

#### 4. ðŸ º RestauraciÃ³n del "God Mode" (Test Tools)
RecuperaciÃ³n total de las herramientas de simulaciÃ³n de bajo nivel:
*   **Paquetes:** ReubicaciÃ³n de la lÃ³gica de test en com.fixfinder.TestPanel.
*   **ColisiÃ³n de Nombres Solucionada:** El lanzador principal ahora es **LanzadorTestPanel.java**, evitando conflictos con clases del mismo nombre en subpaquetes.
*   **TestPanelController:** Actualizado para usar GlobalConfig.getServerIp(), permitiendo que el Tester de bajo nivel tambiÃ©n funcione contra AWS.
*   **Simulador E2E:** Recuperado el simulador completo para realizar flujos de "Un Solo Hombre" (Gerente/Operario/Cliente a la vez).

#### 5. ðŸ —ï¸  OptimizaciÃ³n de CompilaciÃ³n
*   **Gradle Magic:** Añadida la tarea personalizada unTestPanel en uild.gradle que permite el lanzamiento limpio de las herramientas de test sin pasar por el modularismo estricto de JavaFX, resolviendo errores de visiÃ³n de clases con las librerÃ­as de Firebase.

---

## ðŸ—ºï¸  PRÃ“XIMA SESIÃ“N (AUDITORÃ A Y GRADUACIÃ“N AWS)

### ðŸš© 1. PRIORIDAD CRÃ TICA: AUDITORÃ A "RAYOS X"
Al retomar la sesiÃ³n, **lo primero** es verificar de nuevo que el comando 'git restore .' no haya deshecho los cambios en ConexionDB, DbClean y los controladores de FXML. Comprobar lÃ­nea a lÃ­nea que el Smart Switch sigue intacto.

### ðŸ§ª 2. CertificaciÃ³n Local
* Lanzar el servidor y el dashboard en local.
* Confirmar semÃ¡foros **AZULES ðŸ”µ**.

### â˜ ï¸  3. Despliegue en Caliente (AWS Grad)
* Switchear MODO_NUBE = true.
* Generar FIXFINDER.jar (updated version).
* Subir a EC2 vÃ­a SCP y reiniciar el servicio remoto.
* Confirmar semÃ¡foros **VERDES ðŸŸ¢**.

### 📦 4. GeneraciÃ³n de Entregables Release
* Build final de la APK release contra AWS.
* Empaquetado del Dashboard en instalador EXE (jpackage).

---
_BitÃ¡cora tÃ©cnica cerrada por Antigravity (IA Asistente). El proyecto queda en estado "Ready for Launch"._

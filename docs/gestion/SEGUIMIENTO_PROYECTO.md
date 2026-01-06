# Seguimiento del Proyecto - FIXFINDER

## 🛑 ROLES Y RESPONSABILIDADES

Este proyecto sigue una metodología estricta de colaboración:

1.  **EL USUARIO (Arquitecto):**

    - Toma TODAS las decisiones.
    - Define el "qué" y el "cuándo".
    - Tiene la última palabra en arquitectura y funcionalidad.

2.  **LA IA (Profesor y Mano de Obra):**
    - **NO escribe código sin permiso explícito.**
    - Actúa como **PROFESOR**: Guía, explica conceptos, propone mejores prácticas, y expone PROS y CONTRAS de cada decisión.
    - Actúa como **MANO DE OBRA**: Ejecuta las órdenes del Arquitecto una vez aprobadas.
    - Su objetivo es que el usuario ENTIENDA lo que se está construyendo, no solo que funcione.

---

## 🟢 Estado Actual: Sistema de Usuarios y Red Funcional ✅

Hemos alcanzado un hito crítico: **El sistema de Registro de Usuarios está completo, integrado y validado.**
La aplicación ya permite el flujo completo de alta para todos los actores a través de la red (Sockets).

**Logros Recientes:**

- **Protocolo de Red (`REGISTRO`):** Implementado en Servidor (`GestorConexion`) manejando JSONs complejos polimórficos.
- **Persistencia Transaccional:**
  - Registro atómico de `Empresa` + `Gerente`.
  - Registro de `Operario` con validación de clave foránea (`idEmpresa`) y transacciones manuales corregidas.
  - Registro de `Cliente` funcional.
- **Cliente de Pruebas (Dashboard JavaFX):**
  - Se ha evolucionado el "Dashboard" para servir como herramienta de test integral.
  - Formularios dinámicos para dar de alta Empresas, Clientes y Operarios.
  - Feedback visual de errores (Logs en pantalla).

---

## 🚀 Siguientes Pasos: Gestión de Trabajos

Con los actores ya creados en el sistema, el siguiente paso es implementar la lógica central del negocio: **La solicitud y gestión de servicios de reparación.**

### 1. Funcionalidad: Crear Trabajo (`CREAR_TRABAJO`)

- **Desde el Cliente:** Enviar solicitud con Título, Descripción y Categoría.
- **En Servidor:**
  - Validar cliente.
  - Crear registro en tabla `trabajo` (Estado inicial: `PENDIENTE`).
  - Responder con ID del trabajo.

### 2. Funcionalidad: Gestión para Empresa (`LISTAR_TRABAJOS`, `ASIGNAR_OPERARIO`)

- La empresa debe poder ver qué trabajos se han solicitado en su área/categoría (o asignación directa, según definamos).
- Asignar un Operario libre al trabajo.

### 3. Dashboard

- Añadir pestaña "Solicitar Servicio" para probar la creación de trabajos.
- Añadir vista para que la Empresa vea las solicitudes.

---

## 🗺️ Roadmap Actualizado

- [x] **Fase 1: Infraestructura y BD** (Completado)
- [x] **Fase 2: Lógica de Negocio (Servicios)** (Completado)
- [x] **Fase 3: Capa de Red - Autenticación y Registro** (COMPLETADO ✅)
  - [x] Protocolo Login.
  - [x] Protocolo Registro (Empresa/Op/Cli).
  - [x] Validación Transaccional.
- [x] **Fase 4: Capa de Red - Gestión de Trabajos** (EN PROCESO)
  - [x] Solicitar Trabajo (`CREAR_TRABAJO`).
  - [x] Listar Trabajos (`LISTAR_TRABAJOS`) con vista por roles.
  - [ ] Filtrado Negocio Empresa (Privacidad).
  - [ ] Detalle de Trabajo (UI).
  - [ ] Asignar Operario.
  - [ ] Finalizar Trabajo.
- [x] **Fase 5: Herramientas de Prueba (UI Dashboard)** (Adelantado y Funcional)
  - [x] Pestaña Registro.
  - [x] Pestaña Login.
  - [x] Pestaña Solicitar Servicio.
  - [x] Pestaña Mis Trabajos (Tabla dinámica).

---

## 📝 Estado Detallado y Pendientes (Sesión Actual)

### ✅ Completado

1.  **Refactorización del Dashboard:**
    - Limpieza de nombres de clase FXML y adición de imports para corregir `LoadException`.
    - Modularización de la lógica de red en `ServicioCliente.java` y `RespuestaServidor.java`.
2.  **Gestión de Trabajos (Básico):**
    - Implementación del protocolo `CREAR_TRABAJO` con título y descripción.
    - Implementación de `LISTAR_TRABAJOS`.
3.  **Corrección de Bugs Críticos:**
    - **Rol Gerente:** Se corrigió un error grave en `OperarioDAO` y `UsuarioDAO` donde el rol `GERENTE` se guardaba y leía hardcodeado como `OPERARIO`. Ahora el sistema distingue correctamente y permite al Gerente ver todos los trabajos.

### ⏳ Pendiente (Próxima Prioridad)

**1. Lógica de Negocio y Privacidad (Empresas)**
El Gerente actualmente ve _todos_ los trabajos. Se debe refinar esta lógica para garantizar la privacidad y flujo correcto entre competencias:

- **Regla de Visibilidad:**
  - Un Gerente debe ver **Trabajos PENDIENTES** (Mercado libre, disponibles para coger).
  - Un Gerente debe ver **Trabajos ASIGNADOS** a operarios de **SU** propia empresa.
  - Un Gerente **NO** debe ver trabajos ya aceptados/asignados por **OTRAS** empresas.
- **Implementación:** Requiere filtro en backend (Service/DAO) comparando `idEmpresa` del operario asignado.

**2. Mejoras UI (Tabla de Trabajos)**

- La tabla actual es básica. Se necesita ver todos los detalles del trabajo (descripción completa, dirección, datos extendidos del cliente/operario).
- **Solución propuesta:** Implementar evento de selección o **Doble Clic** en la tabla para abrir una ventana emergente (Popup/Alert) con la ficha completa del trabajo.

## 📄 Documentación Adicional

- [Flujo Principal de Uso (Happy Path)](FLUJO_PRINCIPAL.md): Descripción detallada del ciclo de vida del servicio.

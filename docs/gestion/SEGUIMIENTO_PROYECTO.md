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
- [ ] **Fase 4: Capa de Red - Gestión de Trabajos** (EN PROCESO)
  - [ ] Solicitar Trabajo.
  - [ ] Asignar Operario.
  - [ ] Finalizar Trabajo.
- [x] **Fase 5: Herramientas de Prueba (UI Dashboard)** (Adelantado y Funcional)

## 📄 Documentación Adicional

- [Flujo Principal de Uso (Happy Path)](FLUJO_PRINCIPAL.md): Descripción detallada del ciclo de vida del servicio.

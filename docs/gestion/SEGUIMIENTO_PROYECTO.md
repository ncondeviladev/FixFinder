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

## 🟢 Estado Actual: Consistencia de Datos ✅

Hemos logrado un **hito importante**: La capa de Persistencia (Datos) es CONSISTENTE.

- **Modelos de Datos:** Clases POJO (`Usuario`, `Empresa`, `Trabajo`, etc.) bien definidas.
- **DAOs (Acceso a Datos):** Implementados y seguros (`UsuarioDAO`, `EmpresaDAO`, `TrabajoDAO`, etc.), con transacciones y manejo de excepciones.
- **Base de Datos:** Estructura SQL y tablas creadas.

Ahora tenemos unos cimientos sólidos sobre los que edificar la lógica.

---

## 🚀 Siguientes Pasos: Lógica de Negocio y Simulación

El siguiente objetivo es dotar de "inteligencia" a los datos mediante la **Capa de Servicios** y probarla sin depender de una interfaz gráfica compleja.

### 1. Implementación de Servicios (Business Logic Layer)

Debemos crear las clases que encapsulen las REGLAS DE NEGOCIO. El DAO solo guarda/lee, pero el Service "piensa".

- **`UsuarioService`**: Ya iniciado. Debe gestionar Login, validaciones de registro, hashing de claves.
- **`EmpresaService`**: Validar altas de empresas, garantizar unicidad de CIF, gestionar especialidades.
- **`TrabajoService`**:
  - Validar que un trabajo tenga cliente.
  - Lógica de asignación: ¿El operario está libre? ¿Tiene la especialidad correcta?
  - Transiciones de estado: PENDIENTE -> ASIGNADO -> FINALIZADO.
- **`OperarioService`**: Gestionar disponibilidad (Ocupado/Libre), ubicación y filtrado de operarios compatibles.

### 2. Interfaz de Simulación (Terminal)

Para validar toda esta lógica sin perder tiempo en botones y diseños (GUI) por ahora, crearemos un menú interactivo en consola.

**Funcionalidad esperada del menú:**

1.  Login (Usuario/Empresa/Operario).
2.  (Como Admin) Registrar una Empresa y Operarios.
3.  (Como Cliente) Crear una solicitud de Trabajo.
4.  (Como Empresa) Listar trabajos pendientes y asignar un Operario.
5.  (Como Operario) Ver trabajos y marcarlos como finalizados.

---

## 🗺️ Roadmap Actualizado

- [x] **Fase 1: Infraestructura y BD** (Completado)
- [ ] **Fase 2: Lógica de Negocio (Servicios)** (PRIORIDAD ALTA)
- [ ] **Fase 3: Simulación en Terminal** (PRIORIDAD ALTA)
- [ ] **Fase 4: Integración UI (JavaFX)** (Pospuesto)

## 📄 Documentación Adicional

- [Flujo Principal de Uso (Happy Path)](FLUJO_PRINCIPAL.md): Descripción detallada del ciclo de vida del servicio.

# Anexo: Migración a Spring Boot

## ¿Por qué Spring Boot?

La arquitectura original de FixFinder, basada en sockets TCP puros con protocolo binario artesanal, demostró ser una solución sólida y de gran aprendizaje. Sin embargo, a medida que el sistema crecía, se hizo evidente la necesidad de un framework que ofreciera:

- **Estandarización de endpoints** mediante una API REST bien definida.
- **Gestión automática de la persistencia** con un ORM maduro.
- **Simplificación del despliegue** gracias a la configuración por perfiles y el empaquetado en un solo JAR.
- **Ecosistema de herramientas** para seguridad, validación, pruebas y documentación.

## ¿Qué se ha migrado?

### 1. Comunicación: De Sockets TCP a API REST + WebSockets

**Antes (original):** El cliente y el servidor se comunicaban mediante un protocolo binario propio sobre sockets TCP. Cada mensaje llevaba una cabecera de 4 bytes (Big-Endian) con la longitud del payload JSON, y se utilizaban IDs de transacción (txid) para correlacionar peticiones y respuestas asíncronas. La concurrencia se gestionaba manualmente con un modelo **Dispatcher-Worker** y un **Broadcaster** para notificaciones push.

**Ahora (Spring Boot):** Los clientes se comunican con el servidor mediante **HTTP REST** para las operaciones CRUD, y mediante **WebSockets con STOMP** para las notificaciones en tiempo real. Esto elimina la complejidad del protocolo binario, simplifica la depuración y permite que cualquier cliente HTTP (navegador, Postman, script) pueda interactuar con el sistema.

```mermaid
graph TD
    subgraph "Original - Sockets TCP"
        A[Cliente Flutter] -->|Socket TCP + Protocolo binario| B[Servidor Java]
        C[Dashboard JavaFX] -->|Socket TCP + Protocolo binario| B
        B --> D[MySQL - JDBC manual]
        B --> E[Broadcaster artesanal]
    end

    subgraph "Migrado - Spring Boot"
        F[Cliente Flutter] -->|HTTP REST /api/**| G[Spring Boot Server :8080]
        H[Dashboard JavaFX] -->|HTTP REST /api/**| G
        F -->|WebSocket STOMP /ws| G
        H -->|WebSocket STOMP /ws| G
        G --> I[Spring Data JPA]
        I --> J[MySQL - Hibernate]
        G --> K[STOMP Broker /topic /queue]
    end

    style B fill:#f96,stroke:#333,stroke-width:2px
    style G fill:#6f9,stroke:#333,stroke-width:2px
```

### 2. Persistencia: De DAOs manuales a Spring Data JPA

**Antes:** Cada entidad tenía un DAO propio que ejecutaba consultas SQL mediante JDBC. La conexión se gestionaba con `ThreadLocal` para aislar las transacciones de cada hilo de trabajo.

**Ahora:** Spring Data JPA gestiona automáticamente las conexiones, las transacciones y el mapeo objeto-relacional. Basta con definir interfaces que extienden `JpaRepository` para tener operaciones CRUD completas, consultas personalizadas y paginación.

```mermaid
graph LR
    subgraph "Capa de Persistencia Original"
        A[Controlador] --> B[Servicio]
        B --> C[DAO manual JDBC]
        C --> D[(MySQL)]
        C --> E[ThreadLocal para conexiones]
    end

    subgraph "Capa de Persistencia Spring"
        F[Controller REST] --> G[Service @Service]
        G --> H[Repository extends JpaRepository]
        H --> I[(MySQL)]
        H --> J[Hibernate EntityManager]
    end

    style C fill:#f96,stroke:#333,stroke-width:2px
    style H fill:#6f9,stroke:#333,stroke-width:2px
```

### 3. Arquitectura general del sistema migrado

La nueva arquitectura sigue el patrón clásico de **Spring Boot en tres capas**:

- **Controlador REST** → Recibe peticiones HTTP, delega en servicios, devuelve JSON.
- **Servicio** → Contiene la lógica de negocio, anotada con `@Service` y `@Transactional`.
- **Repositorio** → Capa de acceso a datos, extiende `JpaRepository`.

Las notificaciones en tiempo real (antes gestionadas por el `Broadcaster` artesanal) ahora se realizan mediante **WebSockets STOMP**, con un broker simple que difunde mensajes a los tópicos `/topic` (broadcast) y `/queue` (mensajes privados).

```mermaid
graph TB
    subgraph "Arquitectura Spring Boot"
        direction TB
        R[Router HTTP :8080] -->|/api/auth/**| C_Auth[AuthController]
        R -->|/api/empresas/**| C_Emp[EmpresaController]
        R -->|/api/trabajos/**| C_Trab[TrabajoController]
        R -->|/api/presupuestos/**| C_Pres[PresupuestoController]
        R -->|/api/usuarios/**| C_Usu[UsuarioController]
        R -->|/api/operarios/**| C_Ope[OperarioController]

        C_Auth --> S_Auth[UsuarioService]
        C_Emp --> S_Emp[EmpresaService]
        C_Trab --> S_Trab[TrabajoService]
        C_Pres --> S_Pres[PresupuestoService]
        S_Auth --> R_Usuario[UsuarioRepository]
        S_Emp --> R_Empresa[EmpresaRepository]
        S_Trab --> R_Trabajo[TrabajoRepository]
        S_Pres --> R_Presupuesto[PresupuestoRepository]
        R_Usuario --> DB[(MySQL)]
        R_Empresa --> DB
        R_Trabajo --> DB
        R_Presupuesto --> DB
    end

    subgraph "Tiempo Real"
        WS[WebSocket /ws STOMP] -->|/topic/trabajos| B[Broadcast a todos]
        WS -->|/queue/privado| P[Mensajes privados]
    end

    C_Trab -->|Notificar| WS
    C_Pres -->|Notificar| WS

    style R fill:#4a9,stroke:#333,stroke-width:2px
    style DB fill:#f93,stroke:#333,stroke-width:3px
```

### 4. Configuración por perfiles

Spring Boot permite definir perfiles de configuración para diferentes entornos. FixFinder ahora utiliza dos perfiles:

| Perfil   | Base de datos                     | Uso               |
|----------|-----------------------------------|-------------------|
| `local`  | MySQL en `localhost:3306`         | Desarrollo local  |
| `cloud`  | AWS RDS (MySQL gestionado)        | Producción        |

El perfil activo se selecciona mediante `spring.profiles.active=local` o `spring.profiles.active=cloud` en `application.yml`, eliminando la necesidad de cambiar manualmente las conexiones.

### 5. Dependencias principales del nuevo build

```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'        // API REST
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'   // JPA + Hibernate
    implementation 'org.springframework.boot:spring-boot-starter-websocket'  // WebSockets STOMP
    implementation 'org.springframework.boot:spring-boot-starter-validation' // Validación
    runtimeOnly     'com.mysql:mysql-connector-j'                            // Driver MySQL
    implementation 'com.google.firebase:firebase-admin:9.2.0'                // Firebase Storage
}
```

## ¿Qué se conserva?

- **Los modelos de datos** (entidades Java) se mantienen, ahora anotados con `@Entity`, `@Table`, etc.
- **Firebase Storage** sigue siendo el sistema de almacenamiento de imágenes, ya que funciona independientemente del framework.
- **La lógica de negocio** se traslada intacta a los nuevos `@Service`, adaptando las firmas de los métodos.
- **El Dashboard JavaFX y la App Flutter** siguen siendo los mismos clientes, solo que ahora se comunican mediante HTTP REST + WebSockets en lugar de sockets TCP directos.

## Resumen del cambio

| Aspecto               | Original (Sockets TCP)                       | Migrado (Spring Boot)                       |
|-----------------------|----------------------------------------------|---------------------------------------------|
| Comunicación          | Sockets TCP + protocolo binario (4 bytes)    | HTTP REST + WebSockets STOMP                |
| Persistencia          | DAOs manuales con JDBC + ThreadLocal         | Spring Data JPA (JpaRepository)             |
| Concurrencia          | Dispatcher-Worker + hilos manuales           | Gestionada por Tomcat + Spring              |
| Tiempo real           | Broadcaster artesanal (Observer)             | STOMP Broker (/topic, /queue)               |
| Configuración         | Constantes hardcodeadas                      | application.yml con perfiles                |
| Validación            | Manual en cada procesador                    | `@Valid` + anotaciones Jakarta              |
| Despliegue            | JAR + script manual con variables de entorno | fat JAR con perfil activo (`--spring.profiles.active=cloud`) |
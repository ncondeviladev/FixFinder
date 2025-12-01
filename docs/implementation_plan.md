# Implementation Plan - FIXFINDER

## Goal Description
Desarrollo de un ecosistema Cliente-Servidor completo para la gestión de servicios de reparaciones/mantenimiento. El sistema integra múltiples tecnologías (Java, JavaFX, Flutter, Kotlin, MySQL) para cumplir con los requisitos académicos de 2º de DAM (PSP, Acceso a Datos, DI, PMDM).

## User Review Required
> [!NOTE]
> **Protocol Decision**: Se utilizará **JSON sobre Sockets TCP** como protocolo principal. Esto unifica la comunicación para todos los clientes (JavaFX, Flutter, Kotlin) y cumple rigurosamente con los requisitos de PSP (Sockets) y PMDM (Parsing JSON).
> Estructura base: `{ "action": "LOGIN", "data": { ... }, "token": "..." }`

> [!NOTE]
> **Database Strategy**: Esquema centralizado. Herencia de usuarios mediante relación 1:1 (Usuario -> Operario).

## Proposed Architecture

### 1. Capa de Datos (MySQL/MariaDB)
**Esquema Relacional Propuesto:**
- **Empresa**: `id`, `nombre`, `cif`, `configuracion_json`
- **Usuario**: `id`, `email`, `password_hash`, `rol` (ADMIN, GERENTE, OPERARIO, CLIENTE), `id_empresa`
- **Operario**: `id_usuario` (FK/PK), `dni`, `especialidad`, `estado` (DISPONIBLE, OCUPADO), `lat`, `lon`
- **Trabajo**: `id`, `id_cliente`, `id_operario`, `estado` (PENDIENTE, EN_PROCESO, FINALIZADO), `descripcion`, `fecha_creacion`
- **Factura**: `id`, `id_trabajo`, `url_pdf`, `importe`, `estado_pago`

### 2. Capa de Servicio (Backend Java)
- **Conexión BD**: JDBC nativo (sin Hibernate/JPA para cumplir requisitos explícitos si es necesario, o DAO pattern manual).
- **Protocolo de Comunicación (JSON-Socket)**:
    - **Request**: `RequestObject { String type; JsonNode payload; }`
    - **Response**: `ResponseObject { int status; String message; JsonNode data; }`
    - **Puerto**: 5000 (Principal).
- **Gestión de Archivos**:
    - Las facturas PDF se generan en el servidor (`/var/data/facturas` o local).
    - Se envían al cliente como `byte[]` codificado en Base64 dentro del JSON de respuesta o mediante un segundo socket de transferencia de archivos (más avanzado para PSP).

### 3. Capa de Clientes
- **Móvil Cliente (Flutter)**: Para usuarios finales.
- **Móvil Operario (Kotlin/Compose)**: Para técnicos.
- **Escritorio (JavaFX)**: Panel de gestión para Gerentes y Super Admin.

## Verification Plan
### Automated Tests
- JUnit para lógica de negocio y DAOs.
- Pruebas de integración para Sockets.

### Manual Verification
- Flujos completos de creación de trabajo -> asignación -> facturación.
- Pruebas de conexión simultánea de múltiples clientes.

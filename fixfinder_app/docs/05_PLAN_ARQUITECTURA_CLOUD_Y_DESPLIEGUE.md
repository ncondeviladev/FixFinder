# 05. Plan de Arquitectura Cloud y Despliegue - FIXFINDER

Este documento actúa como hoja de ruta para la transición de la aplicación **FixFinder** desde un entorno de desarrollo local "monolítico" a una arquitectura distribuida profesional desplegada en la nube.

---

## 1. Visión Global de la Arquitectura

El sistema evolucionará a una arquitectura **Cliente-Servidor Híbrida**.

- **NÚCLEO (La Verdad):** Un servidor Java y MySQL en la nube.
- **CLIENTES (Las Caras):** App Móvil (Flutter) y App de Escritorio (JavaFX).
- **SATÉLITE (El Ayudante):** Firebase para tareas multimedia y notificaciones.

### Diagrama de Distribución Física

| Ubicación        | Componentes       | Función Principal                                         | Conectividad                                         |
| :--------------- | :---------------- | :-------------------------------------------------------- | :--------------------------------------------------- |
| **Nube (VPS)**   | **MySQL Server**  | Base de datos maestra (Texto y Relaciones).               | -                                                    |
| **Nube (VPS)**   | **Servidor Java** | Lógica de negocio, DAOs, Sockets (Puerto 5000).           | Único con acceso a MySQL (Localhost).                |
| **Google Cloud** | **Firebase**      | Almacenamiento de Fotos (Storage) y Notificaciones (FCM). | Accesible vía HTTP (SDK).                            |
| **Móvil**        | **App Flutter**   | Interfaz para Clientes y Operarios.                       | Conecta al Servidor Java (Datos) y Firebase (Fotos). |
| **PC Empresa**   | **App JavaFX**    | Panel de control para el Gerente.                         | Conecta al Servidor Java (Datos).                    |

---

## 2. Estrategia de División del Código

Para facilitar el despliegue futuro sin romper el código actual, reorganizaremos el proyecto en **tres paquetes lógicos** que luego se convertirán en proyectos o módulos independientes.

### Estructura de Paquetes Objetivo

```text
src/main/java/com/fixfinder/
├── common/                  <-- [COMPARTIDO] Entidades que todos deben conocer
│   ├── modelos/             (Cliente.java, Trabajo.java, Usuario.java...)
│   └── dtos/                (Objetos de transporte: LoginRequest, ErrorResponse...)
│
├── server/                  <-- [NUBE] El "Cerebro" que mueve los datos
│   ├── MainServer.java      (Punto de entrada: abre el ServerSocket y espera)
│   ├── core/                (Lógica de Sockets: HiloCliente.java, RouterAcciones.java)
│   └── data/                (TODA LA CAPA DE DATOS ACTUAL)
│       ├── conexion/        (ConexionDB.java, Pool de conexiones)
│       ├── dao/             (Las implementaciones: ClienteDAOImpl, etc.)
│       └── interfaces/      (Las interfaces DAO)
│
└── desktop/                 <-- [PC EMPRESA] La interfaz visual
    ├── MainApp.java         (Punto de entrada JavaFX)
    ├── vista/               (Archivos .fxml)
    ├── controller/          (Controladores de las vistas: ClienteController.java)
    └── network/             (ClienteSocket.java: Sustituye a los DAOs en el cliente)
```

### El Cambio Fundamental (Refactorización)

- **Actualmente (Local):**
  El controlador de la vista (`ClienteController`) llama directamente al DAO (`new ClienteDAOImpl().insertar()`).
- **Futuro (Distribuido):**
  El controlador llamará a un servicio de red (`SocketService.enviar("INSERTAR_CLIENTE", datos)`).
  - **Ventaja:** La vista no sabe si la BD está en China o en Cuenca. Solo pide datos y los recibe.

---

## 3. Integración con Firebase (El Flujo Híbrido)

No usaremos Firebase como Base de Datos principal (NoSQL) para evitar inconsistencias. Lo usaremos como **Servicio de Archivos y Notificaciones**.

### 3.1. ¿Por qué NO usar Firebase Auth?

Para cumplir con los requisitos académicos de **Seguridad y Criptografía**, gestionaremos la autenticación manualmente en nuestro Servidor Java:

1.  **Registro:** El usuario envía contraseña plana -> Servidor la hashea (BCrypt/SHA-256) -> Guarda en MySQL.
2.  **Login:** El usuario envía credenciales -> Servidor verifica hash -> Devuelve OK/ERROR.
    Esto centraliza el control de usuarios en nuestra tabla `usuarios` de MySQL.

### 3.2. Flujo de Datos para Imágenes (Ej: Subir una foto de avería)

1.  **Captura (Móvil):** El usuario toma la foto con la App Flutter.
2.  **Subida (Móvil -> Firebase):** La App sube la imagen directamente a **Firebase Storage**.
3.  **Respuesta (Firebase -> Móvil):** Firebase devuelve una URL pública (ej: `https://firebasestorage.../foto123.jpg`).
4.  **Persistencia (Móvil -> Servidor Java):**
    - La App envía un mensaje Socket al servidor:
      `{ "accion": "CREAR_INCIDENCIA", "foto_url": "https:// firebase...", "desc": "Fuga de agua" }`
5.  **Guardado (Servidor Java -> MySQL):** El servidor usa `IncidenciaDAO` para guardar la URL (texto) en la tabla `trabajos`.

De esta forma, **MySQL** sabe _dónde_ está la foto, pero **Firebase** carga con el peso del archivo.

---

## 4. Plan de Despliegue (Roadmap)

### Fase 1: Desarrollo Local (Estado Actual)

- Todo en un mismo proyecto.
- Base de Datos local (Docker/XAMPP).
- Desarrollamos pensando en la separación (DAOs bien aislados).

### Fase 2: Preparación (Refactorización de Paquetes)

- Mover clases a `com.fixfinder.server`, `com.fixfinder.desktop` y `com.fixfinder.common`.
- Asegurarnos de que `desktop` NUNCA importe nada de `server.data`.

### Fase 3: Separación y Pruebas

- Crear la clase `MainServer` que escucha en `localhost:5000`.
- Modificar el cliente JavaFX para conectar a `localhost:5000` en lugar de llamar a `DAO`.
- Probar que todo funciona en local con la arquitectura cliente-servidor real.

### Fase 4: Despliegue a Producción

1.  **Contratar VPS:** (AWS, DigitalOcean, Hetzner, o máquina virtual gratuita de Oracle).
2.  **Instalar MySQL en VPS:** Migrar esquema `.sql`.
3.  **Subir Servidor:** Compilar el paquete `server` como un `.jar` ejecutable (`java -jar server.jar`) y subirlo al VPS.
4.  **Conexión Móvil:** La App Flutter apunta a la IP pública del VPS.
5.  **Distribución PC:** El `.exe` o `.jar` de Escritorio se instala en los ordenadores de la empresa y apunta a la IP del VPS.

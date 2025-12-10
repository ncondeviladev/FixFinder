#  FIXFINDER

## 1. Requisitos Funcionales (RF)


### 1.1. Gestión de Identidad y Seguridad (Transversal)

| ID         | Requisito              | Prioridad | Descripción                                                                                                                   |
| :--------- | :--------------------- | :-------: | :---------------------------------------------------------------------------------------------------------------------------- |
| **RF-001** | Autenticación (Login)  |   Alta    | El sistema validará credenciales (email/pass) contra la BD. Si es correcto, devolverá un token o sesión y el rol del usuario. |
| **RF-002** | Registro de Cliente    |   Alta    | La App Móvil permitirá registrar nuevos usuarios solicitando datos personales y contraseña.                                   |
| **RF-003** | Encriptación de Claves |   Alta    | El sistema nunca almacenará contraseñas en texto plano; usará algoritmos de hash (PBKDF2/BCrypt) antes de guardar en SQL.     |

### 1.2. Módulo de Empresa (Escritorio - JavaFX)

| ID         | Requisito                | Prioridad | Descripción                                                                                                                           |
| :--------- | :----------------------- | :-------: | :------------------------------------------------------------------------------------------------------------------------------------ |
| **RF-010** | Dashboard en Tiempo Real |   Alta    | La interfaz debe actualizarse automáticamente (sin recargar) cuando lleguen nuevas solicitudes mediante escucha en hilos secundarios. |
| **RF-011** | Filtrado de Incidencias  |   Media   | El administrador podrá filtrar la tabla de trabajos por Estado (Pendiente/Asignado) y Urgencia.                                       |
| **RF-012** | Asignación de Técnicos   |   Alta    | El sistema permitirá vincular una incidencia "Pendiente" a un operario existente, cambiando su estado a "Asignada".                   |
| **RF-013** | Gestión de Facturación   |   Baja    | Generación de un resumen de costes o PDF simple al cerrar la incidencia.                                                              |

### 1.3. Módulo Cliente y Operario (Apps Móviles)

| ID         | Requisito             | Prioridad | Descripción                                                                                         |
| :--------- | :-------------------- | :-------: | :-------------------------------------------------------------------------------------------------- |
| **RF-020** | Reporte de Incidencia |   Alta    | El cliente podrá enviar un formulario con Título, Descripción y Ubicación.                          |
| **RF-021** | Adjuntar Evidencias   |   Media   | El sistema permitirá enviar imágenes de la avería, que se almacenarán en el servidor (no en la BD). |
| **RF-022** | Recepción de Tareas   |   Alta    | El operario verá únicamente las tareas asignadas a su ID.                                           |
| **RF-023** | Cambio de Estado      |   Alta    | El operario podrá transitar la incidencia de "Asignada" -> "En Proceso" -> "Finalizada".            |

---

## 2. Requisitos No Funcionales (RNF) - TÉCNICOS

Justificación de la arquitectura y decisiones complejas de diseño.

### 2.1. Comunicaciones y Arquitectura

| ID             | Requisito               | Descripción Técnica                                                                                                              |
| :------------- | :---------------------- | :------------------------------------------------------------------------------------------------------------------------------- |
| **RNF-NET-01** | Arquitectura C/S        | El sistema operará bajo arquitectura Cliente-Servidor pura, sin servidores web intermedios (Apache/Nginx).                       |
| **RNF-NET-02** | Protocolo Personalizado | La comunicación se realizará mediante Sockets TCP/IP en el puerto 5000.                                                          |
| **RNF-NET-03** | Intercambio de Datos    | El formato de intercambio de mensajes será JSON (serializado con Jackson) para desacoplar las tecnologías de cliente y servidor. |

### 2.2. Concurrencia y Rendimiento

| ID             | Requisito                  | Descripción Técnica                                                                                                                       |
| :------------- | :------------------------- | :---------------------------------------------------------------------------------------------------------------------------------------- |
| **RNF-CON-01** | Multihilo (Multithreading) | El servidor debe atender a cada cliente en un hilo dedicado (GestorCliente) para no bloquear el puerto principal.                         |
| **RNF-CON-02** | Control de Aforo           | El sistema limitará el número de conexiones simultáneas mediante Semáforos (máx. 10) para evitar saturación, rechazando conexiones extra. |
| **RNF-UI-01**  | Fluidez de Interfaz        | El cliente de escritorio no debe congelarse durante las peticiones de red. Las tareas largas se ejecutarán en background threads.         |

### 2.3. Persistencia y Datos

| ID             | Requisito              | Descripción Técnica                                                                                                 |
| :------------- | :--------------------- | :------------------------------------------------------------------------------------------------------------------ |
| **RNF-DAT-01** | Motor de Base de Datos | Se utilizará MySQL como SGBD relacional.                                                                            |
| **RNF-DAT-02** | Patrón de Acceso       | El acceso a datos se desacoplará mediante el patrón DAO (Data Access Object) para facilitar el mantenimiento.       |
| **RNF-DAT-03** | Gestión de Archivos    | Las imágenes no se guardarán como BLOB en la BD, sino como archivos físicos en el sistema de ficheros del servidor. |

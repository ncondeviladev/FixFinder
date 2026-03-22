# Memoria Técnica del Proyecto

**Proyecto:** FixFinder — Sistema de Gestión de Incidencias y Reparaciones  
**Alumno:** Noé Conde Vila  
**Curso:** 2.º DAM — IES Maria Enríquez — Curso 2025-26  
**Fecha de entrega:** 29 de mayo de 2026

---

## Índice

1. [Introducción](#1-introducción)
2. [Presentación de las tecnologías](#2-presentación-de-las-tecnologías)
3. [Análisis del proyecto](#3-análisis-del-proyecto)
4. [Diseño del proyecto](#4-diseño-del-proyecto)
5. [Implementación del proyecto](#5-implementación-del-proyecto)
6. [Estudio de los resultados obtenidos](#6-estudio-de-los-resultados-obtenidos)
7. [Conclusiones](#7-conclusiones)
8. [Bibliografía y recursos utilizados](#8-bibliografía-y-recursos-utilizados)
9. [Anexos](#9-anexos)

---

# 1. Introducción

## 1.1. Presentación y motivación del proyecto

> [Escribe aquí la presentación del proyecto. Explica qué es FixFinder, por qué surgió la idea, cuál es el problema que resuelve y cuál es el objetivo principal de la aplicación.]

---

## 1.2. Factor diferenciador del proyecto

> [Escribe aquí qué hace a FixFinder diferente de otras soluciones existentes. Por ejemplo: la arquitectura cliente-servidor con sockets TCP, la integración de escritorio JavaFX + app Flutter + servidor Java puro, el flujo completo E2E de incidencias, etc.]

---

## 1.3. Análisis de la situación de partida

> [Escribe aquí el contexto inicial del proyecto: estado del arte (apps de mantenimiento similares), carencias detectadas en el sector, punto de partida técnico y de conocimiento.]

---

## 1.4. Objetivos a conseguir con el proyecto

> [Lista los objetivos técnicos y funcionales del proyecto. Pueden ser puntos o párrafos. Ejemplos: sistema de gestión de incidencias en tiempo real, sistema de facturación automatizado, app móvil para clientes, panel de escritorio para gerentes, etc.]

---

## 1.5. Relación con los contenidos de los módulos

| Módulo | Relación con el proyecto |
|---|---|
| **Acceso a Datos** | [Describe aquí cómo se aplican los patrones DAO, repositorios, JDBC, MySQL, etc.] |
| **Desarrollo de Interfaces (DI)** | [Describe la UI con JavaFX, FXML, estilos CSS y el Dashboard de escritorio.] |
| **Programación Multimedia y Dispositivos Móviles (PMDM)** | [Describe la app Flutter: pantallas, navegación, Firebase, imágenes, etc.] |
| **Programación de Servicios y Procesos (PSP)** | [Describe la arquitectura de sockets TCP, hilos por cliente, comunicación JSON, etc.] |
| **Sistemas de Gestión Empresarial (SGE)** | [Describe la facturación, presupuestos, gestión de empresas, clientes, operarios, etc.] |
| **Optativa Nube** | [Describe el uso de Firebase Storage y la preparación del despliegue en AWS EC2 + RDS.] |
| **Sostenibilidad** | [Escribe aquí cómo el proyecto contribuye a la digitalización del sector de mantenimiento, reducción de papel, eficiencia, etc.] |
| **Digitalización** | [Describe cómo el proyecto digitaliza procesos manuales de gestión de incidencias y servicios de reparación.] |

---

# 2. Presentación de las tecnologías

## 2.1. Justificación de la elección de las tecnologías

> [Escribe aquí una justificación razonada de por qué se han elegido las tecnologías utilizadas en el proyecto. Estructura la justificación por capas o por módulo: Java + Sockets para el servidor, JavaFX para el escritorio, Flutter para móvil, MySQL para la base de datos, Firebase para la nube. Argumenta las ventajas frente a alternativas (ej. Spring Boot vs Java puro, React Native vs Flutter, etc.).]

#### Stack tecnológico utilizado

| Componente | Tecnología | Justificación |
|---|---|---|
| **Servidor** | Java 21 (Puro - Sockets TCP) | [Escribe aquí...] |
| **Base de Datos** | MySQL 8 + JDBC | [Escribe aquí...] |
| **Cliente Escritorio** | JavaFX 21 + FXML + CSS | [Escribe aquí...] |
| **App Móvil** | Flutter / Dart | [Escribe aquí...] |
| **Almacenamiento Nube** | Firebase Storage | [Escribe aquí...] |
| **Control de Versiones** | Git + GitHub | [Escribe aquí...] |
| **Autenticación Nube** | Firebase Authentication | [Escribe aquí...] |
| **Despliegue (Futuro)** | AWS EC2 + RDS | [Escribe aquí...] |

---

# 3. Análisis del proyecto

## 3.1. Requerimientos funcionales y no funcionales

#### Requerimientos funcionales

> [Escribe aquí o inserta una tabla de los requerimientos funcionales del sistema. Ejemplos: registro de usuarios, creación de incidencias, asignación de operarios, generación de presupuestos, facturación, notificaciones, etc.]

| ID | Requerimiento | Prioridad |
|---|---|---|
| RF-01 | [Descripción...] | Alta |
| RF-02 | [Descripción...] | Alta |
| RF-03 | [Descripción...] | Media |
| ... | ... | ... |

#### Requerimientos no funcionales

> [Escribe aquí la tabla de requerimientos no funcionales. Ejemplos: rendimiento, seguridad (bcrypt), escalabilidad, usabilidad, compatibilidad, etc.]

| ID | Requerimiento | Descripción |
|---|---|---|
| RNF-01 | [Nombre...] | [Descripción...] |
| RNF-02 | [Nombre...] | [Descripción...] |
| ... | ... | ... |

#### Análisis de costes y viabilidad del proyecto

> [Escribe aquí el análisis de la viabilidad técnica y económica. Puedes incluir costes de infraestructura (Firebase gratuito, AWS Free Tier), costes de desarrollo estimados en horas, herramientas gratuitas utilizadas (IntelliJ Community, Android Studio, etc.).]

---

## 3.2. Temporalización del proyecto

#### Hitos del proyecto

> [Especifica aquí los hitos principales del desarrollo. Puedes usar una lista ordenada cronológicamente: diseño inicial, implementación del servidor, implementación de la app, pruebas, etc.]

| Hito | Descripción | Fecha Aproximada |
|---|---|---|
| 1 | [Hito 1...] | [Fecha...] |
| 2 | [Hito 2...] | [Fecha...] |
| ... | ... | ... |

#### Diagrama de Gantt

> [Inserta aquí la imagen del diagrama de Gantt o una tabla Markdown equivalente representando la planificación temporal del proyecto.]

---

## 3.3. Casos de uso

#### Descripción de los casos de uso

> [Describe aquí los actores del sistema (Cliente, Operario, Gerente, Admin) y los principales casos de uso de cada uno. Por ejemplo: el cliente puede crear incidencias, ver el estado de sus reparaciones, aceptar presupuestos, etc.]

**Actores principales:**
- **Cliente:** [Descripción...]
- **Operario:** [Descripción...]  
- **Gerente:** [Descripción...]
- **Administrador:** [Descripción...]

#### Diagrama de Casos de Uso (UML)

> *Este diagrama muestra los actores del sistema y sus interacciones con las funcionalidades principales, siguiendo la notación UML estándar.*

![Diagrama de Casos de Uso UML](diagramas/diagrama_casos_de_uso_UML.png)

<details>
<summary>Ver código fuente Mermaid</summary>

```mermaid
---
config:
  theme: 
  layout: elk
---
<!-- El contenido completo se encuentra en diagramas/diagrama_casos_de_uso_UML.txt -->
```
</details>

---

#### Diagrama de Flujo de Casos de Uso

> *Este diagrama representa el flujo completo de un proceso de incidencia, desde la creación por el cliente hasta el cierre y valoración, mostrando las transiciones de estado y los actores involucrados en cada paso.*

![Diagrama de Flujo de Casos de Uso](diagramas/diagrama_casos_de_uso_FLUJO.png)

<details>
<summary>Ver código fuente Mermaid</summary>

```mermaid
<!-- El contenido completo se encuentra en diagramas/diagrama_casos_de_uso_FLUJO.txt -->
```
</details>

---

## 3.4. Diagrama de clases inicial

#### Descripción de las clases (diseño inicial)

> [Describe aquí el diseño de clases que tenías en mente al inicio del proyecto. No tiene por qué ser el definitivo. Menciona las entidades principales: Usuario, Cliente, Operario, Empresa, Trabajo, Presupuesto, Factura, etc.]

#### Diagrama de Clases Simplificado (Inicial)

> *Visión simplificada del modelo de clases que sirvió de punto de partida para el desarrollo, mostrando las relaciones de herencia y asociación entre las entidades principales.*

![Diagrama de Clases Simplificado](diagramas/diagrama_clases_simple.png)

<details>
<summary>Ver código fuente Mermaid</summary>

```mermaid
<!-- El contenido completo se encuentra en diagramas/diagrama_clases_simple.txt -->
```
</details>

---

#### Diagrama Entidad-Relación

> *Modelo relacional de la base de datos, mostrando todas las tablas, sus atributos principales y las relaciones (claves primarias y foráneas) entre ellas.*

![Diagrama Entidad-Relación](diagramas/diagrama_entidad_relacion.png)

<details>
<summary>Ver código fuente Mermaid</summary>

```mermaid
<!-- El contenido completo se encuentra en diagramas/diagrama_entidad_relacion.txt -->
```
</details>

---

## 3.5. Wireframes de interfaces

> [Inserta aquí las imágenes de los wireframes o mockups de las pantallas principales de la aplicación móvil y del dashboard de escritorio. Si no tienes imágenes, describe las pantallas: Lista de trabajos, Detalle de trabajo, Creación de incidencia, Perfil de usuario, Panel del gerente, etc.]

---

## 3.6. Otros diagramas y descripciones

> [Esta sección es de libre uso para incluir cualquier otro diagrama relevante de la fase de análisis. En este caso, se incluye el diagrama de flujo completo del sistema.]

#### Diagrama de Flujo Completo del Sistema

> *Representa el ciclo de vida completo de una incidencia en FixFinder, desde que el cliente la reporta hasta que se cierra con una factura pagada y valoración final.*

![Diagrama de Flujo Completo](diagramas/diagrama_flujo_completo.png)

<details>
<summary>Ver código fuente Mermaid</summary>

```mermaid
<!-- El contenido completo se encuentra en diagramas/diagrama_flujo_completo.txt -->
```
</details>

---

# 4. Diseño del proyecto

## 4.1. Arquitectura del sistema

#### Descripción de la arquitectura

> [Describe aquí la arquitectura del sistema en capas: Capa de presentación (Flutter App + Dashboard JavaFX), Capa de red (Sockets TCP, protocolo JSON personalizado), Capa de lógica de negocio (Servicios), Capa de acceso a datos (Patrón DAO) y Capa de persistencia (MySQL + Firebase). Menciona el patrón Thread-per-client del servidor.]

#### Diagrama de Arquitectura del Backend

> *Diagrama completo de las capas del servidor Java: desde el ServidorCentral que acepta conexiones TCP hasta el acceso a la base de datos MySQL, pasando por los procesadores de cada acción y la capa de servicios de negocio.*

![Diagrama de Arquitectura del Backend](diagramas/diagrama_backend.png)

<details>
<summary>Ver código fuente Mermaid</summary>

```mermaid
<!-- El contenido completo se encuentra en diagramas/diagrama_backend.txt -->
```
</details>

---

#### Diagrama de Despliegue Local

> *Muestra la configuración de red local del sistema durante el desarrollo y las pruebas: el servidor Java corriendo en el PC de desarrollo, conectado a MySQL, y siendo accedido tanto por el Dashboard (misma máquina) como por los emuladores Android y dispositivos físicos en la misma red Wi-Fi.*

![Diagrama de Despliegue Local](diagramas/diagrama_despliegue_local.png)

<details>
<summary>Ver código fuente Mermaid</summary>

```mermaid
<!-- El contenido completo se encuentra en diagramas/diagrama_despliegue_local.txt -->
```
</details>

---

#### Diagrama de Despliegue AWS (Producción)

> *Arquitectura de producción planificada en AWS Free Tier: instancia EC2 corriendo el servidor Java en un contenedor Docker, base de datos en RDS MySQL, y comunicación con los clientes a través de una IP elástica pública.*

![Diagrama de Despliegue AWS](diagramas/diagrama_despliege_aws.png)

<details>
<summary>Ver código fuente Mermaid</summary>

```mermaid
<!-- El contenido completo se encuentra en diagramas/diagrama_despliege_aws.txt -->
```
</details>

---

## 4.2. Diagrama de clases definitivo

#### Descripción de las clases (diseño final)

> [Describe aquí el modelo de clases definitivo tras el desarrollo. Destaca cambios respecto al diseño inicial: herencia entre Usuario, Cliente y Operario, patrón DAO, patrón Repository (DataRepository), Servicios, Modelos FX para JavaFX, etc.]

#### Diagrama de Clases Completo (Definitivo)

> *Modelo de clases completo del sistema con todas las relaciones de herencia, composición y asociación entre las capas de Presentación, Servicios, DAO y Modelos del dominio.*

![Diagrama de Clases Completo](diagramas/diagrama_clases_completo.png)

<details>
<summary>Ver código fuente Mermaid</summary>

```mermaid
<!-- El contenido completo se encuentra en diagramas/diagrama_clases_completo.txt -->
```
</details>

---

## 4.3. Diseño de la interfaz de usuario

#### Mockups y pantallas principales

> [Inserta aquí capturas de pantalla reales de la aplicación (tanto del Dashboard JavaFX como de la App móvil Flutter) o los mockups de diseño previo. Describe brevemente cada pantalla: su propósito, los componentes principales y el flujo de navegación al que pertenece.]

**Pantallas de la App Móvil (Flutter):**

> [Captura 1 — Pantalla de Login]  
> [Escribe aquí una descripción breve de esta pantalla...]

> [Captura 2 — Dashboard Cliente (Lista de Trabajos)]  
> [Escribe aquí una descripción breve de esta pantalla...]

> [Captura 3 — Detalle de Trabajo / Incidencia]  
> [Escribe aquí una descripción breve de esta pantalla...]

> [Captura 4 — Perfil de Usuario]  
> [Escribe aquí una descripción breve de esta pantalla...]

**Pantallas del Dashboard de Escritorio (JavaFX):**

> [Captura 5 — Pantalla de Login del Dashboard]  
> [Escribe aquí una descripción breve de esta pantalla...]

> [Captura 6 — Panel Principal (Lista de Incidencias del Gerente)]  
> [Escribe aquí una descripción breve de esta pantalla...]

> [Captura 7 — Gestión de Operarios]  
> [Escribe aquí una descripción breve de esta pantalla...]

---

#### Diagrama de Navegación de la App Móvil

> *Mapa de navegación completo de la aplicación Flutter, mostrando todas las pantallas disponibles para cada rol (Cliente, Operario) y las transiciones entre ellas.*

![Diagrama de Navegación](diagramas/diagrama_navegacion.png)

<details>
<summary>Ver código fuente Mermaid</summary>

```mermaid
<!-- El contenido completo se encuentra en diagramas/diagrama_navegacion.txt -->
```
</details>

---

## 4.4. Otros diagramas y descripciones

#### Diagrama de Componentes de la App Móvil

> *Diagrama de la arquitectura interna de la aplicación Flutter, mostrando la organización en capas: pantallas, servicios, modelos, providers y componentes reutilizables (widgets).*

![Diagrama de Componentes de la App](diagramas/diagrama_componentes_app.png)

<details>
<summary>Ver código fuente Mermaid</summary>

```mermaid
<!-- El contenido completo se encuentra en diagramas/diagrama_componentes_app.txt -->
```
</details>

---

#### Diagrama de Secuencia — Comunicación por Sockets

> *Diagrama de secuencia que ilustra el protocolo de comunicación TCP/JSON entre los clientes (App y Dashboard) y el servidor, incluyendo el handshake de conexión, el formato de los mensajes (cabecera de 4 bytes + JSON) y el ciclo de petición-respuesta.*

![Diagrama de Secuencia de Sockets](diagramas/diagrama_secuencia_sockets.png)

<details>
<summary>Ver código fuente Mermaid</summary>

```mermaid
<!-- El contenido completo se encuentra en diagramas/diagrama_secuencia_sockets.txt -->
```
</details>

---

# 5. Implementación del proyecto

## 5.1. Estructura del proyecto

> [Describe aquí la estructura de directorios del proyecto. Puedes incluir un árbol de carpetas simplificado mostrando los módulos: FIXFINDER (servidor + dashboard legacy), fixfinder_app (app Flutter), DOCS (documentación y diagramas).]

```
FF/
├── FIXFINDER/            # Módulo Java (Servidor + Dashboard Tester)
│   └── src/main/java/com/fixfinder/
│       ├── cliente/      # ServicioCliente, ClienteSocket
│       ├── controladores/# DashboardController, SimuladorController
│       ├── data/         # DAOs, DataRepository
│       ├── modelos/      # Usuario, Cliente, Operario, Trabajo, Empresa...
│       ├── red/          # ServidorCentral, GestorConexion, Procesadores
│       ├── service/      # Servicios de negocio (UsuarioService, etc.)
│       ├── ui/dashboard/ # AppDashboardPrincipal, DashboardPrincipal...
│       └── utilidades/   # SessionManager, GestorPassword, etc.
├── fixfinder_app/        # Módulo Flutter (App Móvil)
│   └── lib/
│       ├── models/       # usuario.dart, trabajo.dart, etc.
│       ├── providers/    # trabajo_provider.dart
│       ├── screens/      # login, dashboard, perfil, detalle_trabajo...
│       ├── services/     # auth_service.dart, socket_service.dart...
│       └── widgets/      # Componentes reutilizables
└── DOCS/                 # Documentación y diagramas
    └── diagramas/        # Imágenes PNG + código Mermaid TXT
```

### Arquitectura de despliegue en producción (AWS)

Para el despliegue en producción, el sistema está planificado sobre la capa gratuita de **Amazon Web Services (AWS Free Tier)**:

| Componente | Servicio AWS | Descripción |
|---|---|---|
| **Servidor de aplicaciones** | **EC2** (`t3.micro`) | Instancia Linux con Docker que ejecuta el servidor Java (Socket Server en puerto 5000). Se conecta a RDS para persistencia. IP elástica pública para acceso externo. |
| **Base de datos** | **RDS** (`db.t3.micro` MySQL 8) | Instancia MySQL gestionada por AWS. Accesible desde EC2 vía endpoint interno. Separada del servidor para mayor seguridad y escalabilidad. |
| **Almacenamiento de media** | **Firebase Storage** | Las imágenes de perfil y fotos de trabajo se suben directamente desde los clientes (App Flutter) a Firebase, sin pasar por EC2. |
| **Clientes** | App Flutter + Dashboard JavaFX | Se conectan directamente a la IP elástica de EC2 en el puerto 5000 mediante sockets TCP. |

```
╔═══════════════════════════ AWS Free Tier ═══════╗
║                                                 ║
║  ┌─────────────────┐     ┌─────────────────┐    ║
║  │  EC2 (t3.micro)  │►───►│ RDS MySQL 8    │    ║
║  │  Docker + Java  │     │ (db.t3.micro)   │    ║
║  │  Puerto :5000   │     │  Puerto :3306   │    ║
║  └─────────────────┘     └─────────────────┘    ║
║           ▲                                     ║
╚═══════════◊═════════════════════════════════════╝
            │ TCP :5000
  ┌─────────┴──────────┐
  │  Clientes          │
  │ App Flutter        │─────► Firebase Storage
  │ Dashboard JavaFX   │     (Imágenes)
  └───────────────────┘
```


> [Describe aquí los módulos principales del sistema y su función. Puedes estructurarlo en tres bloques: Servidor Java, Dashboard JavaFX y App Flutter.]

#### Servidor Java (Backend)

> [Describe los componentes clave del servidor: `ServidorCentral`, `GestorConexion`, los `Procesadores` de cada acción, la capa de `Servicios`, los `DAOs` y el `DataRepository`. Menciona el patrón Thread-per-client y el protocolo JSON.]

#### Dashboard de Escritorio (JavaFX)

> [Describe el dashboard: acceso por roles (Gerente/Admin), gestión de incidencias, gestión de operarios, registro de empresas/usuarios, el simulador E2E, etc.]

#### Aplicación Móvil (Flutter)

> [Describe la app móvil: roles (Cliente y Operario), pantallas principales, comunicación via Sockets, gestión de fotos con Firebase Storage, notificaciones, etc.]

---

## 5.2. Descripción de los módulos y componentes principales

> [Describe aquí los módulos principales del sistema y su función. Puedes estructurarlo en tres bloques: Servidor Java, Dashboard JavaFX y App Flutter.]

#### Servidor Java (Backend)

> [Describe los componentes clave del servidor: `ServidorCentral`, `GestorConexion`, los `Procesadores` de cada acción, la capa de `Servicios`, los `DAOs` y el `DataRepository`. Menciona el patrón Thread-per-client y el protocolo JSON.]

#### Dashboard de Escritorio (JavaFX)

> [Describe el dashboard: acceso por roles (Gerente/Admin), gestión de incidencias, gestión de operarios, registro de empresas/usuarios, el simulador E2E, etc.]

#### Aplicación Móvil (Flutter)

> [Describe la app móvil: roles (Cliente y Operario), pantallas principales, comunicación via Sockets, gestión de fotos con Firebase Storage, notificaciones, etc.]

---

## 5.3. Despliegue de la aplicación

> El entorno **local** (servidor corriendo en el PC con MySQL local y emuladores Android) se ha utilizado exclusivamente durante el **desarrollo y las pruebas**. El despliegue **final y definitivo** de la aplicación es sobre **AWS Free Tier**: el servidor Java se ejecuta en una instancia EC2 dentro de un contenedor Docker, la base de datos MySQL se aloja en RDS gestionado, y los clientes (App Flutter y Dashboard JavaFX) se conectan a través de la IP elástica pública de EC2. Las imágenes siguen subiéndose directamente a Firebase Storage desde los clientes.

---

## 5.4. Capturas de pantalla y ejemplos de código

> [Inserta aquí capturas de pantalla que demuestren el funcionamiento del sistema en ejecución. Incluye al menos: el servidor en funcionamiento, el dashboard del gerente con una incidencia, la app móvil mostrando el listado de trabajos, y un flujo completo de creación de incidencia hasta su resolución.]

> **Nota:** Los fragmentos de código relevantes se encuentran en el Anexo A (código fuente completo).

---

# 6. Estudio de los resultados obtenidos

## 6.1. Evaluación del proyecto respecto a los objetivos iniciales

> [Valora aquí en qué medida se han cumplido los objetivos planteados al inicio. ¿Qué está completamente implementado? ¿Qué quedó pendiente? ¿Se superaron las expectativas en algún aspecto?]

---

## 6.2. Problemas encontrados y soluciones aplicadas

> [Describe aquí los principales problemas técnicos y de diseño que surgieron durante el desarrollo, y cómo se resolvieron. Ejemplos: gestión de hilos en el servidor, sincronización de la UI de JavaFX con hilos de red, compatibilidad de módulos JavaFX, etc.]

| Problema | Solución aplicada |
|---|---|
| [Problema 1...] | [Solución 1...] |
| [Problema 2...] | [Solución 2...] |
| [Problema 3...] | [Solución 3...] |
| ... | ... |

---

## 6.3. Futuras mejoras y ampliaciones

> [Lista aquí las mejoras que quedarían pendientes para una versión futura del proyecto. Ejemplos: push notifications reales, despliegue completo en AWS, sistema de chat en tiempo real, aplicación web administrativa, app para iOS, etc.]

- [ ] [Mejora 1...]
- [ ] [Mejora 2...]
- [ ] [Mejora 3...]
- [ ] [Mejora 4...]

---

# 7. Conclusiones

## 7.1. Relación con los contenidos de los módulos

> [Reflexión final sobre cómo el proyecto ha permitido aplicar y consolidar los conocimientos de cada uno de los módulos estudiados durante el ciclo. Sé específico con ejemplos concretos del proyecto para cada módulo.]

---

## 7.2. Valoración personal del proyecto

> [Escribe aquí tu valoración personal del proceso de desarrollo: qué has aprendido, qué cambiarías si empezaras de nuevo, qué aspectos te han resultado más difíciles y más satisfactorios, etc.]

---

# 8. Bibliografía y recursos utilizados

- [Documentación oficial de JavaFX — openjfx.io](https://openjfx.io/)
- [Documentación oficial de Flutter — flutter.dev](https://flutter.dev/docs)
- [Firebase Documentation — firebase.google.com](https://firebase.google.com/docs)
- [MySQL 8.0 Reference Manual — dev.mysql.com](https://dev.mysql.com/doc/)
- [Jackson Databind Documentation — FasterXML](https://github.com/FasterXML/jackson-databind)
- [AWS Free Tier Documentation — aws.amazon.com](https://aws.amazon.com/free/)
- [Escribe aquí otros recursos consultados...]

---

# 9. Anexos

### Anexo A — Código Fuente

> [Referencia al archivo ZIP con el código fuente completo del proyecto, incluyendo los tres módulos: servidor Java, dashboard JavaFX y app Flutter.]

### Anexo B — Guía de Instalación y Configuración

> [Referencia o texto de la guía de instalación del sistema: requisitos previos (JDK 21, Flutter, MySQL 8, Firebase), pasos de configuración de la base de datos (SCHEMA.sql), variables de configuración (puerto, host, credenciales), arranque del servidor y de los clientes.]

### Anexo C — Esquema de Base de Datos

> [Referencia al script SQL de creación del esquema de la base de datos (`SCHEMA.sql` o equivalente), incluyendo la creación de todas las tablas, índices y relaciones.]

### Anexo D — Códigos Mermaid de los Diagramas

Los ficheros `.txt` con el código fuente completo de todos los diagramas se encuentran en la carpeta `DOCS/diagramas/`:

| Fichero | Diagrama |
|---|---|
| `diagrama_backend.txt` | Arquitectura del Backend por capas |
| `diagrama_casos_de_uso_UML.txt` | Casos de uso UML |
| `diagrama_casos_de_uso_FLUJO.txt` | Flujo de casos de uso |
| `diagrama_clases_completo.txt` | Diagrama de clases completo (definitivo) |
| `diagrama_clases_simple.txt` | Diagrama de clases simplificado (inicial) |
| `diagrama_componentes_app.txt` | Componentes de la app Flutter |
| `diagrama_despliege_aws.txt` | Despliegue en AWS (producción) |
| `diagrama_despliegue_local.txt` | Despliegue local (desarrollo) |
| `diagrama_entidad_relacion.txt` | Entidad-Relación de la base de datos |
| `diagrama_flujo_completo.txt` | Flujo completo del sistema |
| `diagrama_navegacion.txt` | Navegación de la app móvil |
| `diagrama_secuencia_sockets.txt` | Secuencia de comunicación por sockets |

---

*Documento generado en Markdown — preparado para conversión a PDF.*  
*Última actualización: marzo 2026.*
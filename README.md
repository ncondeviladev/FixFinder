# FixFinder 🛠️
### Ecosistema Integral de Gestión de Incidencias y Mantenimiento

![Logo](DOCS/FFLogo.png)

**FixFinder** es un proyecto académico de fin de ciclo (DAM) diseñado para eliminar las barreras en el sector de las reparaciones domésticas y técnicas. La intención principal es ahorrar al cliente el tedioso proceso de buscar empresas de servicios de confianza, mientras que para las empresas supone una fuente constante y organizada de nuevos clientes.

---

## 🚀 El Ecosistema

El proyecto se divide en tres componentes principales que trabajan de forma coordinada:

### 1. App Móvil Dual (Flutter) 📱
Una única aplicación que adapta su interfaz y funcionalidades según el rol del usuario que inicie sesión:
*   **Modo Cliente:** Reporte de averías con fotos, recepción de presupuestos competitivos y valoración del servicio.
*   **Modo Operario:** Recepción de tareas asignadas, visualización de detalles técnicos y finalización de incidencias con evidencias fotográficas.
*   **Puntos clave:** Interfaz reactiva, gestión multimedia en la nube y sistema de navegación por estados.

<p align="center">
  <img src="DOCS/capturas/app_login.png" width="200" />
  <img src="DOCS/capturas/app_panel.png" width="200" />
  <img src="DOCS/capturas/app_ope_incidencia.png" width="200" />
</p>

### 2. Dashboard de Gestión (JavaFX) 💻
Panel administrativo exclusivo para **Gerentes**. Permite la monitorización global de incidencias, emisión de presupuestos, gestión de personal y analítica básica del rendimiento de la empresa.
*   **Puntos clave:** Arquitectura multihilo avanzada y sistema de filtrado dinámico para la gestión de grandes volúmenes de datos.

![Dashboard](DOCS/capturas/dash_panel.png)

### 3. Servidor Central (Backend Java) ⚙️
El "cerebro" del sistema que orquestra todas las comunicaciones de forma segura y eficiente.
*   **Puntos clave:** Sockets TCP con protocolo binario de bajo nivel, lo que garantiza un control total sobre el flujo de datos sin depender de frameworks externos.

---

## 🛠️ Stack Tecnológico

*   **Lenguajes:** Java 21, Dart.
*   **Frameworks:** Flutter (Mobile), JavaFX (Desktop).
*   **Persistencia:** MySQL 8.0 (AWS RDS), Firebase Storage (Multimedia).
*   **Infraestructura:** Amazon Web Services (EC2), Docker (Entorno Local).
*   **Seguridad:** Encriptación BCrypt para contraseñas.

---

## 🏗️ Arquitectura Técnica y Hitos

FixFinder destaca por la implementación de patrones de ingeniería de alto nivel:
- **Protocolo de Red:** Comunicación por Sockets TCP con cabeceras de 4 bytes (Big-Endian) para evitar la fragmentación de paquetes.
- **Concurrencia Avanzada:** Modelo **Dispatcher-Worker** con hilos especializados y aislamiento de conexiones mediante `ThreadLocal`.
- **Anti-Bloqueo:** Implementación del **Hilo Lector Avaro** en el Dashboard para vaciar el buffer TCP y mantener la UI siempre fluida.
- **Tiempo Real:** Sistema **Broadcaster** basado en el patrón Observer para notificaciones PUSH instantáneas a todos los clientes.
- **Robustez:** Lógica de **Atomicidad** en la aceptación de presupuestos y sistema de **Surgical Cleanup** para garantizar tests de integración no destructivos.

![Arquitectura](DOCS/diagramas/diagrama_backend.png)

---

## 📖 Documentación Completa

Para una inmersión profunda en las decisiones de diseño, retos técnicos y guía de implementación, consulta la documentación oficial en la carpeta `DOCS`:

- 📑 **[Memoria Técnica Académica](DOCS/MEMORIA.md)**: Documentación detallada del proyecto.
- 📜 **[Tesis Técnica](DOCS/TESIS_TECNICA.md)**: Justificación técnica de la arquitectura.

---

## 🔧 Instalación Rápida

1.  **Base de Datos:** Importar `DOCS/diseno/ESQUEMA_BD.sql` en MySQL.
2.  **Servidor:** Ejecutar `./gradlew runServer` en el directorio `FIXFINDER`.
3.  **App Móvil:** `flutter run` en `fixfinder_app`.
4.  **Dashboard:** Ejecutar el binario `FixFinder_Dashboard.exe`.

---

_Proyecto desarrollado por **Noé Conde Vila** como Proyecto Final de Ciclo (DAM) — 2026._

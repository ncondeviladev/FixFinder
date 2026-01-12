# Especificación de Requisitos de Software (SRS) - FIXFINDER

## 1. Introducción

Este documento define los requisitos funcionales y no funcionales del sistema **FIXFINDER**, siguiendo las pautas académicas estándar. Establece el contrato de comportamiento del software para garantizar que satisface las necesidades de negocio de gestión de reparaciones e incidencias.

---

## 2. Requerimientos Funcionales

Describen las acciones específicas que el sistema debe ser capaz de realizar. Se dividen en reglas de negocio y funcionalidad operativa.

### 2.1. Características y Funcionalidad (Casos de Uso)

#### **Módulo Común (Identidad)**

| ID         | Descripción                      | Regla de Negocio                                                                                                                                      |
| :--------- | :------------------------------- | :---------------------------------------------------------------------------------------------------------------------------------------------------- |
| **RF-001** | **Autenticación y Autorización** | El sistema debe validar credenciales y determinar si el usuario es Admin, Cliente u Operario para restringir el acceso a las vistas correspondientes. |
| **RF-002** | **Registro de Usuarios**         | Permitir el alta de nuevos clientes desde la App Móvil verificando datos únicos (email).                                                              |

#### **Módulo de Gestión (Administrador)**

| ID         | Descripción                       | Regla de Negocio                                                                 |
| :--------- | :-------------------------------- | :------------------------------------------------------------------------------- |
| **RF-010** | **Monitorización en Tiempo Real** | El Dashboard debe reflejar incidencias entrantes sin recarga manual.             |
| **RF-011** | **Gestión de Presupuestos**       | Capacidad para crear y enviar valoraciones económicas antes de asignar técnicos. |
| **RF-012** | **Asignación de Recursos**        | Vincular una incidencia aceptada a un operario disponible.                       |
| **RF-013** | **Facturación y Cierre**          | Generación automática de documentos de cobro al validar la finalización.         |

#### **Módulo Operativo (Cliente / Técnico)**

| ID         | Descripción               | Regla de Negocio                                                                     |
| :--------- | :------------------------ | :----------------------------------------------------------------------------------- |
| **RF-020** | **Reporte de Averías**    | Entrada de datos estructurados (Título, Descripción, Urgencia) y multimedia (Fotos). |
| **RF-022** | **Aprobación de Costes**  | El cliente debe poder aceptar o rechazar presupuestos explícitamente.                |
| **RF-024** | **Imputación de Trabajo** | El técnico debe registrar horas y materiales para justificar el coste final.         |

### 2.2. Plataformas de Despliegue

- **Cliente de Escritorio (Administración):**
  - Sistema Operativo: Windows 10/11.
  - Tecnología: JavaFX (JDK 21+).
- **Aplicación Móvil (Cliente/Operario):**
  - Sistema Operativo: Android 12+.
  - Tecnología: Android Nativo o Flutter (según fase).

### 2.3. Especificaciones de Diseño (UI/UX)

- **Estética:** Diseño moderno y "Premium", utilizando paletas de colores oscuros o de alto contraste para facilitar la lectura en campo.
- **Tipografía:** Fuentes sans-serif limpias (ej. Inter, Roboto) para máxima legibilidad.
- **Feedback:** Uso de micro-animaciones para confirmar acciones críticas (envío de incidencias, validaciones).

### 2.4. Funcionalidad Back-end

- **API:** Comunicación mediante **Sockets TCP/IP** puros (Puerto 5000), utilizando protocolo JSON personalizado.
- **Base de Datos:** MySQL/MariaDB para almacenamiento relacional persistente.

---

## 3. Requerimientos No Funcionales

Definen cómo debe comportarse el sistema en términos de rendimiento, seguridad y calidad.

### 3.1. Requerimientos de Rendimiento (Performance)

- **Tiempo de Respuesta:**
  - Las interacciones críticas (login, crear incidencia) deben responder en **< 2 segundos**.
  - La interfaz de escritorio no debe congelarse (uso de hilos background para red).
- **Rendimiento (Throughput):**
  - El servidor debe soportar concurrencia mediante un pool de hilos (_Thread-per-client_).
  - **Control de Aforo:** Uso de Semáforos para limitar conexiones simultáneas (safety cap) y evitar denegación de servicio.

### 3.2. Requerimientos de Seguridad

- **Autenticación:** Mecanismo de tokens o sesión persistente segura.
- **Cifrado (Confidencialidad):**
  - Las contraseñas **NUNCA** se almacenan en texto plano. Se usará hashing robusto (SHA-256/BCrypt).
- **Protección de Datos:** Los archivos sensibles (facturas, datos personales) solo accesibles por usuarios autorizados (RBAC - Role Based Access Control).

### 3.3. Requerimientos de Calidad

- **Usabilidad:**
  - Interfaces intuitivas que requieran mínima formación para el operario de campo.
- **Confiabilidad (Reliability):**
  - El servidor debe ser robusto ante desconexiones abruptas de clientes (broken pipes), liberando recursos correctamente.
- **Mantenibilidad:**
  - Código estructurado siguiendo patrones de ingeniería de software (DAO, MVC/MVP) para facilitar evoluciones futuras.

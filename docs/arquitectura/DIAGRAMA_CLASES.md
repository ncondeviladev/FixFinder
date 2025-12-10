# Diagrama de Clases Completo - Arquitectura Objetivo FIXFINDER

Este documento sirve como **guía de implementación** para la aplicación final. Refleja la arquitectura completa (Frontend, Backend, Protocolo y Datos) que debemos alcanzar.

```mermaid
classDiagram
    %% =========================================================
    %% CAPA COMÚN (Protocolo y Modelos compartidos)
    %% =========================================================
    namespace Protocolo {
        class Peticion {
            - TipoAccion accion
            - Object cuerpo
            - String tokenUsuario
        }
        class Respuesta {
            - CodigoEstado estado
            - Object cuerpo
            - String mensaje
        }
        class TipoAccion {
            <<Enumeration>>
            LOGIN
            REGISTRO
            OBTENER_TRABAJOS
            CREAR_TRABAJO
            ASIGNAR_OPERARIO
            ACTUALIZAR_ESTADO
            SUBIR_FOTO
        }
    }

    namespace Modelos {
        class Usuario { #id, #email, #rol: Rol }
        class Operario { -dni, -especialidad: CategoriaServicio }
        class Empresa { -nombre, -especialidades: List<CategoriaServicio> }
        class Trabajo { -titulo, -estado: EstadoTrabajo, -cliente: Usuario }
        class FotoTrabajo { -url }
        class Factura { -total, -pdfUrl }

        Usuario <|-- Operario
        Empresa "1" *-- "N" Usuario
        Trabajo "N" --> "1" Usuario : Cliente
        Trabajo "N" --> "1" Operario : Asignado
    }

    Peticion ..> TipoAccion
    Peticion ..> Modelos : Transporta
    Respuesta ..> Modelos : Transporta

    %% =========================================================
    %% BACKEND (Servidor)
    %% =========================================================
    namespace Servidor {
        class ServidorCentral {
            - port: int
            + iniciar()
            + escuchar()
        }
        class HiloCliente {
            - socket: Socket
            + run()
        }

        class ControladorFrontal {
            %% Despacha la petición al servicio adecuado
            + procesar(Peticion) Respuesta
        }

        class ServicioAuth {
            + login(email, pass) Usuario
            + registrar(Usuario)
        }
        class ServicioTrabajos {
            + crear(Trabajo)
            + listar(Filtros)
            + asignar(idTrabajo, idOperario)
        }
        class ServicioFacturacion {
            + generarFactura(Trabajo)
        }
    }

    namespace Datos {
        class ConexionDB { + getConnection() }
        class UsuarioDAO { + buscarPorEmail() }
        class TrabajoDAO
        class OperarioDAO
        class EmpresaDAO
    }

    namespace UtilidadesBack {
        class GeneradorPDF { + crearFacturaPDF(Factura) }
        class GestorArchivos { + guardarFoto(byte[]) }
    }

    ServidorCentral *-- "N" HiloCliente
    HiloCliente --> ControladorFrontal
    ControladorFrontal --> ServicioAuth
    ControladorFrontal --> ServicioTrabajos
    ControladorFrontal --> ServicioFacturacion

    ServicioAuth ..> UsuarioDAO
    ServicioTrabajos ..> TrabajoDAO
    ServicioTrabajos ..> OperarioDAO
    ServicioFacturacion ..> TrabajoDAO
    ServicioFacturacion ..> GeneradorPDF

    %% =========================================================
    %% FRONTEND (App Escritorio JavaFX)
    %% =========================================================
    namespace ClienteFX {
        class Launcher { + main() }
        class ContextoGlobal {
            %% Singleton para datos de sesión
            - Usuario usuarioSesion
            - ClienteSocket red
        }
        class ClienteSocket {
            + enviar(Peticion)
            + recibir() Respuesta
        }

        %% Controladores de Vistas
        class LoginController {
            - txtEmail, txtPass
            + btnLogin()
        }
        class DashboardController {
            - tablaIncidencias
            - filtros
            + cargarDatos()
            + navegarDetalle()
        }
        class DetalleTrabajoController {
            - lblTitulo, lblEstado
            - listFotos
            + asignarOperario()
            + generarFactura()
        }
        class PerfilEmpresaController {
            - formDatos
            + guardarCambios()
        }
    }

    Launcher ..> LoginController : Inicia
    LoginController ..> DashboardController : Navega
    DashboardController ..> DetalleTrabajoController : Navega
    DashboardController ..> PerfilEmpresaController : Navega

    LoginController ..> ContextoGlobal
    DashboardController ..> ContextoGlobal

    ContextoGlobal --> ClienteSocket
    ClienteSocket ..> Protocolo : Usa
```

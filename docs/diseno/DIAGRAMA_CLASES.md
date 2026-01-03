# Diagrama de Clases - FIXFINDER

A continuación se presentan los diagramas de clases del **Modelo de Dominio**, que representan las entidades principales de información, sus atributos y las relaciones entre ellas.

## 1. Diagrama Simplificado (Solo Clases y Relaciones)


```mermaid
---
config:
  layout: elk
  theme: redux-dark
---
classDiagram
    class Usuario
    class Operario
    class Empresa
    class Trabajo
    class Factura
    class Presupuesto
    class FotoTrabajo
    class Ubicacion
    class Rol { <<enumeration>> }
    class CategoriaServicio { <<enumeration>> }
    class EstadoTrabajo { <<enumeration>> }
    Usuario <|-- Operario : Es un<br/>(Herencia)
    Empresa "1" --> "1..*" CategoriaServicio : Ofrece<br/>(Asociación)
    Usuario "*" --> "0..1" Empresa : Pertenece a<br/>(Asociación)

    Trabajo "*" --> "1" Usuario : Solicitado por<br/>(Asociación)
    Trabajo "*" --> "0..1" Operario : Asignado a<br/>(Asociación)
    Trabajo "*" ..> "1" CategoriaServicio : Clasificado como<br/>(Dependencia)
    Trabajo "*" ..> "1" EstadoTrabajo : Tiene estado<br/>(Dependencia)
    Trabajo "1" *-- "0..*" FotoTrabajo : Contiene<br/>(Composición)
    Trabajo "1" *-- "1" Ubicacion : Localizada en<br/>(Composición)
    Usuario "1" ..> "1" Rol : Tiene rol<br/>(Dependencia)
    Operario "*" ..> "1" CategoriaServicio : Especialidad<br/>(Dependencia)
    Factura "0..1" --> "1" Trabajo : Facturación de<br/>(Asociación)
    Presupuesto "0..*" --> "1" Trabajo : Presupuesto para<br/>(Asociación)
    Presupuesto "*" --> "1" Empresa : Emitido por<br/>(Asociación)
```

## 2. Diagrama Detallado (Completo)

Vista técnica alineada con el código Java, incluyendo atributos completos y todos los métodos (Constructores, Getters y Setters).


```mermaid
---
config:
  layout: elk
  theme: redux-dark
---
classDiagram
    class Usuario {
        <<Abstract>>
        #int id
        #String nombreCompleto
        #String email
        #String passwordHash
        #Rol rol
        #String dni
        #LocalDateTime fechaRegistro
        #String telefono
        #String direccion
        #String urlFoto

        +getId() int
        +setId(int id)
        +getNombreCompleto() String
        +setNombreCompleto(String nombreCompleto)
        +getEmail() String
        +setEmail(String email)
        +getPasswordHash() String
        +setPasswordHash(String passwordHash)
        +getRol() Rol
        +setRol(Rol rol)
        +getDni() String
        +setDni(String dni)
    }

    class Cliente {
        +Cliente()
    }

    class Operario {
        -int idEmpresa
        -CategoriaServicio especialidad
        -boolean estaActivo
        -double latitud
        -double longitud
        -LocalDateTime ultimaActualizacion

        +Operario()
        +getIdEmpresa() int
        +setIdEmpresa(int idEmpresa)
        +getEspecialidad() CategoriaServicio
        +setEspecialidad(CategoriaServicio especialidad)
        +isEstaActivo() boolean
        +setEstaActivo(boolean estaActivo)
    }

    class Empresa {
        -int id
        -String nombre
        -String cif
        -String direccion
        -String telefono
        -String emailContacto
        -String urlFoto
        -List~CategoriaServicio~ especialidades

        +getId() int
        +setId(int id)
        +getNombre() String
        +setNombre(String nombre)
        +getCif() String
        +setCif(String cif)
        +getEspecialidades() List~CategoriaServicio~
    }

    class Trabajo {
        -int id
        -Usuario cliente
        -Operario operarioAsignado
        -CategoriaServicio categoria
        -String titulo
        -String descripcion
        -Ubicacion ubicacion
        -String direccion
        -EstadoTrabajo estado
        -LocalDateTime fechaCreacion
        -LocalDateTime fechaFinalizacion
        -int valoracion
        -String comentarioCliente
        -List~FotoTrabajo~ fotos

        +getId() int
        +setId(int id)
        +getCliente() Usuario
        +setCliente(Usuario cliente)
        +getOperarioAsignado() Operario
        +setOperarioAsignado(Operario operarioAsignado)
        +getEstado() EstadoTrabajo
        +setEstado(EstadoTrabajo estado)
    }

    class Factura {
        -int id
        -Trabajo trabajo
        -String numeroFactura
        -double total
        -LocalDateTime fechaEmision
        -boolean pagada

        +getId() int
        +setId(int id)
        +isPagada() boolean
    }

    class Presupuesto {
        -int id
        -Trabajo trabajo
        -Empresa empresa
        -double monto
        -Date fechaEnvio

        +getId() int
        +setId(int id)
        +getMonto() double
        +setMonto(double monto)
    }

    class FotoTrabajo {
        -int id
        -int idTrabajo
        -String url
    }

    class Ubicacion {
        -double latitud
        -double longitud
    }
    class Rol {
        <<enumeration>>
        ADMIN
        GERENTE
        OPERARIO
        CLIENTE
    }

    class CategoriaServicio {
        <<enumeration>>
        FONTANERIA
        ELECTRICIDAD
        ALBANILERIA
        PINTURA
        ETC
    }

    class EstadoTrabajo {
        <<enumeration>>
        PENDIENTE
        ASIGNADO
        EN_PROCESO
        FINALIZADO
        CANCELADO
    }
    Usuario <|-- Cliente : Es un
    Usuario <|-- Operario : Es un
    Operario "*" --> "1" Empresa : Trabaja para
    Trabajo "*" --> "1" Cliente : Solicitado por
    Trabajo "*" --> "0..1" Operario : Asignado a
    Trabajo "*" ..> "1" EstadoTrabajo : Tiene
    Factura "0..1" --> "1" Trabajo : Facturación de
    Presupuesto "0..*" --> "1" Trabajo : Para
    Presupuesto "*" --> "1" Empresa : Emitido por
    Empresa "1" --> "*" CategoriaServicio : Ofrece
    Usuario ..> Rol : Tiene
    Operario ..> CategoriaServicio : Especialidad
    Trabajo *-- Ubicacion : Lugar
    Trabajo *-- FotoTrabajo : Evidencia
```

# Diagrama de Clases - FIXFINDER

A continuación se presentan los diagramas de clases del **Modelo de Dominio**, que representan las entidades principales de información, sus atributos y las relaciones entre ellas.

## 1. Diagrama Simplificado (Solo Clases y Relaciones)

Vista de alto nivel para entender las entidades y cómo se relacionan entre sí.

```mermaid
classDiagram
    %% CLASES
    class Usuario
    class Operario
    class Empresa
    class Trabajo
    class Factura
    class Presupuesto
    class FotoTrabajo
    class Ubicacion

    %% ENUMS
    class Rol { <<enumeration>> }
    class CategoriaServicio { <<enumeration>> }
    class EstadoTrabajo { <<enumeration>> }

    %% RELACIONES

    %% Herencia
    Usuario <|-- Operario : Es un<br/>(Herencia)

    %% Asociaciones Principales
    Empresa "1" --> "1..*" CategoriaServicio : Ofrece<br/>(Asociación)

    %% RELACIÓN USUARIO - EMPRESA AGREGADA
    Usuario "*" --> "0..1" Empresa : Pertenece a<br/>(Asociación)

    Trabajo "*" --> "1" Usuario : Solicitado por<br/>(Asociación)
    Trabajo "*" --> "0..1" Operario : Asignado a<br/>(Asociación)
    Trabajo "*" ..> "1" CategoriaServicio : Clasificado como<br/>(Dependencia)
    Trabajo "*" ..> "1" EstadoTrabajo : Tiene estado<br/>(Dependencia)

    %% Composiciones
    Trabajo "1" *-- "0..*" FotoTrabajo : Contiene<br/>(Composición)
    Trabajo "1" *-- "1" Ubicacion : Localizada en<br/>(Composición)

    %% Dependencias de Enums
    Usuario "1" ..> "1" Rol : Tiene rol<br/>(Dependencia)
    Operario "*" ..> "1" CategoriaServicio : Especialidad<br/>(Dependencia)

    %% Documentos
    Factura "0..1" --> "1" Trabajo : Facturación de<br/>(Asociación)
    Presupuesto "0..*" --> "1" Trabajo : Presupuesto para<br/>(Asociación)
    Presupuesto "*" --> "1" Empresa : Emitido por<br/>(Asociación)
```

## 2. Diagrama Detallado (Completo)

Vista técnica alineada con el código Java, incluyendo atributos completos y todos los métodos (Constructores, Getters y Setters).

```mermaid
classDiagram
    %% --- CLASES ---
    class Usuario {
        #int id
        #String nombreCompleto
        #String email
        #String passwordHash
        #Rol rol
        #int idEmpresa
        #LocalDateTime fechaRegistro
        #String telefono
        #String direccion
        #String urlFoto

        +Usuario()
        +Usuario(int id, String email, String passwordHash, Rol rol, int idEmpresa)
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
        +getIdEmpresa() int
        +setIdEmpresa(int idEmpresa)
        +getFechaRegistro() LocalDateTime
        +setFechaRegistro(LocalDateTime fechaRegistro)
        +getTelefono() String
        +setTelefono(String telefono)
        +getDireccion() String
        +setDireccion(String direccion)
        +getUrlFoto() String
        +setUrlFoto(String urlFoto)
    }

    class Operario {
        -String dni
        -CategoriaServicio especialidad
        -boolean estaActivo
        -double latitud
        -double longitud
        -LocalDateTime ultimaActualizacion

        +Operario()
        +getDni() String
        +setDni(String dni)
        +getEspecialidad() CategoriaServicio
        +setEspecialidad(CategoriaServicio especialidad)
        +isEstaActivo() boolean
        +setEstaActivo(boolean estaActivo)
        +getLatitud() double
        +setLatitud(double latitud)
        +getLongitud() double
        +setLongitud(double longitud)
        +getUltimaActualizacion() LocalDateTime
        +setUltimaActualizacion(LocalDateTime ultimaActualizacion)
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

        +Empresa()
        +Empresa(int id, String nombre, String cif)
        +getId() int
        +setId(int id)
        +getNombre() String
        +setNombre(String nombre)
        +getCif() String
        +setCif(String cif)
        +getDireccion() String
        +setDireccion(String direccion)
        +getTelefono() String
        +setTelefono(String telefono)
        +getEmailContacto() String
        +setEmailContacto(String emailContacto)
        +getUrlFoto() String
        +setUrlFoto(String urlFoto)
        +getEspecialidades() List~CategoriaServicio~
        +setEspecialidades(List~CategoriaServicio~ especialidades)
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

        +Trabajo()
        +getId() int
        +setId(int id)
        +getCliente() Usuario
        +setCliente(Usuario cliente)
        +getOperarioAsignado() Operario
        +setOperarioAsignado(Operario operarioAsignado)
        +getCategoria() CategoriaServicio
        +setCategoria(CategoriaServicio categoria)
        +getTitulo() String
        +setTitulo(String titulo)
        +getDescripcion() String
        +setDescripcion(String descripcion)
        +getUbicacion() Ubicacion
        +setUbicacion(Ubicacion ubicacion)
        +getDireccion() String
        +setDireccion(String direccion)
        +getEstado() EstadoTrabajo
        +setEstado(EstadoTrabajo estado)
        +getFechaCreacion() LocalDateTime
        +setFechaCreacion(LocalDateTime fechaCreacion)
        +getFechaFinalizacion() LocalDateTime
        +setFechaFinalizacion(LocalDateTime fechaFinalizacion)
        +getValoracion() int
        +setValoracion(int valoracion)
        +getComentarioCliente() String
        +setComentarioCliente(String comentarioCliente)
        +getFotos() List~FotoTrabajo~
        +setFotos(List~FotoTrabajo~ fotos)
    }

    class Factura {
        -int id
        -Trabajo trabajo
        -String numeroFactura
        -double baseImponible
        -double iva
        -double total
        -LocalDateTime fechaEmision
        -String rutaPdf
        -boolean pagada

        +Factura()
        +getId() int
        +setId(int id)
        +getTrabajo() Trabajo
        +setTrabajo(Trabajo trabajo)
        +getNumeroFactura() String
        +setNumeroFactura(String numeroFactura)
        +getBaseImponible() double
        +setBaseImponible(double baseImponible)
        +getIva() double
        +setIva(double iva)
        +getTotal() double
        +setTotal(double total)
        +getFechaEmision() LocalDateTime
        +setFechaEmision(LocalDateTime fechaEmision)
        +getRutaPdf() String
        +setRutaPdf(String rutaPdf)
        +isPagada() boolean
        +setPagada(boolean pagada)
    }

    class Presupuesto {
        -int id
        -Trabajo trabajo
        -Empresa empresa
        -double monto
        -Date fechaEnvio

        +Presupuesto()
        +getId() int
        +setId(int id)
        +getTrabajo() Trabajo
        +setTrabajo(Trabajo trabajo)
        +getEmpresa() Empresa
        +setEmpresa(Empresa empresa)
        +getMonto() double
        +setMonto(double monto)
        +getFechaEnvio() Date
        +setFechaEnvio(Date fechaEnvio)
    }

    class FotoTrabajo {
        -int id
        -int idTrabajo
        -String url

        +FotoTrabajo()
        +FotoTrabajo(int id, int idTrabajo, String url)
        +getId() int
        +setId(int id)
        +getIdTrabajo() int
        +setIdTrabajo(int idTrabajo)
        +getUrl() String
        +setUrl(String url)
    }

    class Ubicacion {
        -double latitud
        -double longitud

        +Ubicacion()
        +getLatitud() double
        +setLatitud(double latitud)
        +getLongitud() double
        +setLongitud(double longitud)
    }

    %% --- ENUMS ---
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
        LIMPIEZA
        CLIMATIZACION
        CARPINTERIA
        CERRAJERIA
        OTROS
        MULTISERVICIO
    }

    class EstadoTrabajo {
        <<enumeration>>
        PENDIENTE
        ASIGNADO
        EN_PROCESO
        FINALIZADO
        CANCELADO
    }

    %% --- RELACIONES CON SALTO DE LINEA <br/> ---

    %% 1. Herencia (IS-A)
    Usuario <|-- Operario : Es un<br/>(Herencia)

    %% 2. Asociaciones y Dependencias
    %% Empresa
    Empresa "1" --> "1..*" CategoriaServicio : Ofrece<br/>(Asociación)

    %% RELACIÓN USUARIO - EMPRESA (AÑADIDA)
    Usuario "*" --> "0..1" Empresa : Pertenece a<br/>(Asociación)

    %% Trabajo
    Trabajo "*" --> "1" Usuario : Solicitado por<br/>(Asociación)
    Trabajo "*" --> "0..1" Operario : Asignado a<br/>(Asociación)
    Trabajo "*" ..> "1" CategoriaServicio : Clasificado como<br/>(Dependencia)
    Trabajo "*" ..> "1" EstadoTrabajo : Tiene estado<br/>(Dependencia)

    %% Composiciones
    Trabajo "1" *-- "0..*" FotoTrabajo : Contiene<br/>(Composición)
    Trabajo "1" *-- "1" Ubicacion : Localizada en<br/>(Composición)

    %% Roles y Especialidades
    Usuario "1" ..> "1" Rol : Tiene rol<br/>(Dependencia)
    Operario "*" ..> "1" CategoriaServicio : Especialidad<br/>(Dependencia)

    %% Documentos Financieros
    Factura "0..1" --> "1" Trabajo : Facturacion de<br/>(Asociación)
    Presupuesto "0..*" --> "1" Trabajo : Presupuesto para<br/>(Asociación)
    Presupuesto "*" --> "1" Empresa : Emitido por<br/>(Asociación)
```

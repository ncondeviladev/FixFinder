-- Base de datos para FIXFINDER
-- Arquitectura Centralizada
-- REFACTORIZADO: Jerarquía de Usuarios (Usuario -> Operario / Cliente)

DROP DATABASE IF EXISTS fixfinder;

CREATE DATABASE fixfinder;

USE fixfinder;

-- 1. Tabla de Empresas (Para el modo multi-tenant/demo)
CREATE TABLE empresa (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    cif VARCHAR(20) UNIQUE NOT NULL,
    direccion VARCHAR(200),
    telefono VARCHAR(20),
    email_contacto VARCHAR(100),
    url_foto VARCHAR(255),
    fecha_alta TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE empresa_especialidad (
    id_empresa INT NOT NULL,
    categoria ENUM(
        'FONTANERIA',
        'ELECTRICIDAD',
        'ALBANILERIA',
        'PINTURA',
        'LIMPIEZA',
        'CLIMATIZACION',
        'CARPINTERIA',
        'CERRAJERIA',
        'OTROS'
    ) NOT NULL,
    PRIMARY KEY (id_empresa, categoria),
    FOREIGN KEY (id_empresa) REFERENCES empresa (id) ON DELETE CASCADE
);

-- 2. Tabla Base de Usuarios (Común para Operarios y Clientes)
CREATE TABLE usuario (
    id INT AUTO_INCREMENT PRIMARY KEY,
    -- id_empresa ELIMINADO de aquí (ahora es específico de Operario)
    email VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL, -- BCrypt
    nombre_completo VARCHAR(100) NOT NULL,
    telefono VARCHAR(20),
    direccion VARCHAR(200),
    url_foto VARCHAR(255),
    dni VARCHAR(20) UNIQUE, -- MOVIDO aquí (era de Operario, ahora común)
    rol ENUM(
        'ADMIN',
        'GERENTE',
        'OPERARIO',
        'CLIENTE'
    ) NOT NULL,
    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (email)
);

-- 3. Tabla Específica de Operarios (Extensión de Usuario)
CREATE TABLE operario (
    id_usuario INT PRIMARY KEY, -- FK y PK al mismo tiempo (1:1)
    id_empresa INT NOT NULL, -- MOVIDO aquí (Vinculación laboral)
    especialidad ENUM(
        'FONTANERIA',
        'ELECTRICIDAD',
        'ALBANILERIA',
        'PINTURA',
        'LIMPIEZA',
        'CLIMATIZACION',
        'CARPINTERIA',
        'CERRAJERIA',
        'OTROS'
    ) NOT NULL,
    estado ENUM(
        'DISPONIBLE',
        'OCUPADO',
        'BAJA'
    ) DEFAULT 'DISPONIBLE',
    latitud DOUBLE,
    longitud DOUBLE,
    ultima_actualizacion TIMESTAMP,
    FOREIGN KEY (id_usuario) REFERENCES usuario (id) ON DELETE CASCADE,
    FOREIGN KEY (id_empresa) REFERENCES empresa (id) ON DELETE CASCADE
);

-- 4. Tabla Específica de Clientes (Extensión de Usuario)
CREATE TABLE cliente (
    id_usuario INT PRIMARY KEY, -- FK y PK al mismo tiempo (1:1)
    -- Aquí irían campos específicos de cliente si los hubiera
    FOREIGN KEY (id_usuario) REFERENCES usuario (id) ON DELETE CASCADE
);

-- 5. Trabajos / Solicitudes de Servicio
CREATE TABLE trabajo (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_cliente INT NOT NULL,
    id_operario INT, -- Puede ser NULL si aún no se ha asignado
    categoria ENUM(
        'FONTANERIA',
        'ELECTRICIDAD',
        'ALBANILERIA',
        'PINTURA',
        'LIMPIEZA',
        'CLIMATIZACION',
        'CARPINTERIA',
        'CERRAJERIA',
        'OTROS'
    ) NOT NULL,
    titulo VARCHAR(150) NOT NULL,
    descripcion TEXT,
    direccion VARCHAR(200),
    ubicacion_lat DOUBLE,
    ubicacion_lon DOUBLE,
    estado ENUM(
        'PENDIENTE',
        'PRESUPUESTADO',
        'ACEPTADO',
        'ASIGNADO',
        'REALIZADO',
        'FINALIZADO',
        'PAGADO',
        'CANCELADO'
    ) DEFAULT 'PENDIENTE',
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_finalizacion TIMESTAMP,
    valoracion INT DEFAULT 0, -- 1 a 5 estrellas
    comentario_cliente TEXT,
    FOREIGN KEY (id_cliente) REFERENCES usuario (id),
    FOREIGN KEY (id_operario) REFERENCES usuario (id)
);

-- Tabla para almacenar las URLs de las fotos de los trabajos
CREATE TABLE foto_trabajo (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_trabajo INT NOT NULL,
    url_archivo TEXT NOT NULL,
    fecha_subida TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_trabajo) REFERENCES trabajo (id) ON DELETE CASCADE
);

-- 6. Presupuestos
CREATE TABLE presupuesto (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_trabajo INT NOT NULL,
    id_empresa INT NOT NULL,
    monto DECIMAL(15, 2) NOT NULL,
    estado VARCHAR(50) DEFAULT 'PENDIENTE',
    fecha_envio TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_trabajo) REFERENCES trabajo (id) ON DELETE CASCADE,
    FOREIGN KEY (id_empresa) REFERENCES empresa (id)
);

-- 7. Facturas
CREATE TABLE factura (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_trabajo INT UNIQUE NOT NULL,
    numero_factura VARCHAR(50) UNIQUE NOT NULL,
    base_imponible DECIMAL(10, 2) NOT NULL,
    iva DECIMAL(10, 2) NOT NULL,
    total DECIMAL(10, 2) NOT NULL,
    fecha_emision TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ruta_pdf VARCHAR(255),
    pagada BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (id_trabajo) REFERENCES trabajo (id)
);

-- 8. Mensajes de Chat
CREATE TABLE mensaje_chat (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_trabajo INT NOT NULL,
    id_remitente INT NOT NULL,
    contenido TEXT NOT NULL,
    fecha_envio TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    leido BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (id_trabajo) REFERENCES trabajo (id),
    FOREIGN KEY (id_remitente) REFERENCES usuario (id)
);

-- Datos de prueba iniciales
INSERT INTO
    empresa (nombre, cif)
VALUES (
        'Reparaciones Express S.L.',
        'B12345678'
    );

-- Usuario 1 (Admin)
INSERT INTO
    usuario (
        email,
        password_hash,
        nombre_completo,
        rol,
        dni
    )
VALUES (
        'admin@express.com',
        'hash123',
        'Admin Sistema',
        'ADMIN',
        '00000000A'
    );

-- Usuario 2 (Operario)
INSERT INTO
    usuario (
        email,
        password_hash,
        nombre_completo,
        rol,
        dni
    )
VALUES (
        'pepe@express.com',
        'hash123',
        'Pepe Gotera',
        'OPERARIO',
        '12345678Z'
    );

-- Operario asociado
INSERT INTO
    operario (
        id_usuario,
        id_empresa,
        especialidad
    )
VALUES (2, 1, 'FONTANERIA');
-- Base de datos para FIXFINDER
-- Arquitectura Centralizada

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

-- 2. Tabla Base de Usuarios (Herencia para Operarios y Clientes)
CREATE TABLE usuario (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_empresa INT NOT NULL, -- FK a Empresa
    email VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL, -- BCrypt
    nombre_completo VARCHAR(100) NOT NULL,
    rol ENUM(
        'ADMIN',
        'GERENTE',
        'OPERARIO',
        'CLIENTE'
    ) NOT NULL,
    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (email),
    FOREIGN KEY (id_empresa) REFERENCES empresa (id) ON DELETE CASCADE
);

-- 3. Tabla Específica de Operarios (Extensión de Usuario)
CREATE TABLE operario (
    id_usuario INT PRIMARY KEY, -- FK y PK al mismo tiempo (1:1)
    dni VARCHAR(20) UNIQUE NOT NULL,
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
    FOREIGN KEY (id_usuario) REFERENCES usuario (id) ON DELETE CASCADE
);

-- 4. Trabajos / Solicitudes de Servicio (Categoria TABLE ELIMINADA)

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
    direccion VARCHAR(200) NOT NULL,
    latitud DOUBLE,
    longitud DOUBLE,
    estado ENUM(
        'PENDIENTE',
        'ASIGNADO',
        'EN_PROCESO',
        'FINALIZADO',
        'CANCELADO'
    ) DEFAULT 'PENDIENTE',
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_finalizacion TIMESTAMP,

-- Calidad y Feedback
valoracion INT DEFAULT 0, -- 1 a 5 estrellas
    comentario_cliente TEXT,

    FOREIGN KEY (id_cliente) REFERENCES usuario (id),
    FOREIGN KEY (id_operario) REFERENCES operario (id_usuario)
);

-- Tabla para almacenar las URLs de las fotos de los trabajos
CREATE TABLE foto_trabajo (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_trabajo INT NOT NULL,
    url_archivo VARCHAR(255) NOT NULL,
    fecha_subida TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_trabajo) REFERENCES trabajo (id) ON DELETE CASCADE
);

-- 6. Facturas
CREATE TABLE factura (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_trabajo INT UNIQUE NOT NULL, -- 1:1 con Trabajo
    numero_factura VARCHAR(50) UNIQUE NOT NULL, -- Ej: 2023-0001
    base_imponible DECIMAL(10, 2) NOT NULL,
    iva DECIMAL(10, 2) NOT NULL, -- Porcentaje o monto, mejor monto calculado
    total DECIMAL(10, 2) NOT NULL,
    fecha_emision TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ruta_pdf VARCHAR(255), -- Ruta local en el servidor
    pagada BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (id_trabajo) REFERENCES trabajo (id)
);

-- 7. Mensajes de Chat (Opcional pero recomendado para Sockets)
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
-- La contraseña sería un hash real en producción
INSERT INTO
    usuario (
        id_empresa,
        email,
        password_hash,
        nombre_completo,
        rol
    )
VALUES (
        1,
        'admin@express.com',
        'hash123',
        'Admin Sistema',
        'ADMIN'
    ),
    (
        1,
        'pepe@express.com',
        'hash123',
        'Pepe Gotera',
        'OPERARIO'
    );

INSERT INTO
    operario (id_usuario, dni, especialidad)
VALUES (2, '12345678Z', 'FONTANERIA');
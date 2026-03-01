-- ============================================================
-- FIXFINDER - SEEDER DE DATOS DE PRUEBA
-- Limpia y recrea: empresas, usuarios, operarios, trabajos
-- ============================================================

USE fixfinder;

-- Limpieza en orden correcto (respetando FK)
SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE mensaje_chat;

TRUNCATE TABLE factura;

TRUNCATE TABLE presupuesto;

TRUNCATE TABLE foto_trabajo;

TRUNCATE TABLE trabajo;

TRUNCATE TABLE cliente;

TRUNCATE TABLE operario;

TRUNCATE TABLE empresa_especialidad;

TRUNCATE TABLE empresa;

TRUNCATE TABLE usuario;

SET FOREIGN_KEY_CHECKS = 1;

-- ── EMPRESAS ─────────────────────────────────────────────────
INSERT INTO
    empresa (
        id,
        nombre,
        cif,
        direccion,
        telefono,
        email_contacto
    )
VALUES (
        1,
        'Levante Reparaciones S.L.',
        'B12345678',
        'Calle Mayor 10, Valencia',
        '961234567',
        'contacto@levante.com'
    ),
    (
        2,
        'Express Fix S.A.',
        'A87654321',
        'Av. del Puerto 45, Valencia',
        '963456789',
        'info@expressfix.com'
    );

INSERT INTO
    empresa_especialidad (id_empresa, categoria)
VALUES (1, 'ELECTRICIDAD'),
    (1, 'FONTANERIA'),
    (1, 'ALBANILERIA'),
    (2, 'CLIMATIZACION'),
    (2, 'PINTURA'),
    (2, 'FONTANERIA');

-- ── USUARIOS ─────────────────────────────────────────────────
-- Admin
INSERT INTO
    usuario (
        id,
        email,
        password_hash,
        nombre_completo,
        rol,
        dni,
        telefono
    )
VALUES (
        1,
        'admin@fixfinder.com',
        'aXJ6HwScHU6OeZ+vxPGiJA==:bgGpNrYF+tiIa2y+FJW+jdSb9W5olhTI/vqeeeXaMbQ=',
        'Admin Sistema',
        'ADMIN',
        '00000000A',
        '600000001'
    );

-- Gerentes
INSERT INTO
    usuario (
        id,
        email,
        password_hash,
        nombre_completo,
        rol,
        dni,
        telefono
    )
VALUES (
        2,
        'gerente.a@levante.com',
        'aXJ6HwScHU6OeZ+vxPGiJA==:bgGpNrYF+tiIa2y+FJW+jdSb9W5olhTI/vqeeeXaMbQ=',
        'Carlos Gerente',
        'GERENTE',
        '11111111B',
        '611111111'
    ),
    (
        3,
        'gerente.b@express.com',
        'aXJ6HwScHU6OeZ+vxPGiJA==:bgGpNrYF+tiIa2y+FJW+jdSb9W5olhTI/vqeeeXaMbQ=',
        'Manolo Gerente',
        'GERENTE',
        '22222222C',
        '622222222'
    );

-- Operarios
INSERT INTO
    usuario (
        id,
        email,
        password_hash,
        nombre_completo,
        rol,
        dni,
        telefono
    )
VALUES (
        4,
        'paco@levante.com',
        'aXJ6HwScHU6OeZ+vxPGiJA==:bgGpNrYF+tiIa2y+FJW+jdSb9W5olhTI/vqeeeXaMbQ=',
        'Paco Fontanero',
        'OPERARIO',
        '33333333D',
        '633333333'
    ),
    (
        5,
        'benito@express.com',
        'aXJ6HwScHU6OeZ+vxPGiJA==:bgGpNrYF+tiIa2y+FJW+jdSb9W5olhTI/vqeeeXaMbQ=',
        'Benito Pintor',
        'OPERARIO',
        '44444444E',
        '644444444'
    );

-- Clientes
INSERT INTO
    usuario (
        id,
        email,
        password_hash,
        nombre_completo,
        rol,
        dni,
        telefono,
        direccion
    )
VALUES (
        6,
        'marta@gmail.com',
        'aXJ6HwScHU6OeZ+vxPGiJA==:bgGpNrYF+tiIa2y+FJW+jdSb9W5olhTI/vqeeeXaMbQ=',
        'Marta Cliente',
        'CLIENTE',
        '55555555F',
        '655555555',
        'Calle Poeta Querol 3, Valencia'
    ),
    (
        7,
        'juan@hotmail.com',
        'aXJ6HwScHU6OeZ+vxPGiJA==:bgGpNrYF+tiIa2y+FJW+jdSb9W5olhTI/vqeeeXaMbQ=',
        'Juan Cliente',
        'CLIENTE',
        '66666666G',
        '666666666',
        'Avenida Blasco Ibañez 12, Valencia'
    );

-- ── OPERARIOS (tabla específica) ─────────────────────────────
INSERT INTO
    operario (
        id_usuario,
        id_empresa,
        especialidad,
        estado
    )
VALUES (
        2,
        1,
        'ELECTRICIDAD',
        'DISPONIBLE'
    ), -- Carlos como gerente de Levante
    (
        3,
        2,
        'CLIMATIZACION',
        'DISPONIBLE'
    ), -- Manolo como gerente de Express
    (
        4,
        1,
        'FONTANERIA',
        'DISPONIBLE'
    ), -- Paco
    (5, 2, 'PINTURA', 'DISPONIBLE');
-- Benito

-- ── CLIENTES (tabla específica) ──────────────────────────────
INSERT INTO cliente (id_usuario) VALUES (6), (7);

INSERT INTO
    trabajo (
        id_cliente,
        id_operario,
        categoria,
        titulo,
        descripcion,
        direccion,
        estado
    )
VALUES (
        6,
        NULL,
        'FONTANERIA',
        'Fuga en bano principal',
        'Hay una fuga de agua debajo del lavabo del bano principal.',
        'Calle Poeta Querol 3, Valencia',
        'PENDIENTE'
    ),
    (
        6,
        NULL,
        'PINTURA',
        'Pintar salon completo',
        'El salon necesita una mano de pintura completa, paredes y techo.',
        'Calle Poeta Querol 3, Valencia',
        'PENDIENTE'
    ),
    (
        6,
        NULL,
        'ELECTRICIDAD',
        'Enchufe roto en cocina',
        'El enchufe de la cocina no funciona, posible cortocircuito.',
        'Calle Poeta Querol 3, Valencia',
        'PENDIENTE'
    ),
    (
        6,
        NULL,
        'ALBANILERIA',
        'Grieta en pared exterior',
        'Grieta de unos 30cm en la pared exterior del garaje.',
        'Calle Poeta Querol 3, Valencia',
        'PENDIENTE'
    );

INSERT INTO
    trabajo (
        id_cliente,
        id_operario,
        categoria,
        titulo,
        descripcion,
        direccion,
        estado
    )
VALUES (
        7,
        NULL,
        'CLIMATIZACION',
        'Aire acondicionado no enfria',
        'El split del dormitorio no baja de 25 grados aunque este al minimo.',
        'Avenida Blasco Ibanez 12, Valencia',
        'PENDIENTE'
    );

SELECT 'Seeder completado correctamente.' AS resultado;
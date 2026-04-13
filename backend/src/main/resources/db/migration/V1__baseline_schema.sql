-- Baseline schema — estado inicial del MVP1
-- Generado desde las entidades JPA. Inmutable una vez commiteado.
-- En una DB existente, Flyway aplica baseline-on-migrate y no ejecuta este archivo.
-- En una instalación nueva, este archivo crea el schema completo.

CREATE TABLE users (
    id       BIGINT       NOT NULL AUTO_INCREMENT,
    email    VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role     VARCHAR(255) NOT NULL DEFAULT 'ADMIN',
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE pacientes (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    nombre           VARCHAR(255) NOT NULL,
    apellido         VARCHAR(255) NOT NULL,
    telefono         VARCHAR(15)  NOT NULL,
    email            VARCHAR(255) NOT NULL,
    fecha_nacimiento DATE,
    user_id          BIGINT,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_pacientes_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT
);

CREATE TABLE tratamientos (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    nombre      VARCHAR(200)    NOT NULL,
    descripcion VARCHAR(1000),
    precio      DECIMAL(10, 2)  NOT NULL,
    activo      TINYINT(1)      NOT NULL,
    foto_url    VARCHAR(500),
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE citas (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    paciente_id       BIGINT       NOT NULL,
    fecha_hora_inicio DATETIME     NOT NULL,
    duracion_minutos  INT          NOT NULL,
    estado            VARCHAR(255) NOT NULL,
    motivo            VARCHAR(500),
    notas             VARCHAR(1000),
    created_at        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_citas_paciente FOREIGN KEY (paciente_id) REFERENCES pacientes (id) ON DELETE RESTRICT
);

CREATE TABLE historial_clinico (
    id               BIGINT         NOT NULL AUTO_INCREMENT,
    paciente_id      BIGINT         NOT NULL,
    cita_id          BIGINT,
    fecha_hora       DATETIME       NOT NULL,
    procedimiento    VARCHAR(500)   NOT NULL,
    notas            VARCHAR(2000),
    tratamiento_id   BIGINT,
    precio_aplicado  DECIMAL(10, 2),
    foto_url         VARCHAR(500),
    created_at       TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_historial_paciente   FOREIGN KEY (paciente_id)    REFERENCES pacientes    (id) ON DELETE RESTRICT,
    CONSTRAINT fk_historial_cita       FOREIGN KEY (cita_id)        REFERENCES citas        (id) ON DELETE RESTRICT,
    CONSTRAINT fk_historial_tratamiento FOREIGN KEY (tratamiento_id) REFERENCES tratamientos (id) ON DELETE RESTRICT
);

CREATE TABLE pagos (
    id          BIGINT         NOT NULL AUTO_INCREMENT,
    paciente_id BIGINT         NOT NULL,
    monto       DECIMAL(10, 2) NOT NULL,
    fecha       DATE           NOT NULL,
    concepto    VARCHAR(500),
    created_at  TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_pagos_paciente FOREIGN KEY (paciente_id) REFERENCES pacientes (id) ON DELETE RESTRICT
);

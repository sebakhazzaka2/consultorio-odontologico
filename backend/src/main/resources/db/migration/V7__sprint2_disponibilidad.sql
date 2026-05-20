-- Duración por servicio (admin configura cuánto tarda cada procedimiento)
ALTER TABLE servicios
    ADD COLUMN duracion_minutos INT NOT NULL DEFAULT 30;

-- Disponibilidad semanal: horarios por día de semana (0=Domingo … 6=Sábado)
CREATE TABLE disponibilidad_semanal (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    dia_semana      INT             NOT NULL COMMENT '0=Domingo,1=Lunes,...,6=Sábado',
    activo          BOOLEAN         NOT NULL DEFAULT TRUE,
    hora_apertura   TIME            NOT NULL,
    hora_cierre     TIME            NOT NULL,
    pausa_inicio    TIME            NULL,
    pausa_fin       TIME            NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_disponibilidad_semanal PRIMARY KEY (id),
    CONSTRAINT uq_disponibilidad_dia UNIQUE (dia_semana)
);

-- Fechas bloqueadas: feriados, vacaciones, días sin atención
CREATE TABLE fechas_bloqueadas (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    fecha       DATE        NOT NULL,
    motivo      VARCHAR(200) NULL,
    created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_fechas_bloqueadas PRIMARY KEY (id),
    CONSTRAINT uq_fecha_bloqueada UNIQUE (fecha)
);

-- Seed: lunes a viernes 09:00–18:00, pausa 13:00–14:00
INSERT INTO disponibilidad_semanal (dia_semana, activo, hora_apertura, hora_cierre, pausa_inicio, pausa_fin) VALUES
    (1, TRUE, '09:00:00', '18:00:00', '13:00:00', '14:00:00'),
    (2, TRUE, '09:00:00', '18:00:00', '13:00:00', '14:00:00'),
    (3, TRUE, '09:00:00', '18:00:00', '13:00:00', '14:00:00'),
    (4, TRUE, '09:00:00', '18:00:00', '13:00:00', '14:00:00'),
    (5, TRUE, '09:00:00', '18:00:00', '13:00:00', '14:00:00'),
    (6, FALSE, '09:00:00', '13:00:00', NULL, NULL),
    (0, FALSE, '09:00:00', '13:00:00', NULL, NULL);

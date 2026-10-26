-- V11: outbox de notificaciones (F2a) + preferencia de recordatorios por paciente.
--
-- notificaciones: cada cambio de cita inserta una fila PENDIENTE en la MISMA transacción
-- (atómico con el cambio de negocio); un despachador programado la envía después, con
-- reintentos. Nunca se guarda el email del destinatario ni el contenido: se resuelven al
-- enviar. ultimo_error se guarda sin direcciones de email (sanitizado en la app).
--
--   clave_dedupe  UNIQUE: idempotencia ("SOLICITUD:12", "RECORDATORIO_24H:12:20261020T1030").
--   cita_id       NULL-able. FK ON DELETE SET NULL (excepción justificada a la regla RESTRICT):
--                 DELETE /citas/{id} borra la cita de verdad; RESTRICT rompería ese endpoint en
--                 cuanto exista una notificación, y CASCADE perdería el historial de qué se envió.
--                 Con SET NULL queda la auditoría y el despachador descarta las pendientes
--                 sin cita (DESCARTADA).
--   referencia_id NULL-able, sin FK: referencia genérica para notificaciones sin cita
--                 (RESET_PASSWORD guarda el id del token de V12). Nunca el token en sí.
--   proximo_intento_at  cuándo es elegible para el despachador. También funciona de "lease":
--                 al tomar una fila se corre unos minutos al futuro para que otra corrida no
--                 la vuelva a tomar mientras se envía.
--
-- Rollback: DROP TABLE notificaciones; ALTER TABLE pacientes DROP COLUMN recordatorios_email;
-- (la versión anterior de la app no usa ninguna de las dos).

CREATE TABLE notificaciones (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    tipo                ENUM('SOLICITUD', 'AVISO_CLINICA', 'CONFIRMACION', 'REAGENDADA',
                             'CANCELACION', 'RECORDATORIO_24H', 'RESET_PASSWORD') NOT NULL,
    canal               ENUM('EMAIL')   NOT NULL,
    clave_dedupe        VARCHAR(150)    NOT NULL,
    cita_id             BIGINT          NULL,
    referencia_id       BIGINT          NULL,
    destinatario_tipo   ENUM('PACIENTE', 'CLINICA', 'ADMIN') NOT NULL,
    estado              ENUM('PENDIENTE', 'ENVIADA', 'FALLIDA', 'DESCARTADA') NOT NULL,
    intentos            INT             NOT NULL DEFAULT 0,
    proximo_intento_at  TIMESTAMP       NOT NULL,
    ultimo_error        VARCHAR(500)    NULL,
    enviada_at          TIMESTAMP       NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_notificaciones_clave_dedupe UNIQUE (clave_dedupe),
    CONSTRAINT fk_notificaciones_cita FOREIGN KEY (cita_id) REFERENCES citas (id) ON DELETE SET NULL,
    INDEX idx_notificaciones_estado_proximo (estado, proximo_intento_at)
);

-- La baja (link en el recordatorio) apaga SOLO los recordatorios; los transaccionales
-- (solicitud, confirmación, cancelación, reagendamiento) se siguen enviando.
ALTER TABLE pacientes ADD COLUMN recordatorios_email BOOLEAN NOT NULL DEFAULT TRUE;

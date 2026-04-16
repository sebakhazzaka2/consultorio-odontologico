-- V3 definió el ENUM en minúsculas pero CitaEstado.java usa constantes en MAYÚSCULAS.
-- Hibernate @Enumerated(EnumType.STRING) llama a Enum.valueOf() que es case-sensitive:
-- valueOf("confirmada") falla porque la constante es CONFIRMADA → 500 en GET /api/citas.
--
-- Esta migración:
--  1. Convierte los datos existentes a mayúsculas.
--  2. Redefine la columna ENUM con valores en mayúsculas para que coincida con el enum Java.

UPDATE citas SET estado = UPPER(estado);

ALTER TABLE citas
    MODIFY COLUMN estado ENUM('PENDIENTE','CONFIRMADA','CANCELADA','COMPLETADA') NOT NULL;

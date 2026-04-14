-- Corrige columnas id y FK creadas como INT por Hibernate ddl-auto=update.
-- Hibernate 6 espera BIGINT para campos Long. Detectado al activar ddl-auto=validate.
-- Aprovecha para normalizar nombres de FK constraints a la convención del proyecto.

-- 1. Drop de los FK constraints existentes
ALTER TABLE pacientes         DROP FOREIGN KEY FKs4xpmvt68heml4pc1m8bay0es;
ALTER TABLE citas             DROP FOREIGN KEY fk_citas_paciente;
ALTER TABLE historial_clinico DROP FOREIGN KEY FKqsgfbnpj2fmtawdfnp7y808qu;
ALTER TABLE historial_clinico DROP FOREIGN KEY FKjys8o2lrdx4qmpus6elgejxj2;

-- 2. Migrar PKs de tablas padre a BIGINT
ALTER TABLE users            MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE pacientes        MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE tratamientos     MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE citas            MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE historial_clinico MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE pagos            MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;

-- 3. Migrar columnas FK a BIGINT
ALTER TABLE pacientes
    MODIFY COLUMN user_id BIGINT;

ALTER TABLE citas
    MODIFY COLUMN paciente_id BIGINT NOT NULL;

ALTER TABLE historial_clinico
    MODIFY COLUMN paciente_id    BIGINT NOT NULL,
    MODIFY COLUMN cita_id        BIGINT,
    MODIFY COLUMN tratamiento_id BIGINT;

ALTER TABLE pagos
    MODIFY COLUMN paciente_id BIGINT NOT NULL;

-- 4. Recrear todos los FK constraints con nombres canónicos (alineados con V1)
ALTER TABLE pacientes
    ADD CONSTRAINT fk_pacientes_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT;

ALTER TABLE citas
    ADD CONSTRAINT fk_citas_paciente
        FOREIGN KEY (paciente_id) REFERENCES pacientes (id) ON DELETE RESTRICT;

ALTER TABLE historial_clinico
    ADD CONSTRAINT fk_historial_paciente
        FOREIGN KEY (paciente_id) REFERENCES pacientes (id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_historial_cita
        FOREIGN KEY (cita_id) REFERENCES citas (id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_historial_tratamiento
        FOREIGN KEY (tratamiento_id) REFERENCES tratamientos (id) ON DELETE RESTRICT;

ALTER TABLE pagos
    ADD CONSTRAINT fk_pagos_paciente
        FOREIGN KEY (paciente_id) REFERENCES pacientes (id) ON DELETE RESTRICT;

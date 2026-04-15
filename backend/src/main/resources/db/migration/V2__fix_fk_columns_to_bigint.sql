-- Corrige columnas id y FK creadas como INT por Hibernate ddl-auto=update.
-- Hibernate 6 espera BIGINT para campos Long. Detectado al activar ddl-auto=validate.
-- Aprovecha para normalizar nombres de FK constraints a la convención del proyecto.
--
-- IDEMPOTENTE: usa SQL dinámico con information_schema para no fallar en instalaciones
-- limpias desde V1 (donde las columnas ya son BIGINT y los nombres ya son canónicos).

-- ============================================================
-- 1. Drop de FK constraints con nombres Hibernate (solo si existen)
-- ============================================================

SELECT COUNT(*) INTO @n FROM information_schema.TABLE_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'pacientes'
  AND CONSTRAINT_NAME = 'FKs4xpmvt68heml4pc1m8bay0es' AND CONSTRAINT_TYPE = 'FOREIGN KEY';
SET @s = IF(@n > 0, 'ALTER TABLE pacientes DROP FOREIGN KEY FKs4xpmvt68heml4pc1m8bay0es', 'SELECT 1');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

SELECT COUNT(*) INTO @n FROM information_schema.TABLE_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'citas'
  AND CONSTRAINT_NAME = 'fk_citas_paciente' AND CONSTRAINT_TYPE = 'FOREIGN KEY';
SET @s = IF(@n > 0, 'ALTER TABLE citas DROP FOREIGN KEY fk_citas_paciente', 'SELECT 1');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

SELECT COUNT(*) INTO @n FROM information_schema.TABLE_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'historial_clinico'
  AND CONSTRAINT_NAME = 'FKqsgfbnpj2fmtawdfnp7y808qu' AND CONSTRAINT_TYPE = 'FOREIGN KEY';
SET @s = IF(@n > 0, 'ALTER TABLE historial_clinico DROP FOREIGN KEY FKqsgfbnpj2fmtawdfnp7y808qu', 'SELECT 1');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

SELECT COUNT(*) INTO @n FROM information_schema.TABLE_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'historial_clinico'
  AND CONSTRAINT_NAME = 'FKjys8o2lrdx4qmpus6elgejxj2' AND CONSTRAINT_TYPE = 'FOREIGN KEY';
SET @s = IF(@n > 0, 'ALTER TABLE historial_clinico DROP FOREIGN KEY FKjys8o2lrdx4qmpus6elgejxj2', 'SELECT 1');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

-- ============================================================
-- 2. Migrar PKs y columnas FK a BIGINT (no-op si ya son BIGINT)
-- ============================================================

ALTER TABLE users             MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE pacientes         MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE tratamientos      MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE citas             MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE historial_clinico MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE pagos             MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;

ALTER TABLE pacientes         MODIFY COLUMN user_id       BIGINT;
ALTER TABLE citas             MODIFY COLUMN paciente_id   BIGINT NOT NULL;
ALTER TABLE historial_clinico MODIFY COLUMN paciente_id   BIGINT NOT NULL,
                              MODIFY COLUMN cita_id       BIGINT,
                              MODIFY COLUMN tratamiento_id BIGINT;
ALTER TABLE pagos             MODIFY COLUMN paciente_id   BIGINT NOT NULL;

-- ============================================================
-- 3. Recrear FK constraints con nombres canónicos (solo si no existen)
-- ============================================================

SELECT COUNT(*) INTO @n FROM information_schema.TABLE_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'pacientes'
  AND CONSTRAINT_NAME = 'fk_pacientes_user' AND CONSTRAINT_TYPE = 'FOREIGN KEY';
SET @s = IF(@n = 0, 'ALTER TABLE pacientes ADD CONSTRAINT fk_pacientes_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT', 'SELECT 1');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

SELECT COUNT(*) INTO @n FROM information_schema.TABLE_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'citas'
  AND CONSTRAINT_NAME = 'fk_citas_paciente' AND CONSTRAINT_TYPE = 'FOREIGN KEY';
SET @s = IF(@n = 0, 'ALTER TABLE citas ADD CONSTRAINT fk_citas_paciente FOREIGN KEY (paciente_id) REFERENCES pacientes (id) ON DELETE RESTRICT', 'SELECT 1');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

SELECT COUNT(*) INTO @n FROM information_schema.TABLE_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'historial_clinico'
  AND CONSTRAINT_NAME = 'fk_historial_paciente' AND CONSTRAINT_TYPE = 'FOREIGN KEY';
SET @s = IF(@n = 0, 'ALTER TABLE historial_clinico ADD CONSTRAINT fk_historial_paciente FOREIGN KEY (paciente_id) REFERENCES pacientes (id) ON DELETE RESTRICT', 'SELECT 1');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

SELECT COUNT(*) INTO @n FROM information_schema.TABLE_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'historial_clinico'
  AND CONSTRAINT_NAME = 'fk_historial_cita' AND CONSTRAINT_TYPE = 'FOREIGN KEY';
SET @s = IF(@n = 0, 'ALTER TABLE historial_clinico ADD CONSTRAINT fk_historial_cita FOREIGN KEY (cita_id) REFERENCES citas (id) ON DELETE RESTRICT', 'SELECT 1');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

SELECT COUNT(*) INTO @n FROM information_schema.TABLE_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'historial_clinico'
  AND CONSTRAINT_NAME = 'fk_historial_tratamiento' AND CONSTRAINT_TYPE = 'FOREIGN KEY';
SET @s = IF(@n = 0, 'ALTER TABLE historial_clinico ADD CONSTRAINT fk_historial_tratamiento FOREIGN KEY (tratamiento_id) REFERENCES tratamientos (id) ON DELETE RESTRICT', 'SELECT 1');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

SELECT COUNT(*) INTO @n FROM information_schema.TABLE_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'pagos'
  AND CONSTRAINT_NAME = 'fk_pagos_paciente' AND CONSTRAINT_TYPE = 'FOREIGN KEY';
SET @s = IF(@n = 0, 'ALTER TABLE pagos ADD CONSTRAINT fk_pagos_paciente FOREIGN KEY (paciente_id) REFERENCES pacientes (id) ON DELETE RESTRICT', 'SELECT 1');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

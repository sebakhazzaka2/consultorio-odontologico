-- V10: email de paciente opcional + único, y origen IMPORT para el importador CSV (F1).
--
-- PRERREQUISITO en instancias con datos: no puede haber emails duplicados después de
-- normalizar (LOWER + TRIM). Si los hay, el ADD CONSTRAINT falla y la migración queda
-- marcada como fallida. Chequear ANTES de deployar:
--   SELECT LOWER(TRIM(email)) e, COUNT(*) FROM pacientes GROUP BY e HAVING COUNT(*) > 1;
-- (verificado vacío en NeoDental el 2026-10-16).
--
-- Orden:
--   1. email pasa a NULL-able (pacientes importados o cargados sin email).
--   2. se normalizan los emails existentes: minúsculas, sin espacios; '' pasa a NULL
--      (la app normaliza igual con EmailNormalizer antes de buscar/guardar).
--   3. constraint UNIQUE con nombre fijo: GlobalExceptionHandler lo reconoce por nombre
--      para devolver 409 "El email ya está registrado". En MySQL un índice UNIQUE admite
--      múltiples NULL, así que varios pacientes sin email conviven sin problema.
--   4. creado_por suma IMPORT (agregar un valor al final del ENUM no reescribe filas).
--
-- MySQL hace commit implícito en cada DDL: si falla el paso 3, los pasos 1 y 2 ya
-- quedaron aplicados (inocuos para la versión anterior de la app).
-- Rollback real = restaurar backup.

ALTER TABLE pacientes MODIFY email VARCHAR(255) NULL;

UPDATE pacientes SET email = NULLIF(LOWER(TRIM(email)), '');

ALTER TABLE pacientes ADD CONSTRAINT uk_pacientes_email UNIQUE (email);

ALTER TABLE pacientes MODIFY creado_por ENUM('ADMIN', 'AGENDA', 'IMPORT') NOT NULL DEFAULT 'ADMIN';

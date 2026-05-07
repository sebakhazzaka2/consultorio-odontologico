-- V6: Multi-rubro — renombrar tratamientos→servicios e historial_clinico→historial_procedimientos
-- Los labels visibles (Historial clínico vs Procedimientos) se configuran por env var, no en DB.

-- 1. Soltar FK que une historial_clinico con tratamientos (necesario antes de renombrar la tabla referenciada)
ALTER TABLE historial_clinico
    DROP FOREIGN KEY fk_historial_tratamiento;

-- 2. Renombrar tablas
RENAME TABLE tratamientos TO servicios;
RENAME TABLE historial_clinico TO historial_procedimientos;

-- 3. Renombrar columna tratamiento_id → servicio_id
ALTER TABLE historial_procedimientos
    CHANGE tratamiento_id servicio_id BIGINT;

-- 4. Re-crear FK con nombre actualizado
ALTER TABLE historial_procedimientos
    ADD CONSTRAINT fk_historial_procedimientos_servicio
        FOREIGN KEY (servicio_id) REFERENCES servicios (id) ON DELETE RESTRICT;

-- No-op: V1__baseline_schema.sql ya incluye `foto_url` en `tratamientos`.
-- El baseline se tomó como snapshot de la instancia viva después de que esta
-- migración ya se había aplicado ahí, así que quedó duplicada en el baseline.
-- En cualquier DB nueva (disaster recovery, cliente nuevo, tests), ejecutar
-- el ADD COLUMN original fallaba con "Duplicate column name 'foto_url'".
-- Se deja el archivo (no se borra) para no romper el checksum de Flyway en
-- instancias que ya tienen V5 registrada en flyway_schema_history.
SELECT 1;

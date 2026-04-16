-- Hibernate 6 valida que @Enumerated(EnumType.STRING) use columna ENUM, no VARCHAR.
-- V1 creó estado como VARCHAR(255); esta migración lo convierte al tipo correcto.
ALTER TABLE citas
    MODIFY COLUMN estado ENUM('pendiente','confirmada','cancelada','completada') NOT NULL;

-- ============================================================
-- Migración: Consultorio Odontológico
-- Tablas: pacientes, citas (con FK a pacientes)
-- Ejecutar en MySQL: mysql -u root -p consultorio_db < migracion-pacientes.sql
-- O copiar y pegar en MySQL Workbench / línea de comandos
-- ============================================================

CREATE DATABASE IF NOT EXISTS consultorio_db;
USE consultorio_db;

-- ------------------------------------------------------------
-- Tabla: pacientes
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS pacientes (
  id INT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(100) NOT NULL,
  apellido VARCHAR(100) NOT NULL,
  telefono VARCHAR(20) NOT NULL,
  email VARCHAR(100) NOT NULL,
  fecha_nacimiento DATE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_apellido (apellido),
  UNIQUE INDEX idx_email (email)
);

-- ------------------------------------------------------------
-- Tabla: citas (depende de pacientes)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS citas (
  id INT AUTO_INCREMENT PRIMARY KEY,
  paciente_id INT NOT NULL,
  fecha DATE NOT NULL,
  hora VARCHAR(10) NOT NULL,
  motivo TEXT,
  estado VARCHAR(20) DEFAULT 'pendiente',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (paciente_id) REFERENCES pacientes(id) ON DELETE RESTRICT,
  UNIQUE INDEX idx_fecha_hora (fecha, hora),
  INDEX idx_fecha (fecha),
  INDEX idx_paciente_id (paciente_id)
);

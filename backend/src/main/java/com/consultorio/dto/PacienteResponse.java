package com.consultorio.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;
import java.time.LocalDate;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PacienteResponse(
    Long id,
    String nombre,
    String apellido,
    String telefono,
    String email,
    LocalDate fechaNacimiento,
    Instant createdAt) {}

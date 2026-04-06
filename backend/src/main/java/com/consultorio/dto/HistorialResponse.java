package com.consultorio.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;
import java.time.LocalDateTime;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record HistorialResponse(
    Long id,
    Long pacienteId,
    String nombrePaciente,
    String apellidoPaciente,
    Long citaId,
    LocalDateTime fechaHora,
    String procedimiento,
    String notas,
    Instant createdAt) {}

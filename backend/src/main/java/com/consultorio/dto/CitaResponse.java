package com.consultorio.dto;

import com.consultorio.model.CitaEstado;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;
import java.time.LocalDateTime;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CitaResponse(
    Long id,
    Long pacienteId,
    String nombrePaciente,
    String apellidoPaciente,
    LocalDateTime fechaHoraInicio,
    Integer duracionMinutos,
    CitaEstado estado,
    String motivo,
    String notas,
    Instant createdAt) {}

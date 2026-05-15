package com.consultorio.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.LocalDateTime;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PublicReservaResponse(
    Long citaId,
    String nombrePaciente,
    String apellidoPaciente,
    String servicio,
    LocalDateTime fechaHoraInicio,
    Integer duracionMinutos,
    String estado,
    String mensaje) {}

package com.consultorio.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PagoResponse(
    Long id,
    Long pacienteId,
    String nombrePaciente,
    String apellidoPaciente,
    BigDecimal monto,
    LocalDate fecha,
    String concepto,
    Instant createdAt) {}

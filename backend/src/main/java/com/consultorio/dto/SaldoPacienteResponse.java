package com.consultorio.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.math.BigDecimal;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record SaldoPacienteResponse(
    Long pacienteId,
    String nombrePaciente,
    String apellidoPaciente,
    BigDecimal totalDeuda,
    BigDecimal totalPagado,
    BigDecimal saldoPendiente) {}

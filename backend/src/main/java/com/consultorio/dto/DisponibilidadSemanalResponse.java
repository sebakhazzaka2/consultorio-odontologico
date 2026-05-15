package com.consultorio.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.LocalTime;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record DisponibilidadSemanalResponse(
    Long id,
    Integer diaSemana,
    Boolean activo,
    LocalTime horaApertura,
    LocalTime horaCierre,
    LocalTime pausaInicio,
    LocalTime pausaFin) {}

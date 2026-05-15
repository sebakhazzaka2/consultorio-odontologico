package com.consultorio.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.math.BigDecimal;
import java.time.Instant;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ServicioResponse(
    Long id,
    String nombre,
    String descripcion,
    BigDecimal precio,
    Boolean activo,
    String fotoUrl,
    Integer duracionMinutos,
    Instant createdAt) {}

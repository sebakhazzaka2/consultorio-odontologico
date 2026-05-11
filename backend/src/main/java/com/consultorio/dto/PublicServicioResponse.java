package com.consultorio.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.math.BigDecimal;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PublicServicioResponse(
    Long id,
    String nombre,
    String descripcion,
    BigDecimal precio,
    String fotoUrl) {}

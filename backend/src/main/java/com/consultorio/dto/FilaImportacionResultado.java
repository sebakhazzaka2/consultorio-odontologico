package com.consultorio.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * Una fila del reporte de importación. {@code fila} es el número de fila de datos,
 * 1-based y sin contar el encabezado (fila 1 = primera línea después del encabezado).
 * El motivo nunca repite datos personales (nombre, teléfono, email).
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record FilaImportacionResultado(int fila, EstadoFilaImportacion estado, String motivo) {}

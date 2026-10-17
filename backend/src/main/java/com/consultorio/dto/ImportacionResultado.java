package com.consultorio.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

/**
 * Reporte de una importación de pacientes (simulación o confirmación).
 * {@code insertados} es 0 en la simulación.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ImportacionResultado(
    int total,
    int ok,
    int errores,
    int revisar,
    int duplicados,
    int yaExisten,
    int insertados,
    boolean confirmado,
    List<FilaImportacionResultado> filas) {}

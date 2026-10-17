package com.consultorio.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * Estado de la suscripción a recordatorios para la pantalla pública {@code /baja}.
 * {@code estado}: "ACTIVO" (recibe recordatorios) o "BAJA". Sin datos del paciente.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record BajaRecordatoriosResponse(String clinica, String estado) {

  public static final String ACTIVO = "ACTIVO";
  public static final String BAJA = "BAJA";
}

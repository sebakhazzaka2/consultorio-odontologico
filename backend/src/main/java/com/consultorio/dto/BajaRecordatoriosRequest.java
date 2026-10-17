package com.consultorio.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * Body de {@code POST /public/notificaciones/baja}. Sin {@code @NotBlank} a propósito: el
 * contrato con el frontend es 400 para token faltante o inválido (Bean Validation daría 422).
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BajaRecordatoriosRequest {

  private String token;

  public BajaRecordatoriosRequest() {}

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }
}

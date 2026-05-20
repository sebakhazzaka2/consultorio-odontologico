package com.consultorio.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Min;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ConfirmarCitaRequest {

  @Min(5)
  private Integer duracionMinutos;

  public ConfirmarCitaRequest() {}

  public Integer getDuracionMinutos() { return duracionMinutos; }
  public void setDuracionMinutos(Integer duracionMinutos) { this.duracionMinutos = duracionMinutos; }
}

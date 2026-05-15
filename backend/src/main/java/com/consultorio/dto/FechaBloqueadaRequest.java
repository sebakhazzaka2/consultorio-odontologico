package com.consultorio.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class FechaBloqueadaRequest {

  @NotNull
  private LocalDate fecha;

  private String motivo;

  public FechaBloqueadaRequest() {}

  public LocalDate getFecha() { return fecha; }
  public void setFecha(LocalDate fecha) { this.fecha = fecha; }

  public String getMotivo() { return motivo; }
  public void setMotivo(String motivo) { this.motivo = motivo; }
}

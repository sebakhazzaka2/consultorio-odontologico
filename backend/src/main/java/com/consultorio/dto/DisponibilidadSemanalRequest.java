package com.consultorio.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class DisponibilidadSemanalRequest {

  @NotNull
  @Min(0)
  @Max(6)
  private Integer diaSemana;

  @NotNull
  private Boolean activo;

  @NotNull
  private LocalTime horaApertura;

  @NotNull
  private LocalTime horaCierre;

  private LocalTime pausaInicio;

  private LocalTime pausaFin;

  public DisponibilidadSemanalRequest() {}

  public Integer getDiaSemana() { return diaSemana; }
  public void setDiaSemana(Integer diaSemana) { this.diaSemana = diaSemana; }

  public Boolean getActivo() { return activo; }
  public void setActivo(Boolean activo) { this.activo = activo; }

  public LocalTime getHoraApertura() { return horaApertura; }
  public void setHoraApertura(LocalTime horaApertura) { this.horaApertura = horaApertura; }

  public LocalTime getHoraCierre() { return horaCierre; }
  public void setHoraCierre(LocalTime horaCierre) { this.horaCierre = horaCierre; }

  public LocalTime getPausaInicio() { return pausaInicio; }
  public void setPausaInicio(LocalTime pausaInicio) { this.pausaInicio = pausaInicio; }

  public LocalTime getPausaFin() { return pausaFin; }
  public void setPausaFin(LocalTime pausaFin) { this.pausaFin = pausaFin; }
}

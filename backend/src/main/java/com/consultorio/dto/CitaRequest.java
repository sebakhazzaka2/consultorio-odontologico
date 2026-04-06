package com.consultorio.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CitaRequest {

  @NotNull
  private Long pacienteId;

  @NotNull
  @Future
  private LocalDateTime fechaHoraInicio;

  @NotNull
  @Min(15)
  @Max(180)
  private Integer duracionMinutos;

  @NotBlank
  private String motivo;

  private String notas;

  public CitaRequest() {}

  public Long getPacienteId() {
    return pacienteId;
  }

  public void setPacienteId(Long pacienteId) {
    this.pacienteId = pacienteId;
  }

  public LocalDateTime getFechaHoraInicio() {
    return fechaHoraInicio;
  }

  public void setFechaHoraInicio(LocalDateTime fechaHoraInicio) {
    this.fechaHoraInicio = fechaHoraInicio;
  }

  public Integer getDuracionMinutos() {
    return duracionMinutos;
  }

  public void setDuracionMinutos(Integer duracionMinutos) {
    this.duracionMinutos = duracionMinutos;
  }

  public String getMotivo() {
    return motivo;
  }

  public void setMotivo(String motivo) {
    this.motivo = motivo;
  }

  public String getNotas() {
    return notas;
  }

  public void setNotas(String notas) {
    this.notas = notas;
  }
}

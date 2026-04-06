package com.consultorio.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class HistorialRequest {

  @NotNull
  private Long pacienteId;

  private Long citaId;

  @NotNull
  private LocalDateTime fechaHora;

  @NotBlank
  private String procedimiento;

  private String notas;

  private Long tratamientoId;

  private String fotoUrl;

  public HistorialRequest() {}

  public Long getPacienteId() {
    return pacienteId;
  }

  public void setPacienteId(Long pacienteId) {
    this.pacienteId = pacienteId;
  }

  public Long getCitaId() {
    return citaId;
  }

  public void setCitaId(Long citaId) {
    this.citaId = citaId;
  }

  public LocalDateTime getFechaHora() {
    return fechaHora;
  }

  public void setFechaHora(LocalDateTime fechaHora) {
    this.fechaHora = fechaHora;
  }

  public String getProcedimiento() {
    return procedimiento;
  }

  public void setProcedimiento(String procedimiento) {
    this.procedimiento = procedimiento;
  }

  public String getNotas() {
    return notas;
  }

  public void setNotas(String notas) {
    this.notas = notas;
  }

  public Long getTratamientoId() {
    return tratamientoId;
  }

  public void setTratamientoId(Long tratamientoId) {
    this.tratamientoId = tratamientoId;
  }

  public String getFotoUrl() {
    return fotoUrl;
  }

  public void setFotoUrl(String fotoUrl) {
    this.fotoUrl = fotoUrl;
  }
}

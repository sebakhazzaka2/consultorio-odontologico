package com.consultorio.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PagoRequest {

  @NotNull
  private Long pacienteId;

  @NotNull
  @DecimalMin("0.01")
  private BigDecimal monto;

  @NotNull
  private LocalDate fecha;

  private String concepto;

  public PagoRequest() {}

  public Long getPacienteId() {
    return pacienteId;
  }

  public void setPacienteId(Long pacienteId) {
    this.pacienteId = pacienteId;
  }

  public BigDecimal getMonto() {
    return monto;
  }

  public void setMonto(BigDecimal monto) {
    this.monto = monto;
  }

  public LocalDate getFecha() {
    return fecha;
  }

  public void setFecha(LocalDate fecha) {
    this.fecha = fecha;
  }

  public String getConcepto() {
    return concepto;
  }

  public void setConcepto(String concepto) {
    this.concepto = concepto;
  }
}

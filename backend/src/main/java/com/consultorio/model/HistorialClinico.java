package com.consultorio.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "historial_clinico")
public class HistorialClinico {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "paciente_id", nullable = false)
  private Paciente paciente;

  @ManyToOne(optional = true)
  @JoinColumn(name = "cita_id", nullable = true)
  private Cita cita;

  @Column(name = "fecha_hora", nullable = false)
  private LocalDateTime fechaHora;

  @Column(nullable = false, length = 500)
  private String procedimiento;

  @Column(nullable = true, length = 2000)
  private String notas;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private Instant createdAt;

  public HistorialClinico() {}

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Paciente getPaciente() {
    return paciente;
  }

  public void setPaciente(Paciente paciente) {
    this.paciente = paciente;
  }

  public Cita getCita() {
    return cita;
  }

  public void setCita(Cita cita) {
    this.cita = cita;
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

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}

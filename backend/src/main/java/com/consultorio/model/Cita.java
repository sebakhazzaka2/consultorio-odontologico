package com.consultorio.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "citas")
public class Cita {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "paciente_id", nullable = false)
  private Paciente paciente;

  @Column(name = "fecha_hora_inicio", nullable = false)
  private LocalDateTime fechaHoraInicio;

  @Column(name = "duracion_minutos", nullable = false)
  private Integer duracionMinutos;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private CitaEstado estado;

  @Column(nullable = true, length = 500)
  private String motivo;

  @Column(nullable = true, length = 1000)
  private String notas;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private Instant createdAt;

  public Cita() {}

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

  public CitaEstado getEstado() {
    return estado;
  }

  public void setEstado(CitaEstado estado) {
    this.estado = estado;
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

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}

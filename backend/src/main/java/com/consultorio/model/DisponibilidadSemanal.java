package com.consultorio.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "disponibilidad_semanal")
public class DisponibilidadSemanal {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // 0=Domingo, 1=Lunes, ..., 6=Sábado (java.time.DayOfWeek usa 1=Lunes, 7=Domingo — mapeamos explícitamente)
  @Column(name = "dia_semana", nullable = false, unique = true)
  private Integer diaSemana;

  @Column(nullable = false)
  private Boolean activo;

  @Column(name = "hora_apertura", nullable = false)
  private LocalTime horaApertura;

  @Column(name = "hora_cierre", nullable = false)
  private LocalTime horaCierre;

  @Column(name = "pausa_inicio", nullable = true)
  private LocalTime pausaInicio;

  @Column(name = "pausa_fin", nullable = true)
  private LocalTime pausaFin;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private Instant updatedAt;

  public DisponibilidadSemanal() {}

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }

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

  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

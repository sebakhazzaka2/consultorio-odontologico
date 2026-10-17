package com.consultorio.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/**
 * Fila del outbox de notificaciones (V11). No guarda destinatario ni contenido: se
 * resuelven al enviar. {@code citaId} es un id plano (sin relación JPA) porque la cita
 * puede borrarse (FK ON DELETE SET NULL) y el despachador la vuelve a leer al enviar.
 */
@Entity
@Table(
    name = "notificaciones",
    uniqueConstraints =
        @UniqueConstraint(name = "uk_notificaciones_clave_dedupe", columnNames = "clave_dedupe"),
    indexes =
        @Index(name = "idx_notificaciones_estado_proximo", columnList = "estado, proximo_intento_at"))
public class Notificacion {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TipoNotificacion tipo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private CanalNotificacion canal;

  @Column(name = "clave_dedupe", nullable = false, length = 150)
  private String claveDedupe;

  @Column(name = "cita_id")
  private Long citaId;

  @Column(name = "referencia_id")
  private Long referenciaId;

  @Enumerated(EnumType.STRING)
  @Column(name = "destinatario_tipo", nullable = false)
  private DestinatarioTipo destinatarioTipo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private EstadoNotificacion estado;

  @Column(nullable = false)
  private int intentos;

  @Column(name = "proximo_intento_at", nullable = false)
  private Instant proximoIntentoAt;

  @Column(name = "ultimo_error", length = 500)
  private String ultimoError;

  @Column(name = "enviada_at")
  private Instant enviadaAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  public Notificacion() {}

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }

  public TipoNotificacion getTipo() { return tipo; }
  public void setTipo(TipoNotificacion tipo) { this.tipo = tipo; }

  public CanalNotificacion getCanal() { return canal; }
  public void setCanal(CanalNotificacion canal) { this.canal = canal; }

  public String getClaveDedupe() { return claveDedupe; }
  public void setClaveDedupe(String claveDedupe) { this.claveDedupe = claveDedupe; }

  public Long getCitaId() { return citaId; }
  public void setCitaId(Long citaId) { this.citaId = citaId; }

  public Long getReferenciaId() { return referenciaId; }
  public void setReferenciaId(Long referenciaId) { this.referenciaId = referenciaId; }

  public DestinatarioTipo getDestinatarioTipo() { return destinatarioTipo; }
  public void setDestinatarioTipo(DestinatarioTipo destinatarioTipo) { this.destinatarioTipo = destinatarioTipo; }

  public EstadoNotificacion getEstado() { return estado; }
  public void setEstado(EstadoNotificacion estado) { this.estado = estado; }

  public int getIntentos() { return intentos; }
  public void setIntentos(int intentos) { this.intentos = intentos; }

  public Instant getProximoIntentoAt() { return proximoIntentoAt; }
  public void setProximoIntentoAt(Instant proximoIntentoAt) { this.proximoIntentoAt = proximoIntentoAt; }

  public String getUltimoError() { return ultimoError; }
  public void setUltimoError(String ultimoError) { this.ultimoError = ultimoError; }

  public Instant getEnviadaAt() { return enviadaAt; }
  public void setEnviadaAt(Instant enviadaAt) { this.enviadaAt = enviadaAt; }

  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

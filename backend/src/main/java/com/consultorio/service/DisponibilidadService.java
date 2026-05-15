package com.consultorio.service;

import com.consultorio.dto.DisponibilidadSemanalRequest;
import com.consultorio.dto.DisponibilidadSemanalResponse;
import com.consultorio.dto.FechaBloqueadaRequest;
import com.consultorio.dto.FechaBloqueadaResponse;
import com.consultorio.exception.ResourceNotFoundException;
import com.consultorio.model.DisponibilidadSemanal;
import com.consultorio.model.FechaBloqueada;
import com.consultorio.repository.DisponibilidadSemanalRepository;
import com.consultorio.repository.FechaBloqueadaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class DisponibilidadService {

  private static final Logger log = LoggerFactory.getLogger(DisponibilidadService.class);

  private final DisponibilidadSemanalRepository semanalRepo;
  private final FechaBloqueadaRepository bloqueadaRepo;

  public DisponibilidadService(DisponibilidadSemanalRepository semanalRepo,
      FechaBloqueadaRepository bloqueadaRepo) {
    this.semanalRepo = semanalRepo;
    this.bloqueadaRepo = bloqueadaRepo;
  }

  // ── Disponibilidad semanal ────────────────────────────────────────────────

  public List<DisponibilidadSemanalResponse> findAllSemanal() {
    return semanalRepo.findAll().stream().map(this::toSemanalResponse).toList();
  }

  public DisponibilidadSemanalResponse upsertDia(DisponibilidadSemanalRequest request) {
    Optional<DisponibilidadSemanal> existente = semanalRepo.findByDiaSemana(request.getDiaSemana());
    DisponibilidadSemanal entidad = existente.orElse(new DisponibilidadSemanal());
    entidad.setDiaSemana(request.getDiaSemana());
    entidad.setActivo(request.getActivo());
    entidad.setHoraApertura(request.getHoraApertura());
    entidad.setHoraCierre(request.getHoraCierre());
    entidad.setPausaInicio(request.getPausaInicio());
    entidad.setPausaFin(request.getPausaFin());
    DisponibilidadSemanal guardado = semanalRepo.save(entidad);
    log.info("Disponibilidad día {} actualizada — activo: {}, {}–{}", guardado.getDiaSemana(),
        guardado.getActivo(), guardado.getHoraApertura(), guardado.getHoraCierre());
    return toSemanalResponse(guardado);
  }

  public Optional<DisponibilidadSemanal> findByDiaSemana(int diaSemana) {
    return semanalRepo.findByDiaSemana(diaSemana);
  }

  // ── Fechas bloqueadas ─────────────────────────────────────────────────────

  public List<FechaBloqueadaResponse> findAllBloqueadas() {
    return bloqueadaRepo.findByFechaGreaterThanEqualOrderByFechaAsc(LocalDate.now())
        .stream().map(this::toBloqueadaResponse).toList();
  }

  @Scheduled(cron = "0 0 3 * * *")
  public void limpiarFechasPasadas() {
    bloqueadaRepo.eliminarPasadas(LocalDate.now());
    log.info("Limpieza de fechas bloqueadas pasadas completada");
  }

  public FechaBloqueadaResponse bloquearFecha(FechaBloqueadaRequest request) {
    if (bloqueadaRepo.existsByFecha(request.getFecha())) {
      throw new IllegalArgumentException("La fecha " + request.getFecha() + " ya está bloqueada");
    }
    FechaBloqueada entidad = new FechaBloqueada();
    entidad.setFecha(request.getFecha());
    entidad.setMotivo(request.getMotivo());
    FechaBloqueada guardado = bloqueadaRepo.save(entidad);
    log.info("Fecha bloqueada: {} — motivo: {}", guardado.getFecha(), guardado.getMotivo());
    return toBloqueadaResponse(guardado);
  }

  public void desbloquearFecha(Long id) {
    bloqueadaRepo.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Fecha bloqueada no encontrada con id: " + id));
    bloqueadaRepo.deleteById(id);
    log.info("Fecha desbloqueada — id: {}", id);
  }

  public boolean esFechaBloqueada(LocalDate fecha) {
    return bloqueadaRepo.existsByFecha(fecha);
  }

  // ── Mappers ───────────────────────────────────────────────────────────────

  private DisponibilidadSemanalResponse toSemanalResponse(DisponibilidadSemanal e) {
    return new DisponibilidadSemanalResponse(
        e.getId(), e.getDiaSemana(), e.getActivo(),
        e.getHoraApertura(), e.getHoraCierre(),
        e.getPausaInicio(), e.getPausaFin());
  }

  private FechaBloqueadaResponse toBloqueadaResponse(FechaBloqueada e) {
    return new FechaBloqueadaResponse(e.getId(), e.getFecha(), e.getMotivo(), e.getCreatedAt());
  }
}

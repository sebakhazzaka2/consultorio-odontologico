package com.consultorio.service;

import com.consultorio.dto.CitaRequest;
import com.consultorio.dto.CitaResponse;
import com.consultorio.exception.ResourceNotFoundException;
import com.consultorio.model.Cita;
import com.consultorio.model.CitaEstado;
import com.consultorio.model.DisponibilidadSemanal;
import com.consultorio.model.Paciente;
import com.consultorio.repository.CitaRepository;
import com.consultorio.repository.PacienteRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CitaService {

  private static final Logger log = LoggerFactory.getLogger(CitaService.class);

  private final CitaRepository citaRepository;
  private final PacienteRepository pacienteRepository;
  private final DisponibilidadService disponibilidadService;

  public CitaService(CitaRepository citaRepository, PacienteRepository pacienteRepository,
      DisponibilidadService disponibilidadService) {
    this.citaRepository = citaRepository;
    this.pacienteRepository = pacienteRepository;
    this.disponibilidadService = disponibilidadService;
  }

  public List<CitaResponse> findAll() {
    return citaRepository.findAll().stream().map(this::toResponse).toList();
  }

  public CitaResponse findById(Long id) {
    Cita cita =
        citaRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada con id: " + id));
    return toResponse(cita);
  }

  @Transactional(isolation = Isolation.REPEATABLE_READ)
  public CitaResponse create(CitaRequest request) {
    lockDiaParaReserva(request.getFechaHoraInicio().toLocalDate());

    Long pacienteId = request.getPacienteId();
    Paciente paciente =
        pacienteRepository
            .findById(pacienteId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Paciente no encontrado con id: " + pacienteId));

    validarMultiploDe15(request.getFechaHoraInicio());

    LocalDateTime fechaHoraFin =
        calcularFin(request.getFechaHoraInicio(), request.getDuracionMinutos());

    if (haySolapamiento(request.getFechaHoraInicio(), fechaHoraFin, null)) {
      log.warn("Solapamiento de cita detectado — inicio: {}, fin: {}", request.getFechaHoraInicio(), fechaHoraFin);
      throw new IllegalArgumentException("El horario se solapa con una cita existente");
    }

    Cita cita = new Cita();
    cita.setPaciente(paciente);
    cita.setFechaHoraInicio(request.getFechaHoraInicio());
    cita.setDuracionMinutos(request.getDuracionMinutos());
    cita.setEstado(CitaEstado.CONFIRMADA);
    cita.setMotivo(request.getMotivo());
    cita.setNotas(request.getNotas());

    Cita creada = citaRepository.save(cita);
    log.info("Cita creada — id: {}, paciente: {}, inicio: {}, duración: {} min",
        creada.getId(), pacienteId, creada.getFechaHoraInicio(), creada.getDuracionMinutos());
    return toResponse(creada);
  }

  @Transactional(isolation = Isolation.REPEATABLE_READ)
  public CitaResponse update(Long id, CitaRequest request) {
    lockDiaParaReserva(request.getFechaHoraInicio().toLocalDate());

    Cita existente =
        citaRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada con id: " + id));

    Long pacienteId = request.getPacienteId();
    Paciente paciente =
        pacienteRepository
            .findById(pacienteId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Paciente no encontrado con id: " + pacienteId));

    validarMultiploDe15(request.getFechaHoraInicio());

    LocalDateTime fechaHoraFin =
        calcularFin(request.getFechaHoraInicio(), request.getDuracionMinutos());

    if (haySolapamiento(request.getFechaHoraInicio(), fechaHoraFin, id)) {
      log.warn("Solapamiento de cita detectado al actualizar id {} — inicio: {}, fin: {}", id, request.getFechaHoraInicio(), fechaHoraFin);
      throw new IllegalArgumentException("El horario se solapa con una cita existente");
    }

    existente.setPaciente(paciente);
    existente.setFechaHoraInicio(request.getFechaHoraInicio());
    existente.setDuracionMinutos(request.getDuracionMinutos());
    existente.setMotivo(request.getMotivo());
    existente.setNotas(request.getNotas());

    Cita actualizada = citaRepository.save(existente);
    log.info("Cita actualizada — id: {}, paciente: {}, nuevo inicio: {}", actualizada.getId(), pacienteId, actualizada.getFechaHoraInicio());
    return toResponse(actualizada);
  }

  public CitaResponse cancelar(Long id) {
    Cita cita =
        citaRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada con id: " + id));

    cita.setEstado(CitaEstado.CANCELADA);
    Cita cancelada = citaRepository.save(cita);
    log.info("Cita cancelada — id: {}", cancelada.getId());
    return toResponse(cancelada);
  }

  @Transactional(isolation = Isolation.REPEATABLE_READ)
  public CitaResponse confirmar(Long id, Integer duracionMinutosOverride) {
    // findByIdForUpdate, no findById: hace falta leer la cita para saber qué día
    // lockear, pero una lectura NORMAL acá fijaría el snapshot REPEATABLE READ de la
    // transacción ANTES del lock del día (misma clase de bug que en reservar(), ver
    // el comentario ahí). findByIdForUpdate usa FOR UPDATE, que no fija ese snapshot.
    Cita cita =
        citaRepository
            .findByIdForUpdate(id)
            .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada con id: " + id));

    lockDiaParaReserva(cita.getFechaHoraInicio().toLocalDate());

    if (duracionMinutosOverride != null) {
      cita.setDuracionMinutos(duracionMinutosOverride);
    }

    LocalDateTime fechaHoraFin = calcularFin(cita.getFechaHoraInicio(), cita.getDuracionMinutos());
    if (haySolapamiento(cita.getFechaHoraInicio(), fechaHoraFin, cita.getId())) {
      log.warn("Solapamiento de cita detectado al confirmar id {} — inicio: {}, fin: {}",
          id, cita.getFechaHoraInicio(), fechaHoraFin);
      throw new IllegalArgumentException("El horario se solapa con una cita existente");
    }

    cita.setEstado(CitaEstado.CONFIRMADA);
    Cita confirmada = citaRepository.save(cita);
    log.info("Cita confirmada — id: {}, duración: {} min", confirmada.getId(), confirmada.getDuracionMinutos());
    return toResponse(confirmada);
  }

  public List<String> getDisponibilidad(LocalDate fecha, int duracionMinutos) {
    // Día bloqueado (feriado, vacación)
    if (disponibilidadService.esFechaBloqueada(fecha)) {
      log.info("Disponibilidad — fecha {} bloqueada, 0 slots", fecha);
      return List.of();
    }

    // Día de semana: DayOfWeek usa 1=Lunes..7=Domingo; DB usa 0=Domingo..6=Sábado
    int diaSemana = fecha.getDayOfWeek().getValue() % 7;
    Optional<DisponibilidadSemanal> configOpt = disponibilidadService.findByDiaSemana(diaSemana);

    if (configOpt.isEmpty() || !configOpt.get().getActivo()) {
      log.info("Disponibilidad — día {} no laborable, 0 slots", diaSemana);
      return List.of();
    }

    DisponibilidadSemanal config = configOpt.get();
    LocalTime apertura = config.getHoraApertura();
    // El último slot válido debe terminar antes o en hora de cierre
    LocalTime limiteInicio = config.getHoraCierre().minusMinutes(duracionMinutos);

    LocalDateTime inicioDia = fecha.atStartOfDay();
    LocalDateTime finDia = fecha.atTime(23, 59);
    List<Cita> citasOcupadas = citaRepository.findByFechaHoraInicioBetween(inicioDia, finDia)
        .stream()
        .filter(c -> c.getEstado() == CitaEstado.CONFIRMADA || c.getEstado() == CitaEstado.PENDIENTE)
        .toList();

    List<String> disponibles = new ArrayList<>();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
    LocalTime slotTime = apertura;

    while (!slotTime.isAfter(limiteInicio)) {
      LocalDateTime slot = fecha.atTime(slotTime);
      LocalDateTime slotFin = slot.plusMinutes(duracionMinutos);

      boolean enPausa = config.getPausaInicio() != null && config.getPausaFin() != null
          && slotTime.isBefore(config.getPausaFin())
          && slotFin.toLocalTime().isAfter(config.getPausaInicio());

      boolean solapado = citasOcupadas.stream().anyMatch(cita -> {
        LocalDateTime citaInicio = cita.getFechaHoraInicio();
        LocalDateTime citaFin = calcularFin(citaInicio, cita.getDuracionMinutos());
        return citaInicio.isBefore(slotFin) && citaFin.isAfter(slot);
      });

      if (!enPausa && !solapado) {
        disponibles.add(slotTime.format(formatter));
      }

      slotTime = slotTime.plusMinutes(15);
    }

    log.info("Disponibilidad — fecha: {}, duración: {} min, slots disponibles: {}",
        fecha, duracionMinutos, disponibles.size());
    return disponibles;
  }

  public void delete(Long id) {
    findById(id);
    citaRepository.deleteById(id);
    log.info("Cita eliminada — id: {}", id);
  }

  private CitaResponse toResponse(Cita cita) {
    Paciente paciente = cita.getPaciente();
    return new CitaResponse(
        cita.getId(),
        paciente.getId(),
        paciente.getNombre(),
        paciente.getApellido(),
        cita.getFechaHoraInicio(),
        cita.getDuracionMinutos(),
        cita.getEstado(),
        cita.getMotivo(),
        cita.getNotas(),
        cita.getCreatedAt());
  }

  private void validarMultiploDe15(LocalDateTime fechaHora) {
    int minutos = fechaHora.getMinute();
    if (minutos % 15 != 0) {
      throw new IllegalArgumentException(
          "La hora debe ser múltiplo de 15 minutos (ej: 9:00, 9:15, 9:30, 9:45)");
    }
  }

  private LocalDateTime calcularFin(LocalDateTime inicio, Integer duracionMinutos) {
    return inicio.plusMinutes(duracionMinutos.longValue());
  }

  /**
   * Toma el lock pesimista (FOR UPDATE) de la config de disponibilidad del día de
   * semana correspondiente a {@code fecha}, dentro de la transacción actual. Hasta
   * que esa transacción haga commit o rollback, cualquier otra que intente lockear
   * el mismo día de semana queda bloqueada — así se serializan las reservas/
   * confirmaciones concurrentes para el mismo horario, sin la ventana que existía
   * con GET_LOCK + RELEASE_LOCK (que se liberaba antes del commit de la transacción).
   *
   * <p>Debe llamarse desde un método {@code @Transactional}, antes de leer
   * disponibilidad o solapamiento — si se llama después, otra transacción ya pudo
   * haber leído y decidido que el slot estaba libre.
   */
  public void lockDiaParaReserva(LocalDate fecha) {
    int diaSemana = fecha.getDayOfWeek().getValue() % 7;
    disponibilidadService.lockDiaSemana(diaSemana);
  }

  private boolean haySolapamiento(LocalDateTime inicio, LocalDateTime fin, Long excluirCitaId) {
    LocalDateTime ventanaInicio = inicio.minusHours(3);
    // CONFIRMADA y PENDIENTE: debe ser la misma regla que getDisponibilidad() usa para
    // calcular los slots públicos (línea ~169), si no, el admin puede pisar sin darse
    // cuenta un horario ya pedido por una reserva pública todavía sin confirmar.
    List<Cita> candidatas = citaRepository.findByFechaHoraInicioBetweenAndEstadoIn(
    ventanaInicio, fin, List.of(CitaEstado.CONFIRMADA, CitaEstado.PENDIENTE));

    return candidatas.stream()
        .filter(cita -> excluirCitaId == null || !cita.getId().equals(excluirCitaId))
        .anyMatch(cita -> {
            LocalDateTime existenteInicio = cita.getFechaHoraInicio();
            LocalDateTime existenteFin = calcularFin(existenteInicio, cita.getDuracionMinutos());
            return existenteInicio.isBefore(fin) && existenteFin.isAfter(inicio);
        });
  }
}

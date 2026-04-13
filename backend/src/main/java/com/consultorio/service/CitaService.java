package com.consultorio.service;

import com.consultorio.dto.CitaRequest;
import com.consultorio.dto.CitaResponse;
import com.consultorio.exception.ResourceNotFoundException;
import com.consultorio.model.Cita;
import com.consultorio.model.CitaEstado;
import com.consultorio.model.Paciente;
import com.consultorio.repository.CitaRepository;
import com.consultorio.repository.PacienteRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CitaService {

  private static final Logger log = LoggerFactory.getLogger(CitaService.class);

  private final CitaRepository citaRepository;
  private final PacienteRepository pacienteRepository;

  public CitaService(CitaRepository citaRepository, PacienteRepository pacienteRepository) {
    this.citaRepository = citaRepository;
    this.pacienteRepository = pacienteRepository;
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

  public CitaResponse create(CitaRequest request) {
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

  public CitaResponse update(Long id, CitaRequest request) {
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

  public CitaResponse confirmar(Long id) {
    Cita cita =
        citaRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada con id: " + id));

    cita.setEstado(CitaEstado.CONFIRMADA);
    Cita confirmada = citaRepository.save(cita);
    log.info("Cita confirmada — id: {}", confirmada.getId());
    return toResponse(confirmada);
  }

  public List<String> getDisponibilidad(LocalDate fecha, int duracionMinutos) {
    LocalDateTime inicioDia = fecha.atStartOfDay();
    LocalDateTime finDia = fecha.atTime(23, 59);

    List<Cita> citasOcupadas = citaRepository.findByFechaHoraInicioBetween(inicioDia, finDia)
        .stream()
        .filter(c -> c.getEstado() == CitaEstado.CONFIRMADA || c.getEstado() == CitaEstado.PENDIENTE)
        .toList();

    List<String> disponibles = new ArrayList<>();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

    LocalTime slotTime = LocalTime.of(9, 0);
    LocalTime limite = LocalTime.of(17, 45);

    while (!slotTime.isAfter(limite)) {
      LocalDateTime slot = fecha.atTime(slotTime);
      LocalDateTime slotFin = slot.plusMinutes(duracionMinutos);

      boolean solapado = citasOcupadas.stream().anyMatch(cita -> {
        LocalDateTime citaInicio = cita.getFechaHoraInicio();
        LocalDateTime citaFin = calcularFin(citaInicio, cita.getDuracionMinutos());
        return citaInicio.isBefore(slotFin) && citaFin.isAfter(slot);
      });

      if (!solapado) {
        disponibles.add(slotTime.format(formatter));
      }

      slotTime = slotTime.plusMinutes(15);
    }

    log.info("Consulta de disponibilidad — fecha: {}, duración: {} min, slots disponibles: {}",
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

  private boolean haySolapamiento(LocalDateTime inicio, LocalDateTime fin, Long excluirCitaId) {
    LocalDateTime ventanaInicio = inicio.minusHours(3);
    List<Cita> candidatas = citaRepository.findByFechaHoraInicioBetweenAndEstado(
    ventanaInicio, fin, CitaEstado.CONFIRMADA);

    return candidatas.stream()
        .filter(cita -> excluirCitaId == null || !cita.getId().equals(excluirCitaId))
        .anyMatch(cita -> {
            LocalDateTime existenteInicio = cita.getFechaHoraInicio();
            LocalDateTime existenteFin = calcularFin(existenteInicio, cita.getDuracionMinutos());
            return existenteInicio.isBefore(fin) && existenteFin.isAfter(inicio);
        });
  }
}

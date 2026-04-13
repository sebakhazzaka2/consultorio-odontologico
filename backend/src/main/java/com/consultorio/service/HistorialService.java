package com.consultorio.service;

import com.consultorio.dto.HistorialRequest;
import com.consultorio.dto.HistorialResponse;
import com.consultorio.exception.ResourceNotFoundException;
import com.consultorio.model.Cita;
import com.consultorio.model.HistorialClinico;
import com.consultorio.model.Paciente;
import com.consultorio.model.Tratamiento;
import com.consultorio.repository.CitaRepository;
import com.consultorio.repository.HistorialRepository;
import com.consultorio.repository.PacienteRepository;
import com.consultorio.repository.TratamientoRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class HistorialService {

  private static final Logger log = LoggerFactory.getLogger(HistorialService.class);

  private final HistorialRepository historialRepository;
  private final PacienteRepository pacienteRepository;
  private final CitaRepository citaRepository;
  private final TratamientoRepository tratamientoRepository;

  public HistorialService(
      HistorialRepository historialRepository,
      PacienteRepository pacienteRepository,
      CitaRepository citaRepository,
      TratamientoRepository tratamientoRepository) {
    this.historialRepository = historialRepository;
    this.pacienteRepository = pacienteRepository;
    this.citaRepository = citaRepository;
    this.tratamientoRepository = tratamientoRepository;
  }

  public List<HistorialResponse> findAll() {
    return historialRepository.findAll().stream().map(this::toResponse).toList();
  }

  public HistorialResponse findById(Long id) {
    HistorialClinico historial =
        historialRepository
            .findById(id)
            .orElseThrow(
                () -> new ResourceNotFoundException("Registro no encontrado con id: " + id));
    return toResponse(historial);
  }

  public List<HistorialResponse> findByPaciente(Long pacienteId) {
    pacienteRepository
        .findById(pacienteId)
        .orElseThrow(
            () -> new ResourceNotFoundException("Paciente no encontrado con id: " + pacienteId));
    return historialRepository
        .findByPacienteIdOrderByFechaHoraDesc(pacienteId)
        .stream()
        .map(this::toResponse)
        .toList();
  }

  public List<HistorialResponse> findByCita(Long citaId) {
    citaRepository
        .findById(citaId)
        .orElseThrow(
            () -> new ResourceNotFoundException("Cita no encontrada con id: " + citaId));
    return historialRepository
        .findByCitaIdOrderByFechaHoraDesc(citaId)
        .stream()
        .map(this::toResponse)
        .toList();
  }

  public HistorialResponse create(HistorialRequest request) {
    Long pacienteId = request.getPacienteId();
    Paciente paciente =
        pacienteRepository
            .findById(pacienteId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Paciente no encontrado con id: " + pacienteId));

    Cita cita = null;
    if (request.getCitaId() != null) {
      Long citaId = request.getCitaId();
      cita =
          citaRepository
              .findById(citaId)
              .orElseThrow(
                  () -> new ResourceNotFoundException("Cita no encontrada con id: " + citaId));
    }

    Tratamiento tratamiento = null;
    if (request.getTratamientoId() != null) {
      Long tratamientoId = request.getTratamientoId();
      tratamiento =
          tratamientoRepository
              .findById(tratamientoId)
              .orElseThrow(
                  () ->
                      new ResourceNotFoundException(
                          "Tratamiento no encontrado con id: " + tratamientoId));
      if (!tratamiento.getActivo()) {
        log.warn("Intento de usar tratamiento inactivo — id: {}", tratamientoId);
        throw new IllegalArgumentException("El tratamiento no está disponible");
      }
    }

    HistorialClinico historial = new HistorialClinico();
    historial.setPaciente(paciente);
    historial.setCita(cita);
    historial.setFechaHora(request.getFechaHora());
    historial.setProcedimiento(request.getProcedimiento());
    historial.setNotas(request.getNotas());
    historial.setTratamiento(tratamiento);
    historial.setPrecioAplicado(tratamiento != null ? tratamiento.getPrecio() : null);
    historial.setFotoUrl(request.getFotoUrl());

    HistorialClinico creado = historialRepository.save(historial);
    log.info("Registro de historial creado — id: {}, paciente: {}, procedimiento: '{}', precio aplicado: {}",
        creado.getId(), pacienteId, creado.getProcedimiento(), creado.getPrecioAplicado());
    return toResponse(creado);
  }

  public HistorialResponse update(Long id, HistorialRequest request) {
    HistorialClinico existente =
        historialRepository
            .findById(id)
            .orElseThrow(
                () -> new ResourceNotFoundException("Registro no encontrado con id: " + id));

    Long pacienteId = request.getPacienteId();
    Paciente paciente =
        pacienteRepository
            .findById(pacienteId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Paciente no encontrado con id: " + pacienteId));

    Cita cita = null;
    if (request.getCitaId() != null) {
      Long citaId = request.getCitaId();
      cita =
          citaRepository
              .findById(citaId)
              .orElseThrow(
                  () -> new ResourceNotFoundException("Cita no encontrada con id: " + citaId));
    }

    Tratamiento tratamiento = null;
    if (request.getTratamientoId() != null) {
      Long tratamientoId = request.getTratamientoId();
      tratamiento =
          tratamientoRepository
              .findById(tratamientoId)
              .orElseThrow(
                  () ->
                      new ResourceNotFoundException(
                          "Tratamiento no encontrado con id: " + tratamientoId));
      if (!tratamiento.getActivo()) {
        log.warn("Intento de usar tratamiento inactivo al actualizar historial id {} — tratamiento id: {}", id, tratamientoId);
        throw new IllegalArgumentException("El tratamiento no está disponible");
      }
    }

    existente.setPaciente(paciente);
    existente.setCita(cita);
    existente.setFechaHora(request.getFechaHora());
    existente.setProcedimiento(request.getProcedimiento());
    existente.setNotas(request.getNotas());
    existente.setTratamiento(tratamiento);
    existente.setPrecioAplicado(tratamiento != null ? tratamiento.getPrecio() : null);
    existente.setFotoUrl(request.getFotoUrl());

    HistorialClinico actualizado = historialRepository.save(existente);
    log.info("Registro de historial actualizado — id: {}, paciente: {}", actualizado.getId(), pacienteId);
    return toResponse(actualizado);
  }

  public void delete(Long id) {
    findById(id);
    historialRepository.deleteById(id);
    log.info("Registro de historial eliminado — id: {}", id);
  }

  private HistorialResponse toResponse(HistorialClinico h) {
    Paciente paciente = h.getPaciente();
    Long citaId = h.getCita() != null ? h.getCita().getId() : null;
    Long tratamientoId = h.getTratamiento() != null ? h.getTratamiento().getId() : null;
    String nombreTratamiento =
        h.getTratamiento() != null ? h.getTratamiento().getNombre() : null;
    return new HistorialResponse(
        h.getId(),
        paciente.getId(),
        paciente.getNombre(),
        paciente.getApellido(),
        citaId,
        h.getFechaHora(),
        h.getProcedimiento(),
        h.getNotas(),
        tratamientoId,
        nombreTratamiento,
        h.getPrecioAplicado(),
        h.getFotoUrl(),
        h.getCreatedAt());
  }
}

package com.consultorio.service;

import com.consultorio.dto.HistorialRequest;
import com.consultorio.dto.HistorialResponse;
import com.consultorio.exception.ResourceNotFoundException;
import com.consultorio.model.Cita;
import com.consultorio.model.HistorialClinico;
import com.consultorio.model.Paciente;
import com.consultorio.repository.CitaRepository;
import com.consultorio.repository.HistorialRepository;
import com.consultorio.repository.PacienteRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class HistorialService {

  private final HistorialRepository historialRepository;
  private final PacienteRepository pacienteRepository;
  private final CitaRepository citaRepository;

  public HistorialService(
      HistorialRepository historialRepository,
      PacienteRepository pacienteRepository,
      CitaRepository citaRepository) {
    this.historialRepository = historialRepository;
    this.pacienteRepository = pacienteRepository;
    this.citaRepository = citaRepository;
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

    HistorialClinico historial = new HistorialClinico();
    historial.setPaciente(paciente);
    historial.setCita(cita);
    historial.setFechaHora(request.getFechaHora());
    historial.setProcedimiento(request.getProcedimiento());
    historial.setNotas(request.getNotas());

    HistorialClinico creado = historialRepository.save(historial);
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

    existente.setPaciente(paciente);
    existente.setCita(cita);
    existente.setFechaHora(request.getFechaHora());
    existente.setProcedimiento(request.getProcedimiento());
    existente.setNotas(request.getNotas());

    HistorialClinico actualizado = historialRepository.save(existente);
    return toResponse(actualizado);
  }

  public void delete(Long id) {
    findById(id);
    historialRepository.deleteById(id);
  }

  private HistorialResponse toResponse(HistorialClinico h) {
    Paciente paciente = h.getPaciente();
    Long citaId = h.getCita() != null ? h.getCita().getId() : null;
    return new HistorialResponse(
        h.getId(),
        paciente.getId(),
        paciente.getNombre(),
        paciente.getApellido(),
        citaId,
        h.getFechaHora(),
        h.getProcedimiento(),
        h.getNotas(),
        h.getCreatedAt());
  }
}

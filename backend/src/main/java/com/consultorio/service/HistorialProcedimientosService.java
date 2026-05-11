package com.consultorio.service;

import com.consultorio.dto.HistorialProcedimientosRequest;
import com.consultorio.dto.HistorialProcedimientosResponse;
import com.consultorio.exception.ResourceNotFoundException;
import com.consultorio.model.Cita;
import com.consultorio.model.HistorialProcedimientos;
import com.consultorio.model.Paciente;
import com.consultorio.model.Servicio;
import com.consultorio.repository.CitaRepository;
import com.consultorio.repository.HistorialProcedimientosRepository;
import com.consultorio.repository.PacienteRepository;
import com.consultorio.repository.ServicioRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class HistorialProcedimientosService {

  private static final Logger log = LoggerFactory.getLogger(HistorialProcedimientosService.class);

  private final HistorialProcedimientosRepository historialRepository;
  private final PacienteRepository pacienteRepository;
  private final CitaRepository citaRepository;
  private final ServicioRepository servicioRepository;

  public HistorialProcedimientosService(
      HistorialProcedimientosRepository historialRepository,
      PacienteRepository pacienteRepository,
      CitaRepository citaRepository,
      ServicioRepository servicioRepository) {
    this.historialRepository = historialRepository;
    this.pacienteRepository = pacienteRepository;
    this.citaRepository = citaRepository;
    this.servicioRepository = servicioRepository;
  }

  public List<HistorialProcedimientosResponse> findAll() {
    return historialRepository.findAll().stream().map(this::toResponse).toList();
  }

  public HistorialProcedimientosResponse findById(Long id) {
    HistorialProcedimientos historial =
        historialRepository
            .findById(id)
            .orElseThrow(
                () -> new ResourceNotFoundException("Registro no encontrado con id: " + id));
    return toResponse(historial);
  }

  public List<HistorialProcedimientosResponse> findByPaciente(Long pacienteId) {
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

  public List<HistorialProcedimientosResponse> findByCita(Long citaId) {
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

  public HistorialProcedimientosResponse create(HistorialProcedimientosRequest request) {
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

    Servicio servicio = null;
    if (request.getServicioId() != null) {
      Long servicioId = request.getServicioId();
      servicio =
          servicioRepository
              .findById(servicioId)
              .orElseThrow(
                  () -> new ResourceNotFoundException("Servicio no encontrado con id: " + servicioId));
      if (!servicio.getActivo()) {
        log.warn("Intento de usar servicio inactivo — id: {}", servicioId);
        throw new IllegalArgumentException("El servicio no está disponible");
      }
    }

    HistorialProcedimientos historial = new HistorialProcedimientos();
    historial.setPaciente(paciente);
    historial.setCita(cita);
    historial.setFechaHora(request.getFechaHora());
    historial.setProcedimiento(request.getProcedimiento());
    historial.setNotas(request.getNotas());
    historial.setServicio(servicio);
    historial.setPrecioAplicado(servicio != null ? servicio.getPrecio() : null);
    historial.setFotoUrl(request.getFotoUrl());

    HistorialProcedimientos creado = historialRepository.save(historial);
    log.info("Registro de historial creado — id: {}, paciente: {}, procedimiento: '{}', precio aplicado: {}",
        creado.getId(), pacienteId, creado.getProcedimiento(), creado.getPrecioAplicado());
    return toResponse(creado);
  }

  public HistorialProcedimientosResponse update(Long id, HistorialProcedimientosRequest request) {
    HistorialProcedimientos existente =
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

    Servicio servicio = null;
    if (request.getServicioId() != null) {
      Long servicioId = request.getServicioId();
      servicio =
          servicioRepository
              .findById(servicioId)
              .orElseThrow(
                  () -> new ResourceNotFoundException("Servicio no encontrado con id: " + servicioId));
      if (!servicio.getActivo()) {
        log.warn("Intento de usar servicio inactivo al actualizar historial id {} — servicio id: {}", id, servicioId);
        throw new IllegalArgumentException("El servicio no está disponible");
      }
    }

    existente.setPaciente(paciente);
    existente.setCita(cita);
    existente.setFechaHora(request.getFechaHora());
    existente.setProcedimiento(request.getProcedimiento());
    existente.setNotas(request.getNotas());
    existente.setServicio(servicio);
    existente.setPrecioAplicado(servicio != null ? servicio.getPrecio() : null);
    existente.setFotoUrl(request.getFotoUrl());

    HistorialProcedimientos actualizado = historialRepository.save(existente);
    log.info("Registro de historial actualizado — id: {}, paciente: {}", actualizado.getId(), pacienteId);
    return toResponse(actualizado);
  }

  public void delete(Long id) {
    findById(id);
    historialRepository.deleteById(id);
    log.info("Registro de historial eliminado — id: {}", id);
  }

  private HistorialProcedimientosResponse toResponse(HistorialProcedimientos h) {
    Paciente paciente = h.getPaciente();
    Long citaId = h.getCita() != null ? h.getCita().getId() : null;
    Long servicioId = h.getServicio() != null ? h.getServicio().getId() : null;
    String nombreServicio = h.getServicio() != null ? h.getServicio().getNombre() : null;
    return new HistorialProcedimientosResponse(
        h.getId(),
        paciente.getId(),
        paciente.getNombre(),
        paciente.getApellido(),
        citaId,
        h.getFechaHora(),
        h.getProcedimiento(),
        h.getNotas(),
        servicioId,
        nombreServicio,
        h.getPrecioAplicado(),
        h.getFotoUrl(),
        h.getCreatedAt());
  }
}

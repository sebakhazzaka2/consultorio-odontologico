package com.consultorio.service;

import com.consultorio.dto.PublicReservaRequest;
import com.consultorio.dto.PublicReservaResponse;
import com.consultorio.exception.ResourceNotFoundException;
import com.consultorio.model.Cita;
import com.consultorio.model.CitaEstado;
import com.consultorio.model.Paciente;
import com.consultorio.model.PacienteOrigen;
import com.consultorio.model.Servicio;
import com.consultorio.repository.CitaRepository;
import com.consultorio.repository.PacienteRepository;
import com.consultorio.repository.ServicioRepository;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PublicReservaService {

  private static final Logger log = LoggerFactory.getLogger(PublicReservaService.class);

  private final CitaRepository citaRepository;
  private final PacienteRepository pacienteRepository;
  private final ServicioRepository servicioRepository;
  private final CitaService citaService;

  public PublicReservaService(CitaRepository citaRepository,
      PacienteRepository pacienteRepository,
      ServicioRepository servicioRepository,
      CitaService citaService) {
    this.citaRepository = citaRepository;
    this.pacienteRepository = pacienteRepository;
    this.servicioRepository = servicioRepository;
    this.citaService = citaService;
  }

  public List<String> getSlots(LocalDate fecha, Long servicioId) {
    Servicio servicio = servicioRepository.findById(servicioId)
        .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado con id: " + servicioId));
    return citaService.getDisponibilidad(fecha, servicio.getDuracionMinutos());
  }

  public PublicReservaResponse reservar(PublicReservaRequest request) {
    Servicio servicio = servicioRepository.findById(request.getServicioId())
        .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado con id: " + request.getServicioId()));

    // Verificar que el slot solicitado esté disponible
    LocalDate fecha = request.getFechaHoraInicio().toLocalDate();
    List<String> slotsDisponibles = citaService.getDisponibilidad(fecha, servicio.getDuracionMinutos());
    String horaSlot = request.getFechaHoraInicio().toLocalTime().toString().substring(0, 5);
    if (!slotsDisponibles.contains(horaSlot)) {
      throw new IllegalArgumentException("El horario seleccionado no está disponible");
    }

    // Deduplicación por email — reutilizar paciente si ya existe
    Paciente paciente = pacienteRepository.findByEmail(request.getEmail())
        .orElseGet(() -> crearPacienteDesdeAgenda(request));

    Cita cita = new Cita();
    cita.setPaciente(paciente);
    cita.setFechaHoraInicio(request.getFechaHoraInicio());
    cita.setDuracionMinutos(servicio.getDuracionMinutos());
    cita.setEstado(CitaEstado.PENDIENTE);
    cita.setMotivo(servicio.getNombre());

    Cita creada = citaRepository.save(cita);
    log.info("Reserva pública creada — cita id: {}, paciente: {} {}, servicio: '{}', inicio: {}",
        creada.getId(), paciente.getNombre(), paciente.getApellido(),
        servicio.getNombre(), creada.getFechaHoraInicio());

    return new PublicReservaResponse(
        creada.getId(),
        paciente.getNombre(),
        paciente.getApellido(),
        servicio.getNombre(),
        creada.getFechaHoraInicio(),
        creada.getDuracionMinutos(),
        creada.getEstado().name(),
        "Tu solicitud fue recibida. Te contactaremos para confirmar tu turno.");
  }

  private Paciente crearPacienteDesdeAgenda(PublicReservaRequest request) {
    Paciente paciente = new Paciente();
    paciente.setNombre(request.getNombre());
    paciente.setApellido(request.getApellido());
    paciente.setTelefono(request.getTelefono());
    paciente.setEmail(request.getEmail());
    paciente.setCreadoPor(PacienteOrigen.AGENDA);
    Paciente creado = pacienteRepository.save(paciente);
    log.info("Paciente creado desde agenda pública — id: {}, email: {}", creado.getId(), creado.getEmail());
    return creado;
  }
}

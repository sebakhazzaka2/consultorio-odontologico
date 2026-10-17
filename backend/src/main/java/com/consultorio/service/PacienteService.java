package com.consultorio.service;

import com.consultorio.dto.PacienteRequest;
import com.consultorio.dto.PacienteResponse;
import com.consultorio.exception.ConflictException;
import com.consultorio.exception.ResourceNotFoundException;
import com.consultorio.model.Paciente;
import com.consultorio.model.PacienteOrigen;
import com.consultorio.repository.PacienteRepository;
import com.consultorio.util.EmailNormalizer;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PacienteService {

  private static final Logger log = LoggerFactory.getLogger(PacienteService.class);

  private final PacienteRepository pacienteRepository;

  public PacienteService(PacienteRepository pacienteRepository) {
    this.pacienteRepository = pacienteRepository;
  }

  public List<PacienteResponse> findAll() {
    return pacienteRepository.findAll().stream().map(this::toResponse).toList();
  }

  public PacienteResponse findById(Long id) {
    Paciente paciente =
        pacienteRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado con id: " + id));
    return toResponse(paciente);
  }

  // El chequeo previo de email da un mensaje claro en el caso común; el constraint
  // uk_pacientes_email (V10) es quien garantiza la unicidad ante altas concurrentes
  // (GlobalExceptionHandler lo traduce al mismo 409). Nunca se loguea el email (PII).
  public PacienteResponse create(PacienteRequest request) {
    String email = EmailNormalizer.normalizar(request.getEmail());
    if (email != null && pacienteRepository.existsByEmail(email)) {
      log.warn("Intento de crear paciente con email ya registrado");
      throw new ConflictException("El email ya está registrado");
    }

    Paciente paciente = toEntity(request);
    Paciente creado = pacienteRepository.save(paciente);
    log.info("Paciente creado — id: {}, nombre: {} {}", creado.getId(), creado.getNombre(), creado.getApellido());
    return toResponse(creado);
  }

  public PacienteResponse update(Long id, PacienteRequest request) {
    Paciente existente =
        pacienteRepository
            .findById(id)
            .orElseThrow(
                () -> new ResourceNotFoundException("Paciente no encontrado con id: " + id));

    String nuevoEmail = EmailNormalizer.normalizar(request.getEmail());
    if (nuevoEmail != null) {
      pacienteRepository
          .findByEmail(nuevoEmail)
          .ifPresent(
              encontrado -> {
                if (!encontrado.getId().equals(id)) {
                  log.warn("Intento de actualizar paciente id {} con email ya registrado", id);
                  throw new ConflictException("El email ya está registrado");
                }
              });
    }

    existente.setNombre(request.getNombre());
    existente.setApellido(request.getApellido());
    existente.setTelefono(request.getTelefono());
    existente.setEmail(nuevoEmail);
    existente.setFechaNacimiento(request.getFechaNacimiento());

    Paciente actualizado = pacienteRepository.save(existente);
    log.info("Paciente actualizado — id: {}", actualizado.getId());
    return toResponse(actualizado);
  }

  public void delete(Long id) {
    // Verifica existencia antes de eliminar.
    pacienteRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado con id: " + id));
    pacienteRepository.deleteById(id);
    log.info("Paciente eliminado — id: {}", id);
  }

  public PacienteResponse toResponse(Paciente paciente) {
    return new PacienteResponse(
        paciente.getId(),
        paciente.getNombre(),
        paciente.getApellido(),
        paciente.getTelefono(),
        paciente.getEmail(),
        paciente.getFechaNacimiento(),
        paciente.getCreadoPor(),
        paciente.getCreatedAt());
  }

  private Paciente toEntity(PacienteRequest request) {
    Paciente paciente = new Paciente();
    paciente.setNombre(request.getNombre());
    paciente.setApellido(request.getApellido());
    paciente.setTelefono(request.getTelefono());
    paciente.setEmail(EmailNormalizer.normalizar(request.getEmail()));
    paciente.setFechaNacimiento(request.getFechaNacimiento());
    paciente.setCreadoPor(PacienteOrigen.ADMIN);
    return paciente;
  }
}


package com.consultorio.service;

import com.consultorio.dto.PacienteRequest;
import com.consultorio.dto.PacienteResponse;
import com.consultorio.exception.ResourceNotFoundException;
import com.consultorio.model.Paciente;
import com.consultorio.repository.PacienteRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PacienteService {

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

  public PacienteResponse create(PacienteRequest request) {
    if (pacienteRepository.existsByEmail(request.getEmail())) {
      throw new IllegalArgumentException("El email ya está registrado");
    }

    Paciente paciente = toEntity(request);
    Paciente creado = pacienteRepository.save(paciente);
    return toResponse(creado);
  }

  public PacienteResponse update(Long id, PacienteRequest request) {
    Paciente existente =
        pacienteRepository
            .findById(id)
            .orElseThrow(
                () -> new ResourceNotFoundException("Paciente no encontrado con id: " + id));

    String nuevoEmail = request.getEmail();
    if (nuevoEmail != null) {
      pacienteRepository
          .findByEmail(nuevoEmail)
          .ifPresent(
              encontrado -> {
                if (!encontrado.getId().equals(id)) {
                  throw new IllegalArgumentException("El email ya está registrado");
                }
              });
    }

    existente.setNombre(request.getNombre());
    existente.setApellido(request.getApellido());
    existente.setTelefono(request.getTelefono());
    existente.setEmail(request.getEmail());
    existente.setFechaNacimiento(request.getFechaNacimiento());

    Paciente actualizado = pacienteRepository.save(existente);
    return toResponse(actualizado);
  }

  public void delete(Long id) {
    // Verifica existencia antes de eliminar.
    pacienteRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado con id: " + id));
    pacienteRepository.deleteById(id);
  }

  private PacienteResponse toResponse(Paciente paciente) {
    return new PacienteResponse(
        paciente.getId(),
        paciente.getNombre(),
        paciente.getApellido(),
        paciente.getTelefono(),
        paciente.getEmail(),
        paciente.getFechaNacimiento(),
        paciente.getCreatedAt());
  }

  private Paciente toEntity(PacienteRequest request) {
    Paciente paciente = new Paciente();
    paciente.setNombre(request.getNombre());
    paciente.setApellido(request.getApellido());
    paciente.setTelefono(request.getTelefono());
    paciente.setEmail(request.getEmail());
    paciente.setFechaNacimiento(request.getFechaNacimiento());
    return paciente;
  }
}


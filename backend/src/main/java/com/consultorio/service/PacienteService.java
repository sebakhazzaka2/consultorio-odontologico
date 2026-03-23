package com.consultorio.service;

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

  public List<Paciente> findAll() {
    return pacienteRepository.findAll();
  }

  public Paciente findById(Long id) {
    return pacienteRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado con id: " + id));
  }

  public Paciente create(Paciente paciente) {
    if (pacienteRepository.existsByEmail(paciente.getEmail())) {
      throw new IllegalArgumentException("El email ya está registrado");
    }

    return pacienteRepository.save(paciente);
  }

  public Paciente update(Long id, Paciente datos) {
    Paciente existente = findById(id);

    String nuevoEmail = datos.getEmail();
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

    existente.setNombre(datos.getNombre());
    existente.setApellido(datos.getApellido());
    existente.setTelefono(datos.getTelefono());
    existente.setEmail(datos.getEmail());
    existente.setFechaNacimiento(datos.getFechaNacimiento());

    return pacienteRepository.save(existente);
  }

  public void delete(Long id) {
    // Verifica existencia antes de eliminar.
    findById(id);
    pacienteRepository.deleteById(id);
  }
}


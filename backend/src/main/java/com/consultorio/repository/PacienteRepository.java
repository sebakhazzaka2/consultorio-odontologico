package com.consultorio.repository;

import com.consultorio.model.Paciente;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PacienteRepository extends JpaRepository<Paciente, Long> {

  boolean existsByEmail(String email);

  Optional<Paciente> findByEmail(String email);
}

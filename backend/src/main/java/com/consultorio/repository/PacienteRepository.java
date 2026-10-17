package com.consultorio.repository;

import com.consultorio.model.Paciente;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PacienteRepository extends JpaRepository<Paciente, Long> {

  boolean existsByEmail(String email);

  Optional<Paciente> findByEmail(String email);

  /** Solo email y teléfono de todos los pacientes (detección de existentes al importar). */
  interface Contacto {
    String getEmail();

    String getTelefono();
  }

  @Query("select p.email as email, p.telefono as telefono from Paciente p")
  List<Contacto> findAllContactos();
}

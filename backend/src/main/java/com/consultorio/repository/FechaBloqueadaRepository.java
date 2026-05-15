package com.consultorio.repository;

import com.consultorio.model.FechaBloqueada;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FechaBloqueadaRepository extends JpaRepository<FechaBloqueada, Long> {
  boolean existsByFecha(LocalDate fecha);
  Optional<FechaBloqueada> findByFecha(LocalDate fecha);
}

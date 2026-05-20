package com.consultorio.repository;

import com.consultorio.model.DisponibilidadSemanal;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DisponibilidadSemanalRepository extends JpaRepository<DisponibilidadSemanal, Long> {
  Optional<DisponibilidadSemanal> findByDiaSemana(Integer diaSemana);
}

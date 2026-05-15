package com.consultorio.repository;

import com.consultorio.model.FechaBloqueada;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface FechaBloqueadaRepository extends JpaRepository<FechaBloqueada, Long> {
  boolean existsByFecha(LocalDate fecha);
  Optional<FechaBloqueada> findByFecha(LocalDate fecha);
  List<FechaBloqueada> findByFechaGreaterThanEqualOrderByFechaAsc(LocalDate desde);

  @Modifying
  @Transactional
  @Query("DELETE FROM FechaBloqueada f WHERE f.fecha < :hoy")
  void eliminarPasadas(LocalDate hoy);
}

package com.consultorio.repository;

import com.consultorio.model.DisponibilidadSemanal;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DisponibilidadSemanalRepository extends JpaRepository<DisponibilidadSemanal, Long> {
  Optional<DisponibilidadSemanal> findByDiaSemana(Integer diaSemana);

  /**
   * Mismo registro que findByDiaSemana, pero con SELECT ... FOR UPDATE (lock
   * pesimista de fila). Usado para serializar reservas/confirmaciones del mismo
   * día de semana: el lock lo mantiene MySQL hasta que la transacción que lo pidió
   * hace commit o rollback — no hace falta (ni conviene) liberarlo a mano, evita la
   * ventana que existía con GET_LOCK + RELEASE_LOCK en un finally antes del commit.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select d from DisponibilidadSemanal d where d.diaSemana = :diaSemana")
  Optional<DisponibilidadSemanal> findByDiaSemanaForUpdate(@Param("diaSemana") Integer diaSemana);
}

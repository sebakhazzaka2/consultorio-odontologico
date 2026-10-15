package com.consultorio.repository;
import com.consultorio.model.CitaEstado;
import com.consultorio.model.Cita;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CitaRepository extends JpaRepository<Cita, Long> {

  List<Cita> findByFechaHoraInicioBetween(LocalDateTime inicio, LocalDateTime fin);

  List<Cita> findByFechaHoraInicioBetweenAndEstado(
    LocalDateTime inicio, LocalDateTime fin, CitaEstado estado);

  List<Cita> findByFechaHoraInicioBetweenAndEstadoIn(
    LocalDateTime inicio, LocalDateTime fin, Collection<CitaEstado> estados);

  /**
   * findById pero con SELECT ... FOR UPDATE. Necesario en confirmar(): para saber
   * qué día lockear hace falta leer la cita primero, pero una lectura NORMAL ahí
   * fijaría el snapshot REPEATABLE READ de la transacción antes del lock del día
   * (ver CitaService.confirmar). Una lectura con lock no fija ese snapshot, así que
   * esta sí es segura como primera sentencia.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from Cita c where c.id = :id")
  Optional<Cita> findByIdForUpdate(@Param("id") Long id);
}

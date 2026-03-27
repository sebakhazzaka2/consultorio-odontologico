package com.consultorio.repository;
import com.consultorio.model.CitaEstado;
import com.consultorio.model.Cita;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CitaRepository extends JpaRepository<Cita, Long> {

  List<Cita> findByFechaHoraInicioBetween(LocalDateTime inicio, LocalDateTime fin);

  List<Cita> findByFechaHoraInicioBetweenAndEstado(
    LocalDateTime inicio, LocalDateTime fin, CitaEstado estado);
}

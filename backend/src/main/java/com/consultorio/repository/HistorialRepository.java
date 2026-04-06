package com.consultorio.repository;

import com.consultorio.model.HistorialClinico;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistorialRepository extends JpaRepository<HistorialClinico, Long> {

  List<HistorialClinico> findByPacienteIdOrderByFechaHoraDesc(Long pacienteId);

  List<HistorialClinico> findByCitaIdOrderByFechaHoraDesc(Long citaId);
}

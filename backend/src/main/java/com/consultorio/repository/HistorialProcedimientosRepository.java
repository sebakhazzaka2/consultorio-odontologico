package com.consultorio.repository;

import com.consultorio.model.HistorialProcedimientos;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistorialProcedimientosRepository extends JpaRepository<HistorialProcedimientos, Long> {

  List<HistorialProcedimientos> findByPacienteIdOrderByFechaHoraDesc(Long pacienteId);

  List<HistorialProcedimientos> findByCitaIdOrderByFechaHoraDesc(Long citaId);
}

package com.consultorio.repository;

import com.consultorio.model.Pago;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PagoRepository extends JpaRepository<Pago, Long> {

  List<Pago> findByPacienteIdOrderByFechaDesc(Long pacienteId);
}

package com.consultorio.repository;

import com.consultorio.model.Servicio;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServicioRepository extends JpaRepository<Servicio, Long> {

  List<Servicio> findByActivoTrue();

  List<Servicio> findByActivoTrueOrderByNombreAsc();
}

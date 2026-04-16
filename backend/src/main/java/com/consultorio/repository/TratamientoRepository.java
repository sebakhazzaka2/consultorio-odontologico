package com.consultorio.repository;

import com.consultorio.model.Tratamiento;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TratamientoRepository extends JpaRepository<Tratamiento, Long> {

  List<Tratamiento> findByActivoTrue();

  List<Tratamiento> findByActivoTrueOrderByNombreAsc();
}

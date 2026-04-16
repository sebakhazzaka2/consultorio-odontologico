package com.consultorio.controller;

import com.consultorio.BaseIntegrationTest;
import com.consultorio.model.Tratamiento;
import com.consultorio.repository.TratamientoRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PublicTratamientoControllerTest extends BaseIntegrationTest {

  @Autowired
  private TratamientoRepository tratamientoRepository;

  @BeforeEach
  void setUp() {
    Tratamiento activo = new Tratamiento();
    activo.setNombre("Limpieza dental");
    activo.setDescripcion("Profilaxis completa");
    activo.setPrecio(new BigDecimal("1500.00"));
    activo.setActivo(true);
    tratamientoRepository.save(activo);

    Tratamiento inactivo = new Tratamiento();
    inactivo.setNombre("Blanqueamiento");
    inactivo.setDescripcion("Tratamiento estético");
    inactivo.setPrecio(new BigDecimal("3000.00"));
    inactivo.setActivo(false);
    tratamientoRepository.save(inactivo);
  }

  @Test
  void getTratamientosPublicos_sinToken_retorna200() throws Exception {
    mockMvc.perform(get("/public/tratamientos"))
        .andExpect(status().isOk());
  }

  @Test
  void getTratamientosPublicos_retornaSoloActivos() throws Exception {
    mockMvc.perform(get("/public/tratamientos"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].nombre").value("Limpieza dental"));
  }

  @Test
  void getTratamientosPublicos_noRetornaActivo() throws Exception {
    mockMvc.perform(get("/public/tratamientos"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.nombre == 'Blanqueamiento')]").isEmpty());
  }
}

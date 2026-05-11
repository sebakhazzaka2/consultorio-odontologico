package com.consultorio.controller;

import com.consultorio.BaseIntegrationTest;
import com.consultorio.model.Servicio;
import com.consultorio.repository.ServicioRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PublicServicioControllerTest extends BaseIntegrationTest {

  @Autowired
  private ServicioRepository servicioRepository;

  @BeforeEach
  void setUp() {
    Servicio activo = new Servicio();
    activo.setNombre("Limpieza dental");
    activo.setDescripcion("Profilaxis completa");
    activo.setPrecio(new BigDecimal("1500.00"));
    activo.setActivo(true);
    servicioRepository.save(activo);

    Servicio inactivo = new Servicio();
    inactivo.setNombre("Blanqueamiento");
    inactivo.setDescripcion("Tratamiento estético");
    inactivo.setPrecio(new BigDecimal("3000.00"));
    inactivo.setActivo(false);
    servicioRepository.save(inactivo);
  }

  @Test
  void getServiciosPublicos_sinToken_retorna200() throws Exception {
    mockMvc.perform(get("/public/servicios"))
        .andExpect(status().isOk());
  }

  @Test
  void getServiciosPublicos_retornaSoloActivos() throws Exception {
    mockMvc.perform(get("/public/servicios"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].nombre").value("Limpieza dental"));
  }

  @Test
  void getServiciosPublicos_noRetornaInactivo() throws Exception {
    mockMvc.perform(get("/public/servicios"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.nombre == 'Blanqueamiento')]").isEmpty());
  }
}

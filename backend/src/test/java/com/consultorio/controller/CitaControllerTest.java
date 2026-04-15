package com.consultorio.controller;

import com.consultorio.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CitaControllerTest extends BaseIntegrationTest {

  private String token;

  @BeforeEach
  void setUp() throws Exception {
    token = registerAndLogin("admin@test.com", "clave123");
  }

  @Test
  void getCitas_sinToken_retorna401() throws Exception {
    mockMvc.perform(get("/citas"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void crearCita_sinToken_retorna401() throws Exception {
    mockMvc.perform(post("/citas")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void crearCita_cuerpoInvalido_retorna422() throws Exception {
    // Body vacío — pacienteId, fechaHoraInicio, duracionMinutos y motivo son obligatorios
    mockMvc.perform(post("/citas")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void cancelarCita_idInexistente_retorna404() throws Exception {
    mockMvc.perform(patch("/citas/999/cancelar")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isNotFound());
  }
}

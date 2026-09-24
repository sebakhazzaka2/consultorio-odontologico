package com.consultorio.controller;

import com.consultorio.BaseIntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PublicReservaControllerTest extends BaseIntegrationTest {

  private String body(String website, String nombre) throws Exception {
    return objectMapper.writeValueAsString(Map.of(
        "nombre", nombre,
        "apellido", "Perez",
        "telefono", "099123456",
        "email", "bot@test.com",
        "website", website,
        "servicio_id", 1,
        "fecha_hora_inicio", "2030-01-07T10:00:00"));
  }

  @Test
  void reservar_honeypotCompletado_retorna422() throws Exception {
    mockMvc.perform(post("/public/reservas")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body("http://spam.example", "Ana")))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void reservar_nombreDemasiadoLargo_retorna422() throws Exception {
    mockMvc.perform(post("/public/reservas")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body("", "x".repeat(101))))
        .andExpect(status().isUnprocessableEntity());
  }
}

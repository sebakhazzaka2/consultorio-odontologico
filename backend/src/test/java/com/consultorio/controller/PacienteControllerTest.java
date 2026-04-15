package com.consultorio.controller;

import com.consultorio.BaseIntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PacienteControllerTest extends BaseIntegrationTest {

  private String token;

  @BeforeEach
  void setUp() throws Exception {
    token = registerAndLogin("admin@test.com", "clave123");
  }

  @Test
  void getPacientes_sinToken_retorna401() throws Exception {
    mockMvc.perform(get("/pacientes"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void crearPaciente_sinToken_retorna401() throws Exception {
    mockMvc.perform(post("/pacientes")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void crearPaciente_cuerpoInvalido_retorna422() throws Exception {
    // nombre vacío viola @NotBlank → MethodArgumentNotValidException → 422
    String body = objectMapper.writeValueAsString(
        Map.of("nombre", "", "apellido", "García",
               "telefono", "099123456", "email", "x@test.com"));

    mockMvc.perform(post("/pacientes")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void crearPaciente_bodyValido_retorna201() throws Exception {
    String body = objectMapper.writeValueAsString(
        Map.of("nombre", "Juan", "apellido", "García",
               "telefono", "099123456", "email", "juan@test.com"));

    mockMvc.perform(post("/pacientes")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.nombre").value("Juan"))
        .andExpect(jsonPath("$.apellido").value("García"));
  }
}

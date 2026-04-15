package com.consultorio.controller;

import com.consultorio.BaseIntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest extends BaseIntegrationTest {

  @Test
  void register_nuevoUsuario_retorna201() throws Exception {
    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                Map.of("email", "nuevo@test.com", "password", "clave123"))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.message").value("Usuario registrado exitosamente"));
  }

  @Test
  void register_emailDuplicado_retorna400() throws Exception {
    String body = objectMapper.writeValueAsString(
        Map.of("email", "dup@test.com", "password", "clave123"));

    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isCreated());

    // Segundo registro con el mismo email → IllegalArgumentException → 400
    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest());
  }

  @Test
  void login_credencialesValidas_retornaToken() throws Exception {
    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                Map.of("email", "admin@test.com", "password", "clave123"))))
        .andExpect(status().isCreated());

    mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                Map.of("email", "admin@test.com", "password", "clave123"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").isNotEmpty());
  }

  @Test
  void login_passwordIncorrecta_retorna500() throws Exception {
    // AuthService relanza RuntimeException sin handler específico → GlobalExceptionHandler → 500.
    // TODO: mejorar en P2 — login fallido debería devolver 401.
    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                Map.of("email", "admin@test.com", "password", "clave123"))))
        .andExpect(status().isCreated());

    mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                Map.of("email", "admin@test.com", "password", "incorrecta"))))
        .andExpect(status().isInternalServerError());
  }
}

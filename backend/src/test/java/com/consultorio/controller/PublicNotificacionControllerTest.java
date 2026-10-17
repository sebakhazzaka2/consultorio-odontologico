package com.consultorio.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.consultorio.BaseIntegrationTest;
import com.consultorio.exception.TokenBajaInvalidoException;
import com.consultorio.model.Paciente;
import com.consultorio.repository.PacienteRepository;
import com.consultorio.service.notificacion.BajaTokenService;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** Baja de recordatorios: público (sin Authorization en ningún request), contrato fijo. */
class PublicNotificacionControllerTest extends BaseIntegrationTest {

  private static final String URL = "/public/notificaciones/baja";

  @Autowired private PacienteRepository pacienteRepository;
  @Autowired private BajaTokenService tokens;

  private Paciente paciente;

  @BeforeEach
  void setUp() {
    paciente = pacienteRepository.saveAndFlush(
        new Paciente("Ana", "Pérez", "099123456", "ana@baja.com", null));
  }

  private ResultActions postBaja(String token) throws Exception {
    return mockMvc.perform(post(URL)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(Map.of("token", token))));
  }

  private boolean recordatoriosEnBase() {
    pacienteRepository.flush();
    return pacienteRepository.findById(paciente.getId()).orElseThrow().isRecordatoriosEmail();
  }

  @Test
  void get_tokenValido_devuelveClinicaYEstadoActivo() throws Exception {
    mockMvc.perform(get(URL).param("token", tokens.generar(paciente.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.clinica").exists())
        .andExpect(jsonPath("$.estado").value("ACTIVO"));
  }

  @Test
  void post_daDeBaja_204_yEsIdempotente() throws Exception {
    String token = tokens.generar(paciente.getId());
    postBaja(token).andExpect(status().isNoContent());
    assertThat(recordatoriosEnBase()).isFalse();

    postBaja(token).andExpect(status().isNoContent());
    assertThat(recordatoriosEnBase()).isFalse();

    mockMvc.perform(get(URL).param("token", token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("BAJA"));
  }

  @Test
  void tokenAdulterado_400EnGetYPost_sinCambiarNada() throws Exception {
    String token = tokens.generar(paciente.getId());
    String adulterado = token.substring(0, token.length() - 2) + (token.endsWith("AA") ? "BB" : "AA");

    mockMvc.perform(get(URL).param("token", adulterado))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value(TokenBajaInvalidoException.MENSAJE));
    postBaja(adulterado)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value(TokenBajaInvalidoException.MENSAJE));
    assertThat(recordatoriosEnBase()).isTrue();
  }

  @Test
  void pacienteInexistente_400() throws Exception {
    String token = tokens.generar(paciente.getId() + 1000);
    mockMvc.perform(get(URL).param("token", token)).andExpect(status().isBadRequest());
    postBaja(token).andExpect(status().isBadRequest());
  }

  @Test
  void tokenFaltanteOBodyVacio_400() throws Exception {
    mockMvc.perform(get(URL)).andExpect(status().isBadRequest());
    mockMvc.perform(get(URL).param("token", "basura")).andExpect(status().isBadRequest());
    mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest());
    mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }
}

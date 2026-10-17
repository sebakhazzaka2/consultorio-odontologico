package com.consultorio.controller;

import com.consultorio.BaseIntegrationTest;
import com.consultorio.model.Paciente;
import com.consultorio.repository.PacienteRepository;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PacienteControllerTest extends BaseIntegrationTest {

  @Autowired
  private PacienteRepository pacienteRepository;

  /** Body de paciente válido; {@code email} puede ser null (se manda como null en el JSON). */
  private String pacienteBody(String nombre, String email) throws Exception {
    Map<String, Object> body = new HashMap<>();
    body.put("nombre", nombre);
    body.put("apellido", "García");
    body.put("telefono", "099123456");
    body.put("email", email);
    return objectMapper.writeValueAsString(body);
  }

  private ResultActions crear(String nombre, String email) throws Exception {
    return mockMvc.perform(post("/pacientes")
        .header("Authorization", "Bearer " + token)
        .contentType(MediaType.APPLICATION_JSON)
        .content(pacienteBody(nombre, email)));
  }

  private long crearYDevolverId(String nombre, String email) throws Exception {
    String json = crear(nombre, email).andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();
    JsonNode node = objectMapper.readTree(json);
    return node.get("id").asLong();
  }

  @Test
  void crearPaciente_sinEmail_retorna201ConEmailNull() throws Exception {
    long id = crearYDevolverId("Juan", null);
    assertThat(pacienteRepository.findById(id)).get().extracting(Paciente::getEmail).isNull();
  }

  @Test
  void crearPaciente_emailEnBlanco_seGuardaComoNull() throws Exception {
    long id = crearYDevolverId("Juan", "   ");
    assertThat(pacienteRepository.findById(id)).get().extracting(Paciente::getEmail).isNull();
  }

  @Test
  void crearDosPacientesSinEmail_ambosOk() throws Exception {
    crear("Juan", null).andExpect(status().isCreated());
    crear("Pedro", "").andExpect(status().isCreated());
    pacienteRepository.flush();
    assertThat(pacienteRepository.findAll()).filteredOn(p -> p.getEmail() == null).hasSize(2);
  }

  @Test
  void crearPaciente_emailConMayusculasYEspacios_seGuardaNormalizado() throws Exception {
    long id = crearYDevolverId("Juan", "  Juan.Garcia@Test.COM ");
    assertThat(pacienteRepository.findById(id)).get()
        .extracting(Paciente::getEmail).isEqualTo("juan.garcia@test.com");
  }

  @Test
  void crearPaciente_emailYaRegistradoConOtroFormato_retorna409() throws Exception {
    crear("Juan", "foo@bar.com").andExpect(status().isCreated());

    crear("Otro", " Foo@Bar.com ")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("El email ya está registrado"));
  }

  @Test
  void actualizarPaciente_aEmailDeOtroPacienteConOtroCase_retorna409() throws Exception {
    crear("Juan", "foo@bar.com").andExpect(status().isCreated());
    long otroId = crearYDevolverId("Otro", "otro@bar.com");

    mockMvc.perform(put("/pacientes/" + otroId)
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(pacienteBody("Otro", "FOO@bar.com")))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("El email ya está registrado"));
  }

  @Test
  void actualizarPaciente_sinEmailYConSuPropioEmail_retorna200() throws Exception {
    long id = crearYDevolverId("Juan", "juan@bar.com");

    // Mismo email con otro formato: es el propio paciente, no conflicto.
    mockMvc.perform(put("/pacientes/" + id)
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(pacienteBody("Juan", "JUAN@bar.com")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("juan@bar.com"));

    // Quitarle el email.
    mockMvc.perform(put("/pacientes/" + id)
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(pacienteBody("Juan", null)))
        .andExpect(status().isOk());
    pacienteRepository.flush();
    assertThat(pacienteRepository.findById(id)).get().extracting(Paciente::getEmail).isNull();
  }

  @Test
  void crearPaciente_emailInvalido_retorna422() throws Exception {
    crear("Juan", "no-es-un-email").andExpect(status().isUnprocessableEntity());
  }

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

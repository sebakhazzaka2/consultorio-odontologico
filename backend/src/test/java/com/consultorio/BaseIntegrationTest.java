package com.consultorio;

import com.consultorio.model.User;
import com.consultorio.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base para tests de integración.
 *
 * <p>Levanta el contexto completo de Spring (seguridad, filtros JWT, servicios, repositorios)
 * contra H2 en memoria. {@code @Transactional} garantiza que cada test empiece con la BD limpia:
 * todo lo escrito durante el test se deshace automáticamente al terminar.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
public abstract class BaseIntegrationTest {

  @Autowired
  protected MockMvc mockMvc;

  @Autowired
  protected ObjectMapper objectMapper;

  @Autowired
  protected UserRepository userRepository;

  @Autowired
  protected PasswordEncoder passwordEncoder;

  /** Crea un usuario ADMIN directo en BD (/auth/register ya no es público). */
  protected void createAdmin(String email, String password) {
    userRepository.save(new User(email, passwordEncoder.encode(password), "ADMIN"));
  }

  /**
   * Crea un usuario y hace login, devolviendo el JWT.
   * Ambas operaciones corren dentro de la transacción del test y se revierten al final.
   */
  protected String registerAndLogin(String email, String password) throws Exception {
    createAdmin(email, password);

    MvcResult result = mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password))))
        .andExpect(status().isOk())
        .andReturn();

    Map<String, String> body = objectMapper.readValue(
        result.getResponse().getContentAsString(), new TypeReference<>() {});
    return body.get("token");
  }
}

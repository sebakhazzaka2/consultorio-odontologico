package com.consultorio.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.consultorio.BaseIntegrationTest;
import com.consultorio.model.Paciente;
import com.consultorio.model.PacienteOrigen;
import com.consultorio.repository.PacienteRepository;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

class PacienteImportControllerTest extends BaseIntegrationTest {

  private static final String HEADER = "nombre;apellido;telefono;email;fecha_nacimiento\n";

  @Autowired private PacienteRepository pacienteRepository;

  private String token;

  @BeforeEach
  void login() throws Exception {
    token = registerAndLogin("admin-import@test.dev", "password123");
  }

  // ── simulación / confirmación ──────────────────────────────────────

  @Test
  void simulacion_noInsertaNada() throws Exception {
    importar(HEADER + "Ana;López;099111222;ana@test.dev;01/02/1990\n", false)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(1))
        .andExpect(jsonPath("$.ok").value(1))
        .andExpect(jsonPath("$.insertados").value(0))
        .andExpect(jsonPath("$.confirmado").value(false))
        .andExpect(jsonPath("$.ya_existen").value(0))
        .andExpect(jsonPath("$.filas[0].fila").value(1))
        .andExpect(jsonPath("$.filas[0].estado").value("OK"));

    assertThat(pacienteRepository.count()).isZero();
  }

  @Test
  void confirmacion_insertaFilasOkConOrigenImport() throws Exception {
    String csv = HEADER
        + "Ana;López;099 111 222;ANA@Test.dev ;01/02/1990\n"
        + "Beto;Pérez;24001234;;\n"
        + "Carla;Gómez;abc;;\n";

    importar(csv, true)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ok").value(2))
        .andExpect(jsonPath("$.errores").value(1))
        .andExpect(jsonPath("$.insertados").value(2))
        .andExpect(jsonPath("$.confirmado").value(true));

    List<Paciente> pacientes = pacienteRepository.findAll();
    assertThat(pacientes).hasSize(2).allMatch(p -> p.getCreadoPor() == PacienteOrigen.IMPORT);
    Paciente ana = pacienteRepository.findByEmail("ana@test.dev").orElseThrow();
    assertThat(ana.getTelefono()).isEqualTo("099111222");
    assertThat(ana.getFechaNacimiento()).hasToString("1990-02-01");
    Paciente beto = pacientes.stream().filter(p -> p.getNombre().equals("Beto")).findFirst().orElseThrow();
    assertThat(beto.getEmail()).isNull();
    assertThat(beto.getTelefono()).isEqualTo("24001234");
  }

  @Test
  void confirmarDosVeces_laSegundaNoInsertaNada() throws Exception {
    String csv = HEADER
        + "Ana;López;099111222;ana@test.dev;\n"
        + "Beto;Pérez;099333444;;\n";

    importar(csv, true).andExpect(jsonPath("$.insertados").value(2));
    importar(csv, true)
        .andExpect(jsonPath("$.ok").value(0))
        .andExpect(jsonPath("$.ya_existen").value(2))
        .andExpect(jsonPath("$.insertados").value(0));

    assertThat(pacienteRepository.count()).isEqualTo(2);
  }

  @Test
  void telefonoConPrefijoInternacional_seGuardaEnE164() throws Exception {
    importar(HEADER + "Ana;López;+598 99 111 222;;\nBeto;Pérez;0059899333444;;\n", true)
        .andExpect(jsonPath("$.insertados").value(2));

    assertThat(pacienteRepository.findAll()).extracting(Paciente::getTelefono)
        .containsExactlyInAnyOrder("59899111222", "59899333444");
  }

  // ── duplicados ─────────────────────────────────────────────────────

  @Test
  void duplicadosEnElArchivo() throws Exception {
    String csv = HEADER
        + "Ana;López;099111222;ana@test.dev;\n"
        + "Ana;Lopez;099999999;ANA@test.dev;\n"   // mismo email, otro formato
        + "Beto;Pérez;099333444;;\n"
        + "Bruno;Pérez;099 333 444;;\n";           // sin email, mismo teléfono

    JsonNode r = json(importar(csv, false).andExpect(status().isOk()));

    assertThat(r.get("ok").asInt()).isEqualTo(2);
    assertThat(r.get("duplicados").asInt()).isEqualTo(2);
    assertThat(estado(r, 1)).isEqualTo("DUPLICADO_ARCHIVO");
    assertThat(motivo(r, 1)).contains("fila 1");
    assertThat(estado(r, 3)).isEqualTo("DUPLICADO_ARCHIVO");
    assertThat(motivo(r, 3)).contains("fila 3");
  }

  @Test
  void pacienteExistentePorEmailConOtroFormato_yaExiste() throws Exception {
    pacienteRepository.save(new Paciente("Ana", "López", "099111222", "ana@test.dev", null));

    importar(HEADER + "Otra;Persona;099555666;  Ana@TEST.dev;\n", true)
        .andExpect(jsonPath("$.ya_existen").value(1))
        .andExpect(jsonPath("$.filas[0].estado").value("YA_EXISTE"))
        .andExpect(jsonPath("$.insertados").value(0));
  }

  @Test
  void pacienteExistenteSinEmail_seEncuentraPorTelefonoEscritoDistinto() throws Exception {
    pacienteRepository.save(new Paciente("Ana", "López", "099572537", null, null));

    importar(HEADER + "Ana;López;099 572 537;;\nAna2;López;+598 99 572 537;;\n", false)
        .andExpect(jsonPath("$.ya_existen").value(2))
        .andExpect(jsonPath("$.filas[0].estado").value("YA_EXISTE"))
        .andExpect(jsonPath("$.filas[1].estado").value("YA_EXISTE"));
  }

  @Test
  void mismoNombreDistintoContacto_noEsDuplicado() throws Exception {
    pacienteRepository.save(new Paciente("Ana", "López", "099111222", "ana@test.dev", null));

    importar(HEADER + "Ana;López;099999888;otra@test.dev;\nAna;López;099777666;;\n", false)
        .andExpect(jsonPath("$.ok").value(2));
  }

  // ── validaciones por fila ──────────────────────────────────────────

  @Test
  void datosInvalidos_sonError_sinRepetirDatosPersonales() throws Exception {
    String csv = HEADER
        + "Ana;López;12;;\n"                          // teléfono inválido
        + "Beto;Pérez;;;\n"                           // sin teléfono
        + "Carla;Gómez;099111222;carla@;\n"           // email inválido
        + "Dani;Ruiz;099333444;dani@sinpunto;\n"      // email sin dominio con punto
        + "Eva;Sosa;099555666;;31/02/1990\n"          // fecha inexistente
        + "Fede;Mora;099777888;;01/01/2999\n"         // fecha futura
        + ";Mora;099777889;;\n";                      // sin nombre

    JsonNode r = json(importar(csv, true).andExpect(status().isOk()));

    assertThat(r.get("errores").asInt()).isEqualTo(7);
    assertThat(r.get("insertados").asInt()).isZero();
    String cuerpo = r.toString();
    for (String pii : List.of("carla@", "dani@sinpunto", "099111222", "Carla", "Mora")) {
      assertThat(cuerpo).doesNotContain(pii);
    }
    assertThat(motivo(r, 0)).contains("Teléfono inválido");
    assertThat(motivo(r, 1)).contains("Falta el teléfono");
    assertThat(motivo(r, 2)).contains("Email inválido");
    assertThat(motivo(r, 4)).contains("Fecha de nacimiento inválida");
    assertThat(motivo(r, 5)).contains("futuro");
    assertThat(motivo(r, 6)).contains("Falta el nombre");
    assertThat(pacienteRepository.count()).isZero();
  }

  @Test
  void fechaDMSinCeros_seAcepta() throws Exception {
    importar(HEADER + "Ana;López;099111222;;5/3/1990\n", true).andExpect(jsonPath("$.insertados").value(1));
    assertThat(pacienteRepository.findAll().get(0).getFechaNacimiento()).hasToString("1990-03-05");
  }

  @Test
  void nombreCompletoDeUnaPalabra_esRevisarYNoSeInserta() throws Exception {
    String csv = "Clienta,Celular\nAna María López,099111222\nCher,099333444\n";

    importar(csv, true)
        .andExpect(jsonPath("$.ok").value(1))
        .andExpect(jsonPath("$.revisar").value(1))
        .andExpect(jsonPath("$.filas[1].estado").value("REVISAR"))
        .andExpect(jsonPath("$.insertados").value(1));

    Paciente p = pacienteRepository.findAll().get(0);
    assertThat(p.getNombre()).isEqualTo("Ana María");
    assertThat(p.getApellido()).isEqualTo("López");
  }

  @Test
  void archivoLatin1ConComa() throws Exception {
    byte[] latin = "Nombre,Apellido,Teléfono,Cumpleaños\nÁngela,Muñoz,099111222,01/02/1990\n"
        .getBytes(Charset.forName("windows-1252"));

    importarBytes(latin, true).andExpect(jsonPath("$.insertados").value(1));

    Paciente p = pacienteRepository.findAll().get(0);
    assertThat(p.getNombre()).isEqualTo("Ángela");
    assertThat(p.getApellido()).isEqualTo("Muñoz");
  }

  @Test
  void plantillaDeDocs_seImportaSinErrores() throws Exception {
    byte[] plantilla = Files.readAllBytes(Path.of("..", "docs", "plantilla-importacion-pacientes.csv"));

    importarBytes(plantilla, false)
        .andExpect(jsonPath("$.total").value(2))
        .andExpect(jsonPath("$.ok").value(2));
  }

  // ── archivo inválido / seguridad ───────────────────────────────────

  @Test
  void faltaColumnaObligatoria_400() throws Exception {
    importar("nombre;apellido;email\nAna;López;a@b.com\n", false)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("\"email\"")));
  }

  @Test
  void archivoVacio_400() throws Exception {
    importar("", false).andExpect(status().isBadRequest());
  }

  @Test
  void sinCampoArchivo_400() throws Exception {
    mockMvc.perform(multipart("/pacientes/importar").header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest());
  }

  @Test
  void sinToken_401() throws Exception {
    mockMvc.perform(multipart("/pacientes/importar").file(archivo(HEADER.getBytes(StandardCharsets.UTF_8))))
        .andExpect(status().isUnauthorized());
    assertThat(pacienteRepository.count()).isZero();
  }

  // ── helpers ────────────────────────────────────────────────────────

  private ResultActions importar(String csv, boolean confirmar) throws Exception {
    return importarBytes(csv.getBytes(StandardCharsets.UTF_8), confirmar);
  }

  private ResultActions importarBytes(byte[] contenido, boolean confirmar) throws Exception {
    return mockMvc.perform(multipart("/pacientes/importar")
        .file(archivo(contenido))
        .param("confirmar", String.valueOf(confirmar))
        .header("Authorization", "Bearer " + token));
  }

  private static MockMultipartFile archivo(byte[] contenido) {
    return new MockMultipartFile("archivo", "clientas.csv", "text/csv", contenido);
  }

  private JsonNode json(ResultActions r) throws Exception {
    return objectMapper.readTree(r.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
  }

  private static String estado(JsonNode r, int i) {
    return r.get("filas").get(i).get("estado").asText();
  }

  private static String motivo(JsonNode r, int i) {
    return r.get("filas").get(i).get("motivo").asText();
  }
}

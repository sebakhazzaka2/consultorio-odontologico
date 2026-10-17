package com.consultorio.mysql;

import static org.assertj.core.api.Assertions.assertThat;

import com.consultorio.dto.ImportacionResultado;
import com.consultorio.service.importacion.PacienteImportService;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;

/** Importación confirmada contra MySQL real: ENUM creado_por IMPORT, email único y NULL. */
class PacienteImportMySqlTest extends AbstractMySqlTest {

  @Autowired private JdbcTemplate jdbc;
  @Autowired private PacienteImportService importService;

  @BeforeEach
  @AfterEach
  void limpiar() {
    jdbc.update("DELETE FROM citas");
    jdbc.update("DELETE FROM pacientes");
  }

  @Test
  void confirmarInsertaConOrigenImportYEsIdempotente() {
    jdbc.update("INSERT INTO pacientes (nombre, apellido, telefono, email) VALUES "
        + "('Exis', 'Tente', '099000001', 'existe@test.dev')");
    String csv = "nombre;apellido;telefono;email;fecha_nacimiento\n"
        + "Ana;López;099 111 222;ANA@test.dev;01/02/1990\n"
        + "Beto;Pérez;099333444;;\n"
        + "Carla;Gómez;099555666;;\n"
        + "Otra;Persona;099777888;Existe@Test.dev;\n";

    ImportacionResultado primera = importService.importar(archivo(csv), true);
    assertThat(primera.insertados()).isEqualTo(3);
    assertThat(primera.yaExisten()).isEqualTo(1);

    assertThat(jdbc.queryForObject(
        "SELECT COUNT(*) FROM pacientes WHERE creado_por = 'IMPORT'", Integer.class)).isEqualTo(3);
    assertThat(jdbc.queryForObject(
        "SELECT COUNT(*) FROM pacientes WHERE creado_por = 'IMPORT' AND email IS NULL", Integer.class))
        .isEqualTo(2);
    assertThat(jdbc.queryForObject(
        "SELECT telefono FROM pacientes WHERE email = 'ana@test.dev'", String.class)).isEqualTo("099111222");

    ImportacionResultado segunda = importService.importar(archivo(csv), true);
    assertThat(segunda.insertados()).isZero();
    assertThat(segunda.yaExisten()).isEqualTo(4);
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM pacientes", Integer.class)).isEqualTo(4);
  }

  private static MockMultipartFile archivo(String csv) {
    return new MockMultipartFile("archivo", "c.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));
  }
}

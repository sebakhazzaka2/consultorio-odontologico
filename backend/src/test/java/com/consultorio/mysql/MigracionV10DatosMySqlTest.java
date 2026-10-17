package com.consultorio.mysql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * V10 sobre datos existentes, como en una instancia live: se migra un schema aparte
 * (mismo contenedor, otra base) hasta V9, se cargan emails "sucios" y recién ahí se
 * aplica V10. No toca la base que usa el contexto de Spring.
 *
 * <p>Se conecta como root (Testcontainers usa la misma password para root) porque el
 * usuario de test solo tiene permisos sobre su propia base.
 */
class MigracionV10DatosMySqlTest extends AbstractMySqlTest {

  private static final String SCHEMA = "consultorio_v10_datos";
  private static final String INSERT =
      "INSERT INTO pacientes (nombre, apellido, telefono, email) VALUES (?, ?, ?, ?)";

  private DriverManagerDataSource dataSource;
  private JdbcTemplate jdbc;

  @BeforeEach
  void crearSchemaVacio() {
    DriverManagerDataSource root = dataSource("");
    new JdbcTemplate(root).execute("DROP DATABASE IF EXISTS " + SCHEMA);
    new JdbcTemplate(root).execute("CREATE DATABASE " + SCHEMA);
    dataSource = dataSource(SCHEMA);
    jdbc = new JdbcTemplate(dataSource);
  }

  @AfterEach
  void borrarSchema() {
    new JdbcTemplate(dataSource("")).execute("DROP DATABASE IF EXISTS " + SCHEMA);
  }

  private static DriverManagerDataSource dataSource(String schema) {
    String url = "jdbc:mysql://" + MYSQL.getHost() + ":" + MYSQL.getMappedPort(3306) + "/" + schema
        + "?allowPublicKeyRetrieval=true&useSSL=false";
    return new DriverManagerDataSource(url, "root", MYSQL.getPassword());
  }

  private Flyway flyway(String target) {
    return Flyway.configure()
        .dataSource(dataSource)
        .locations("classpath:db/migration")
        .target(target)
        .load();
  }

  @Test
  void v10NormalizaEmailsExistentesYPermiteVariosNull() {
    flyway("9").migrate();
    jdbc.update(INSERT, "Ana", "Mayus", "099000001", " Ana@Example.COM ");
    jdbc.update(INSERT, "Beto", "Vacio", "099000002", "");
    jdbc.update(INSERT, "Caro", "Blanco", "099000003", "   ");
    jdbc.update(INSERT, "Dani", "Limpio", "099000004", "dani@example.com");

    flyway("10").migrate();

    List<Map<String, Object>> filas =
        jdbc.queryForList("SELECT apellido, email FROM pacientes ORDER BY id");
    assertThat(filas).extracting(f -> f.get("email"))
        .containsExactly("ana@example.com", null, null, "dani@example.com");
    String version = jdbc.queryForObject(
        "SELECT version FROM flyway_schema_history WHERE success = 1 ORDER BY installed_rank DESC LIMIT 1",
        String.class);
    assertThat(version).isEqualTo("10");
  }

  @Test
  void v10FallaSiHayDuplicadosTrasNormalizar() {
    // El prerrequisito documentado en V10: si existen, el UNIQUE no se puede crear.
    flyway("9").migrate();
    jdbc.update(INSERT, "Ana", "Uno", "099000001", "ana@example.com");
    jdbc.update(INSERT, "Ana", "Dos", "099000002", " ANA@example.com");

    assertThatThrownBy(() -> flyway("10").migrate())
        .isInstanceOf(FlywayException.class)
        .hasMessageContaining("uk_pacientes_email");
  }
}

package com.consultorio.mysql;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** Flyway aplicó todas las migraciones sobre MySQL real y Hibernate validó el schema. */
class MigracionesMySqlTest extends AbstractMySqlTest {

  @Autowired
  private JdbcTemplate jdbc;

  @Test
  void flywayLlegaAV9SinMigracionesFallidas() {
    List<Integer> versiones = jdbc.queryForList(
        "SELECT CAST(version AS UNSIGNED) FROM flyway_schema_history WHERE success = 1 AND version IS NOT NULL",
        Integer.class);
    assertThat(java.util.Collections.max(versiones)).isEqualTo(9);
    Integer fallidas = jdbc.queryForObject(
        "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 0", Integer.class);
    assertThat(fallidas).isZero();
  }
}

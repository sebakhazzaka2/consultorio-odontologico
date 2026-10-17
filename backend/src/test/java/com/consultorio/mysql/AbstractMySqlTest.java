package com.consultorio.mysql;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MySQLContainer;

/**
 * Base para tests contra MySQL 8 real (Testcontainers) con Flyway V1..V10 y
 * {@code ddl-auto=validate}, igual que prod. NO es transaccional: los tests commitean
 * de verdad (necesario para probar concurrencia entre threads), así que cada subclase
 * limpia lo que crea.
 *
 * <p>El contenedor es un singleton estático: arranca una vez por JVM y lo comparten
 * todas las subclases (Ryuk lo baja al terminar). Si no hay Docker, los tests se
 * saltean (no fallan).
 */
@SpringBootTest
public abstract class AbstractMySqlTest {

  static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
      .withDatabaseName("consultorio_test")
      .withUsername("test")
      .withPassword("test");

  static final boolean DOCKER_DISPONIBLE = DockerClientFactory.instance().isDockerAvailable();

  static {
    if (DOCKER_DISPONIBLE) {
      MYSQL.start();
    }
  }

  /**
   * Sin Docker: en CI (env CI definida) es un error, para que los tests de MySQL
   * nunca se salteen en silencio; en local se saltean con un mensaje claro.
   */
  @BeforeAll
  static void verificarDocker() {
    if (!DOCKER_DISPONIBLE) {
      if (System.getenv("CI") != null) {
        fail("Docker no disponible en CI: los tests de MySQL no pueden saltearse");
      }
      assumeTrue(false, "Docker no disponible: se saltean los tests de MySQL (Testcontainers)");
    }
  }

  @DynamicPropertySource
  static void mysqlProps(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
    registry.add("spring.datasource.username", MYSQL::getUsername);
    registry.add("spring.datasource.password", MYSQL::getPassword);
    registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
    registry.add("spring.flyway.enabled", () -> "true");
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
  }
}

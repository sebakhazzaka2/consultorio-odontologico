package com.consultorio.mysql;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base para tests contra MySQL 8 real (Testcontainers) con Flyway V1..V9 y
 * {@code ddl-auto=validate}, igual que prod. NO es transaccional: los tests commitean
 * de verdad (necesario para probar concurrencia entre threads), así que cada subclase
 * limpia lo que crea.
 *
 * <p>El contenedor es un singleton estático: arranca una vez por JVM y lo comparten
 * todas las subclases (Ryuk lo baja al terminar). Si no hay Docker, los tests se
 * saltean (no fallan).
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
public abstract class AbstractMySqlTest {

  static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
      .withDatabaseName("consultorio_test")
      .withUsername("test")
      .withPassword("test");

  static {
    // Solo arrancar si hay Docker; si no, @Testcontainers saltea la clase.
    if (DockerClientFactory.instance().isDockerAvailable()) {
      MYSQL.start();
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

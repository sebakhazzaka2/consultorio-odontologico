package com.consultorio.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class StartupEnvValidatorTest {

  private static final String[] VALID_BASE = {
    "jwt.secret=test-secret-key-with-at-least-32-characters",
    "app.cors.allowed-origins=https://example.com",
    "app.admin.email=admin@example.com",
    "app.admin.password=una-password-segura"
  };

  private static final String[] VALID_CLINIC = {
    "clinic.nombre=Clinica Test",
    "clinic.direccion=Calle Falsa 123",
    "clinic.telefono=+59890000000",
    "clinic.whatsapp=59890000000",
    "clinic.email=test@example.com"
  };

  private final ApplicationContextRunner prodRunner = new ApplicationContextRunner()
      .withUserConfiguration(StartupEnvValidator.class)
      .withPropertyValues("spring.profiles.active=prod")
      .withPropertyValues(VALID_BASE);

  @Test
  void prodSinClinicVars_fallaElArranqueYListaLasCinco() {
    prodRunner
        .withPropertyValues("clinic.direccion=   ")
        .run(context -> {
          assertThat(context).hasFailed();
          assertThat(context.getStartupFailure())
              .isInstanceOf(IllegalStateException.class)
              .hasMessageContaining("CLINIC_NOMBRE is not set")
              .hasMessageContaining("CLINIC_DIRECCION is not set")
              .hasMessageContaining("CLINIC_TELEFONO is not set")
              .hasMessageContaining("CLINIC_WHATSAPP is not set")
              .hasMessageContaining("CLINIC_EMAIL is not set");
        });
  }

  @Test
  void prodConTodasLasClinicVars_arranca() {
    prodRunner
        .withPropertyValues(VALID_CLINIC)
        .run(context -> {
          assertThat(context).hasNotFailed();
          assertThat(context).hasSingleBean(StartupEnvValidator.class);
        });
  }

  @Test
  void sinPerfilProd_elValidadorNoSeCarga() {
    new ApplicationContextRunner()
        .withUserConfiguration(StartupEnvValidator.class)
        .run(context -> {
          assertThat(context).hasNotFailed();
          assertThat(context).doesNotHaveBean(StartupEnvValidator.class);
        });
  }
}

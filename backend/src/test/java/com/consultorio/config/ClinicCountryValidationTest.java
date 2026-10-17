package com.consultorio.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/** Valida CLINIC_COUNTRY al bindear ClinicProperties (todos los perfiles). */
class ClinicCountryValidationTest {

  @Configuration
  @EnableConfigurationProperties(ClinicProperties.class)
  static class Config {}

  private final ApplicationContextRunner runner = new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
      .withUserConfiguration(Config.class);

  @Test
  void sinVariableUsaUruguay() {
    runner.run(ctx -> {
      assertThat(ctx).hasNotFailed();
      assertThat(ctx.getBean(ClinicProperties.class).getCountry()).isEqualTo("598");
    });
  }

  @Test
  void vacioUsaUruguay() {
    runner.withPropertyValues("clinic.country=")
        .run(ctx -> assertThat(ctx.getBean(ClinicProperties.class).getCountry()).isEqualTo("598"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"598", "55", " 55 "})
  void paisesSoportados(String valor) {
    runner.withPropertyValues("clinic.country=" + valor)
        .run(ctx -> {
          assertThat(ctx).hasNotFailed();
          assertThat(ctx.getBean(ClinicProperties.class).getCountry()).isEqualTo(valor.trim());
        });
  }

  @ParameterizedTest
  @ValueSource(strings = {"54", "+598", "UY", "5980", "1"})
  void paisNoSoportadoHaceFallarElArranque(String valor) {
    runner.withPropertyValues("clinic.country=" + valor)
        .run(ctx -> {
          assertThat(ctx).hasFailed();
          StringBuilder sb = new StringBuilder();
          for (Throwable c = ctx.getStartupFailure(); c != null; c = c.getCause()) {
            sb.append(c.getMessage()).append('\n');
          }
          assertThat(sb.toString()).contains("CLINIC_COUNTRY");
        });
  }
}

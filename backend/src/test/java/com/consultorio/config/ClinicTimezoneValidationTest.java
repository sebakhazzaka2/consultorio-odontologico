package com.consultorio.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/** CLINIC_TIMEZONE se valida al arrancar (todos los perfiles) y arma el Clock. */
class ClinicTimezoneValidationTest {

  @Configuration
  @EnableConfigurationProperties(ClinicProperties.class)
  @Import(TimeConfig.class)
  static class Config {}

  private final ApplicationContextRunner runner = new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
      .withUserConfiguration(Config.class);

  @Test
  void sinVariable_usaMontevideoYElClockTieneEsaZona() {
    runner.run(ctx -> {
      assertThat(ctx).hasNotFailed();
      assertThat(ctx.getBean(Clock.class).getZone()).isEqualTo(ZoneId.of("America/Montevideo"));
    });
  }

  @Test
  void zonaValida_seUsa() {
    runner.withPropertyValues("clinic.timezone=America/Sao_Paulo")
        .run(ctx -> assertThat(ctx.getBean(Clock.class).getZone()).isEqualTo(ZoneId.of("America/Sao_Paulo")));
  }

  @Test
  void zonaInvalida_fallaElArranque() {
    runner.withPropertyValues("clinic.timezone=America/Montevide0")
        .run(ctx -> {
          assertThat(ctx).hasFailed();
          StringBuilder sb = new StringBuilder();
          for (Throwable c = ctx.getStartupFailure(); c != null; c = c.getCause()) {
            sb.append(c.getMessage()).append('\n');
          }
          assertThat(sb.toString()).contains("CLINIC_TIMEZONE");
        });
  }
}

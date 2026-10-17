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

/** Valida BRAND_* al bindear ClinicProperties (aplica en todos los perfiles, no solo prod). */
class ClinicBrandingValidationTest {

  @Configuration
  @EnableConfigurationProperties(ClinicProperties.class)
  static class Config {}

  private final ApplicationContextRunner runner = new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
      .withUserConfiguration(Config.class);

  @Test
  void sinBrandingArrancaConDefaultsVacios() {
    runner.run(ctx -> {
      assertThat(ctx).hasNotFailed();
      ClinicProperties.Branding b = ctx.getBean(ClinicProperties.class).getBranding();
      assertThat(b.getPrimary()).isEmpty();
      assertThat(b.getLogoUrl()).isEmpty();
    });
  }

  @Test
  void valoresVaciosExplicitosSonValidos() {
    runner.withPropertyValues("clinic.branding.primary=", "clinic.branding.logo-url=")
        .run(ctx -> assertThat(ctx).hasNotFailed());
  }

  @Test
  void brandingValidoSeBindea() {
    runner.withPropertyValues(
            "clinic.branding.primary=#8A2BE2",
            "clinic.branding.primary-hover=#6a1fb5",
            "clinic.branding.accent=#FF69B4",
            "clinic.branding.logo-url=https://cdn.example.com/logo.png")
        .run(ctx -> {
          assertThat(ctx).hasNotFailed();
          ClinicProperties.Branding b = ctx.getBean(ClinicProperties.class).getBranding();
          assertThat(b.getPrimary()).isEqualTo("#8A2BE2");
          assertThat(b.getAccent()).isEqualTo("#FF69B4");
          assertThat(b.getLogoUrl()).isEqualTo("https://cdn.example.com/logo.png");
        });
  }

  @Test
  void logoRelativoDeUploadsEsValido() {
    runner.withPropertyValues("clinic.branding.logo-url=/api/uploads/branding/logo.svg")
        .run(ctx -> assertThat(ctx).hasNotFailed());
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "red", "#fff", "#GGGGGG", "#12345", "#1234567", "8A2BE2",
      "#000000;background:url(x)", "#000000}</style><script>", "expression(alert(1))"
  })
  void colorInvalidoHaceFallarElArranque(String valor) {
    runner.withPropertyValues("clinic.branding.primary=" + valor)
        .run(ctx -> {
          assertThat(ctx).hasFailed();
          assertThat(messages(ctx.getStartupFailure())).contains("BRAND_PRIMARY");
        });
  }

  @Test
  void hoverYAccentInvalidosTambienFallan() {
    runner.withPropertyValues("clinic.branding.primary-hover=azul")
        .run(ctx -> assertThat(ctx).hasFailed());
    runner.withPropertyValues("clinic.branding.accent=#12")
        .run(ctx -> assertThat(ctx).hasFailed());
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "javascript:alert(1)", "http://example.com/logo.png", "data:image/png;base64,AAAA",
      "//evil.com/logo.png", "/api/other/logo.png", "/api/uploads/../../etc/passwd",
      "https://x.com/a\"onerror=\"alert(1)", "https://x.com/a b.png", "ftp://x.com/a.png"
  })
  void logoInvalidoHaceFallarElArranque(String valor) {
    runner.withPropertyValues("clinic.branding.logo-url=" + valor)
        .run(ctx -> {
          assertThat(ctx).hasFailed();
          assertThat(messages(ctx.getStartupFailure())).contains("BRAND_LOGO_URL");
        });
  }

  private static String messages(Throwable t) {
    StringBuilder sb = new StringBuilder();
    for (Throwable c = t; c != null; c = c.getCause()) {
      sb.append(c.getMessage()).append('\n');
    }
    return sb.toString();
  }
}

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

  // --- Emails (FEATURE_EMAIL_NOTIFICATIONS) ---

  @Test
  void emailsApagados_noExigeMail() {
    prodRunner
        .withPropertyValues(VALID_CLINIC)
        .withPropertyValues("clinic.flags.email-notifications=false")
        .run(context -> assertThat(context).hasNotFailed());
  }

  @Test
  void emailsPrendidosConAuth_sinCredencialesNiUrl_fallaYListaTodo() {
    prodRunner
        .withPropertyValues(VALID_CLINIC)
        .withPropertyValues("clinic.flags.email-notifications=true", "spring.mail.host=smtp-relay.brevo.com")
        .run(context -> {
          assertThat(context).hasFailed();
          assertThat(context.getStartupFailure())
              .hasMessageContaining("MAIL_USERNAME is not set")
              .hasMessageContaining("MAIL_PASSWORD is not set")
              .hasMessageContaining("APP_BASE_URL or DOMAIN");
        });
  }

  @Test
  void emailsPrendidosConAuth_credencialesYDomain_arranca() {
    prodRunner
        .withPropertyValues(VALID_CLINIC)
        .withPropertyValues(
            "clinic.flags.email-notifications=true",
            "spring.mail.host=smtp-relay.brevo.com",
            "spring.mail.username=usuario",
            "spring.mail.password=clave",
            "notifications.domain=clinica.turnosuy.com")
        .run(context -> assertThat(context).hasNotFailed());
  }

  @Test
  void emailsPrendidosSinAuth_noExigeCredenciales() {
    // Relay sin autenticación (p. ej. Mailpit): MAIL_SMTP_AUTH=false.
    prodRunner
        .withPropertyValues(VALID_CLINIC)
        .withPropertyValues(
            "clinic.flags.email-notifications=true",
            "spring.mail.host=mailpit",
            "spring.mail.properties.mail.smtp.auth=false",
            "notifications.base-url=http://localhost:4200")
        .run(context -> assertThat(context).hasNotFailed());
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

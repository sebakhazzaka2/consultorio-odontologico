package com.consultorio.config;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class StartupEnvValidator implements ApplicationListener<ContextRefreshedEvent> {

  private static final Logger log = LoggerFactory.getLogger(StartupEnvValidator.class);

  private static final int JWT_SECRET_MIN_LENGTH = 32;
  private static final String INSECURE_ADMIN_PASSWORD = "admin123";

  @Value("${jwt.secret}")
  private String jwtSecret;

  @Value("${app.cors.allowed-origins}")
  private String corsAllowedOrigins;

  @Value("${app.admin.email}")
  private String adminEmail;

  @Value("${app.admin.password}")
  private String adminPassword;

  @Value("${clinic.nombre:}")
  private String clinicNombre;

  @Value("${clinic.direccion:}")
  private String clinicDireccion;

  @Value("${clinic.telefono:}")
  private String clinicTelefono;

  @Value("${clinic.whatsapp:}")
  private String clinicWhatsapp;

  @Value("${clinic.email:}")
  private String clinicEmail;

  private boolean validated = false;

  @Override
  public void onApplicationEvent(ContextRefreshedEvent event) {
    if (validated) return;
    validated = true;

    List<String> errors = new ArrayList<>();

    if (isBlank(jwtSecret)) {
      errors.add("JWT_SECRET is not set");
    } else if (jwtSecret.length() < JWT_SECRET_MIN_LENGTH) {
      errors.add("JWT_SECRET must be at least " + JWT_SECRET_MIN_LENGTH + " characters (got " + jwtSecret.length() + ")");
    }

    if (isBlank(corsAllowedOrigins)) {
      errors.add("CORS_ALLOWED_ORIGINS is not set");
    }

    if (isBlank(adminEmail)) {
      errors.add("ADMIN_EMAIL is not set");
    }

    if (isBlank(adminPassword)) {
      errors.add("ADMIN_PASSWORD is not set");
    } else if (INSECURE_ADMIN_PASSWORD.equals(adminPassword)) {
      errors.add("ADMIN_PASSWORD must not be the default development value");
    }

    // Datos de marca: sin defaults de cliente, cada instancia los define en su .env.prod
    requireNotBlank(errors, clinicNombre, "CLINIC_NOMBRE");
    requireNotBlank(errors, clinicDireccion, "CLINIC_DIRECCION");
    requireNotBlank(errors, clinicTelefono, "CLINIC_TELEFONO");
    requireNotBlank(errors, clinicWhatsapp, "CLINIC_WHATSAPP");
    requireNotBlank(errors, clinicEmail, "CLINIC_EMAIL");

    if (!errors.isEmpty()) {
      log.error("=== STARTUP FAILED: missing or insecure environment variables ===");
      errors.forEach(e -> log.error("  - {}", e));
      log.error("Set the required variables and restart the application.");
      // Lanzar (en vez de System.exit) aborta el refresh: SpringApplication.run falla y
      // el proceso termina con código != 0 igual, pero el validador se puede testear.
      throw new IllegalStateException("Startup environment validation failed: " + String.join("; ", errors));
    }

    log.info("Environment validation passed.");
  }

  private void requireNotBlank(List<String> errors, String value, String envName) {
    if (isBlank(value)) {
      errors.add(envName + " is not set");
    }
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}

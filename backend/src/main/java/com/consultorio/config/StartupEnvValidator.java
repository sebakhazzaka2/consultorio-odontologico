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

    if (!errors.isEmpty()) {
      log.error("=== STARTUP FAILED: missing or insecure environment variables ===");
      errors.forEach(e -> log.error("  - {}", e));
      log.error("Set the required variables and restart the application.");
      System.exit(1);
    }

    log.info("Environment validation passed.");
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}

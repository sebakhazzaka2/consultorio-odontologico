package com.consultorio;

import java.util.Arrays;
import java.util.List;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ConsultorioApplication {

  public static void main(String[] args) {
    String activeProfile = System.getenv().getOrDefault("SPRING_PROFILES_ACTIVE", "");
    if (activeProfile.contains("prod")) {
      validateProdEnvVars();
    }
    SpringApplication.run(ConsultorioApplication.class, args);
  }

  private static void validateProdEnvVars() {
    List<String> missing = Arrays.stream(new String[]{"JWT_SECRET", "DB_PASSWORD", "CORS_ALLOWED_ORIGINS"})
        .filter(v -> {
          String val = System.getenv(v);
          return val == null || val.isBlank();
        })
        .toList();
    if (!missing.isEmpty()) {
      System.err.println("ERROR: Faltan variables de entorno requeridas en prod: " + missing);
      System.exit(1);
    }
  }
}


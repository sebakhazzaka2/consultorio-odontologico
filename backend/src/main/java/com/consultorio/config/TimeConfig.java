package com.consultorio.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Reloj de la instancia en la zona de la clínica (CLINIC_TIMEZONE). La JVM de prod corre
 * en UTC y las citas se guardan en hora local de la clínica: el código nuevo obtiene
 * "ahora" con {@code LocalDateTime.now(clock)} / {@code clock.instant()}, nunca con
 * {@code now()} sin argumentos. Los tests lo reemplazan por un {@code Clock.fixed}.
 */
@Configuration
public class TimeConfig {

  @Bean
  public Clock clock(ClinicProperties clinic) {
    return Clock.system(clinic.zoneId());
  }
}

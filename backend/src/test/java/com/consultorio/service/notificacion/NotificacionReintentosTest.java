package com.consultorio.service.notificacion;

import static org.assertj.core.api.Assertions.assertThat;

import com.consultorio.model.Cita;
import com.consultorio.model.CitaEstado;
import com.consultorio.model.EstadoNotificacion;
import com.consultorio.model.Notificacion;
import com.consultorio.model.Paciente;
import com.consultorio.model.TipoNotificacion;
import com.consultorio.repository.CitaRepository;
import com.consultorio.repository.NotificacionRepository;
import com.consultorio.repository.PacienteRepository;
import java.net.ServerSocket;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * SMTP caído (puerto sin servidor): el despachador agenda reintentos con backoff y al
 * quinto fallo deja la notificación FALLIDA, con un error sin direcciones de email.
 */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:notif_fallo;DB_CLOSE_DELAY=-1;MODE=MySQL;NON_KEYWORDS=VALUE",
    "clinic.flags.email-notifications=true",
    "clinic.nombre=Clínica Test",
    "clinic.email=recepcion@test.uy",
    "spring.mail.host=127.0.0.1",
    "spring.mail.username=usuario",
    "spring.mail.password=clave",
    "spring.mail.properties.mail.smtp.connectiontimeout=2000",
    "spring.mail.properties.mail.smtp.timeout=2000"
})
class NotificacionReintentosTest {

  @DynamicPropertySource
  static void puertoCerrado(DynamicPropertyRegistry registry) throws Exception {
    // Un puerto libre y sin nadie escuchando: la conexión se rechaza al instante.
    int puerto;
    try (ServerSocket s = new ServerSocket(0)) {
      puerto = s.getLocalPort();
    }
    int p = puerto;
    registry.add("spring.mail.port", () -> p);
  }

  @Autowired private NotificacionService service;
  @Autowired private NotificacionDispatcher dispatcher;
  @Autowired private NotificacionRepository notificacionRepository;
  @Autowired private CitaRepository citaRepository;
  @Autowired private PacienteRepository pacienteRepository;

  @AfterEach
  void limpiar() {
    notificacionRepository.deleteAll();
    citaRepository.deleteAll();
    pacienteRepository.deleteAll();
  }

  @Test
  void reintentaConBackoffYQuedaFallidaAlQuintoIntento() {
    Paciente p = pacienteRepository.save(
        new Paciente("Ana", "Pérez", "099123456", "ana.perez@example.com", null));
    Cita c = new Cita();
    c.setPaciente(p);
    c.setFechaHoraInicio(LocalDateTime.now().plusDays(3).withSecond(0).withNano(0));
    c.setDuracionMinutos(30);
    c.setEstado(CitaEstado.CONFIRMADA);
    citaRepository.save(c);
    service.encolar(TipoNotificacion.CONFIRMACION, c);

    Duration[] esperadas = {
        Duration.ofMinutes(1), Duration.ofMinutes(5), Duration.ofMinutes(30), Duration.ofHours(2)};
    for (int intento = 1; intento <= 4; intento++) {
      Instant antes = Instant.now();
      assertThat(dispatcher.despachar()).isEqualTo(1);
      Notificacion n = notificacionRepository.findAll().get(0);
      assertThat(n.getEstado()).isEqualTo(EstadoNotificacion.PENDIENTE);
      assertThat(n.getIntentos()).isEqualTo(intento);
      assertThat(n.getUltimoError()).isNotBlank().doesNotContain("@");
      Duration espera = Duration.between(antes, n.getProximoIntentoAt());
      assertThat(espera).isBetween(esperadas[intento - 1].minusSeconds(5), esperadas[intento - 1].plusSeconds(5));
      // Todavía no venció: otra corrida no la toma.
      assertThat(dispatcher.despachar()).isZero();
      vencer(n);
    }

    assertThat(dispatcher.despachar()).isEqualTo(1);
    Notificacion n = notificacionRepository.findAll().get(0);
    assertThat(n.getEstado()).isEqualTo(EstadoNotificacion.FALLIDA);
    assertThat(n.getIntentos()).isEqualTo(5);
    assertThat(n.getUltimoError()).hasSizeLessThanOrEqualTo(500).doesNotContain("@");
    vencer(n);
    assertThat(dispatcher.despachar()).isZero();
  }

  private void vencer(Notificacion n) {
    Notificacion actual = notificacionRepository.findById(n.getId()).orElseThrow();
    actual.setProximoIntentoAt(Instant.now().minusSeconds(1));
    notificacionRepository.save(actual);
  }
}

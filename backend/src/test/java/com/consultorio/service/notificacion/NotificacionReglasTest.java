package com.consultorio.service.notificacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.consultorio.model.TipoNotificacion;
import jakarta.mail.MessagingException;
import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/** Reglas puras del núcleo: claves de dedupe, backoff, saneamiento y URLs públicas. */
class NotificacionReglasTest {

  private static final LocalDateTime INICIO = LocalDateTime.of(2026, 10, 20, 10, 30);

  // --- Claves de dedupe ---

  @Test
  void clavesSinInicio() {
    assertThat(ClavesDedupe.deCita(TipoNotificacion.SOLICITUD, 12L, INICIO)).isEqualTo("SOLICITUD:12");
    assertThat(ClavesDedupe.deCita(TipoNotificacion.AVISO_CLINICA, 12L, INICIO)).isEqualTo("AVISO_CLINICA:12");
    assertThat(ClavesDedupe.deCita(TipoNotificacion.CANCELACION, 12L, INICIO)).isEqualTo("CANCELACION:12");
  }

  @Test
  void clavesConInicio() {
    assertThat(ClavesDedupe.deCita(TipoNotificacion.CONFIRMACION, 12L, INICIO))
        .isEqualTo("CONFIRMACION:12:20261020T1030");
    assertThat(ClavesDedupe.deCita(TipoNotificacion.REAGENDADA, 12L, INICIO))
        .isEqualTo("REAGENDADA:12:20261020T1030");
    assertThat(ClavesDedupe.deCita(TipoNotificacion.RECORDATORIO_24H, 12L, INICIO))
        .isEqualTo("RECORDATORIO_24H:12:20261020T1030");
  }

  @Test
  void reagendarCambiaLaClaveYLosSegundosNoImportan() {
    String antes = ClavesDedupe.deCita(TipoNotificacion.RECORDATORIO_24H, 12L, INICIO);
    String despues = ClavesDedupe.deCita(TipoNotificacion.RECORDATORIO_24H, 12L, INICIO.plusHours(1));
    assertThat(antes).isNotEqualTo(despues);
    assertThat(ClavesDedupe.deCita(TipoNotificacion.CONFIRMACION, 12L, INICIO.withSecond(42)))
        .isEqualTo("CONFIRMACION:12:20261020T1030");
  }

  @Test
  void claveReset() {
    assertThat(ClavesDedupe.deReset(7L)).isEqualTo("RESET_PASSWORD:7");
    assertThatThrownBy(() -> ClavesDedupe.deCita(TipoNotificacion.RESET_PASSWORD, 7L, INICIO))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> ClavesDedupe.deCita(TipoNotificacion.SOLICITUD, null, INICIO))
        .isInstanceOf(IllegalArgumentException.class);
  }

  // --- Backoff ---

  @Test
  void backoffPorIntentoFallido() {
    assertThat(PoliticaReintentos.esperaTras(1)).isEqualTo(Duration.ofMinutes(1));
    assertThat(PoliticaReintentos.esperaTras(2)).isEqualTo(Duration.ofMinutes(5));
    assertThat(PoliticaReintentos.esperaTras(3)).isEqualTo(Duration.ofMinutes(30));
    assertThat(PoliticaReintentos.esperaTras(4)).isEqualTo(Duration.ofHours(2));
    assertThat(PoliticaReintentos.esperaTras(5)).isEqualTo(Duration.ofHours(6));
    assertThat(PoliticaReintentos.esperaTras(9)).isEqualTo(Duration.ofHours(6));
  }

  @Test
  void agotadoAlQuintoIntento() {
    assertThat(PoliticaReintentos.agotado(4)).isFalse();
    assertThat(PoliticaReintentos.agotado(5)).isTrue();
  }

  @Test
  void saneaEmailsYTrunca() {
    String error = PoliticaReintentos.sanear(new RuntimeException("envoltorio",
        new MessagingException("550 Invalid recipient: Ana.Perez+x@Example.com\nrejected")));
    assertThat(error).doesNotContain("@").contains("[email]").doesNotContain("\n")
        .startsWith("RuntimeException: envoltorio <- MessagingException: 550");
    assertThat(PoliticaReintentos.sanear(new RuntimeException("x".repeat(2000)))).hasSize(500);
  }

  // --- URLs públicas ---

  @Test
  void baseUrlPrioridad() {
    assertThat(UrlsPublicas.resolverBaseUrl("https://turnos.ejemplo.com/", "otro.com"))
        .isEqualTo("https://turnos.ejemplo.com");
    assertThat(UrlsPublicas.resolverBaseUrl("", "neodental.turnosuy.com"))
        .isEqualTo("https://neodental.turnosuy.com");
    assertThat(UrlsPublicas.resolverBaseUrl(null, " ")).isEqualTo("http://localhost:4200");
  }

  @Test
  void logoAbsoluto() {
    String base = "https://x.turnosuy.com";
    assertThat(UrlsPublicas.logoAbsoluto(base, "/api/uploads/logo.png"))
        .isEqualTo("https://x.turnosuy.com/api/uploads/logo.png");
    assertThat(UrlsPublicas.logoAbsoluto(base, "https://cdn.ejemplo.com/l.png"))
        .isEqualTo("https://cdn.ejemplo.com/l.png");
    assertThat(UrlsPublicas.logoAbsoluto(base, "")).isNull();
    assertThat(UrlsPublicas.logoAbsoluto(base, null)).isNull();
  }

  @Test
  void whatsappUrl() {
    assertThat(UrlsPublicas.whatsappUrl("+598 99 123 456")).isEqualTo("https://wa.me/59899123456");
    assertThat(UrlsPublicas.whatsappUrl("")).isNull();
  }
}

package com.consultorio.service.notificacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.consultorio.config.ClinicProperties;
import com.consultorio.model.Cita;
import com.consultorio.model.TipoNotificacion;
import com.consultorio.repository.NotificacionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;

class NotificacionServiceTest {

  private static final Instant AHORA = Instant.parse("2026-10-17T15:00:00Z");

  private NotificacionRepository repo;
  private ClinicProperties clinic;
  private NotificacionService service;
  private Cita cita;

  @BeforeEach
  void setUp() {
    repo = mock(NotificacionRepository.class);
    clinic = new ClinicProperties();
    Clock clock = Clock.fixed(AHORA, ZoneId.of("America/Montevideo"));
    service = new NotificacionService(repo, clinic, clock, mock(MensajeFactory.class),
        mock(EmailChannel.class), mock(PlatformTransactionManager.class));
    cita = new Cita();
    cita.setId(5L);
    cita.setFechaHoraInicio(LocalDateTime.of(2026, 10, 20, 9, 0));
  }

  @Test
  void flagApagado_noEncolaNada() {
    assertThat(service.encolar(TipoNotificacion.SOLICITUD, cita)).isFalse();
    verifyNoInteractions(repo);
  }

  @Test
  void flagPrendido_insertaPendienteConClaveYDestinatario() {
    clinic.getFlags().setEmailNotifications(true);
    assertThat(service.encolar(TipoNotificacion.AVISO_CLINICA, cita)).isTrue();
    verify(repo).insertarPendienteSiNoExiste(eq("AVISO_CLINICA"), eq("EMAIL"), eq("AVISO_CLINICA:5"),
        eq(5L), isNull(), eq("CLINICA"), eq(AHORA), eq(AHORA));
  }

  @Test
  void claveExistente_seIgnoraSinInsertar() {
    clinic.getFlags().setEmailNotifications(true);
    when(repo.existsByClaveDedupe("CONFIRMACION:5:20261020T0900")).thenReturn(true);
    assertThat(service.encolar(TipoNotificacion.CONFIRMACION, cita)).isFalse();
    verify(repo, never()).insertarPendienteSiNoExiste(anyString(), anyString(), anyString(),
        any(), any(), anyString(), any(), any());
  }

  @Test
  void resetNoSeEncola() {
    clinic.getFlags().setEmailNotifications(true);
    assertThatThrownBy(() -> service.encolar(TipoNotificacion.RESET_PASSWORD, cita))
        .isInstanceOf(IllegalArgumentException.class);
  }
}

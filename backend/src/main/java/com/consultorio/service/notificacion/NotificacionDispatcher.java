package com.consultorio.service.notificacion;

import com.consultorio.config.ClinicProperties;
import com.consultorio.config.NotificationProperties;
import com.consultorio.model.CanalNotificacion;
import com.consultorio.model.Cita;
import com.consultorio.model.CitaEstado;
import com.consultorio.model.EstadoNotificacion;
import com.consultorio.model.Notificacion;
import com.consultorio.model.Paciente;
import com.consultorio.model.TipoNotificacion;
import com.consultorio.repository.CitaRepository;
import com.consultorio.repository.NotificacionRepository;
import io.sentry.Sentry;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Despachador del outbox. Cada {@code notifications.dispatch-delay-ms} (60 s):
 *
 * <ol>
 *   <li><b>Toma</b> un lote de PENDIENTE vencidas en una transacción corta:
 *       {@code SELECT ... FOR UPDATE SKIP LOCKED} + suma el intento y corre
 *       {@code proximo_intento_at} 15 min (lease). Dos corridas solapadas (o dos réplicas)
 *       nunca toman la misma fila.
 *   <li><b>Resuelve y envía</b> cada una FUERA de transacción (el SMTP puede tardar):
 *       destinatario y datos se leen en ese momento; si ya no corresponde enviarla queda
 *       DESCARTADA con el motivo.
 *   <li><b>Registra</b> el resultado: ENVIADA, reintento con backoff, o FALLIDA al 5.º fallo
 *       (+ Sentry).
 * </ol>
 *
 * Logs y {@code ultimo_error}: solo ids, tipos y errores saneados; nunca emails ni nombres.
 */
@Component
public class NotificacionDispatcher {

  private static final Logger log = LoggerFactory.getLogger(NotificacionDispatcher.class);

  private final NotificacionRepository repository;
  private final CitaRepository citaRepository;
  private final ClinicProperties clinic;
  private final NotificationProperties props;
  private final MensajeFactory mensajes;
  private final List<NotificationChannel> canales;
  private final Clock clock;
  private final TransactionTemplate tx;

  public NotificacionDispatcher(
      NotificacionRepository repository,
      CitaRepository citaRepository,
      ClinicProperties clinic,
      NotificationProperties props,
      MensajeFactory mensajes,
      List<NotificationChannel> canales,
      Clock clock,
      PlatformTransactionManager txManager) {
    this.repository = repository;
    this.citaRepository = citaRepository;
    this.clinic = clinic;
    this.props = props;
    this.mensajes = mensajes;
    this.canales = List.copyOf(canales);
    this.clock = clock;
    this.tx = new TransactionTemplate(txManager);
  }

  @Scheduled(
      fixedDelayString = "${notifications.dispatch-delay-ms:60000}",
      initialDelayString = "${notifications.dispatch-initial-delay-ms:30000}")
  public void ejecutarProgramado() {
    if (!props.isDispatcherEnabled()) {
      return;
    }
    try {
      despachar();
    } catch (RuntimeException e) {
      // Un error acá no llega solo a Sentry (sin sentry-logback): se reporta explícito.
      log.error("Error en el despachador de notificaciones: {}", PoliticaReintentos.sanear(e));
      Sentry.captureException(new IllegalStateException(
          "Despachador de notificaciones: " + PoliticaReintentos.sanear(e)));
    }
  }

  /** Procesa un lote. Público para tests y para invocarlo a mano. Devuelve cuántas tomó. */
  public int despachar() {
    List<Long> ids = tomarLote();
    for (Long id : ids) {
      procesar(id);
    }
    return ids.size();
  }

  private List<Long> tomarLote() {
    Instant ahora = clock.instant();
    List<Long> ids = tx.execute(s -> {
      List<Long> bloqueadas = repository.bloquearVencidas(ahora, props.getBatchSize());
      if (!bloqueadas.isEmpty()) {
        repository.tomar(bloqueadas, ahora.plus(PoliticaReintentos.LEASE));
      }
      return bloqueadas;
    });
    return ids == null ? List.of() : ids;
  }

  private void procesar(Long id) {
    Optional<Notificacion> encontrada = repository.findById(id);
    if (encontrada.isEmpty()) {
      return;
    }
    Notificacion n = encontrada.get();
    Resolucion r;
    try {
      r = resolver(n);
    } catch (RuntimeException e) {
      // Error armando el mensaje (plantilla, datos): se trata como fallo reintentable.
      registrarFallo(n, e);
      return;
    }
    if (r.motivoDescarte() != null) {
      n.setEstado(EstadoNotificacion.DESCARTADA);
      n.setUltimoError(r.motivoDescarte());
      repository.save(n);
      log.info("Notificación descartada — id: {}, tipo: {}, motivo: {}", n.getId(), n.getTipo(), r.motivoDescarte());
      return;
    }
    NotificationChannel canal = canalPara(n.getCanal());
    try {
      if (canal == null) {
        throw new EnvioNotificacionException("Canal no disponible: " + n.getCanal());
      }
      canal.enviar(n, r.mensaje());
      n.setEstado(EstadoNotificacion.ENVIADA);
      n.setEnviadaAt(clock.instant());
      n.setUltimoError(null);
      repository.save(n);
      log.info("Notificación enviada — id: {}, tipo: {}, intento: {}", n.getId(), n.getTipo(), n.getIntentos());
    } catch (RuntimeException e) {
      registrarFallo(n, e);
    }
  }

  private NotificationChannel canalPara(CanalNotificacion canal) {
    for (NotificationChannel c : canales) {
      if (c.canal() == canal) {
        return c;
      }
    }
    return null;
  }

  /** {@code intentos} ya incluye el intento actual (se sumó al tomar la fila). */
  private void registrarFallo(Notificacion n, RuntimeException e) {
    String error = PoliticaReintentos.sanear(e);
    n.setUltimoError(error);
    if (PoliticaReintentos.agotado(n.getIntentos())) {
      n.setEstado(EstadoNotificacion.FALLIDA);
      repository.save(n);
      log.error("Notificación FALLIDA tras {} intentos — id: {}, tipo: {}, error: {}",
          n.getIntentos(), n.getId(), n.getTipo(), error);
      Sentry.captureException(new EnvioNotificacionException(
          "Notificación " + n.getId() + " (" + n.getTipo() + ") FALLIDA tras " + n.getIntentos()
              + " intentos: " + error));
    } else {
      n.setProximoIntentoAt(clock.instant().plus(PoliticaReintentos.esperaTras(n.getIntentos())));
      repository.save(n);
      log.warn("Notificación con error, se reintenta — id: {}, tipo: {}, intento: {}, error: {}",
          n.getId(), n.getTipo(), n.getIntentos(), error);
    }
  }

  /**
   * Decide al momento de enviar: o un mensaje listo, o el motivo para descartarla.
   * Los motivos no llevan datos personales (quedan en {@code ultimo_error}).
   */
  Resolucion resolver(Notificacion n) {
    if (n.getTipo() == TipoNotificacion.RESET_PASSWORD) {
      // El reset se envía en el momento (NotificacionService.enviarResetPassword): el token
      // no se guarda, así que una fila PENDIENTE de este tipo no se puede renderizar.
      return Resolucion.descartar("RESET_PASSWORD solo se envía en forma inmediata");
    }
    if (!clinic.getFlags().isEmailNotifications()) {
      return Resolucion.descartar("FEATURE_EMAIL_NOTIFICATIONS apagado");
    }
    if (n.getCitaId() == null) {
      return Resolucion.descartar("La cita fue eliminada");
    }
    Optional<Cita> encontrada = citaRepository.findById(n.getCitaId());
    if (encontrada.isEmpty()) {
      return Resolucion.descartar("La cita fue eliminada");
    }
    Cita cita = encontrada.get();
    Paciente paciente = cita.getPaciente();

    if (ClavesDedupe.incluyeInicio(n.getTipo())
        && !ClavesDedupe.deCita(n.getTipo(), cita.getId(), cita.getFechaHoraInicio()).equals(n.getClaveDedupe())) {
      return Resolucion.descartar("La cita cambió de horario");
    }
    if (!cita.getFechaHoraInicio().isAfter(LocalDateTime.now(clock))) {
      return Resolucion.descartar("La cita ya pasó");
    }
    switch (n.getTipo()) {
      case RECORDATORIO_24H -> {
        if (cita.getEstado() != CitaEstado.CONFIRMADA) {
          return Resolucion.descartar("La cita ya no está confirmada");
        }
        if (!paciente.isRecordatoriosEmail()) {
          return Resolucion.descartar("El paciente se dio de baja de los recordatorios");
        }
      }
      case CONFIRMACION, REAGENDADA, SOLICITUD, AVISO_CLINICA -> {
        if (cita.getEstado() == CitaEstado.CANCELADA) {
          return Resolucion.descartar("La cita fue cancelada");
        }
      }
      default -> { }
    }

    String destinatario;
    switch (n.getDestinatarioTipo()) {
      case PACIENTE -> {
        destinatario = paciente.getEmail();
        if (destinatario == null || destinatario.isBlank()) {
          return Resolucion.descartar("El paciente no tiene email");
        }
      }
      case CLINICA -> {
        destinatario = clinic.getEmail();
        if (destinatario == null || destinatario.isBlank()) {
          return Resolucion.descartar("CLINIC_EMAIL no configurado");
        }
      }
      default -> {
        return Resolucion.descartar("Destinatario " + n.getDestinatarioTipo() + " no soportado en el outbox");
      }
    }
    return Resolucion.enviar(mensajes.paraCita(n.getTipo(), cita, destinatario.trim()));
  }

  record Resolucion(Mensaje mensaje, String motivoDescarte) {
    static Resolucion enviar(Mensaje m) {
      return new Resolucion(m, null);
    }

    static Resolucion descartar(String motivo) {
      return new Resolucion(null, motivo);
    }
  }
}

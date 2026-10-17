package com.consultorio.service.notificacion;

import com.consultorio.config.ClinicProperties;
import com.consultorio.model.CanalNotificacion;
import com.consultorio.model.Cita;
import com.consultorio.model.DestinatarioTipo;
import com.consultorio.model.EstadoNotificacion;
import com.consultorio.model.Notificacion;
import com.consultorio.model.TipoNotificacion;
import com.consultorio.repository.NotificacionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Punto de entrada para generar notificaciones.
 *
 * <ul>
 *   <li>{@link #encolar}: outbox. Lo llaman los servicios de citas (PR B) DENTRO de su
 *       transacción: si la operación hace rollback, no queda nada encolado.
 *   <li>{@link #enviarResetPassword}: envío inmediato (fuera del outbox) para el reset de
 *       contraseña (PR D), porque el token no puede guardarse en texto plano.
 * </ul>
 */
@Service
public class NotificacionService {

  private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);

  private final NotificacionRepository repository;
  private final ClinicProperties clinic;
  private final Clock clock;
  private final MensajeFactory mensajes;
  private final EmailChannel emailChannel;
  private final TransactionTemplate txNueva;

  public NotificacionService(
      NotificacionRepository repository,
      ClinicProperties clinic,
      Clock clock,
      MensajeFactory mensajes,
      EmailChannel emailChannel,
      PlatformTransactionManager txManager) {
    this.repository = repository;
    this.clinic = clinic;
    this.clock = clock;
    this.mensajes = mensajes;
    this.emailChannel = emailChannel;
    this.txNueva = new TransactionTemplate(txManager);
    this.txNueva.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  /** Destinatario de cada tipo de cita: el aviso va a la clínica, el resto al paciente. */
  public static DestinatarioTipo destinatarioDe(TipoNotificacion tipo) {
    return switch (tipo) {
      case AVISO_CLINICA -> DestinatarioTipo.CLINICA;
      case RESET_PASSWORD -> DestinatarioTipo.ADMIN;
      default -> DestinatarioTipo.PACIENTE;
    };
  }

  /**
   * Encola una notificación de cita (PENDIENTE, elegible ya). Corre en la transacción del
   * caller (REQUIRED) para ser atómica con el cambio de negocio.
   *
   * <p>Con FEATURE_EMAIL_NOTIFICATIONS apagado no hace nada. Si la clave de dedupe ya existe
   * (doble click, reintento, mismo inicio) se ignora en silencio sin afectar la transacción
   * del caller: primero un {@code exists} (caso común, no gasta escritura) y después un
   * INSERT que absorbe el choque de UNIQUE a nivel SQL (carrera entre dos transacciones),
   * sin pasar por una excepción de Hibernate que dejaría la transacción rollback-only.
   *
   * <p>No valida email del paciente ni estado de la cita: eso se resuelve al enviar
   * (queda registro DESCARTADA con el motivo).
   *
   * @param tipo cualquier tipo menos RESET_PASSWORD
   * @param cita cita ya persistida (con id); para CONFIRMACION, REAGENDADA y
   *     RECORDATORIO_24H se usa su {@code fechaHoraInicio} actual en la clave
   * @return false si el flag está apagado o la clave ya existía; true si se intentó el
   *     INSERT (en una carrera simultánea puede ser true aunque la otra transacción haya
   *     insertado primero: igual queda una sola fila)
   */
  @Transactional(propagation = Propagation.REQUIRED)
  public boolean encolar(TipoNotificacion tipo, Cita cita) {
    if (tipo == TipoNotificacion.RESET_PASSWORD) {
      throw new IllegalArgumentException("RESET_PASSWORD se envía con enviarResetPassword()");
    }
    if (!clinic.getFlags().isEmailNotifications()) {
      return false;
    }
    String clave = ClavesDedupe.deCita(tipo, cita.getId(), cita.getFechaHoraInicio());
    if (repository.existsByClaveDedupe(clave)) {
      log.debug("Notificación duplicada ignorada — clave: {}", clave);
      return false;
    }
    // TIMESTAMP de MySQL es de segundos y REDONDEA la fracción: sin truncar, una fila
    // encolada en x.6 s quedaría con proximo_intento_at = x+1 s (aún no vencida).
    Instant ahora = clock.instant().truncatedTo(ChronoUnit.SECONDS);
    // El conteo de filas no se usa: con useAffectedRows=false (default de Connector/J) un
    // ON DUPLICATE KEY sin cambios también reporta 1. Si otra transacción ganó la carrera
    // entre el exists y este INSERT, la fila simplemente no se duplica.
    repository.insertarPendienteSiNoExiste(
        tipo.name(),
        CanalNotificacion.EMAIL.name(),
        clave,
        cita.getId(),
        null,
        destinatarioDe(tipo).name(),
        ahora,
        ahora);
    log.info("Notificación encolada — tipo: {}, cita: {}", tipo, cita.getId());
    return true;
  }

  /**
   * Envía YA el mail de reset de contraseña (sincrónico, fuera del outbox) y deja una fila
   * de auditoría sin el token: {@code RESET_PASSWORD:{tokenId}}, destinatario ADMIN,
   * {@code referencia_id = tokenId}, estado final ENVIADA / FALLIDA / DESCARTADA.
   *
   * <p>No depende de FEATURE_EMAIL_NOTIFICATIONS; solo de que el SMTP esté configurado.
   * Nunca lanza excepción (el endpoint de "olvidé mi contraseña" responde siempre igual)
   * ni loguea el email. La fila de auditoría se escribe en una transacción propia
   * (REQUIRES_NEW): queda aunque la del caller haga rollback. No reintenta: el usuario
   * puede pedir otro link.
   *
   * @param tokenId id de la fila del token (V12, PR D); también es la clave de dedupe
   * @param emailAdmin destinatario
   * @param tokenPlano token en claro, solo para armar el link; no se persiste
   * @return true si el servidor SMTP aceptó el mensaje
   */
  public boolean enviarResetPassword(Long tokenId, String emailAdmin, String tokenPlano) {
    String clave = ClavesDedupe.deReset(tokenId);
    Notificacion n = new Notificacion();
    n.setTipo(TipoNotificacion.RESET_PASSWORD);
    n.setCanal(CanalNotificacion.EMAIL);
    n.setClaveDedupe(clave);
    n.setReferenciaId(tokenId);
    n.setDestinatarioTipo(DestinatarioTipo.ADMIN);
    Instant ahora = clock.instant();
    n.setCreatedAt(ahora);
    n.setProximoIntentoAt(ahora);

    boolean enviado = false;
    if (!emailChannel.estaConfigurado()) {
      n.setEstado(EstadoNotificacion.DESCARTADA);
      n.setUltimoError("SMTP no configurado (MAIL_HOST/MAIL_USERNAME)");
      log.warn("Reset de contraseña sin enviar: SMTP no configurado — referencia: {}", tokenId);
    } else if (emailAdmin == null || emailAdmin.isBlank() || tokenPlano == null || tokenPlano.isBlank()) {
      n.setEstado(EstadoNotificacion.DESCARTADA);
      n.setUltimoError("Falta destinatario o token");
    } else {
      n.setIntentos(1);
      try {
        emailChannel.enviar(n, mensajes.resetPassword(emailAdmin.trim(), tokenPlano));
        n.setEstado(EstadoNotificacion.ENVIADA);
        n.setEnviadaAt(clock.instant());
        enviado = true;
        log.info("Reset de contraseña enviado — referencia: {}", tokenId);
      } catch (RuntimeException e) {
        n.setEstado(EstadoNotificacion.FALLIDA);
        n.setUltimoError(PoliticaReintentos.sanear(e));
        log.warn("Reset de contraseña falló — referencia: {}, error: {}", tokenId, n.getUltimoError());
      }
    }

    try {
      txNueva.executeWithoutResult(s -> {
        if (!repository.existsByClaveDedupe(clave)) {
          repository.save(n);
        }
      });
    } catch (RuntimeException e) {
      log.warn("No se pudo registrar la auditoría del reset — referencia: {}", tokenId);
    }
    return enviado;
  }
}

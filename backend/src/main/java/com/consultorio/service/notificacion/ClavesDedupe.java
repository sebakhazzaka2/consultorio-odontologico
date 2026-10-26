package com.consultorio.service.notificacion;

import com.consultorio.model.TipoNotificacion;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Claves de idempotencia del outbox ({@code notificaciones.clave_dedupe}, UNIQUE).
 * Las de confirmación, reagendamiento y recordatorio incluyen el inicio de la cita:
 * reagendar genera una notificación nueva y la vieja queda obsoleta (el despachador la
 * descarta porque su clave ya no coincide con el inicio actual).
 */
public final class ClavesDedupe {

  private static final DateTimeFormatter INICIO = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmm");

  private ClavesDedupe() {}

  /** true si la clave del tipo depende del inicio de la cita. */
  public static boolean incluyeInicio(TipoNotificacion tipo) {
    return tipo == TipoNotificacion.CONFIRMACION
        || tipo == TipoNotificacion.REAGENDADA
        || tipo == TipoNotificacion.RECORDATORIO_24H;
  }

  /** Clave para un tipo ligado a una cita (todos menos RESET_PASSWORD). */
  public static String deCita(TipoNotificacion tipo, Long citaId, LocalDateTime inicio) {
    if (tipo == TipoNotificacion.RESET_PASSWORD) {
      throw new IllegalArgumentException("RESET_PASSWORD no es una notificación de cita");
    }
    if (citaId == null) {
      throw new IllegalArgumentException("La cita no tiene id");
    }
    if (incluyeInicio(tipo)) {
      if (inicio == null) {
        throw new IllegalArgumentException("La cita no tiene fecha de inicio");
      }
      return tipo.name() + ":" + citaId + ":" + INICIO.format(inicio);
    }
    return tipo.name() + ":" + citaId;
  }

  /** Clave del reset de contraseña: una por token emitido (id de la fila de V12). */
  public static String deReset(Long referenciaId) {
    if (referenciaId == null) {
      throw new IllegalArgumentException("Falta la referencia del reset");
    }
    return TipoNotificacion.RESET_PASSWORD.name() + ":" + referenciaId;
  }
}

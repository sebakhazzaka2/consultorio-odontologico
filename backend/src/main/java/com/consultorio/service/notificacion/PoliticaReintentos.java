package com.consultorio.service.notificacion;

import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;

/** Reintentos del despachador y saneamiento de errores para guardar/loguear. */
public final class PoliticaReintentos {

  /** A los 5 envíos fallidos la notificación pasa a FALLIDA (y se avisa a Sentry). */
  public static final int MAX_INTENTOS = 5;

  /**
   * Espera antes del reintento según cuántos envíos fallaron: 1 → 1 min, 2 → 5 min,
   * 3 → 30 min, 4 → 2 h. El quinto fallo ya es FALLIDA. 6 h queda como tope defensivo
   * si se sube MAX_INTENTOS.
   */
  static final List<Duration> ESPERAS = List.of(
      Duration.ofMinutes(1),
      Duration.ofMinutes(5),
      Duration.ofMinutes(30),
      Duration.ofHours(2),
      Duration.ofHours(6));

  /** Cuánto se "reserva" una fila tomada antes de que otra corrida pueda retomarla. */
  public static final Duration LEASE = Duration.ofMinutes(15);

  static final int MAX_LARGO_ERROR = 500;

  private static final Pattern EMAIL =
      Pattern.compile("[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}");

  private PoliticaReintentos() {}

  /** true si después de {@code intentos} envíos fallidos hay que darla por FALLIDA. */
  public static boolean agotado(int intentos) {
    return intentos >= MAX_INTENTOS;
  }

  /** Espera hasta el próximo intento tras {@code intentosFallidos} (1-based). */
  public static Duration esperaTras(int intentosFallidos) {
    int i = Math.max(1, intentosFallidos) - 1;
    return ESPERAS.get(Math.min(i, ESPERAS.size() - 1));
  }

  /**
   * Error apto para {@code ultimo_error}, logs y Sentry: clase + mensaje, cadena de causas,
   * emails reemplazados por [email], sin saltos de línea y truncado a 500 caracteres.
   */
  public static String sanear(Throwable error) {
    StringBuilder sb = new StringBuilder();
    for (Throwable t = error; t != null && sb.length() < 2 * MAX_LARGO_ERROR; t = t.getCause()) {
      if (sb.length() > 0) sb.append(" <- ");
      sb.append(t.getClass().getSimpleName());
      if (t.getMessage() != null) sb.append(": ").append(t.getMessage());
      if (t.getCause() == t) break;
    }
    return sanear(sb.toString());
  }

  public static String sanear(String texto) {
    if (texto == null) return null;
    String limpio = EMAIL.matcher(texto).replaceAll("[email]").replaceAll("\\s+", " ").trim();
    return limpio.length() <= MAX_LARGO_ERROR ? limpio : limpio.substring(0, MAX_LARGO_ERROR);
  }
}

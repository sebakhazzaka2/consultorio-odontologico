package com.consultorio.util;

import java.util.Locale;

/**
 * Forma canónica del email de un paciente: sin espacios alrededor y en minúsculas;
 * un valor en blanco pasa a {@code null}. Es la misma regla que aplicó V10 a los datos
 * existentes ({@code NULLIF(LOWER(TRIM(email)), '')}), así que todo lo que se busca o
 * se guarda pasa por acá antes de tocar {@code pacientes.email} (único).
 */
public final class EmailNormalizer {

  private EmailNormalizer() {}

  public static String normalizar(String email) {
    if (email == null) {
      return null;
    }
    String limpio = email.trim().toLowerCase(Locale.ROOT);
    return limpio.isEmpty() ? null : limpio;
  }
}

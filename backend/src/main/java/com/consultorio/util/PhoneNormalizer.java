package com.consultorio.util;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Normaliza un teléfono escrito "a mano" a dígitos E.164 (código de país + número
 * nacional, sin "+"), según el país de la instancia ({@code CLINIC_COUNTRY}).
 * Función pura: no depende de Spring ni de la base, el país se pasa como argumento.
 *
 * <p>Reglas, en orden:
 * <ol>
 *   <li>Solo se admiten dígitos, espacios, guiones, puntos, paréntesis y un {@code +}
 *       inicial; se descarta todo menos los dígitos. Cualquier otro carácter (letras,
 *       "/", etc.) → vacío. Un prefijo internacional {@code 00} inicial se quita.</li>
 *   <li>Si el número empieza con un código de país conocido (598 o 55) y lo que sigue es
 *       un número nacional válido para ese país → se devuelve tal cual. Se tolera un 0 de
 *       larga distancia después del código ({@code +598 099…}, {@code +55 011…}).
 *       Si el número venía con {@code +} o {@code 00}, tiene que cumplir esta regla.</li>
 *   <li>Si no, se interpreta como número nacional del país de la instancia:
 *     <ul>
 *       <li><b>Uruguay (598)</b>: el número nacional E.164 tiene 8 dígitos. Celular
 *           {@code 09XXXXXXX} (9 dígitos) → {@code 5989XXXXXXX}; celular sin el 0
 *           {@code 9XXXXXXX} → {@code 5989XXXXXXX}; fijo de Montevideo {@code 2XXXXXXX}
 *           o del interior {@code 4XXXXXXX} → {@code 598} + número.</li>
 *       <li><b>Brasil (55)</b>: un 0 de larga distancia inicial se quita ({@code 011…});
 *           después DDD (2 dígitos, 11–99, ninguno de los dos en 0) + celular de 9 dígitos
 *           que empieza con 9, o fijo de 8 dígitos que empieza con 2–5 (rango de ANATEL
 *           para fijos). Resultado: {@code 55} + DDD + número (12 o 13 dígitos).</li>
 *     </ul>
 *   </li>
 *   <li>Cualquier otra cosa → {@link Optional#empty()}.</li>
 * </ol>
 *
 * <p>El resultado tiene como máximo 13 dígitos, así que siempre entra en
 * {@code pacientes.telefono} ({@code ^[0-9]{7,15}$}).
 */
public final class PhoneNormalizer {

  public static final String URUGUAY = "598";
  public static final String BRASIL = "55";

  private static final Pattern CARACTERES_PERMITIDOS = Pattern.compile("^\\+?[0-9 .()\\-]+$");
  // Número nacional UY sin 0: celular 9XXXXXXX, fijo 2XXXXXXX (Montevideo) o 4XXXXXXX (interior)
  private static final Pattern UY_NACIONAL = Pattern.compile("^[924][0-9]{7}$");
  // DDD (11–99, sin ceros) + celular 9XXXXXXXX o fijo [2-5]XXXXXXX
  private static final Pattern BR_NACIONAL = Pattern.compile("^[1-9]{2}(9[0-9]{8}|[2-5][0-9]{7})$");

  private PhoneNormalizer() {}

  public static Optional<String> toE164Digits(String raw, String country) {
    if (raw == null) {
      return Optional.empty();
    }
    String texto = raw.trim();
    if (texto.isEmpty() || !CARACTERES_PERMITIDOS.matcher(texto).matches()) {
      return Optional.empty();
    }
    boolean internacional = texto.startsWith("+");
    String digitos = texto.replaceAll("[^0-9]", "");
    if (!internacional && digitos.startsWith("00")) {
      internacional = true;
      digitos = digitos.substring(2);
    }
    if (digitos.isEmpty()) {
      return Optional.empty();
    }

    Optional<String> conCodigo = conCodigoDePais(digitos);
    if (conCodigo.isPresent() || internacional) {
      return conCodigo;
    }
    return nacional(digitos, country);
  }

  /** El número ya trae 598 o 55 adelante y lo que sigue es un nacional válido. */
  private static Optional<String> conCodigoDePais(String digitos) {
    if (digitos.startsWith(URUGUAY)) {
      String resto = sinCeroInicial(digitos.substring(URUGUAY.length()));
      if (UY_NACIONAL.matcher(resto).matches()) {
        return Optional.of(URUGUAY + resto);
      }
    }
    if (digitos.startsWith(BRASIL)) {
      String resto = sinCeroInicial(digitos.substring(BRASIL.length()));
      if (BR_NACIONAL.matcher(resto).matches()) {
        return Optional.of(BRASIL + resto);
      }
    }
    return Optional.empty();
  }

  private static Optional<String> nacional(String digitos, String country) {
    if (URUGUAY.equals(country)) {
      String resto = digitos.length() == 9 && digitos.startsWith("09") ? digitos.substring(1) : digitos;
      return UY_NACIONAL.matcher(resto).matches() ? Optional.of(URUGUAY + resto) : Optional.empty();
    }
    if (BRASIL.equals(country)) {
      String resto = sinCeroInicial(digitos);
      return BR_NACIONAL.matcher(resto).matches() ? Optional.of(BRASIL + resto) : Optional.empty();
    }
    return Optional.empty();
  }

  private static String sinCeroInicial(String s) {
    return s.startsWith("0") ? s.substring(1) : s;
  }
}

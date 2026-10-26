package com.consultorio.exception;

/**
 * Token de baja de recordatorios mal formado, adulterado o de un paciente que ya no
 * existe. Extiende IllegalArgumentException: GlobalExceptionHandler lo devuelve como 400.
 * Mismo mensaje en todos los casos para no revelar si el paciente existe.
 */
public class TokenBajaInvalidoException extends IllegalArgumentException {

  public static final String MENSAJE = "El enlace para darte de baja no es válido.";

  public TokenBajaInvalidoException() {
    super(MENSAJE);
  }
}

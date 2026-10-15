package com.consultorio.exception;

/**
 * Se lanza cuando no se pudo obtener el lock de MySQL (GET_LOCK) para procesar una
 * reserva pública dentro del timeout — otra reserva para el mismo día se está
 * procesando en simultáneo. El cliente puede reintentar.
 */
public class ReservaEnProcesoException extends RuntimeException {

  public ReservaEnProcesoException(String message) {
    super(message);
  }
}

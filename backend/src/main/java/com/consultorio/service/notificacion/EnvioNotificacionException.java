package com.consultorio.service.notificacion;

/** Falla de envío en un canal. El mensaje no debe incluir direcciones de email. */
public class EnvioNotificacionException extends RuntimeException {

  public EnvioNotificacionException(String message) {
    super(message);
  }

  public EnvioNotificacionException(String message, Throwable cause) {
    super(message, cause);
  }
}

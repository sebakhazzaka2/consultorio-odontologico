package com.consultorio.service.notificacion;

import com.consultorio.model.CanalNotificacion;
import com.consultorio.model.Notificacion;

/**
 * Canal de envío (Strategy). Hoy solo {@link EmailChannel}; F2b agrega WhatsApp sin tocar
 * el despachador: se registra un bean más y se elige por {@link #canal()}.
 */
public interface NotificationChannel {

  CanalNotificacion canal();

  /** true si el canal tiene la configuración mínima para intentar un envío. */
  boolean estaConfigurado();

  /**
   * Envía el mensaje en forma sincrónica.
   *
   * @throws EnvioNotificacionException si el envío falló (el despachador reintenta)
   */
  void enviar(Notificacion notificacion, Mensaje mensaje);
}

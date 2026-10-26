package com.consultorio.model;

/** Qué evento originó la notificación (define plantilla, destinatario y clave de dedupe). */
public enum TipoNotificacion {
  SOLICITUD, AVISO_CLINICA, CONFIRMACION, REAGENDADA, CANCELACION, RECORDATORIO_24H, RESET_PASSWORD
}

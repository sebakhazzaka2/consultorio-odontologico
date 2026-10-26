package com.consultorio.model;

/** PENDIENTE espera envío o reintento; ENVIADA ok; FALLIDA agotó reintentos; DESCARTADA no correspondía enviarla al momento de despachar. */
public enum EstadoNotificacion {
  PENDIENTE, ENVIADA, FALLIDA, DESCARTADA
}

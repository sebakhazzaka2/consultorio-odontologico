package com.consultorio.service.notificacion;

/**
 * Mensaje ya renderizado, listo para un canal. {@code destinatario} es la dirección
 * resuelta al momento del envío (nunca se persiste ni se loguea).
 */
public record Mensaje(String destinatario, String asunto, String html, String texto) {

  @Override
  public String toString() {
    // Sin destinatario ni contenido: puede terminar en un log.
    return "Mensaje[asunto=" + asunto + "]";
  }
}

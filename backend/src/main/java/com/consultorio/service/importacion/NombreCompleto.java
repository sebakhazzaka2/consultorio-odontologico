package com.consultorio.service.importacion;

/**
 * Separa un nombre completo en nombre y apellido en el ÚLTIMO espacio:
 * "Ana María López" → nombre "Ana María", apellido "López". Con una sola palabra el
 * apellido queda vacío (la fila va a revisión). Los apellidos compuestos ("Ana de León")
 * quedan mal separados: para eso están las columnas explícitas nombre/apellido.
 */
public record NombreCompleto(String nombre, String apellido) {

  public static NombreCompleto separar(String completo) {
    String limpio = completo == null ? "" : completo.trim().replaceAll("\\s+", " ");
    int ultimo = limpio.lastIndexOf(' ');
    if (ultimo < 0) {
      return new NombreCompleto(limpio, "");
    }
    return new NombreCompleto(limpio.substring(0, ultimo), limpio.substring(ultimo + 1));
  }
}

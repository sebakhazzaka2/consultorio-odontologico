package com.consultorio.exception;

/**
 * El archivo de importación no se puede procesar como un todo (vacío, sin encabezado,
 * columnas obligatorias faltantes, demasiadas filas, CSV mal formado). Extiende
 * {@link IllegalArgumentException}, que {@link GlobalExceptionHandler} ya responde con 400.
 * El mensaje va al usuario: nunca incluir datos personales del archivo.
 */
public class ArchivoImportacionInvalidoException extends IllegalArgumentException {

  public ArchivoImportacionInvalidoException(String message) {
    super(message);
  }
}

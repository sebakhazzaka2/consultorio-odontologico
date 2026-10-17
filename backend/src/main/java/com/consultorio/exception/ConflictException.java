package com.consultorio.exception;

/** Conflicto con datos existentes (ej. email de paciente ya registrado) → 409. */
public class ConflictException extends RuntimeException {

  public ConflictException(String message) {
    super(message);
  }
}

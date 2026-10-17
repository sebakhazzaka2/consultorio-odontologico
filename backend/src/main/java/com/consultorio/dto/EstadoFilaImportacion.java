package com.consultorio.dto;

/** Resultado de una fila del CSV de importación de pacientes. */
public enum EstadoFilaImportacion {
  /** Válida y nueva: se inserta al confirmar. */
  OK,
  /** Dato inválido u obligatorio faltante: no se inserta. */
  ERROR,
  /** Válida pero dudosa (ej. nombre de una sola palabra): no se inserta hasta corregirla. */
  REVISAR,
  /** Mismo email (o, sin email, mismo teléfono) que una fila anterior del archivo. */
  DUPLICADO_ARCHIVO,
  /** Ya hay un paciente con ese email (o, sin email, con ese teléfono). */
  YA_EXISTE
}

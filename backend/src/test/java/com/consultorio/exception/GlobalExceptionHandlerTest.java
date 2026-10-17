package com.consultorio.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.consultorio.exception.GlobalExceptionHandler.ErrorResponse;
import java.sql.SQLIntegrityConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  private static DataIntegrityViolationException violacion(String mensajeCausa) {
    return new DataIntegrityViolationException("could not execute statement",
        new SQLIntegrityConstraintViolationException(mensajeCausa));
  }

  @Test
  void emailDuplicadoEnMySql_retorna409ConMensajeDeEmail() {
    ResponseEntity<ErrorResponse> res = handler.handleDataIntegrityViolation(violacion(
        "Duplicate entry 'foo@bar.com' for key 'pacientes.uk_pacientes_email'"));

    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(res.getBody().message()).isEqualTo("El email ya está registrado");
  }

  @Test
  void emailDuplicadoEnH2_mayusculas_retorna409ConMensajeDeEmail() {
    ResponseEntity<ErrorResponse> res = handler.handleDataIntegrityViolation(violacion(
        "Unique index or primary key violation: \"PUBLIC.UK_PACIENTES_EMAIL_INDEX_A ON PUBLIC.PACIENTES(EMAIL)\""));

    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(res.getBody().message()).isEqualTo("El email ya está registrado");
  }

  @Test
  void otraViolacion_mantieneElMensajeGenerico() {
    ResponseEntity<ErrorResponse> res = handler.handleDataIntegrityViolation(violacion(
        "Cannot delete or update a parent row: a foreign key constraint fails (`citas`, CONSTRAINT `fk_citas_paciente`)"));

    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(res.getBody().message())
        .isEqualTo("No se puede eliminar: el registro tiene datos asociados (historial u otros)");
  }

  @Test
  void conflictException_retorna409ConSuMensaje() {
    ResponseEntity<ErrorResponse> res =
        handler.handleConflict(new ConflictException("El email ya está registrado"));

    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(res.getBody().message()).isEqualTo("El email ya está registrado");
  }
}

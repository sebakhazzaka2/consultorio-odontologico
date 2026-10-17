package com.consultorio.dto;

import com.consultorio.util.EmailNormalizer;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PacienteRequest {

  @NotBlank
  private String nombre;

  @NotBlank
  private String apellido;

  @NotBlank
  @Pattern(regexp = "^[0-9]{7,15}$", message = "El telefono debe tener entre 7 y 15 digitos")
  private String telefono;

  // Opcional desde V10. Se normaliza al deserializar (ver setEmail), así @Email valida
  // el valor ya sin espacios y un email en blanco llega como null.
  @Email
  private String email;

  @PastOrPresent
  private LocalDate fechaNacimiento;

  // Opcional (V11). null = alta con true / edición que conserva el valor guardado.
  private Boolean recordatoriosEmail;

  public PacienteRequest() {}

  public Boolean getRecordatoriosEmail() {
    return recordatoriosEmail;
  }

  public void setRecordatoriosEmail(Boolean recordatoriosEmail) {
    this.recordatoriosEmail = recordatoriosEmail;
  }

  public String getNombre() {
    return nombre;
  }

  public void setNombre(String nombre) {
    this.nombre = nombre;
  }

  public String getApellido() {
    return apellido;
  }

  public void setApellido(String apellido) {
    this.apellido = apellido;
  }

  public String getTelefono() {
    return telefono;
  }

  public void setTelefono(String telefono) {
    this.telefono = telefono;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = EmailNormalizer.normalizar(email);
  }

  public LocalDate getFechaNacimiento() {
    return fechaNacimiento;
  }

  public void setFechaNacimiento(LocalDate fechaNacimiento) {
    this.fechaNacimiento = fechaNacimiento;
  }
}

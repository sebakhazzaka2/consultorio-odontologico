package com.consultorio.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDateTime;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PublicReservaRequest {

  @NotBlank
  private String nombre;

  @NotBlank
  private String apellido;

  @NotBlank
  @Pattern(regexp = "^[0-9]{7,15}$", message = "El teléfono debe tener entre 7 y 15 dígitos")
  private String telefono;

  @NotBlank
  @Email
  private String email;

  @NotNull
  private Long servicioId;

  @NotNull
  private LocalDateTime fechaHoraInicio;

  public PublicReservaRequest() {}

  public String getNombre() { return nombre; }
  public void setNombre(String nombre) { this.nombre = nombre; }

  public String getApellido() { return apellido; }
  public void setApellido(String apellido) { this.apellido = apellido; }

  public String getTelefono() { return telefono; }
  public void setTelefono(String telefono) { this.telefono = telefono; }

  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email; }

  public Long getServicioId() { return servicioId; }
  public void setServicioId(Long servicioId) { this.servicioId = servicioId; }

  public LocalDateTime getFechaHoraInicio() { return fechaHoraInicio; }
  public void setFechaHoraInicio(LocalDateTime fechaHoraInicio) { this.fechaHoraInicio = fechaHoraInicio; }
}

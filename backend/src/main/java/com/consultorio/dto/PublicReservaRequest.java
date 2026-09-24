package com.consultorio.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PublicReservaRequest {

  @NotBlank
  @Size(max = 100)
  private String nombre;

  @NotBlank
  @Size(max = 100)
  private String apellido;

  @NotBlank
  @Pattern(regexp = "^[0-9]{7,15}$", message = "El teléfono debe tener entre 7 y 15 dígitos")
  private String telefono;

  @NotBlank
  @Email
  @Size(max = 150)
  private String email;

  /** Honeypot anti-spam: el formulario lo deja vacío; si viene con contenido es un bot. */
  @Size(max = 0)
  private String website;

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

  public String getWebsite() { return website; }
  public void setWebsite(String website) { this.website = website; }

  public Long getServicioId() { return servicioId; }
  public void setServicioId(Long servicioId) { this.servicioId = servicioId; }

  public LocalDateTime getFechaHoraInicio() { return fechaHoraInicio; }
  public void setFechaHoraInicio(LocalDateTime fechaHoraInicio) { this.fechaHoraInicio = fechaHoraInicio; }
}

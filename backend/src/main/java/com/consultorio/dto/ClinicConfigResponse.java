package com.consultorio.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ClinicConfigResponse {

  private String nombre;
  private String tagline;
  private String direccion;
  private String ciudad;
  private String horario;
  private String telefono;
  private String whatsapp;
  private String email;
  private String nosotros;
  private boolean reviewsEnabled;

  public String getNombre() { return nombre; }
  public void setNombre(String nombre) { this.nombre = nombre; }

  public String getTagline() { return tagline; }
  public void setTagline(String tagline) { this.tagline = tagline; }

  public String getDireccion() { return direccion; }
  public void setDireccion(String direccion) { this.direccion = direccion; }

  public String getCiudad() { return ciudad; }
  public void setCiudad(String ciudad) { this.ciudad = ciudad; }

  public String getHorario() { return horario; }
  public void setHorario(String horario) { this.horario = horario; }

  public String getTelefono() { return telefono; }
  public void setTelefono(String telefono) { this.telefono = telefono; }

  public String getWhatsapp() { return whatsapp; }
  public void setWhatsapp(String whatsapp) { this.whatsapp = whatsapp; }

  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email; }

  public String getNosotros() { return nosotros; }
  public void setNosotros(String nosotros) { this.nosotros = nosotros; }

  public boolean isReviewsEnabled() { return reviewsEnabled; }
  public void setReviewsEnabled(boolean reviewsEnabled) { this.reviewsEnabled = reviewsEnabled; }
}

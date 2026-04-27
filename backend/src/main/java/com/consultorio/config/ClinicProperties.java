package com.consultorio.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "clinic")
public class ClinicProperties {

  private String nombre = "Dental Montecaseros";
  private String tagline = "Odontología integral con atención cercana";
  private String direccion = "Monte Caseros 2687a";
  private String ciudad = "Montevideo, Uruguay";
  private String horario = "Lunes a Viernes 09:00 – 18:00";
  private String telefono = "+59899572537";
  private String whatsapp = "59899572537";
  private String email = "dentalmontecaseros@gmail.com";
  private String nosotros = "";
  private String fotoUbicacionUrl = "";
  private String googlePlaceId = "";

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

  public String getFotoUbicacionUrl() { return fotoUbicacionUrl; }
  public void setFotoUbicacionUrl(String fotoUbicacionUrl) { this.fotoUbicacionUrl = fotoUbicacionUrl; }

  public String getGooglePlaceId() { return googlePlaceId; }
  public void setGooglePlaceId(String googlePlaceId) { this.googlePlaceId = googlePlaceId; }
}

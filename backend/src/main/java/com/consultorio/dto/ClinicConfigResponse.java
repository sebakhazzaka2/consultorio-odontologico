package com.consultorio.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ClinicConfigResponse {

  public record FeatureResponse(String titulo, String texto, String icono) {}

  private String nombre;
  private String tagline;
  private String direccion;
  private String ciudad;
  private String horario;
  private String horarioApertura;
  private String horarioCierre;
  private String diasLaborales;
  private String telefono;
  private String whatsapp;
  private String email;
  private String nosotros;
  private String fotoUbicacionUrl;
  private boolean reviewsEnabled;
  private String statsPacientes;
  private String statsAnosExperiencia;
  private String statsCalificacion;
  private List<FeatureResponse> features;

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

  public String getHorarioApertura() { return horarioApertura; }
  public void setHorarioApertura(String h) { this.horarioApertura = h; }

  public String getHorarioCierre() { return horarioCierre; }
  public void setHorarioCierre(String h) { this.horarioCierre = h; }

  public String getDiasLaborales() { return diasLaborales; }
  public void setDiasLaborales(String d) { this.diasLaborales = d; }

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

  public boolean isReviewsEnabled() { return reviewsEnabled; }
  public void setReviewsEnabled(boolean reviewsEnabled) { this.reviewsEnabled = reviewsEnabled; }

  public String getStatsPacientes() { return statsPacientes; }
  public void setStatsPacientes(String statsPacientes) { this.statsPacientes = statsPacientes; }

  public String getStatsAnosExperiencia() { return statsAnosExperiencia; }
  public void setStatsAnosExperiencia(String statsAnosExperiencia) { this.statsAnosExperiencia = statsAnosExperiencia; }

  public String getStatsCalificacion() { return statsCalificacion; }
  public void setStatsCalificacion(String statsCalificacion) { this.statsCalificacion = statsCalificacion; }

  public List<FeatureResponse> getFeatures() { return features; }
  public void setFeatures(List<FeatureResponse> features) { this.features = features; }
}

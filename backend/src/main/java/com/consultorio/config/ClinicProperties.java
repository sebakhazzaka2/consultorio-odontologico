package com.consultorio.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "clinic")
public class ClinicProperties {

  public static class Feature {
    private String titulo = "";
    private String texto = "";
    private String icono = "";

    public Feature() {}
    public Feature(String titulo, String texto, String icono) {
      this.titulo = titulo; this.texto = texto; this.icono = icono;
    }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }
    public String getIcono() { return icono; }
    public void setIcono(String icono) { this.icono = icono; }
  }

  private String nombre = "Dental Montecaseros";
  private String tagline = "Odontologia integral con atencion cercana";
  private String direccion = "Monte Caseros 2687a";
  private String ciudad = "Montevideo, Uruguay";
  private String horario = "Lunes a Viernes 09:00 - 18:00";
  private String horarioApertura = "09:00";
  private String horarioCierre = "18:00";
  private String diasLaborales = "1,2,3,4,5";
  private String telefono = "+59899572537";
  private String whatsapp = "59899572537";
  private String email = "dentalmontecaseros@gmail.com";
  private String nosotros = "";
  private String fotoUbicacionUrl = "";
  private String googlePlaceId = "";
  private String statsPacientes = "";
  private String statsAnosExperiencia = "";
  private String statsCalificacion = "";
  private List<Feature> features = new ArrayList<>(List.of(
    new Feature("Agenda online", "Reserva tu turno con confirmacion inmediata, sin llamadas ni esperas.", "calendario"),
    new Feature("Historial seguro", "Tu ficha clinica completa, tratamientos y notas accesibles en cualquier momento.", "documento"),
    new Feature("Atencion personalizada", "Profesionales matriculados que te acompanan durante todo el tratamiento.", "usuario"),
    new Feature("Tecnologia actual", "Equipamiento moderno y protocolos clinicos actualizados.", "tecnologia")
  ));

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
  public void setHorarioApertura(String horarioApertura) { this.horarioApertura = horarioApertura; }

  public String getHorarioCierre() { return horarioCierre; }
  public void setHorarioCierre(String horarioCierre) { this.horarioCierre = horarioCierre; }

  public String getDiasLaborales() { return diasLaborales; }
  public void setDiasLaborales(String diasLaborales) { this.diasLaborales = diasLaborales; }

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

  public String getStatsPacientes() { return statsPacientes; }
  public void setStatsPacientes(String statsPacientes) { this.statsPacientes = statsPacientes; }

  public String getStatsAnosExperiencia() { return statsAnosExperiencia; }
  public void setStatsAnosExperiencia(String statsAnosExperiencia) { this.statsAnosExperiencia = statsAnosExperiencia; }

  public String getStatsCalificacion() { return statsCalificacion; }
  public void setStatsCalificacion(String statsCalificacion) { this.statsCalificacion = statsCalificacion; }

  public List<Feature> getFeatures() { return features; }
  public void setFeatures(List<Feature> features) { this.features = features; }
}

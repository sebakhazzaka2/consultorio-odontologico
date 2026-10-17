package com.consultorio.config;

import java.util.ArrayList;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "clinic")
@Validated
public class ClinicProperties {

  /**
   * Colores y logo por instancia (BRAND_*). Todo opcional (vacío = look por defecto).
   * Los valores terminan en CSS (variables) y en un atributo src, así que se validan
   * estrictamente en TODOS los perfiles: un valor inválido hace fallar el arranque.
   */
  public static class Branding {
    public static final String HEX_REGEX = "^(#[0-9a-fA-F]{6})?$";
    // Vacío, https://... sin espacios ni caracteres de inyección, o ruta /api/uploads/... sin ".."
    public static final String LOGO_URL_REGEX =
        "^(|https://[^\\s\"'<>()\\\\]+|/api/uploads/(?!.*\\.\\.)[A-Za-z0-9._~%/-]+)$";

    @Pattern(regexp = HEX_REGEX, message = "BRAND_PRIMARY must be a hex color like #AABBCC")
    private String primary = "";
    @Pattern(regexp = HEX_REGEX, message = "BRAND_PRIMARY_HOVER must be a hex color like #AABBCC")
    private String primaryHover = "";
    @Pattern(regexp = HEX_REGEX, message = "BRAND_ACCENT must be a hex color like #AABBCC")
    private String accent = "";
    @Pattern(regexp = LOGO_URL_REGEX,
        message = "BRAND_LOGO_URL must be https://... or a relative /api/uploads/... path")
    private String logoUrl = "";

    public String getPrimary() { return primary; }
    public void setPrimary(String v) { this.primary = v == null ? "" : v.trim(); }
    public String getPrimaryHover() { return primaryHover; }
    public void setPrimaryHover(String v) { this.primaryHover = v == null ? "" : v.trim(); }
    public String getAccent() { return accent; }
    public void setAccent(String v) { this.accent = v == null ? "" : v.trim(); }
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String v) { this.logoUrl = v == null ? "" : v.trim(); }
  }

  @Valid
  private Branding branding = new Branding();

  public Branding getBranding() { return branding; }
  public void setBranding(Branding branding) { this.branding = branding; }

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

  private String nombre = "";
  private String tagline = "";
  private String direccion = "";
  private String ciudad = "";
  private String horario = "Lunes a Viernes 09:00 - 18:00";
  private String horarioApertura = "09:00";
  private String horarioCierre = "18:00";
  private String diasLaborales = "1,2,3,4,5";
  private String telefono = "";
  private String whatsapp = "";
  private String email = "";
  private String nosotros = "";
  private String fotoUbicacionUrl = "";
  private String googlePlaceId = "";
  private String statsPacientes = "";
  private String statsAnosExperiencia = "";
  private String statsCalificacion = "";
  // Sin imagen por defecto: el frontend muestra un fondo neutro si la lista está vacía
  private List<String> heroImagenes = new ArrayList<>();
  private String heroEyebrow = "";
  private String labelServicio = "Servicio";
  private String labelServicioPlural = "";
  private String labelHistorial = "Historial clínico";
  private String featuresTitle = "Una experiencia clínica pensada en vos";
  private String featuresSubtitle = "Combinamos atención profesional con herramientas modernas para que reservar tu turno sea simple de principio a fin.";
  private List<Feature> features = new ArrayList<>(List.of(
    new Feature("Agenda online", "Reserva tu turno con confirmacion inmediata, sin llamadas ni esperas.", "calendario"),
    new Feature("Historial seguro", "Tu historial completo y tus notas accesibles en cualquier momento.", "documento"),
    new Feature("Atencion personalizada", "Un equipo profesional que te acompaña en cada visita.", "usuario"),
    new Feature("Tecnologia actual", "Equipamiento moderno y procesos siempre actualizados.", "tecnologia")
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

  public List<String> getHeroImagenes() { return heroImagenes; }
  public void setHeroImagenes(List<String> heroImagenes) { this.heroImagenes = heroImagenes; }

  public String getHeroEyebrow() { return heroEyebrow; }
  public void setHeroEyebrow(String heroEyebrow) { this.heroEyebrow = heroEyebrow; }

  /** Plural configurado; si está vacío se deriva agregando "s" al singular. */
  public String getLabelServicioPlural() {
    return (labelServicioPlural == null || labelServicioPlural.isBlank())
        ? labelServicio + "s" : labelServicioPlural;
  }
  public void setLabelServicioPlural(String labelServicioPlural) { this.labelServicioPlural = labelServicioPlural; }

  public String getLabelServicio() { return labelServicio; }
  public void setLabelServicio(String labelServicio) { this.labelServicio = labelServicio; }

  public String getLabelHistorial() { return labelHistorial; }
  public void setLabelHistorial(String labelHistorial) { this.labelHistorial = labelHistorial; }

  public String getFeaturesTitle() { return featuresTitle; }
  public void setFeaturesTitle(String featuresTitle) { this.featuresTitle = featuresTitle; }

  public String getFeaturesSubtitle() { return featuresSubtitle; }
  public void setFeaturesSubtitle(String featuresSubtitle) { this.featuresSubtitle = featuresSubtitle; }

  public List<Feature> getFeatures() { return features; }
  public void setFeatures(List<Feature> features) { this.features = features; }
}

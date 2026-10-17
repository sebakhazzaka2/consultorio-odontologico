package com.consultorio.service.notificacion;

import com.consultorio.config.ClinicProperties;
import com.consultorio.config.NotificationProperties;
import com.consultorio.model.Cita;
import com.consultorio.model.Paciente;
import com.consultorio.model.TipoNotificacion;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Arma el {@link Mensaje} de cada tipo: asunto + variables + plantillas Thymeleaf.
 * Los datos se toman en el momento del envío (cita y clínica actuales), no al encolar.
 */
@Component
public class MensajeFactory {

  /** Navy por defecto de la marca si la instancia no define BRAND_PRIMARY. */
  public static final String COLOR_DEFAULT = "#0B1A3A";
  /** Vigencia del link de reset que muestra la plantilla (la define PR D, V12). */
  public static final int MINUTOS_VALIDEZ_RESET = 30;

  private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
  private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

  private final ClinicProperties clinic;
  private final NotificationProperties props;
  private final PlantillasEmail plantillas;
  private final BajaTokenService bajaTokens;

  public MensajeFactory(
      ClinicProperties clinic,
      NotificationProperties props,
      PlantillasEmail plantillas,
      BajaTokenService bajaTokens) {
    this.clinic = clinic;
    this.props = props;
    this.plantillas = plantillas;
    this.bajaTokens = bajaTokens;
  }

  public String baseUrl() {
    return UrlsPublicas.resolverBaseUrl(props.getBaseUrl(), props.getDomain());
  }

  /** Mensaje de un tipo ligado a una cita, para el destinatario ya resuelto. */
  public Mensaje paraCita(TipoNotificacion tipo, Cita cita, String destinatario) {
    if (tipo == TipoNotificacion.RESET_PASSWORD) {
      throw new IllegalArgumentException("RESET_PASSWORD no es una notificación de cita");
    }
    Map<String, Object> v = variablesComunes();
    Paciente p = cita.getPaciente();
    String fecha = FECHA.format(cita.getFechaHoraInicio());
    String hora = HORA.format(cita.getFechaHoraInicio());
    v.put("pacienteNombre", p.getNombre());
    v.put("fecha", fecha);
    v.put("hora", hora);
    v.put("duracion", cita.getDuracionMinutos());
    v.put("servicio", vacioANull(cita.getMotivo()));
    v.put("reservarUrl", v.get("baseUrl") + "/reservar");

    if (tipo == TipoNotificacion.AVISO_CLINICA) {
      // Va a la propia clínica: sus datos del paciente, para que lo contacte.
      v.put("pacienteNombreCompleto", (p.getNombre() + " " + p.getApellido()).trim());
      v.put("pacienteTelefono", p.getTelefono());
      v.put("pacienteEmail", p.getEmail());
      v.put("adminUrl", v.get("baseUrl") + "/admin");
    }
    if (tipo == TipoNotificacion.RECORDATORIO_24H) {
      v.put("bajaUrl", v.get("baseUrl") + "/baja?token=" + bajaTokens.generar(p.getId()));
    }

    String asunto = asunto(tipo, fecha, hora);
    v.put("asunto", asunto);
    PlantillasEmail.Renderizado r = plantillas.renderizar(plantilla(tipo), v);
    return new Mensaje(destinatario, asunto, r.html(), r.texto());
  }

  /**
   * Mail de reset de contraseña. {@code tokenPlano} solo vive en este mensaje (link
   * {@code /restablecer?token=...}); nunca se guarda en la base.
   */
  public Mensaje resetPassword(String destinatario, String tokenPlano) {
    Map<String, Object> v = variablesComunes();
    v.put("resetUrl", v.get("baseUrl") + "/restablecer?token="
        + URLEncoder.encode(tokenPlano, StandardCharsets.UTF_8));
    v.put("minutosValidez", MINUTOS_VALIDEZ_RESET);
    String asunto = asunto(TipoNotificacion.RESET_PASSWORD, null, null);
    v.put("asunto", asunto);
    PlantillasEmail.Renderizado r = plantillas.renderizar(plantilla(TipoNotificacion.RESET_PASSWORD), v);
    return new Mensaje(destinatario, asunto, r.html(), r.texto());
  }

  String asunto(TipoNotificacion tipo, String fecha, String hora) {
    String nombre = nombreClinica();
    return switch (tipo) {
      case SOLICITUD -> "Recibimos tu solicitud de turno - " + nombre;
      case AVISO_CLINICA -> "Nueva solicitud de turno: " + fecha + " " + hora;
      case CONFIRMACION -> "Tu turno está confirmado: " + fecha + " " + hora + " - " + nombre;
      case REAGENDADA -> "Tu turno cambió: " + fecha + " " + hora + " - " + nombre;
      case CANCELACION -> "Tu turno del " + fecha + " fue cancelado - " + nombre;
      case RECORDATORIO_24H -> "Recordatorio: tu turno del " + fecha + " a las " + hora + " - " + nombre;
      case RESET_PASSWORD -> "Restablecé tu contraseña - " + nombre;
    };
  }

  static String plantilla(TipoNotificacion tipo) {
    return tipo.name().toLowerCase(Locale.ROOT).replace('_', '-');
  }

  private Map<String, Object> variablesComunes() {
    Map<String, Object> v = new HashMap<>();
    String base = baseUrl();
    v.put("baseUrl", base);
    v.put("clinica", nombreClinica());
    v.put("logoUrl", UrlsPublicas.logoAbsoluto(base, clinic.getBranding().getLogoUrl()));
    String color = clinic.getBranding().getPrimary();
    v.put("color", (color == null || color.isBlank()) ? COLOR_DEFAULT : color);
    v.put("direccion", direccionCompleta());
    v.put("telefono", vacioANull(clinic.getTelefono()));
    v.put("whatsappUrl", UrlsPublicas.whatsappUrl(clinic.getWhatsapp()));
    v.put("emailClinica", vacioANull(clinic.getEmail()));
    return v;
  }

  private String nombreClinica() {
    String n = clinic.getNombre();
    return (n == null || n.isBlank()) ? "Tu clínica" : n.trim();
  }

  private String direccionCompleta() {
    String dir = vacioANull(clinic.getDireccion());
    String ciudad = vacioANull(clinic.getCiudad());
    if (dir == null) return ciudad;
    return ciudad == null ? dir : dir + ", " + ciudad;
  }

  private static String vacioANull(String s) {
    return (s == null || s.isBlank()) ? null : s.trim();
  }
}

package com.consultorio.service.notificacion;

/** Resolución de la URL pública de la instancia para links y logo de los mails. */
public final class UrlsPublicas {

  static final String BASE_LOCAL = "http://localhost:4200";

  private UrlsPublicas() {}

  /**
   * APP_BASE_URL si está; si no, {@code https://DOMAIN}; si no, el dev server de Angular.
   * Siempre sin barra final.
   */
  public static String resolverBaseUrl(String appBaseUrl, String domain) {
    String base;
    if (appBaseUrl != null && !appBaseUrl.isBlank()) {
      base = appBaseUrl.trim();
    } else if (domain != null && !domain.isBlank()) {
      base = "https://" + domain.trim();
    } else {
      base = BASE_LOCAL;
    }
    while (base.endsWith("/")) {
      base = base.substring(0, base.length() - 1);
    }
    return base;
  }

  /**
   * Logo como URL absoluta (los clientes de correo no resuelven rutas relativas).
   * {@code https://...} queda igual, {@code /api/uploads/...} se prefija con la base, y
   * vacío devuelve null (la plantilla muestra el nombre de la clínica en texto).
   */
  public static String logoAbsoluto(String baseUrl, String logoUrl) {
    if (logoUrl == null || logoUrl.isBlank()) {
      return null;
    }
    String logo = logoUrl.trim();
    if (logo.startsWith("https://") || logo.startsWith("http://")) {
      return logo;
    }
    return baseUrl + (logo.startsWith("/") ? logo : "/" + logo);
  }

  /** {@code https://wa.me/<dígitos>} o null si no hay WhatsApp configurado. */
  public static String whatsappUrl(String whatsapp) {
    if (whatsapp == null) {
      return null;
    }
    String digitos = whatsapp.replaceAll("[^0-9]", "");
    return digitos.isEmpty() ? null : "https://wa.me/" + digitos;
  }
}

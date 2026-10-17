package com.consultorio.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Transporte de las notificaciones (prefijo {@code notifications}). El flag que prende o
 * apaga la feature vive en {@link ClinicProperties.Flags} (regla del repo); la conexión
 * SMTP en {@code spring.mail.*} (MAIL_HOST, MAIL_PORT, MAIL_USERNAME, MAIL_PASSWORD,
 * MAIL_SMTP_AUTH, MAIL_STARTTLS).
 */
@ConfigurationProperties(prefix = "notifications")
public class NotificationProperties {

  public static final String DEFAULT_MAIL_FROM = "turnos@turnosuy.com";

  /** MAIL_FROM: remitente único verificado en Brevo. El nombre visible es CLINIC_NOMBRE. */
  private String mailFrom = DEFAULT_MAIL_FROM;

  /** APP_BASE_URL: base de links y logo en los mails. Vacío = derivado de DOMAIN. */
  private String baseUrl = "";

  /** DOMAIN (el mismo del .env.prod que usa Caddy). */
  private String domain = "";

  /** Si el despachador programado corre. Siempre true en prod; false en tests. */
  private boolean dispatcherEnabled = true;

  /** Cuántas notificaciones toma el despachador por corrida. */
  private int batchSize = 20;

  public String getMailFrom() { return mailFrom; }
  public void setMailFrom(String v) {
    this.mailFrom = (v == null || v.isBlank()) ? DEFAULT_MAIL_FROM : v.trim();
  }

  public String getBaseUrl() { return baseUrl; }
  public void setBaseUrl(String v) { this.baseUrl = v == null ? "" : v.trim(); }

  public String getDomain() { return domain; }
  public void setDomain(String v) { this.domain = v == null ? "" : v.trim(); }

  public boolean isDispatcherEnabled() { return dispatcherEnabled; }
  public void setDispatcherEnabled(boolean v) { this.dispatcherEnabled = v; }

  public int getBatchSize() { return batchSize; }
  public void setBatchSize(int v) { this.batchSize = v; }
}

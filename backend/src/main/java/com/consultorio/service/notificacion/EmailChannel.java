package com.consultorio.service.notificacion;

import com.consultorio.config.ClinicProperties;
import com.consultorio.config.NotificationProperties;
import com.consultorio.model.CanalNotificacion;
import com.consultorio.model.Notificacion;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Email por SMTP (Brevo en prod, Mailpit en dev, GreenMail en tests).
 *
 * <p>From = MAIL_FROM (único dominio verificado) con nombre visible = CLINIC_NOMBRE;
 * Reply-To = CLINIC_EMAIL, así las respuestas del paciente le llegan a la clínica.
 * Cuerpo multipart/alternative: texto plano + HTML (mejor entregabilidad que solo HTML).
 */
@Component
public class EmailChannel implements NotificationChannel {

  private final ObjectProvider<JavaMailSender> mailSender;
  private final ClinicProperties clinic;
  private final NotificationProperties props;
  private final String host;
  private final String username;
  private final boolean smtpAuth;

  public EmailChannel(
      ObjectProvider<JavaMailSender> mailSender,
      ClinicProperties clinic,
      NotificationProperties props,
      @Value("${spring.mail.host:}") String host,
      @Value("${spring.mail.username:}") String username,
      @Value("${spring.mail.properties.mail.smtp.auth:true}") boolean smtpAuth) {
    this.mailSender = mailSender;
    this.clinic = clinic;
    this.props = props;
    this.host = host;
    this.username = username;
    this.smtpAuth = smtpAuth;
  }

  @Override
  public CanalNotificacion canal() {
    return CanalNotificacion.EMAIL;
  }

  /** Host SMTP definido y, si el servidor exige auth (Brevo), usuario cargado. */
  @Override
  public boolean estaConfigurado() {
    return mailSender.getIfAvailable() != null
        && host != null && !host.isBlank()
        && (!smtpAuth || (username != null && !username.isBlank()));
  }

  @Override
  public void enviar(Notificacion notificacion, Mensaje mensaje) {
    JavaMailSender sender = mailSender.getIfAvailable();
    if (sender == null || !estaConfigurado()) {
      throw new EnvioNotificacionException("SMTP no configurado (MAIL_HOST/MAIL_USERNAME)");
    }
    try {
      MimeMessage mime = sender.createMimeMessage();
      mime.setFrom(new InternetAddress(props.getMailFrom(), nombreVisible(), "UTF-8"));
      InternetAddress replyTo = replyTo();
      if (replyTo != null) {
        mime.setReplyTo(new InternetAddress[] {replyTo});
      }
      mime.setRecipient(MimeMessage.RecipientType.TO, new InternetAddress(mensaje.destinatario(), true));
      mime.setSubject(mensaje.asunto(), StandardCharsets.UTF_8.name());

      MimeBodyPart texto = new MimeBodyPart();
      texto.setText(mensaje.texto(), StandardCharsets.UTF_8.name(), "plain");
      MimeBodyPart html = new MimeBodyPart();
      html.setText(mensaje.html(), StandardCharsets.UTF_8.name(), "html");
      // Orden de multipart/alternative: del más simple al preferido.
      MimeMultipart alternativa = new MimeMultipart("alternative");
      alternativa.addBodyPart(texto);
      alternativa.addBodyPart(html);
      mime.setContent(alternativa);

      sender.send(mime);
    } catch (MessagingException | MailException | UnsupportedEncodingException e) {
      // El mensaje de JavaMail puede traer direcciones: se sanea antes de propagar.
      throw new EnvioNotificacionException(PoliticaReintentos.sanear(e), null);
    }
  }

  private String nombreVisible() {
    String nombre = clinic.getNombre();
    return (nombre == null || nombre.isBlank()) ? "Turnos" : nombre.trim();
  }

  /** CLINIC_EMAIL si es una dirección válida; si no, sin Reply-To. */
  private InternetAddress replyTo() {
    String email = clinic.getEmail();
    if (email == null || email.isBlank()) {
      return null;
    }
    try {
      return new InternetAddress(email.trim(), true);
    } catch (AddressException e) {
      return null;
    }
  }
}

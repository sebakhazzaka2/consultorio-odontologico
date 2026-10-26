package com.consultorio.service.notificacion;

import static org.assertj.core.api.Assertions.assertThat;

import com.consultorio.model.Cita;
import com.consultorio.model.CitaEstado;
import com.consultorio.model.EstadoNotificacion;
import com.consultorio.model.Notificacion;
import com.consultorio.model.Paciente;
import com.consultorio.model.TipoNotificacion;
import com.consultorio.repository.CitaRepository;
import com.consultorio.repository.NotificacionRepository;
import com.consultorio.repository.PacienteRepository;
import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetup;
import jakarta.mail.Address;
import jakarta.mail.Multipart;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Envío real por SMTP contra GreenMail (H2, contexto completo). NO transaccional: el
 * despachador usa sus propias transacciones, así que cada test limpia lo que crea.
 */
@SpringBootTest(properties = {
    // Base H2 propia: este contexto commitea de verdad y no debe tocar la "testdb" compartida.
    "spring.datasource.url=jdbc:h2:mem:notif_ok;DB_CLOSE_DELAY=-1;MODE=MySQL;NON_KEYWORDS=VALUE",
    "clinic.flags.email-notifications=true",
    "clinic.nombre=Clínica Ñandú",
    "clinic.email=recepcion@nandu.uy",
    "clinic.whatsapp=59899000000",
    "notifications.domain=nandu.turnosuy.com",
    "notifications.mail-from=turnos@turnosuy.com",
    "spring.mail.host=127.0.0.1",
    "spring.mail.port=" + NotificacionEnvioGreenMailTest.PUERTO,
    "spring.mail.username=usuario-smtp",
    "spring.mail.password=clave-smtp",
    "spring.mail.properties.mail.smtp.auth=true"
})
class NotificacionEnvioGreenMailTest {

  static final int PUERTO = 3125;

  @RegisterExtension
  static GreenMailExtension greenMail = new GreenMailExtension(
      new ServerSetup(PUERTO, "127.0.0.1", ServerSetup.PROTOCOL_SMTP))
      .withConfiguration(GreenMailConfiguration.aConfig().withUser("usuario-smtp", "clave-smtp"));

  @Autowired private NotificacionService service;
  @Autowired private NotificacionDispatcher dispatcher;
  @Autowired private NotificacionRepository notificacionRepository;
  @Autowired private CitaRepository citaRepository;
  @Autowired private PacienteRepository pacienteRepository;

  @AfterEach
  void limpiar() {
    notificacionRepository.deleteAll();
    citaRepository.deleteAll();
    pacienteRepository.deleteAll();
  }

  private Cita crearCita(String email, CitaEstado estado) {
    Paciente p = new Paciente("José", "Núñez", "099123456",
        email == null ? null : UUID.randomUUID() + email, null);
    pacienteRepository.save(p);
    Cita c = new Cita();
    c.setPaciente(p);
    c.setFechaHoraInicio(LocalDateTime.now().plusDays(2).withHour(10).withMinute(30).withSecond(0).withNano(0));
    c.setDuracionMinutos(30);
    c.setEstado(estado);
    c.setMotivo("Limpieza dental");
    return citaRepository.save(c);
  }

  private Notificacion unica() {
    assertThat(notificacionRepository.findAll()).hasSize(1);
    return notificacionRepository.findAll().get(0);
  }

  @Test
  void enviaMultipartConNombreVisibleReplyToYAsuntoUtf8() throws Exception {
    Cita cita = crearCita("@paciente.uy", CitaEstado.CONFIRMADA);
    assertThat(service.encolar(TipoNotificacion.CONFIRMACION, cita)).isTrue();

    assertThat(dispatcher.despachar()).isEqualTo(1);

    MimeMessage[] recibidos = greenMail.getReceivedMessages();
    assertThat(recibidos).hasSize(1);
    MimeMessage m = recibidos[0];

    InternetAddress from = (InternetAddress) m.getFrom()[0];
    assertThat(from.getAddress()).isEqualTo("turnos@turnosuy.com");
    assertThat(from.getPersonal()).isEqualTo("Clínica Ñandú");
    Address[] replyTo = m.getReplyTo();
    assertThat(((InternetAddress) replyTo[0]).getAddress()).isEqualTo("recepcion@nandu.uy");
    assertThat(m.getAllRecipients()[0].toString()).isEqualTo(cita.getPaciente().getEmail());
    assertThat(m.getSubject()).startsWith("Tu turno está confirmado").contains("Clínica Ñandú");
    // El header viaja codificado (RFC 2047), no en crudo.
    assertThat(m.getHeader("Subject")[0]).contains("=?UTF-8?");

    assertThat(m.getContentType()).startsWith("multipart/alternative");
    Multipart partes = (Multipart) m.getContent();
    assertThat(partes.getCount()).isEqualTo(2);
    assertThat(partes.getBodyPart(0).getContentType()).startsWith("text/plain").contains("UTF-8");
    assertThat(partes.getBodyPart(1).getContentType()).startsWith("text/html").contains("UTF-8");
    assertThat((String) partes.getBodyPart(0).getContent()).contains("José").contains("Limpieza dental");
    assertThat((String) partes.getBodyPart(1).getContent()).contains("Clínica Ñandú");

    Notificacion n = unica();
    assertThat(n.getEstado()).isEqualTo(EstadoNotificacion.ENVIADA);
    assertThat(n.getIntentos()).isEqualTo(1);
    assertThat(n.getEnviadaAt()).isNotNull();
    // Una segunda corrida no la reenvía.
    assertThat(dispatcher.despachar()).isZero();
    assertThat(greenMail.getReceivedMessages()).hasSize(1);
  }

  @Test
  void avisoClinicaVaAlEmailDeLaClinica() throws Exception {
    Cita cita = crearCita("@paciente.uy", CitaEstado.PENDIENTE);
    service.encolar(TipoNotificacion.AVISO_CLINICA, cita);
    dispatcher.despachar();
    MimeMessage[] recibidos = greenMail.getReceivedMessages();
    assertThat(recibidos).hasSize(1);
    assertThat(recibidos[0].getAllRecipients()[0].toString()).isEqualTo("recepcion@nandu.uy");
  }

  @Test
  void encolarDosVecesLaMismaClave_unaSolaFilaYUnSoloMail() {
    Cita cita = crearCita("@paciente.uy", CitaEstado.PENDIENTE);
    assertThat(service.encolar(TipoNotificacion.SOLICITUD, cita)).isTrue();
    assertThat(service.encolar(TipoNotificacion.SOLICITUD, cita)).isFalse();
    dispatcher.despachar();
    assertThat(notificacionRepository.findAll()).hasSize(1);
    assertThat(greenMail.getReceivedMessages()).hasSize(1);
  }

  @Test
  void pacienteSinEmail_descartada() {
    Cita cita = crearCita(null, CitaEstado.CONFIRMADA);
    service.encolar(TipoNotificacion.CONFIRMACION, cita);
    dispatcher.despachar();
    Notificacion n = unica();
    assertThat(n.getEstado()).isEqualTo(EstadoNotificacion.DESCARTADA);
    assertThat(n.getUltimoError()).isEqualTo("El paciente no tiene email");
    assertThat(greenMail.getReceivedMessages()).isEmpty();
  }

  @Test
  void recordatorio_descartadoSiNoEstaConfirmadaSiCambioDeHoraOSiHayBaja() {
    Cita pendiente = crearCita("@a.uy", CitaEstado.PENDIENTE);
    service.encolar(TipoNotificacion.RECORDATORIO_24H, pendiente);

    Cita reagendada = crearCita("@b.uy", CitaEstado.CONFIRMADA);
    service.encolar(TipoNotificacion.RECORDATORIO_24H, reagendada);
    reagendada.setFechaHoraInicio(reagendada.getFechaHoraInicio().plusHours(2));
    citaRepository.save(reagendada);

    Cita conBaja = crearCita("@c.uy", CitaEstado.CONFIRMADA);
    conBaja.getPaciente().setRecordatoriosEmail(false);
    pacienteRepository.save(conBaja.getPaciente());
    service.encolar(TipoNotificacion.RECORDATORIO_24H, conBaja);

    dispatcher.despachar();

    assertThat(notificacionRepository.findAll())
        .extracting(Notificacion::getEstado).containsOnly(EstadoNotificacion.DESCARTADA);
    assertThat(notificacionRepository.findAll()).extracting(Notificacion::getUltimoError)
        .containsExactlyInAnyOrder(
            "La cita ya no está confirmada",
            "La cita cambió de horario",
            "El paciente se dio de baja de los recordatorios");
    assertThat(greenMail.getReceivedMessages()).isEmpty();
  }

  @Test
  void citaBorrada_descartada() {
    Cita cita = crearCita("@paciente.uy", CitaEstado.CONFIRMADA);
    service.encolar(TipoNotificacion.CONFIRMACION, cita);
    // En H2 la FK no existe (schema de Hibernate): se simula el SET NULL de MySQL.
    Notificacion n = unica();
    n.setCitaId(null);
    notificacionRepository.save(n);
    citaRepository.delete(cita);

    dispatcher.despachar();
    assertThat(unica().getEstado()).isEqualTo(EstadoNotificacion.DESCARTADA);
    assertThat(unica().getUltimoError()).isEqualTo("La cita fue eliminada");
  }

  @Test
  void resetPassword_envioInmediatoConAuditoriaSinToken() throws Exception {
    boolean enviado = service.enviarResetPassword(77L, "admin@nandu.uy", "token-secreto-123");
    assertThat(enviado).isTrue();
    MimeMessage[] recibidos = greenMail.getReceivedMessages();
    assertThat(recibidos).hasSize(1);
    Multipart partes = (Multipart) recibidos[0].getContent();
    assertThat((String) partes.getBodyPart(0).getContent())
        .contains("https://nandu.turnosuy.com/restablecer?token=token-secreto-123");

    Notificacion n = unica();
    assertThat(n.getTipo()).isEqualTo(TipoNotificacion.RESET_PASSWORD);
    assertThat(n.getClaveDedupe()).isEqualTo("RESET_PASSWORD:77");
    assertThat(n.getReferenciaId()).isEqualTo(77L);
    assertThat(n.getEstado()).isEqualTo(EstadoNotificacion.ENVIADA);
    assertThat(n.getUltimoError()).isNull();
    // El despachador no la toca (no está PENDIENTE).
    assertThat(dispatcher.despachar()).isZero();
  }
}

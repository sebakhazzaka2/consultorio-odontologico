package com.consultorio.service.notificacion;

import static org.assertj.core.api.Assertions.assertThat;

import com.consultorio.config.ClinicProperties;
import com.consultorio.config.NotificationProperties;
import com.consultorio.model.Cita;
import com.consultorio.model.CitaEstado;
import com.consultorio.model.Paciente;
import com.consultorio.model.TipoNotificacion;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Renderiza las plantillas reales (HTML + texto) de los 7 tipos. */
class MensajeFactoryTest {

  private ClinicProperties clinic;
  private NotificationProperties props;
  private BajaTokenService tokens;
  private MensajeFactory factory;
  private Cita cita;

  @BeforeEach
  void setUp() {
    clinic = new ClinicProperties();
    clinic.setNombre("Clínica Sonrisa");
    clinic.setDireccion("Av. Italia 1234");
    clinic.setCiudad("Montevideo");
    clinic.setTelefono("29001234");
    clinic.setWhatsapp("+598 99 123 456");
    clinic.setEmail("hola@sonrisa.uy");
    props = new NotificationProperties();
    props.setDomain("sonrisa.turnosuy.com");
    tokens = new BajaTokenService("test-secret-key-for-tests-only-minimum-256-bits-padding-here");
    factory = new MensajeFactory(clinic, props, new PlantillasEmail(), tokens);

    Paciente p = new Paciente("Ana <b>", "Pérez", "099123456", "ana@example.com", null);
    p.setId(9L);
    cita = new Cita();
    cita.setId(3L);
    cita.setPaciente(p);
    cita.setFechaHoraInicio(LocalDateTime.of(2026, 10, 20, 9, 5));
    cita.setDuracionMinutos(30);
    cita.setEstado(CitaEstado.CONFIRMADA);
    cita.setMotivo("Limpieza");
  }

  @ParameterizedTest
  @EnumSource(value = TipoNotificacion.class, names = "RESET_PASSWORD", mode = EnumSource.Mode.EXCLUDE)
  void todosLosTiposDeCitaRenderizan(TipoNotificacion tipo) {
    Mensaje m = factory.paraCita(tipo, cita, "dest@example.com");
    assertThat(m.destinatario()).isEqualTo("dest@example.com");
    assertThat(m.asunto()).isNotBlank();
    assertThat(m.html()).contains("<html").contains("Clínica Sonrisa").contains("20/10/2026")
        .contains("09:05").contains("Limpieza").contains("https://wa.me/59899123456")
        .contains("#0B1A3A").contains("Av. Italia 1234, Montevideo")
        // th:text escapa: un nombre con HTML no se inyecta.
        .doesNotContain("<b>");
    assertThat(m.texto()).contains("Clínica Sonrisa").contains("20/10/2026").contains("09:05")
        .doesNotContain("[(").doesNotContain("th:");
  }

  @Test
  void sinLogo_muestraNombre_conLogoRelativo_urlAbsolutaYColorDeMarca() {
    Mensaje sin = factory.paraCita(TipoNotificacion.CONFIRMACION, cita, "a@b.com");
    assertThat(sin.html()).doesNotContain("<img");

    clinic.getBranding().setLogoUrl("/api/uploads/logo.png");
    clinic.getBranding().setPrimary("#123456");
    Mensaje con = factory.paraCita(TipoNotificacion.CONFIRMACION, cita, "a@b.com");
    assertThat(con.html()).contains("src=\"https://sonrisa.turnosuy.com/api/uploads/logo.png\"")
        .contains("#123456");
  }

  @Test
  void recordatorioTraeLinkDeBajaValido() {
    Mensaje m = factory.paraCita(TipoNotificacion.RECORDATORIO_24H, cita, "a@b.com");
    String prefijo = "https://sonrisa.turnosuy.com/baja?token=";
    assertThat(m.texto()).contains(prefijo);
    int i = m.texto().indexOf(prefijo) + prefijo.length();
    String token = m.texto().substring(i).split("\\s")[0];
    assertThat(tokens.verificar(token)).contains(9L);
    assertThat(m.html()).contains(prefijo);
  }

  @Test
  void avisoClinicaTraeDatosDelPacienteYLinkAlAdmin() {
    Mensaje m = factory.paraCita(TipoNotificacion.AVISO_CLINICA, cita, "hola@sonrisa.uy");
    assertThat(m.texto()).contains("Pérez").contains("099123456").contains("ana@example.com")
        .contains("https://sonrisa.turnosuy.com/admin");
  }

  @Test
  void otrosTiposNoIncluyenLinkDeBaja() {
    assertThat(factory.paraCita(TipoNotificacion.CONFIRMACION, cita, "a@b.com").texto()).doesNotContain("/baja");
  }

  @Test
  void resetPassword() {
    Mensaje m = factory.resetPassword("admin@sonrisa.uy", "abc+/=");
    assertThat(m.texto()).contains("https://sonrisa.turnosuy.com/restablecer?token=abc%2B%2F%3D")
        .contains("30 minutos");
    assertThat(m.html()).contains("restablecer?token=abc%2B%2F%3D");
    assertThat(m.asunto()).contains("Clínica Sonrisa");
  }
}

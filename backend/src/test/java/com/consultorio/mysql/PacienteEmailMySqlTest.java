package com.consultorio.mysql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.consultorio.dto.PublicReservaRequest;
import com.consultorio.exception.GlobalExceptionHandler;
import com.consultorio.model.DisponibilidadSemanal;
import com.consultorio.model.Paciente;
import com.consultorio.model.PacienteOrigen;
import com.consultorio.model.Servicio;
import com.consultorio.repository.DisponibilidadSemanalRepository;
import com.consultorio.repository.PacienteRepository;
import com.consultorio.repository.ServicioRepository;
import com.consultorio.service.PublicReservaService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * V10 contra MySQL real: email NULL-able y único (uk_pacientes_email), origen IMPORT,
 * y la reserva pública reusando el paciente aunque el email venga con otro formato.
 */
class PacienteEmailMySqlTest extends AbstractMySqlTest {

  private static final String INSERT =
      "INSERT INTO pacientes (nombre, apellido, telefono, email) VALUES (?, ?, ?, ?)";

  @Autowired private JdbcTemplate jdbc;
  @Autowired private PacienteRepository pacienteRepository;
  @Autowired private PublicReservaService publicReservaService;
  @Autowired private DisponibilidadSemanalRepository disponibilidadRepository;
  @Autowired private ServicioRepository servicioRepository;

  @BeforeEach
  @AfterEach
  void limpiar() {
    jdbc.update("DELETE FROM citas");
    jdbc.update("DELETE FROM pacientes");
    jdbc.update("DELETE FROM servicios");
  }

  @Test
  void variosPacientesSinEmail_seAceptan() {
    jdbc.update(INSERT, "Ana", "Uno", "099000001", null);
    jdbc.update(INSERT, "Beto", "Dos", "099000002", null);

    Integer sinEmail = jdbc.queryForObject(
        "SELECT COUNT(*) FROM pacientes WHERE email IS NULL", Integer.class);
    assertThat(sinEmail).isEqualTo(2);
  }

  @Test
  void emailRepetido_loRechazaElConstraintUnico() {
    jdbc.update(INSERT, "Ana", "Uno", "099000001", "foo@bar.com");

    assertThatThrownBy(() -> jdbc.update(INSERT, "Beto", "Dos", "099000002", "foo@bar.com"))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining("uk_pacientes_email");
  }

  @Test
  void emailRepetidoViaJpa_elHandlerDevuelve409DeEmail() {
    pacienteRepository.saveAndFlush(new Paciente("Ana", "Uno", "099000001", "foo@bar.com", null));

    // Simula el alta concurrente que pasa el chequeo previo del service: el que frena
    // es el constraint, y el handler lo traduce con el mensaje real de MySQL.
    DataIntegrityViolationException ex = assertThrows(
        DataIntegrityViolationException.class,
        () -> pacienteRepository.saveAndFlush(
            new Paciente("Beto", "Dos", "099000002", "foo@bar.com", null)));

    var res = new GlobalExceptionHandler().handleDataIntegrityViolation(ex);
    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(res.getBody().message()).isEqualTo("El email ya está registrado");
  }

  @Test
  void creadoPorImport_seAceptaYSeLeeComoEnum() {
    jdbc.update("INSERT INTO pacientes (nombre, apellido, telefono, email, creado_por) "
        + "VALUES ('Imp', 'Ortado', '099000003', NULL, 'IMPORT')");
    Long id = jdbc.queryForObject("SELECT id FROM pacientes WHERE nombre = 'Imp'", Long.class);

    assertThat(pacienteRepository.findById(id)).get()
        .extracting(Paciente::getCreadoPor).isEqualTo(PacienteOrigen.IMPORT);
  }

  @Test
  void reservaPublica_emailConOtroFormato_reusaElPacienteExistente() {
    DisponibilidadSemanal lunes = disponibilidadRepository.findByDiaSemana(1)
        .orElseGet(DisponibilidadSemanal::new);
    lunes.setDiaSemana(1);
    lunes.setActivo(true);
    lunes.setHoraApertura(LocalTime.of(9, 0));
    lunes.setHoraCierre(LocalTime.of(18, 0));
    lunes.setPausaInicio(null);
    lunes.setPausaFin(null);
    disponibilidadRepository.save(lunes);

    Servicio servicio = new Servicio();
    servicio.setNombre("Limpieza");
    servicio.setPrecio(new BigDecimal("1000.00"));
    servicio.setActivo(true);
    servicio.setDuracionMinutos(30);
    Long servicioId = servicioRepository.save(servicio).getId();

    Long pacienteId = pacienteRepository
        .save(new Paciente("Foo", "Bar", "099000004", "foo@bar.com", null)).getId();

    PublicReservaRequest req = new PublicReservaRequest();
    req.setNombre("Foo");
    req.setApellido("Bar");
    req.setTelefono("099000004");
    req.setEmail("FOO@bar.com ");
    req.setServicioId(servicioId);
    req.setFechaHoraInicio(LocalDateTime.of(2030, 1, 7, 11, 0)); // lunes
    publicReservaService.reservar(req);

    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM pacientes", Integer.class)).isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT paciente_id FROM citas", Long.class)).isEqualTo(pacienteId);
  }
}

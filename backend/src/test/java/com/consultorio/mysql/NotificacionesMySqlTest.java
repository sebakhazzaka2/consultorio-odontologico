package com.consultorio.mysql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import com.consultorio.model.CanalNotificacion;
import com.consultorio.model.Cita;
import com.consultorio.model.CitaEstado;
import com.consultorio.model.EstadoNotificacion;
import com.consultorio.model.Notificacion;
import com.consultorio.model.Paciente;
import com.consultorio.model.TipoNotificacion;
import com.consultorio.repository.CitaRepository;
import com.consultorio.repository.NotificacionRepository;
import com.consultorio.repository.PacienteRepository;
import com.consultorio.service.notificacion.EmailChannel;
import com.consultorio.service.notificacion.NotificacionDispatcher;
import com.consultorio.service.notificacion.NotificacionService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Outbox (V11) contra MySQL 8 real: UNIQUE de dedupe, el INSERT que ignora duplicados no
 * rompe la transacción del caller, FK ON DELETE SET NULL, y dos despachadores en paralelo
 * no envían dos veces la misma fila (FOR UPDATE SKIP LOCKED).
 */
@TestPropertySource(properties = {
    "clinic.flags.email-notifications=true",
    "clinic.nombre=Clinica MySQL",
    "clinic.email=recepcion@mysql.test",
    "notifications.batch-size=3"
})
class NotificacionesMySqlTest extends AbstractMySqlTest {

  @Autowired private JdbcTemplate jdbc;
  @Autowired private NotificacionService service;
  @Autowired private NotificacionDispatcher dispatcher;
  @Autowired private NotificacionRepository notificacionRepository;
  @Autowired private CitaRepository citaRepository;
  @Autowired private PacienteRepository pacienteRepository;
  @Autowired private PlatformTransactionManager txManager;

  /** Canal falso: cuenta envíos por id de notificación y tarda un poco (solapa corridas). */
  @MockBean private EmailChannel emailChannel;

  private final Map<Long, AtomicInteger> enviosPorId = new ConcurrentHashMap<>();

  @BeforeEach
  @AfterEach
  void limpiar() {
    jdbc.update("DELETE FROM notificaciones");
    jdbc.update("DELETE FROM citas");
    jdbc.update("DELETE FROM pacientes");
    enviosPorId.clear();
  }

  @BeforeEach
  void canalFalso() {
    when(emailChannel.canal()).thenReturn(CanalNotificacion.EMAIL);
    when(emailChannel.estaConfigurado()).thenReturn(true);
    doAnswer(inv -> {
      Notificacion n = inv.getArgument(0);
      enviosPorId.computeIfAbsent(n.getId(), k -> new AtomicInteger()).incrementAndGet();
      Thread.sleep(30);
      return null;
    }).when(emailChannel).enviar(any(), any());
  }

  private Cita crearCita(int n) {
    Paciente p = pacienteRepository.save(
        new Paciente("Paciente" + n, "Test", "09900000" + n, "p" + n + "@mysql.test", null));
    Cita c = new Cita();
    c.setPaciente(p);
    c.setFechaHoraInicio(LocalDateTime.now().plusDays(2).withHour(9 + n).withMinute(0).withSecond(0).withNano(0));
    c.setDuracionMinutos(30);
    c.setEstado(CitaEstado.CONFIRMADA);
    return citaRepository.save(c);
  }

  private int contarNotificaciones() {
    return jdbc.queryForObject("SELECT COUNT(*) FROM notificaciones", Integer.class);
  }

  @Test
  void claveDedupeEsUnica() {
    String insert = "INSERT INTO notificaciones (tipo, canal, clave_dedupe, destinatario_tipo, estado,"
        + " proximo_intento_at) VALUES ('SOLICITUD', 'EMAIL', 'SOLICITUD:1', 'PACIENTE', 'PENDIENTE', NOW())";
    jdbc.update(insert);
    assertThatThrownBy(() -> jdbc.update(insert)).isInstanceOf(DuplicateKeyException.class);
  }

  @Test
  void insertDuplicadoNoRompeLaTransaccionDelCaller() {
    Cita cita = crearCita(1);
    TransactionTemplate tx = new TransactionTemplate(txManager);
    tx.executeWithoutResult(s -> {
      Instant ahora = Instant.now();
      // Saltea el exists previo a propósito: es la carrera entre dos transacciones.
      notificacionRepository.insertarPendienteSiNoExiste("SOLICITUD", "EMAIL", "SOLICITUD:" + cita.getId(),
          cita.getId(), null, "PACIENTE", ahora, ahora);
      notificacionRepository.insertarPendienteSiNoExiste("SOLICITUD", "EMAIL", "SOLICITUD:" + cita.getId(),
          cita.getId(), null, "PACIENTE", ahora, ahora);
      // Por el camino normal también: exists + no-op.
      service.encolar(TipoNotificacion.SOLICITUD, cita);
      // Un cambio de negocio en la misma transacción tiene que commitear igual.
      Cita enTx = citaRepository.findById(cita.getId()).orElseThrow();
      enTx.setNotas("cambio en la misma transaccion");
      citaRepository.save(enTx);
      assertThat(s.isRollbackOnly()).isFalse();
    });
    assertThat(contarNotificaciones()).isEqualTo(1);
    assertThat(citaRepository.findById(cita.getId()).orElseThrow().getNotas())
        .isEqualTo("cambio en la misma transaccion");
  }

  @Test
  void encolarEsAtomicoConElRollbackDelCaller() {
    Cita cita = crearCita(1);
    TransactionTemplate tx = new TransactionTemplate(txManager);
    tx.executeWithoutResult(s -> {
      service.encolar(TipoNotificacion.CONFIRMACION, cita);
      s.setRollbackOnly();
    });
    assertThat(contarNotificaciones()).isZero();
  }

  @Test
  void borrarLaCitaDejaLaNotificacionSinCitaYElDespachadorLaDescarta() {
    Cita cita = crearCita(1);
    service.encolar(TipoNotificacion.CONFIRMACION, cita);
    citaRepository.deleteById(cita.getId());

    Long citaId = jdbc.queryForObject("SELECT cita_id FROM notificaciones", Long.class);
    assertThat(citaId).isNull();
    dispatcher.despachar();
    Notificacion n = notificacionRepository.findAll().get(0);
    assertThat(n.getEstado()).isEqualTo(EstadoNotificacion.DESCARTADA);
    assertThat(enviosPorId).isEmpty();
  }

  @Test
  void dosDespachadoresEnParaleloNoEnvianDosVeces() throws Exception {
    List<Long> esperadas = new ArrayList<>();
    for (int i = 1; i <= 6; i++) {
      Cita c = crearCita(i);
      service.encolar(TipoNotificacion.SOLICITUD, c);
      service.encolar(TipoNotificacion.CONFIRMACION, c);
    }
    notificacionRepository.findAll().forEach(n -> esperadas.add(n.getId()));
    assertThat(esperadas).hasSize(12);

    CountDownLatch largada = new CountDownLatch(1);
    Callable<Integer> corredor = () -> {
      largada.await();
      int total = 0;
      int tomadas;
      do {
        tomadas = dispatcher.despachar();
        total += tomadas;
      } while (tomadas > 0);
      return total;
    };
    ExecutorService pool = Executors.newFixedThreadPool(2);
    try {
      Future<Integer> a = pool.submit(corredor);
      Future<Integer> b = pool.submit(corredor);
      largada.countDown();
      int totalA = a.get(60, TimeUnit.SECONDS);
      int totalB = b.get(60, TimeUnit.SECONDS);
      // Entre los dos tomaron exactamente las 12, sin repetir.
      assertThat(totalA + totalB).isEqualTo(12);
    } finally {
      pool.shutdownNow();
    }

    assertThat(enviosPorId.keySet()).containsExactlyInAnyOrderElementsOf(esperadas);
    assertThat(enviosPorId.values()).allSatisfy(c -> assertThat(c.get()).isEqualTo(1));
    assertThat(notificacionRepository.findAll())
        .allSatisfy(n -> {
          assertThat(n.getEstado()).isEqualTo(EstadoNotificacion.ENVIADA);
          assertThat(n.getIntentos()).isEqualTo(1);
        });
  }
}

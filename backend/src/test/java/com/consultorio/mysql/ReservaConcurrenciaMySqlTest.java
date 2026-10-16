package com.consultorio.mysql;

import static org.assertj.core.api.Assertions.assertThat;

import com.consultorio.dto.PublicReservaRequest;
import com.consultorio.model.DisponibilidadSemanal;
import com.consultorio.model.Servicio;
import com.consultorio.repository.DisponibilidadSemanalRepository;
import com.consultorio.repository.ServicioRepository;
import com.consultorio.service.PublicReservaService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Reserva pública concurrente contra MySQL real: N threads piden el MISMO slot con
 * emails distintos. Solo una debe ganar; el resto falla por "horario no disponible"
 * (IllegalArgumentException) o por timeout del lock (PessimisticLockingFailureException).
 * Cubre el orden "FOR UPDATE antes de cualquier lectura normal" bajo REPEATABLE READ
 * (ver tasks/lessons.md, 2026-10-15). H2 no sirve para esto.
 */
class ReservaConcurrenciaMySqlTest extends AbstractMySqlTest {

  private static final int THREADS = 8;
  // Lunes 2030-01-07 (dia_semana = 1 en la DB)
  private static final LocalDateTime SLOT = LocalDateTime.of(2030, 1, 7, 10, 0);

  @Autowired private PublicReservaService publicReservaService;
  @Autowired private DisponibilidadSemanalRepository disponibilidadRepository;
  @Autowired private ServicioRepository servicioRepository;
  @Autowired private JdbcTemplate jdbc;

  private Long servicioId;

  @BeforeEach
  void seed() {
    limpiar();
    // V7 ya siembra los 7 días; se fija el lunes de forma determinista.
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
    servicioId = servicioRepository.save(servicio).getId();
  }

  @AfterEach
  void limpiar() {
    jdbc.update("DELETE FROM citas");
    jdbc.update("DELETE FROM pacientes");
    jdbc.update("DELETE FROM servicios");
  }

  @RepeatedTest(5)
  void mismoSlotConcurrente_soloUnaReservaGana() throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(THREADS);
    CountDownLatch listos = new CountDownLatch(THREADS);
    CountDownLatch largada = new CountDownLatch(1);
    List<Future<Object>> futuros = new ArrayList<>();
    try {
      for (int i = 0; i < THREADS; i++) {
        final int n = i;
        futuros.add(pool.submit(() -> {
          PublicReservaRequest req = new PublicReservaRequest();
          req.setNombre("Paciente" + n);
          req.setApellido("Test");
          req.setTelefono("0991234" + (10 + n));
          req.setEmail("paciente" + n + "@test.com");
          req.setServicioId(servicioId);
          req.setFechaHoraInicio(SLOT);
          listos.countDown();
          largada.await();
          try {
            return publicReservaService.reservar(req);
          } catch (Exception e) {
            return e;
          }
        }));
      }
      assertThat(listos.await(30, TimeUnit.SECONDS)).isTrue();
      largada.countDown();

      int exitos = 0;
      List<Exception> fallas = new ArrayList<>();
      for (Future<Object> f : futuros) {
        Object r = f.get(60, TimeUnit.SECONDS);
        if (r instanceof Exception e) {
          fallas.add(e);
        } else {
          exitos++;
        }
      }

      assertThat(exitos).as("reservas exitosas").isEqualTo(1);
      assertThat(fallas).hasSize(THREADS - 1);
      assertThat(fallas).allSatisfy(e -> assertThat(e)
          .isInstanceOfAny(IllegalArgumentException.class, PessimisticLockingFailureException.class));
      Integer citas = jdbc.queryForObject(
          "SELECT COUNT(*) FROM citas WHERE fecha_hora_inicio = ?", Integer.class, SLOT);
      assertThat(citas).as("citas en el slot (sin doble reserva)").isEqualTo(1);
    } finally {
      pool.shutdownNow();
    }
  }
}

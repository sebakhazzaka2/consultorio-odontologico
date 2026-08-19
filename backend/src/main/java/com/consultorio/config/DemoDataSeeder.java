package com.consultorio.config;

import com.consultorio.model.Cita;
import com.consultorio.model.CitaEstado;
import com.consultorio.model.HistorialProcedimientos;
import com.consultorio.model.Pago;
import com.consultorio.model.Paciente;
import com.consultorio.model.Servicio;
import com.consultorio.repository.CitaRepository;
import com.consultorio.repository.HistorialProcedimientosRepository;
import com.consultorio.repository.PacienteRepository;
import com.consultorio.repository.PagoRepository;
import com.consultorio.repository.ServicioRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Carga datos ficticios (servicios, pacientes, citas, historial, pagos) para que la
 * instancia demo de portfolio no arranque vacía. Gateado por SEED_DEMO_DATA=true,
 * que solo se setea en la instancia Render — nunca en un cliente real.
 */
@Component
@ConditionalOnProperty(name = "app.seed-demo-data", havingValue = "true")
@Order(2)
public class DemoDataSeeder implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

  private final ServicioRepository servicioRepository;
  private final PacienteRepository pacienteRepository;
  private final CitaRepository citaRepository;
  private final HistorialProcedimientosRepository historialRepository;
  private final PagoRepository pagoRepository;

  public DemoDataSeeder(
      ServicioRepository servicioRepository,
      PacienteRepository pacienteRepository,
      CitaRepository citaRepository,
      HistorialProcedimientosRepository historialRepository,
      PagoRepository pagoRepository) {
    this.servicioRepository = servicioRepository;
    this.pacienteRepository = pacienteRepository;
    this.citaRepository = citaRepository;
    this.historialRepository = historialRepository;
    this.pagoRepository = pagoRepository;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (pacienteRepository.count() > 0) {
      log.info("Demo data already seeded, skipping.");
      return;
    }

    Servicio limpieza = servicio("Limpieza dental", "Limpieza y control de rutina.", "45000", 30,
        "https://images.unsplash.com/photo-1606811841689-23dfddce3e95?auto=format&fit=crop&w=800&q=70");
    Servicio blanqueamiento = servicio("Blanqueamiento", "Blanqueamiento dental profesional en consultorio.", "120000", 60,
        "https://images.unsplash.com/photo-1571772996211-2f02c9727629?auto=format&fit=crop&w=800&q=70");
    Servicio ortodoncia = servicio("Consulta de ortodoncia", "Evaluación inicial para tratamiento de ortodoncia.", "80000", 45,
        "https://images.unsplash.com/photo-1609840114035-3c981b782dfe?auto=format&fit=crop&w=800&q=70");
    Servicio extraccion = servicio("Extracción simple", "Extracción de pieza dental sin complicaciones.", "65000", 30,
        "https://images.unsplash.com/photo-1588776814546-1ffcf47267a5?auto=format&fit=crop&w=800&q=70");
    Servicio endodoncia = servicio("Endodoncia", "Tratamiento de conducto para salvar piezas dañadas.", "150000", 90,
        "https://images.unsplash.com/photo-1606811971618-4486d14f3f99?auto=format&fit=crop&w=800&q=70");
    Servicio implantes = servicio("Implantes dentales", "Reemplazo de piezas perdidas con implante de titanio.", "350000", 90,
        "https://images.unsplash.com/photo-1609207825181-52d3214556dd?auto=format&fit=crop&w=800&q=70");
    Servicio estetica = servicio("Odontología estética", "Carillas y diseño de sonrisa personalizado.", "95000", 60,
        "https://images.unsplash.com/photo-1607613009820-a29f7bb81c04?auto=format&fit=crop&w=800&q=70");
    servicioRepository.saveAll(
        java.util.List.of(limpieza, blanqueamiento, ortodoncia, extraccion, endodoncia, implantes, estetica));

    Paciente ana = pacienteRepository.save(
        new Paciente("Ana", "Rodríguez", "099111222", "ana.rodriguez@example.com", LocalDate.of(1990, 4, 12)));
    Paciente martin = pacienteRepository.save(
        new Paciente("Martín", "Fernández", "099222333", "martin.fernandez@example.com", LocalDate.of(1985, 9, 3)));
    Paciente lucia = pacienteRepository.save(
        new Paciente("Lucía", "Pérez", "099333444", "lucia.perez@example.com", LocalDate.of(2001, 1, 25)));

    LocalDate today = LocalDate.now();
    citaRepository.save(cita(ana, today, LocalTime.of(9, 30), 30, CitaEstado.CONFIRMADA, "Control de rutina"));
    citaRepository.save(cita(martin, today, LocalTime.of(11, 0), 60, CitaEstado.CONFIRMADA, "Blanqueamiento"));
    citaRepository.save(cita(lucia, today.plusDays(2), LocalTime.of(10, 0), 45, CitaEstado.PENDIENTE, "Evaluación ortodoncia"));
    citaRepository.save(cita(ana, today.plusDays(5), LocalTime.of(15, 0), 30, CitaEstado.PENDIENTE, "Extracción"));
    citaRepository.save(cita(martin, today.minusDays(10), LocalTime.of(9, 0), 30, CitaEstado.COMPLETADA, "Limpieza"));
    citaRepository.save(cita(lucia, today.minusDays(3), LocalTime.of(14, 0), 30, CitaEstado.CANCELADA, "Consulta general"));

    HistorialProcedimientos h1 = new HistorialProcedimientos();
    h1.setPaciente(martin);
    h1.setFechaHora(today.minusDays(10).atTime(9, 0));
    h1.setProcedimiento("Limpieza dental");
    h1.setServicio(limpieza);
    h1.setPrecioAplicado(limpieza.getPrecio());
    historialRepository.save(h1);

    HistorialProcedimientos h2 = new HistorialProcedimientos();
    h2.setPaciente(ana);
    h2.setFechaHora(today.minusDays(30).atTime(10, 0));
    h2.setProcedimiento("Blanqueamiento");
    h2.setServicio(blanqueamiento);
    h2.setPrecioAplicado(blanqueamiento.getPrecio());
    historialRepository.save(h2);

    Pago pago = new Pago();
    pago.setPaciente(martin);
    pago.setMonto(new BigDecimal("30000"));
    pago.setFecha(today.minusDays(9));
    pago.setConcepto("Pago parcial limpieza dental");
    pagoRepository.save(pago);

    log.info("Demo data seeded: {} servicios, {} pacientes, {} citas", 7, 3, 6);
  }

  private Servicio servicio(String nombre, String descripcion, String precio, int duracionMinutos, String fotoUrl) {
    Servicio s = new Servicio();
    s.setNombre(nombre);
    s.setDescripcion(descripcion);
    s.setPrecio(new BigDecimal(precio));
    s.setActivo(true);
    s.setDuracionMinutos(duracionMinutos);
    s.setFotoUrl(fotoUrl);
    return s;
  }

  private Cita cita(
      Paciente paciente, LocalDate fecha, LocalTime hora, int duracionMinutos, CitaEstado estado, String motivo) {
    Cita c = new Cita();
    c.setPaciente(paciente);
    c.setFechaHoraInicio(LocalDateTime.of(fecha, hora));
    c.setDuracionMinutos(duracionMinutos);
    c.setEstado(estado);
    c.setMotivo(motivo);
    return c;
  }
}

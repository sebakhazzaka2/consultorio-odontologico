package com.consultorio.service;

import com.consultorio.dto.PublicReservaRequest;
import com.consultorio.dto.PublicReservaResponse;
import com.consultorio.exception.ReservaEnProcesoException;
import com.consultorio.exception.ResourceNotFoundException;
import com.consultorio.model.Cita;
import com.consultorio.model.CitaEstado;
import com.consultorio.model.Paciente;
import com.consultorio.model.PacienteOrigen;
import com.consultorio.model.Servicio;
import com.consultorio.repository.CitaRepository;
import com.consultorio.repository.PacienteRepository;
import com.consultorio.repository.ServicioRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublicReservaService {

  private static final Logger log = LoggerFactory.getLogger(PublicReservaService.class);

  // Segundos que esperamos el GET_LOCK antes de rendirnos y pedirle al cliente que reintente.
  private static final int LOCK_TIMEOUT_SEGUNDOS = 10;

  private final CitaRepository citaRepository;
  private final PacienteRepository pacienteRepository;
  private final ServicioRepository servicioRepository;
  private final CitaService citaService;
  private final EntityManager entityManager;

  public PublicReservaService(CitaRepository citaRepository,
      PacienteRepository pacienteRepository,
      ServicioRepository servicioRepository,
      CitaService citaService,
      EntityManager entityManager) {
    this.citaRepository = citaRepository;
    this.pacienteRepository = pacienteRepository;
    this.servicioRepository = servicioRepository;
    this.citaService = citaService;
    this.entityManager = entityManager;
  }

  public List<String> getSlots(LocalDate fecha, Long servicioId) {
    Servicio servicio = servicioRepository.findById(servicioId)
        .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado con id: " + servicioId));
    return citaService.getDisponibilidad(fecha, servicio.getDuracionMinutos());
  }

  @Transactional
  public PublicReservaResponse reservar(PublicReservaRequest request) {
    Servicio servicio = servicioRepository.findById(request.getServicioId())
        .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado con id: " + request.getServicioId()));

    LocalDate fecha = request.getFechaHoraInicio().toLocalDate();

    // Lock a nivel de conexión MySQL (GET_LOCK), uno por día: dos reservas públicas
    // para el mismo día no pueden evaluar disponibilidad + guardar en simultáneo.
    // Sin esto, dos requests concurrentes para el mismo horario pasan ambas el chequeo
    // de getDisponibilidad() (ninguna ve todavía la cita de la otra) y las dos insertan.
    // Vive dentro de @Transactional: misma conexión física de principio a fin, así el
    // RELEASE_LOCK siempre corre en la misma sesión que pidió el lock.
    String lockName = "reserva_publica_" + fecha;
    Object lockResult = entityManager
        .createNativeQuery("SELECT GET_LOCK(:name, :timeout)")
        .setParameter("name", lockName)
        .setParameter("timeout", LOCK_TIMEOUT_SEGUNDOS)
        .getSingleResult();
    if (!Integer.valueOf(1).equals(lockResult)) {
      log.warn("No se pudo obtener el lock de reserva para la fecha {} (timeout {}s)", fecha, LOCK_TIMEOUT_SEGUNDOS);
      throw new ReservaEnProcesoException(
          "Hay otra reserva en proceso para ese día. Intentá de nuevo en unos segundos.");
    }
    try {
      return reservarConLock(request, servicio, fecha);
    } finally {
      entityManager.createNativeQuery("SELECT RELEASE_LOCK(:name)")
          .setParameter("name", lockName)
          .getSingleResult();
    }
  }

  private PublicReservaResponse reservarConLock(PublicReservaRequest request, Servicio servicio, LocalDate fecha) {
    // Verificar que el slot solicitado esté disponible
    List<String> slotsDisponibles = citaService.getDisponibilidad(fecha, servicio.getDuracionMinutos());
    String horaSlot = request.getFechaHoraInicio().toLocalTime().toString().substring(0, 5);
    if (!slotsDisponibles.contains(horaSlot)) {
      throw new IllegalArgumentException("El horario seleccionado no está disponible");
    }

    Paciente paciente = resolverPaciente(request);

    Cita cita = new Cita();
    cita.setPaciente(paciente);
    cita.setFechaHoraInicio(request.getFechaHoraInicio());
    cita.setDuracionMinutos(servicio.getDuracionMinutos());
    cita.setEstado(CitaEstado.PENDIENTE);
    cita.setMotivo(servicio.getNombre());

    Cita creada = citaRepository.save(cita);
    log.info("Reserva pública creada — cita id: {}, paciente id: {}, servicio id: {}, inicio: {}",
        creada.getId(), paciente.getId(), servicio.getId(), creada.getFechaHoraInicio());

    // Devolvemos el nombre/apellido tal como los mandó quien reservó, nunca los que
    // ya estaban guardados en el paciente existente (dedupe por email): si alguien
    // reserva con un email ajeno, esto evita que la respuesta filtre el nombre real
    // de ese paciente a un desconocido no autenticado.
    return new PublicReservaResponse(
        creada.getId(),
        request.getNombre(),
        request.getApellido(),
        servicio.getNombre(),
        creada.getFechaHoraInicio(),
        creada.getDuracionMinutos(),
        creada.getEstado().name(),
        "Tu solicitud fue recibida. Te contactaremos para confirmar tu turno.");
  }

  /**
   * Dedupe por email, pero solo si el nombre coincide con el paciente ya guardado.
   * Si alguien reserva con el email de otro paciente pero un nombre distinto, lo
   * tratamos como una persona distinta (paciente nuevo) en vez de colgarle una cita
   * a la cuenta real — email solo no alcanza para verificar identidad en un endpoint
   * público sin login.
   */
  private Paciente resolverPaciente(PublicReservaRequest request) {
    return pacienteRepository.findByEmail(request.getEmail())
        .filter(existente -> coincideNombre(existente, request))
        .orElseGet(() -> crearPacienteDesdeAgenda(request));
  }

  private boolean coincideNombre(Paciente existente, PublicReservaRequest request) {
    return normalizar(existente.getNombre()).equals(normalizar(request.getNombre()))
        && normalizar(existente.getApellido()).equals(normalizar(request.getApellido()));
  }

  private String normalizar(String valor) {
    return Objects.requireNonNullElse(valor, "").trim().toLowerCase();
  }

  private Paciente crearPacienteDesdeAgenda(PublicReservaRequest request) {
    Paciente paciente = new Paciente();
    paciente.setNombre(request.getNombre());
    paciente.setApellido(request.getApellido());
    paciente.setTelefono(request.getTelefono());
    paciente.setEmail(request.getEmail());
    paciente.setCreadoPor(PacienteOrigen.AGENDA);
    Paciente creado = pacienteRepository.save(paciente);
    log.info("Paciente creado desde agenda pública — id: {}", creado.getId());
    return creado;
  }
}

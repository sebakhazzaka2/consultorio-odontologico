package com.consultorio.service;

import com.consultorio.dto.PublicReservaRequest;
import com.consultorio.dto.PublicReservaResponse;
import com.consultorio.exception.ResourceNotFoundException;
import com.consultorio.model.Cita;
import com.consultorio.model.CitaEstado;
import com.consultorio.model.Paciente;
import com.consultorio.model.PacienteOrigen;
import com.consultorio.model.Servicio;
import com.consultorio.repository.CitaRepository;
import com.consultorio.repository.PacienteRepository;
import com.consultorio.repository.ServicioRepository;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublicReservaService {

  private static final Logger log = LoggerFactory.getLogger(PublicReservaService.class);

  private final CitaRepository citaRepository;
  private final PacienteRepository pacienteRepository;
  private final ServicioRepository servicioRepository;
  private final CitaService citaService;

  public PublicReservaService(CitaRepository citaRepository,
      PacienteRepository pacienteRepository,
      ServicioRepository servicioRepository,
      CitaService citaService) {
    this.citaRepository = citaRepository;
    this.pacienteRepository = pacienteRepository;
    this.servicioRepository = servicioRepository;
    this.citaService = citaService;
  }

  public List<String> getSlots(LocalDate fecha, Long servicioId) {
    Servicio servicio = servicioRepository.findById(servicioId)
        .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado con id: " + servicioId));
    return citaService.getDisponibilidad(fecha, servicio.getDuracionMinutos());
  }

  @Transactional(isolation = Isolation.REPEATABLE_READ)
  public PublicReservaResponse reservar(PublicReservaRequest request) {
    LocalDate fecha = request.getFechaHoraInicio().toLocalDate();

    // Lock pesimista de fila (FOR UPDATE) — DEBE ser la primerísima sentencia SQL de
    // la transacción, antes de CUALQUIER lectura normal (ni siquiera el servicio).
    // Motivo: bajo REPEATABLE READ (default de MySQL), el snapshot de la transacción
    // se fija en su primera lectura NORMAL (no FOR UPDATE) — una vez fijado, lecturas
    // normales posteriores en la misma transacción siguen viendo ese snapshot viejo,
    // aunque ya hayan pasado por un FOR UPDATE en el medio. Si acá se leyera el
    // servicio (lectura normal) antes de este lock, el snapshot quedaría fijado
    // ANTES de esperar al lock, y la lectura de disponibilidad de más abajo (también
    // normal) no vería una cita recién comiteada por la transacción que estábamos
    // esperando — doble reserva real, verificado con un test de 2 threads vía JDBC
    // contra MySQL: con la lectura de servicio antes del lock, 5/5 corridas insertan
    // 2 citas para el mismo slot; con el lock primero, 5/5 insertan 1 sola.
    citaService.lockDiaParaReserva(fecha);

    Servicio servicio = servicioRepository.findById(request.getServicioId())
        .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado con id: " + request.getServicioId()));

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
   * Dedupe por email: reutiliza el paciente existente si el email ya está
   * registrado. Deliberadamente NO exige que el nombre coincida: email es la única
   * clave de identidad que tenemos en un endpoint público sin login, y
   * {@code pacientes.email} no tiene constraint único en la base — el resto de la
   * app (ej. {@code PacienteService}) asume que hay a lo sumo un paciente por email.
   * Crear un segundo paciente con el mismo email rompe esa asunción: cualquier
   * {@code findByEmail} posterior para ese email (login futuro, edición de otro
   * paciente, esta misma reserva) revienta con
   * {@code IncorrectResultSizeDataAccessException} y bloquea reservas legítimas
   * (acentos, un padre reservando para su hijo con el mismo mail familiar, etc.).
   * El nombre/apellido real nunca se expone en la respuesta (ver {@code reservar}),
   * así que no filtramos PII aunque reusemos el paciente.
   */
  private Paciente resolverPaciente(PublicReservaRequest request) {
    return pacienteRepository.findByEmail(request.getEmail())
        .orElseGet(() -> crearPacienteDesdeAgenda(request));
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

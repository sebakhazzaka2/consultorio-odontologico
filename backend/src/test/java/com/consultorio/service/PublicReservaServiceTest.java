package com.consultorio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.consultorio.dto.PublicReservaRequest;
import com.consultorio.dto.PublicReservaResponse;
import com.consultorio.exception.ReservaEnProcesoException;
import com.consultorio.model.Paciente;
import com.consultorio.model.Servicio;
import com.consultorio.repository.CitaRepository;
import com.consultorio.repository.PacienteRepository;
import com.consultorio.repository.ServicioRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Tests unitarios con mocks — no levantan Spring ni tocan H2. La verificación del
 * GET_LOCK/RELEASE_LOCK de verdad (contra MySQL real) queda para un test manual en
 * un ambiente con MySQL, porque H2 no implementa esas funciones (MODE=MySQL no las
 * emula) y romper eso por culpa de los tests de integración no es el objetivo acá.
 */
class PublicReservaServiceTest {

  private CitaRepository citaRepository;
  private PacienteRepository pacienteRepository;
  private ServicioRepository servicioRepository;
  private CitaService citaService;
  private EntityManager entityManager;
  private PublicReservaService service;

  private Servicio servicio;

  @BeforeEach
  void setUp() {
    citaRepository = mock(CitaRepository.class);
    pacienteRepository = mock(PacienteRepository.class);
    servicioRepository = mock(ServicioRepository.class);
    citaService = mock(CitaService.class);
    entityManager = mock(EntityManager.class);
    service = new PublicReservaService(
        citaRepository, pacienteRepository, servicioRepository, citaService, entityManager);

    servicio = new Servicio();
    servicio.setId(1L);
    servicio.setNombre("Limpieza");
    servicio.setDuracionMinutos(30);
    when(servicioRepository.findById(1L)).thenReturn(Optional.of(servicio));

    // GET_LOCK y RELEASE_LOCK: simulamos que siempre se obtiene el lock (resultado 1).
    Query lockQuery = mock(Query.class);
    when(lockQuery.setParameter(anyString(), any())).thenReturn(lockQuery);
    when(lockQuery.getSingleResult()).thenReturn(1);
    when(entityManager.createNativeQuery(anyString())).thenReturn(lockQuery);

    when(citaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
  }

  private PublicReservaRequest request(String nombre, String apellido, String email) {
    PublicReservaRequest req = new PublicReservaRequest();
    req.setNombre(nombre);
    req.setApellido(apellido);
    req.setTelefono("099123456");
    req.setEmail(email);
    req.setServicioId(1L);
    req.setFechaHoraInicio(LocalDateTime.of(2030, 1, 7, 10, 0));
    when(citaService.getDisponibilidad(req.getFechaHoraInicio().toLocalDate(), 30))
        .thenReturn(List.of("10:00"));
    return req;
  }

  @Test
  void reservar_emailExistente_mismoNombre_reutilizaPaciente() {
    Paciente existente = new Paciente();
    existente.setId(5L);
    existente.setNombre("Ana");
    existente.setApellido("Pérez");
    existente.setEmail("ana@test.com");
    when(pacienteRepository.findByEmail("ana@test.com")).thenReturn(Optional.of(existente));

    PublicReservaRequest req = request("ana", " pérez ", "ana@test.com");
    service.reservar(req);

    verify(pacienteRepository, org.mockito.Mockito.never()).save(any());
  }

  @Test
  void reservar_emailExistente_nombreDistinto_creaPacienteNuevo_noFiltraElReal() {
    Paciente existente = new Paciente();
    existente.setId(5L);
    existente.setNombre("Ana");
    existente.setApellido("Pérez");
    existente.setEmail("ana@test.com");
    when(pacienteRepository.findByEmail("ana@test.com")).thenReturn(Optional.of(existente));
    when(pacienteRepository.save(any())).thenAnswer(inv -> {
      Paciente p = inv.getArgument(0);
      p.setId(99L);
      return p;
    });

    // Alguien que conoce el email de Ana pero no es ella, manda otro nombre.
    PublicReservaRequest req = request("Desconocido", "Atacante", "ana@test.com");
    PublicReservaResponse response = service.reservar(req);

    // Se crea un paciente nuevo (no se reutiliza el de Ana)...
    verify(pacienteRepository).save(any());
    // ...y la respuesta nunca expone el nombre real de Ana.
    assertThat(response.nombrePaciente()).isEqualTo("Desconocido");
    assertThat(response.apellidoPaciente()).isEqualTo("Atacante");
  }

  @Test
  void reservar_noConsigueLock_lanzaReservaEnProceso() {
    Query lockQuery = mock(Query.class);
    when(lockQuery.setParameter(anyString(), any())).thenReturn(lockQuery);
    when(lockQuery.getSingleResult()).thenReturn(0); // GET_LOCK no lo consiguió (timeout)
    when(entityManager.createNativeQuery(anyString())).thenReturn(lockQuery);

    PublicReservaRequest req = request("Ana", "Pérez", "ana@test.com");

    assertThatThrownBy(() -> service.reservar(req))
        .isInstanceOf(ReservaEnProcesoException.class);
  }

  @Test
  void reservar_siempreLiberaElLock_incluseSiFalla() {
    when(citaService.getDisponibilidad(any(), anyInt())).thenReturn(List.of()); // slot no disponible
    PublicReservaRequest req = request("Ana", "Pérez", "ana@test.com");
    req.setFechaHoraInicio(LocalDateTime.of(2030, 1, 7, 11, 0)); // no está en la lista de slots

    assertThatThrownBy(() -> service.reservar(req)).isInstanceOf(IllegalArgumentException.class);

    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    verify(entityManager, org.mockito.Mockito.atLeastOnce()).createNativeQuery(sqlCaptor.capture());
    assertThat(sqlCaptor.getAllValues()).anyMatch(sql -> sql.contains("RELEASE_LOCK"));
  }
}

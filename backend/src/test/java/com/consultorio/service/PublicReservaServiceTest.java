package com.consultorio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.consultorio.dto.PublicReservaRequest;
import com.consultorio.dto.PublicReservaResponse;
import com.consultorio.model.Cita;
import com.consultorio.model.Paciente;
import com.consultorio.model.PacienteOrigen;
import com.consultorio.model.Servicio;
import com.consultorio.repository.CitaRepository;
import com.consultorio.repository.PacienteRepository;
import com.consultorio.repository.ServicioRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Tests unitarios con mocks — no levantan Spring ni tocan H2. El lock real
 * (SELECT ... FOR UPDATE) se verifica aparte, con un test de concurrencia contra
 * MySQL real (no H2: H2 no soporta bien el mismo comportamiento de bloqueo de filas
 * entre conexiones distintas que InnoDB).
 */
class PublicReservaServiceTest {

  private CitaRepository citaRepository;
  private PacienteRepository pacienteRepository;
  private ServicioRepository servicioRepository;
  private CitaService citaService;
  private PublicReservaService service;

  private Servicio servicio;

  @BeforeEach
  void setUp() {
    citaRepository = mock(CitaRepository.class);
    pacienteRepository = mock(PacienteRepository.class);
    servicioRepository = mock(ServicioRepository.class);
    citaService = mock(CitaService.class);
    service = new PublicReservaService(citaRepository, pacienteRepository, servicioRepository, citaService);

    servicio = new Servicio();
    servicio.setId(1L);
    servicio.setNombre("Limpieza");
    servicio.setDuracionMinutos(30);
    when(servicioRepository.findById(1L)).thenReturn(Optional.of(servicio));

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
  void reservar_pideElLockAntesDeLeerDisponibilidad() {
    when(pacienteRepository.findByEmail(any())).thenReturn(Optional.empty());
    when(pacienteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    PublicReservaRequest req = request("Ana", "Pérez", "ana@test.com");
    service.reservar(req);

    verify(citaService).lockDiaParaReserva(req.getFechaHoraInicio().toLocalDate());
  }

  @Test
  void reservar_emailExistente_reutilizaPacienteSinImportarElNombre() {
    // Dedupe es solo por email: name-matching se sacó porque rompía reservas
    // legítimas (acentos, email familiar compartido) sin frenar a un atacante
    // decidido que ya sabe el nombre real de la víctima.
    Paciente existente = new Paciente();
    existente.setId(5L);
    existente.setNombre("Ana");
    existente.setApellido("Pérez");
    existente.setEmail("ana@test.com");
    when(pacienteRepository.findByEmail("ana@test.com")).thenReturn(Optional.of(existente));

    PublicReservaRequest req = request("Alguien Distinto", "Otro Apellido", "ana@test.com");
    PublicReservaResponse response = service.reservar(req);

    // No se crea un segundo paciente con el mismo email (rompería
    // findByEmail en cualquier otro lado de la app que asuma unicidad).
    verify(pacienteRepository, never()).save(any());
    // La respuesta sigue sin filtrar el nombre real guardado de Ana.
    assertThat(response.nombrePaciente()).isEqualTo("Alguien Distinto");
    assertThat(response.apellidoPaciente()).isEqualTo("Otro Apellido");
  }

  @Test
  void reservar_emailConOtroCaseYEspacios_reutilizaPacienteExistente() {
    Paciente existente = new Paciente();
    existente.setId(7L);
    existente.setEmail("foo@bar.com");
    when(pacienteRepository.findByEmail("foo@bar.com")).thenReturn(Optional.of(existente));

    service.reservar(request("Foo", "Bar", "FOO@bar.com "));

    verify(pacienteRepository).findByEmail("foo@bar.com");
    verify(pacienteRepository, never()).save(any());
    ArgumentCaptor<Cita> cita = ArgumentCaptor.forClass(Cita.class);
    verify(citaRepository).save(cita.capture());
    assertThat(cita.getValue().getPaciente().getId()).isEqualTo(7L);
  }

  @Test
  void reservar_emailNuevo_guardaElPacienteConEmailNormalizado() {
    when(pacienteRepository.findByEmail("nueva@test.com")).thenReturn(Optional.empty());
    when(pacienteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    service.reservar(request("Nueva", "Paciente", "  Nueva@Test.com"));

    ArgumentCaptor<Paciente> paciente = ArgumentCaptor.forClass(Paciente.class);
    verify(pacienteRepository).save(paciente.capture());
    assertThat(paciente.getValue().getEmail()).isEqualTo("nueva@test.com");
    assertThat(paciente.getValue().getCreadoPor()).isEqualTo(PacienteOrigen.AGENDA);
  }

  @Test
  void reservar_emailNuevo_creaPaciente() {
    when(pacienteRepository.findByEmail("nuevo@test.com")).thenReturn(Optional.empty());
    when(pacienteRepository.save(any())).thenAnswer(inv -> {
      Paciente p = inv.getArgument(0);
      p.setId(99L);
      return p;
    });

    PublicReservaRequest req = request("Juan", "García", "nuevo@test.com");
    service.reservar(req);

    verify(pacienteRepository).save(any());
  }

  @Test
  void reservar_slotNoDisponible_lanzaExcepcionYNoGuardaNada() {
    PublicReservaRequest req = request("Ana", "Pérez", "ana@test.com");
    // request() ya stubeó getDisponibilidad con "10:00" disponible — lo pisamos acá,
    // DESPUÉS (Mockito usa el último stub que matchee la invocación), para simular
    // que no hay ningún slot libre.
    when(citaService.getDisponibilidad(any(), anyInt())).thenReturn(List.of());

    assertThatThrownBy(() -> service.reservar(req)).isInstanceOf(IllegalArgumentException.class);

    verify(citaRepository, never()).save(any());
  }
}

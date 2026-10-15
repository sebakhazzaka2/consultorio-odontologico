package com.consultorio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.consultorio.model.Cita;
import com.consultorio.model.CitaEstado;
import com.consultorio.model.Paciente;
import com.consultorio.repository.CitaRepository;
import com.consultorio.repository.PacienteRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CitaServiceTest {

  private CitaRepository citaRepository;
  private CitaService citaService;

  @BeforeEach
  void setUp() {
    citaRepository = mock(CitaRepository.class);
    PacienteRepository pacienteRepository = mock(PacienteRepository.class);
    DisponibilidadService disponibilidadService = mock(DisponibilidadService.class);
    citaService = new CitaService(citaRepository, pacienteRepository, disponibilidadService);
    when(citaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
  }

  private Cita citaPendiente(Long id, LocalDateTime inicio, int duracionMinutos) {
    Cita cita = new Cita();
    cita.setId(id);
    cita.setPaciente(new Paciente());
    cita.setFechaHoraInicio(inicio);
    cita.setDuracionMinutos(duracionMinutos);
    cita.setEstado(CitaEstado.PENDIENTE);
    return cita;
  }

  @Test
  void confirmar_sinSolapamiento_pasaACONFIRMADA() {
    LocalDateTime inicio = LocalDateTime.of(2030, 1, 7, 10, 0);
    Cita cita = citaPendiente(1L, inicio, 30);
    when(citaRepository.findById(1L)).thenReturn(Optional.of(cita));
    when(citaRepository.findByFechaHoraInicioBetweenAndEstadoIn(any(), any(), any()))
        .thenReturn(List.of()); // no hay otras citas

    citaService.confirmar(1L, null);

    assertThat(cita.getEstado()).isEqualTo(CitaEstado.CONFIRMADA);
  }

  @Test
  void confirmar_solapaConOtraCitaConfirmada_lanzaExcepcionYNoConfirma() {
    LocalDateTime inicio = LocalDateTime.of(2030, 1, 7, 10, 0);
    Cita pendiente = citaPendiente(1L, inicio, 30);
    when(citaRepository.findById(1L)).thenReturn(Optional.of(pendiente));

    Cita otraConfirmada = citaPendiente(2L, inicio.plusMinutes(15), 30); // se superpone 10:15-10:30
    otraConfirmada.setEstado(CitaEstado.CONFIRMADA);
    when(citaRepository.findByFechaHoraInicioBetweenAndEstadoIn(any(), any(), any()))
        .thenReturn(List.of(otraConfirmada));

    assertThatThrownBy(() -> citaService.confirmar(1L, null))
        .isInstanceOf(IllegalArgumentException.class);

    // La cita pendiente original no debe haber quedado confirmada.
    assertThat(pendiente.getEstado()).isEqualTo(CitaEstado.PENDIENTE);
  }

  @Test
  void confirmar_conDuracionOverrideQueGeneraSolapamiento_lanzaExcepcion() {
    LocalDateTime inicio = LocalDateTime.of(2030, 1, 7, 10, 0);
    // Pendiente de 15 min (10:00-10:15) no se solapa con una cita a las 10:20...
    Cita pendiente = citaPendiente(1L, inicio, 15);
    when(citaRepository.findById(1L)).thenReturn(Optional.of(pendiente));

    Cita otra = citaPendiente(2L, inicio.plusMinutes(20), 30); // 10:20-10:50
    otra.setEstado(CitaEstado.CONFIRMADA);
    when(citaRepository.findByFechaHoraInicioBetweenAndEstadoIn(any(), any(), any()))
        .thenReturn(List.of(otra));

    // ...pero si al confirmar se extiende a 30 min (10:00-10:30), ahora sí se solapa.
    assertThatThrownBy(() -> citaService.confirmar(1L, 30))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void haySolapamiento_considerAPendienteYConfirmada() {
    // create() usa haySolapamiento() internamente — lo probamos vía create() para no
    // depender de que el método privado sea accesible.
    LocalDateTime inicio = LocalDateTime.of(2030, 1, 7, 10, 0);
    com.consultorio.dto.CitaRequest request = new com.consultorio.dto.CitaRequest();
    request.setPacienteId(1L);
    request.setFechaHoraInicio(inicio);
    request.setDuracionMinutos(30);
    request.setMotivo("Consulta");

    PacienteRepository pacienteRepository = mock(PacienteRepository.class);
    when(pacienteRepository.findById(1L)).thenReturn(Optional.of(new Paciente()));
    DisponibilidadService disponibilidadService = mock(DisponibilidadService.class);
    CitaService service = new CitaService(citaRepository, pacienteRepository, disponibilidadService);

    Cita pendienteExistente = citaPendiente(9L, inicio, 30); // PENDIENTE, mismo horario exacto
    when(citaRepository.findByFechaHoraInicioBetweenAndEstadoIn(any(), any(), eq(
        List.of(CitaEstado.CONFIRMADA, CitaEstado.PENDIENTE))))
        .thenReturn(List.of(pendienteExistente));

    assertThatThrownBy(() -> service.create(request))
        .isInstanceOf(IllegalArgumentException.class);
  }
}

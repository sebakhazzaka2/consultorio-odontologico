package com.consultorio.service;

import com.consultorio.dto.PagoRequest;
import com.consultorio.dto.PagoResponse;
import com.consultorio.dto.SaldoPacienteResponse;
import com.consultorio.exception.ResourceNotFoundException;
import com.consultorio.model.HistorialClinico;
import com.consultorio.model.Paciente;
import com.consultorio.model.Pago;
import com.consultorio.repository.HistorialRepository;
import com.consultorio.repository.PacienteRepository;
import com.consultorio.repository.PagoRepository;
import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PagoService {

  private static final Logger log = LoggerFactory.getLogger(PagoService.class);

  private final PagoRepository pagoRepository;
  private final PacienteRepository pacienteRepository;
  private final HistorialRepository historialRepository;

  public PagoService(
      PagoRepository pagoRepository,
      PacienteRepository pacienteRepository,
      HistorialRepository historialRepository) {
    this.pagoRepository = pagoRepository;
    this.pacienteRepository = pacienteRepository;
    this.historialRepository = historialRepository;
  }

  public List<PagoResponse> findAll() {
    return pagoRepository.findAll().stream().map(this::toResponse).toList();
  }

  public PagoResponse findById(Long id) {
    Pago pago =
        pagoRepository
            .findById(id)
            .orElseThrow(
                () -> new ResourceNotFoundException("Pago no encontrado con id: " + id));
    return toResponse(pago);
  }

  public List<PagoResponse> findByPaciente(Long pacienteId) {
    pacienteRepository
        .findById(pacienteId)
        .orElseThrow(
            () -> new ResourceNotFoundException("Paciente no encontrado con id: " + pacienteId));
    return pagoRepository
        .findByPacienteIdOrderByFechaDesc(pacienteId)
        .stream()
        .map(this::toResponse)
        .toList();
  }

  public PagoResponse create(PagoRequest request) {
    Long pacienteId = request.getPacienteId();
    Paciente paciente =
        pacienteRepository
            .findById(pacienteId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Paciente no encontrado con id: " + pacienteId));

    Pago pago = new Pago();
    pago.setPaciente(paciente);
    pago.setMonto(request.getMonto());
    pago.setFecha(request.getFecha());
    pago.setConcepto(request.getConcepto());

    Pago creado = pagoRepository.save(pago);
    log.info("Pago registrado — id: {}, paciente: {}, monto: {}", creado.getId(), pacienteId, creado.getMonto());
    return toResponse(creado);
  }

  public void delete(Long id) {
    findById(id);
    pagoRepository.deleteById(id);
    log.info("Pago eliminado — id: {}", id);
  }

  public SaldoPacienteResponse getSaldoPaciente(Long pacienteId) {
    Paciente paciente =
        pacienteRepository
            .findById(pacienteId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Paciente no encontrado con id: " + pacienteId));

    List<HistorialClinico> historiales =
        historialRepository.findByPacienteIdOrderByFechaHoraDesc(pacienteId);
    BigDecimal totalDeuda =
        historiales.stream()
            .map(HistorialClinico::getPrecioAplicado)
            .filter(precio -> precio != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    List<Pago> pagos = pagoRepository.findByPacienteIdOrderByFechaDesc(pacienteId);
    BigDecimal totalPagado =
        pagos.stream()
            .map(Pago::getMonto)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal saldoPendiente = totalDeuda.subtract(totalPagado);

    log.info("Saldo consultado — paciente: {}, deuda total: {}, pagado: {}, pendiente: {}",
        pacienteId, totalDeuda, totalPagado, saldoPendiente);
    return new SaldoPacienteResponse(
        paciente.getId(),
        paciente.getNombre(),
        paciente.getApellido(),
        totalDeuda,
        totalPagado,
        saldoPendiente);
  }

  private PagoResponse toResponse(Pago pago) {
    Paciente paciente = pago.getPaciente();
    return new PagoResponse(
        pago.getId(),
        paciente.getId(),
        paciente.getNombre(),
        paciente.getApellido(),
        pago.getMonto(),
        pago.getFecha(),
        pago.getConcepto(),
        pago.getCreatedAt());
  }
}

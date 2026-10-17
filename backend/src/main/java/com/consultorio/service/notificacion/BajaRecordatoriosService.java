package com.consultorio.service.notificacion;

import com.consultorio.config.ClinicProperties;
import com.consultorio.dto.BajaRecordatoriosResponse;
import com.consultorio.exception.TokenBajaInvalidoException;
import com.consultorio.model.Paciente;
import com.consultorio.repository.PacienteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Baja de recordatorios por email desde el link del mail (sin login). */
@Service
public class BajaRecordatoriosService {

  private static final Logger log = LoggerFactory.getLogger(BajaRecordatoriosService.class);

  private final BajaTokenService tokens;
  private final PacienteRepository pacienteRepository;
  private final ClinicProperties clinic;

  public BajaRecordatoriosService(
      BajaTokenService tokens, PacienteRepository pacienteRepository, ClinicProperties clinic) {
    this.tokens = tokens;
    this.pacienteRepository = pacienteRepository;
    this.clinic = clinic;
  }

  @Transactional(readOnly = true)
  public BajaRecordatoriosResponse consultar(String token) {
    Paciente p = pacienteDelToken(token);
    return new BajaRecordatoriosResponse(
        clinic.getNombre(),
        p.isRecordatoriosEmail() ? BajaRecordatoriosResponse.ACTIVO : BajaRecordatoriosResponse.BAJA);
  }

  /** Idempotente: dar de baja a alguien ya dado de baja no cambia nada. */
  @Transactional
  public void darDeBaja(String token) {
    Paciente p = pacienteDelToken(token);
    if (p.isRecordatoriosEmail()) {
      p.setRecordatoriosEmail(false);
      pacienteRepository.save(p);
      log.info("Paciente dado de baja de recordatorios por email — id: {}", p.getId());
    }
  }

  private Paciente pacienteDelToken(String token) {
    Long id = tokens.verificar(token).orElseThrow(TokenBajaInvalidoException::new);
    return pacienteRepository.findById(id).orElseThrow(TokenBajaInvalidoException::new);
  }
}

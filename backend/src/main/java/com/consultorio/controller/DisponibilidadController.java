package com.consultorio.controller;

import com.consultorio.dto.DisponibilidadSemanalRequest;
import com.consultorio.dto.DisponibilidadSemanalResponse;
import com.consultorio.dto.FechaBloqueadaRequest;
import com.consultorio.dto.FechaBloqueadaResponse;
import com.consultorio.service.DisponibilidadService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/disponibilidad")
public class DisponibilidadController {

  private final DisponibilidadService disponibilidadService;

  public DisponibilidadController(DisponibilidadService disponibilidadService) {
    this.disponibilidadService = disponibilidadService;
  }

  // ── Disponibilidad semanal ────────────────────────────────────────────────

  @GetMapping("/semanal")
  public List<DisponibilidadSemanalResponse> getSemanal() {
    return disponibilidadService.findAllSemanal();
  }

  @PutMapping("/semanal")
  public DisponibilidadSemanalResponse upsertDia(
      @Valid @RequestBody DisponibilidadSemanalRequest request) {
    return disponibilidadService.upsertDia(request);
  }

  // ── Fechas bloqueadas ─────────────────────────────────────────────────────

  @GetMapping("/fechas-bloqueadas")
  public List<FechaBloqueadaResponse> getFechasBloqueadas() {
    return disponibilidadService.findAllBloqueadas();
  }

  @PostMapping("/fechas-bloqueadas")
  @ResponseStatus(HttpStatus.CREATED)
  public FechaBloqueadaResponse bloquearFecha(
      @Valid @RequestBody FechaBloqueadaRequest request) {
    return disponibilidadService.bloquearFecha(request);
  }

  @DeleteMapping("/fechas-bloqueadas/{id:[0-9]+}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void desbloquearFecha(@PathVariable Long id) {
    disponibilidadService.desbloquearFecha(id);
  }
}

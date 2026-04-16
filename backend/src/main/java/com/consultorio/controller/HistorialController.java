package com.consultorio.controller;

import com.consultorio.dto.HistorialRequest;
import com.consultorio.dto.HistorialResponse;
import com.consultorio.service.HistorialService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/historial")
public class HistorialController {

  private final HistorialService historialService;

  public HistorialController(HistorialService historialService) {
    this.historialService = historialService;
  }

  @GetMapping
  public ResponseEntity<List<HistorialResponse>> findAll() {
    return new ResponseEntity<>(historialService.findAll(), HttpStatus.OK);
  }

  @GetMapping("/{id}")
  public ResponseEntity<HistorialResponse> findById(@PathVariable("id") Long id) {
    return new ResponseEntity<>(historialService.findById(id), HttpStatus.OK);
  }

  @GetMapping("/paciente/{pacienteId}")
  public ResponseEntity<List<HistorialResponse>> findByPaciente(@PathVariable("pacienteId") Long pacienteId) {
    return new ResponseEntity<>(historialService.findByPaciente(pacienteId), HttpStatus.OK);
  }

  @GetMapping("/cita/{citaId}")
  public ResponseEntity<List<HistorialResponse>> findByCita(@PathVariable("citaId") Long citaId) {
    return new ResponseEntity<>(historialService.findByCita(citaId), HttpStatus.OK);
  }

  @PostMapping
  public ResponseEntity<HistorialResponse> create(
      @Valid @RequestBody HistorialRequest historialRequest) {
    return new ResponseEntity<>(historialService.create(historialRequest), HttpStatus.CREATED);
  }

  @PutMapping("/{id}")
  public ResponseEntity<HistorialResponse> update(
      @PathVariable("id") Long id, @Valid @RequestBody HistorialRequest historialRequest) {
    return new ResponseEntity<>(historialService.update(id, historialRequest), HttpStatus.OK);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
    historialService.delete(id);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }
}

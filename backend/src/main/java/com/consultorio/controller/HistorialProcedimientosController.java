package com.consultorio.controller;

import com.consultorio.dto.HistorialProcedimientosRequest;
import com.consultorio.dto.HistorialProcedimientosResponse;
import com.consultorio.service.HistorialProcedimientosService;
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
@RequestMapping("/historial-procedimientos")
public class HistorialProcedimientosController {

  private final HistorialProcedimientosService historialService;

  public HistorialProcedimientosController(HistorialProcedimientosService historialService) {
    this.historialService = historialService;
  }

  @GetMapping
  public ResponseEntity<List<HistorialProcedimientosResponse>> findAll() {
    return new ResponseEntity<>(historialService.findAll(), HttpStatus.OK);
  }

  @GetMapping("/{id}")
  public ResponseEntity<HistorialProcedimientosResponse> findById(@PathVariable("id") Long id) {
    return new ResponseEntity<>(historialService.findById(id), HttpStatus.OK);
  }

  @GetMapping("/paciente/{pacienteId}")
  public ResponseEntity<List<HistorialProcedimientosResponse>> findByPaciente(
      @PathVariable("pacienteId") Long pacienteId) {
    return new ResponseEntity<>(historialService.findByPaciente(pacienteId), HttpStatus.OK);
  }

  @GetMapping("/cita/{citaId}")
  public ResponseEntity<List<HistorialProcedimientosResponse>> findByCita(
      @PathVariable("citaId") Long citaId) {
    return new ResponseEntity<>(historialService.findByCita(citaId), HttpStatus.OK);
  }

  @PostMapping
  public ResponseEntity<HistorialProcedimientosResponse> create(
      @Valid @RequestBody HistorialProcedimientosRequest request) {
    return new ResponseEntity<>(historialService.create(request), HttpStatus.CREATED);
  }

  @PutMapping("/{id}")
  public ResponseEntity<HistorialProcedimientosResponse> update(
      @PathVariable("id") Long id, @Valid @RequestBody HistorialProcedimientosRequest request) {
    return new ResponseEntity<>(historialService.update(id, request), HttpStatus.OK);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
    historialService.delete(id);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }
}

package com.consultorio.controller;

import com.consultorio.dto.PacienteRequest;
import com.consultorio.dto.PacienteResponse;
import com.consultorio.service.PacienteService;
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
@RequestMapping("/pacientes")
public class PacienteController {

  private final PacienteService pacienteService;

  public PacienteController(PacienteService pacienteService) {
    this.pacienteService = pacienteService;
  }

  @GetMapping
  public ResponseEntity<List<PacienteResponse>> findAll() {
    return new ResponseEntity<>(pacienteService.findAll(), HttpStatus.OK);
  }

  @GetMapping("/{id}")
  public ResponseEntity<PacienteResponse> findById(@PathVariable Long id) {
    return new ResponseEntity<>(pacienteService.findById(id), HttpStatus.OK);
  }

  @PostMapping
  public ResponseEntity<PacienteResponse> create(@Valid @RequestBody PacienteRequest pacienteRequest) {
    return new ResponseEntity<>(pacienteService.create(pacienteRequest), HttpStatus.CREATED);
  }

  @PutMapping("/{id}")
  public ResponseEntity<PacienteResponse> update(
      @PathVariable Long id, @Valid @RequestBody PacienteRequest pacienteRequest) {
    return new ResponseEntity<>(pacienteService.update(id, pacienteRequest), HttpStatus.OK);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    pacienteService.delete(id);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }
}

package com.consultorio.controller;

import com.consultorio.dto.PagoRequest;
import com.consultorio.dto.PagoResponse;
import com.consultorio.service.PagoService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pagos")
public class PagoController {

  private final PagoService pagoService;

  public PagoController(PagoService pagoService) {
    this.pagoService = pagoService;
  }

  @GetMapping
  public ResponseEntity<List<PagoResponse>> findAll() {
    return new ResponseEntity<>(pagoService.findAll(), HttpStatus.OK);
  }

  @GetMapping("/{id}")
  public ResponseEntity<PagoResponse> findById(@PathVariable("id") Long id) {
    return new ResponseEntity<>(pagoService.findById(id), HttpStatus.OK);
  }

  @GetMapping("/paciente/{pacienteId}")
  public ResponseEntity<List<PagoResponse>> findByPaciente(@PathVariable("pacienteId") Long pacienteId) {
    return new ResponseEntity<>(pagoService.findByPaciente(pacienteId), HttpStatus.OK);
  }

  @PostMapping
  public ResponseEntity<PagoResponse> create(@Valid @RequestBody PagoRequest pagoRequest) {
    return new ResponseEntity<>(pagoService.create(pagoRequest), HttpStatus.CREATED);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
    pagoService.delete(id);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }
}

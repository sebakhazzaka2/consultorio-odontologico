package com.consultorio.controller;

import com.consultorio.dto.SaldoPacienteResponse;
import com.consultorio.service.PagoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pacientes")
public class SaldoController {

  private final PagoService pagoService;

  public SaldoController(PagoService pagoService) {
    this.pagoService = pagoService;
  }

  @GetMapping("/{id}/saldo")
  public ResponseEntity<SaldoPacienteResponse> getSaldoPaciente(@PathVariable("id") Long id) {
    return new ResponseEntity<>(pagoService.getSaldoPaciente(id), HttpStatus.OK);
  }
}

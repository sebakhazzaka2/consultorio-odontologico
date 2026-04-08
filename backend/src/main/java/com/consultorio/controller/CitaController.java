package com.consultorio.controller;

import com.consultorio.dto.CitaRequest;
import com.consultorio.dto.CitaResponse;
import com.consultorio.service.CitaService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/citas")
public class CitaController {

  private final CitaService citaService;

  public CitaController(CitaService citaService) {
    this.citaService = citaService;
  }

  @GetMapping
  public ResponseEntity<List<CitaResponse>> findAll() {
    return new ResponseEntity<>(citaService.findAll(), HttpStatus.OK);
  }

  @GetMapping("/{id}")
  public ResponseEntity<CitaResponse> findById(@PathVariable Long id) {
    return new ResponseEntity<>(citaService.findById(id), HttpStatus.OK);
  }

  @PostMapping
  public ResponseEntity<CitaResponse> create(@Valid @RequestBody CitaRequest citaRequest) {
    return new ResponseEntity<>(citaService.create(citaRequest), HttpStatus.CREATED);
  }

  @PutMapping("/{id}")
  public ResponseEntity<CitaResponse> update(
      @PathVariable Long id, @Valid @RequestBody CitaRequest citaRequest) {
    return new ResponseEntity<>(citaService.update(id, citaRequest), HttpStatus.OK);
  }

  @PatchMapping("/{id}/cancelar")
  public ResponseEntity<CitaResponse> cancelar(@PathVariable Long id) {
    return new ResponseEntity<>(citaService.cancelar(id), HttpStatus.OK);
  }

  @PatchMapping("/{id}/confirmar")
  public ResponseEntity<CitaResponse> confirmar(@PathVariable Long id) {
    return new ResponseEntity<>(citaService.confirmar(id), HttpStatus.OK);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    citaService.delete(id);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }
}

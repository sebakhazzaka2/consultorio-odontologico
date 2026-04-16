package com.consultorio.controller;

import com.consultorio.dto.PublicTratamientoResponse;
import com.consultorio.service.TratamientoService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public/tratamientos")
public class PublicTratamientoController {

  private final TratamientoService tratamientoService;

  public PublicTratamientoController(TratamientoService tratamientoService) {
    this.tratamientoService = tratamientoService;
  }

  @GetMapping
  public ResponseEntity<List<PublicTratamientoResponse>> findActivos() {
    return ResponseEntity.ok(tratamientoService.findPublicActivos());
  }
}

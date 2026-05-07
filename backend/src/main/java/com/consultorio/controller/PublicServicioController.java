package com.consultorio.controller;

import com.consultorio.dto.PublicServicioResponse;
import com.consultorio.service.ServicioService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public/servicios")
public class PublicServicioController {

  private final ServicioService servicioService;

  public PublicServicioController(ServicioService servicioService) {
    this.servicioService = servicioService;
  }

  @GetMapping
  public ResponseEntity<List<PublicServicioResponse>> findActivos() {
    return ResponseEntity.ok(servicioService.findPublicActivos());
  }
}

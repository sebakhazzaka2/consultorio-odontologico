package com.consultorio.controller;

import com.consultorio.dto.PublicReservaRequest;
import com.consultorio.dto.PublicReservaResponse;
import com.consultorio.service.PublicReservaService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public")
public class PublicReservaController {

  private final PublicReservaService publicReservaService;

  public PublicReservaController(PublicReservaService publicReservaService) {
    this.publicReservaService = publicReservaService;
  }

  @GetMapping("/slots")
  public List<String> getSlots(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
      @RequestParam Long servicioId) {
    return publicReservaService.getSlots(fecha, servicioId);
  }

  @PostMapping("/reservas")
  @ResponseStatus(HttpStatus.CREATED)
  public PublicReservaResponse reservar(@Valid @RequestBody PublicReservaRequest request) {
    return publicReservaService.reservar(request);
  }
}

package com.consultorio.controller;

import com.consultorio.dto.BajaRecordatoriosRequest;
import com.consultorio.dto.BajaRecordatoriosResponse;
import com.consultorio.service.notificacion.BajaRecordatoriosService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Baja de recordatorios (público, sin login; el token HMAC del link es la autorización).
 * Contrato fijo con el frontend: GET → 200 {clinica, estado}; POST → 204; inválido → 400.
 */
@RestController
@RequestMapping("/public/notificaciones")
public class PublicNotificacionController {

  private final BajaRecordatoriosService bajaService;

  public PublicNotificacionController(BajaRecordatoriosService bajaService) {
    this.bajaService = bajaService;
  }

  @GetMapping("/baja")
  public ResponseEntity<BajaRecordatoriosResponse> consultar(
      @RequestParam(name = "token", required = false) String token) {
    return ResponseEntity.ok(bajaService.consultar(token));
  }

  @PostMapping("/baja")
  public ResponseEntity<Void> darDeBaja(@Valid @RequestBody BajaRecordatoriosRequest request) {
    bajaService.darDeBaja(request.getToken());
    return ResponseEntity.noContent().build();
  }
}

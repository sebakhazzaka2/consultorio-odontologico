package com.consultorio.controller;

import com.consultorio.config.ClinicProperties;
import com.consultorio.dto.ClinicConfigResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public")
public class PublicClinicController {

  private final ClinicProperties clinic;

  public PublicClinicController(ClinicProperties clinic) {
    this.clinic = clinic;
  }

  @GetMapping("/config")
  public ResponseEntity<ClinicConfigResponse> getConfig() {
    ClinicConfigResponse response = new ClinicConfigResponse();
    response.setNombre(clinic.getNombre());
    response.setTagline(clinic.getTagline());
    response.setDireccion(clinic.getDireccion());
    response.setCiudad(clinic.getCiudad());
    response.setHorario(clinic.getHorario());
    response.setTelefono(clinic.getTelefono());
    response.setWhatsapp(clinic.getWhatsapp());
    response.setEmail(clinic.getEmail());
    response.setNosotros(clinic.getNosotros());
    response.setReviewsEnabled(!clinic.getGooglePlaceId().isBlank());
    return ResponseEntity.ok(response);
  }
}

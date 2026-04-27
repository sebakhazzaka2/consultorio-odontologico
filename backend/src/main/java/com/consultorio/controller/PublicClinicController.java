package com.consultorio.controller;

import com.consultorio.config.ClinicProperties;
import com.consultorio.dto.ClinicConfigResponse;
import com.consultorio.dto.ClinicConfigResponse.FeatureResponse;
import java.util.List;
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
    response.setHorarioApertura(clinic.getHorarioApertura());
    response.setHorarioCierre(clinic.getHorarioCierre());
    response.setDiasLaborales(clinic.getDiasLaborales());
    response.setTelefono(clinic.getTelefono());
    response.setWhatsapp(clinic.getWhatsapp());
    response.setEmail(clinic.getEmail());
    response.setNosotros(clinic.getNosotros());
    response.setFotoUbicacionUrl(clinic.getFotoUbicacionUrl());
    response.setReviewsEnabled(!clinic.getGooglePlaceId().isBlank());
    response.setStatsPacientes(clinic.getStatsPacientes());
    response.setStatsAnosExperiencia(clinic.getStatsAnosExperiencia());
    response.setStatsCalificacion(clinic.getStatsCalificacion());
    List<FeatureResponse> features = clinic.getFeatures().stream()
        .map(f -> new FeatureResponse(f.getTitulo(), f.getTexto(), f.getIcono()))
        .toList();
    response.setFeatures(features);
    return ResponseEntity.ok(response);
  }
}

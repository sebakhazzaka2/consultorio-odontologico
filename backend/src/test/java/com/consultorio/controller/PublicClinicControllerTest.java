package com.consultorio.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.consultorio.config.ClinicProperties;
import com.consultorio.dto.ClinicConfigResponse;
import org.junit.jupiter.api.Test;

class PublicClinicControllerTest {

  @Test
  void pluralSeDerivaDelSingularSiNoEstaConfigurado() {
    ClinicProperties props = new ClinicProperties();
    props.setLabelServicio("Tratamiento");

    ClinicConfigResponse r = new PublicClinicController(props).getConfig().getBody();

    assertEquals("Tratamiento", r.getLabelServicio());
    assertEquals("Tratamientos", r.getLabelServicioPlural());
  }

  @Test
  void pluralConfiguradoTienePrioridad() {
    ClinicProperties props = new ClinicProperties();
    props.setLabelServicio("Servicio");
    props.setLabelServicioPlural("Prestaciones");

    ClinicConfigResponse r = new PublicClinicController(props).getConfig().getBody();

    assertEquals("Prestaciones", r.getLabelServicioPlural());
  }

  @Test
  void defaultsNeutros() {
    ClinicProperties props = new ClinicProperties();
    assertEquals("", props.getHeroEyebrow());

    props.setHeroEyebrow("Salón de belleza");
    ClinicConfigResponse r = new PublicClinicController(props).getConfig().getBody();

    assertEquals("Salón de belleza", r.getHeroEyebrow());
    assertEquals("Servicios", r.getLabelServicioPlural());
    assertTrue(r.getHeroImagenes().isEmpty());
  }
}

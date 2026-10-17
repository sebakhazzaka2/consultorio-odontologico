package com.consultorio.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
  void sinBrandingNoSeEnviaElObjeto() throws Exception {
    ClinicConfigResponse r = new PublicClinicController(new ClinicProperties()).getConfig().getBody();

    assertNull(r.getBranding());
    String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(r);
    assertFalse(json.contains("branding"));
  }

  @Test
  void brandingSeExponeEnSnakeCaseOmitiendoVacios() throws Exception {
    ClinicProperties props = new ClinicProperties();
    props.getBranding().setPrimary("#8A2BE2");
    props.getBranding().setPrimaryHover("#6A1FB5");
    props.getBranding().setLogoUrl("/api/uploads/branding/logo.png");

    ClinicConfigResponse r = new PublicClinicController(props).getConfig().getBody();

    assertEquals("#8A2BE2", r.getBranding().primary());
    assertEquals("", r.getBranding().accent());
    String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(r);
    assertTrue(json.contains("\"branding\":{\"primary\":\"#8A2BE2\",\"primary_hover\":\"#6A1FB5\","
        + "\"logo_url\":\"/api/uploads/branding/logo.png\"}"), json);
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

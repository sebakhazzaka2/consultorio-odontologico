package com.consultorio.service.notificacion;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BajaTokenServiceTest {

  private final BajaTokenService service =
      new BajaTokenService("test-secret-key-for-tests-only-minimum-256-bits-padding-here");

  @Test
  void idaYVuelta() {
    String token = service.generar(42L);
    assertThat(token).startsWith("42.").matches("[0-9]+\\.[A-Za-z0-9_-]{43}");
    assertThat(service.verificar(token)).contains(42L);
  }

  @Test
  void firmaAdulteradaOIdCambiadoNoValida() {
    String token = service.generar(42L);
    String firma = token.substring(token.indexOf('.') + 1);
    char ultimo = firma.charAt(firma.length() - 1);
    String adulterado = "42." + firma.substring(0, firma.length() - 1) + (ultimo == 'A' ? 'B' : 'A');
    assertThat(service.verificar(adulterado)).isEmpty();
    assertThat(service.verificar("43." + firma)).isEmpty();
  }

  @Test
  void otroSecretoNoValida() {
    BajaTokenService otro = new BajaTokenService("otro-secreto-distinto-de-al-menos-32-caracteres");
    assertThat(otro.verificar(service.generar(42L))).isEmpty();
  }

  @Test
  void malFormadosNoValidan() {
    assertThat(service.verificar(null)).isEmpty();
    assertThat(service.verificar("")).isEmpty();
    assertThat(service.verificar("42")).isEmpty();
    assertThat(service.verificar("42.")).isEmpty();
    assertThat(service.verificar(".abc")).isEmpty();
    assertThat(service.verificar("-1.abc")).isEmpty();
    assertThat(service.verificar("0042." + "a".repeat(43))).isEmpty();
    assertThat(service.verificar("99999999999999999999.abc")).isEmpty();
    assertThat(service.verificar("1." + "a".repeat(300))).isEmpty();
  }
}

package com.consultorio.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class PhoneNormalizerTest {

  @ParameterizedTest(name = "UY: [{0}] -> {1}")
  @CsvSource(delimiter = '|', value = {
      // celulares
      "099572537          | 59899572537",
      "99572537           | 59899572537",
      "099 572 537        | 59899572537",
      "099-572-537        | 59899572537",
      "(099) 572.537      | 59899572537",
      "  099572537        | 59899572537",
      // con código de país
      "+598 99 572 537    | 59899572537",
      "+59899572537       | 59899572537",
      "0059899572537      | 59899572537",
      "59899572537        | 59899572537",
      "+598 099 572 537   | 59899572537",
      // fijos
      "24001234           | 59824001234",
      "2400 1234          | 59824001234",
      "43521234           | 59843521234",
      "+598 2400 1234     | 59824001234",
      // número brasileño con código, en una instancia uruguaya
      "+55 11 98765-4321  | 5511987654321",
      "5511987654321      | 5511987654321",
  })
  void uruguayValidos(String raw, String esperado) {
    assertThat(PhoneNormalizer.toE164Digits(raw, "598")).contains(esperado);
  }

  @ParameterizedTest(name = "UY inválido: [{0}]")
  @ValueSource(strings = {
      "0995725",          // corto
      "0995725377",       // largo
      "1234567",          // 7 dígitos
      "12345678",         // 8 dígitos que no empiezan con 9/2/4
      "08001234",         // 0800
      "099572537999999",  // demasiado largo
      "09957253a",        // letra
      "abc",
      "099/572/537",      // separador no admitido
      "+598 1234 5678",   // nacional UY inválido después del código
      "+54 11 1234 5678", // país desconocido
      "0054111234567",    // país desconocido con 00
      "+099572537",       // "+" sin código de país
      "099+572537",       // "+" en el medio
      "+",
      "--  ()",
  })
  void uruguayInvalidos(String raw) {
    assertThat(PhoneNormalizer.toE164Digits(raw, "598")).isEmpty();
  }

  @ParameterizedTest(name = "BR: [{0}] -> {1}")
  @CsvSource(delimiter = '|', value = {
      "(11) 98765-4321      | 5511987654321",
      "11987654321          | 5511987654321",
      "+55 11 98765-4321    | 5511987654321",
      "005511987654321      | 5511987654321",
      "5511987654321        | 5511987654321",
      "011 98765-4321       | 5511987654321",
      "+55 011 98765-4321   | 5511987654321",
      "11 3456-7890         | 551134567890",
      "(21) 2345-6789       | 552123456789",
      "+55 21 2345 6789     | 552123456789",
      // DDD 55 (Rio Grande do Sul): no se confunde con el código de país
      "55 99876-5432        | 5555998765432",
      "(55) 3456-7890       | 555534567890",
      // número uruguayo con código, en una instancia brasileña
      "+598 99 572 537      | 59899572537",
  })
  void brasilValidos(String raw, String esperado) {
    assertThat(PhoneNormalizer.toE164Digits(raw, "55")).contains(esperado);
  }

  @ParameterizedTest(name = "BR inválido: [{0}]")
  @ValueSource(strings = {
      "(01) 98765-4321",    // DDD empieza con 0 (tras quitar el 0 queda 1 + 9 dígitos)
      "(10) 98765-4321",    // DDD inválido
      "(20) 3456-7890",     // DDD inválido
      "11 8765-4321X",      // letra
      "11 1234-5678",       // fijo que no empieza con 2-5
      "11 88765-4321",      // 9 dígitos que no empiezan con 9
      "98765-4321",         // sin DDD
      "11 98765-43210",     // largo
      "099572537",          // celular uruguayo sin código en instancia BR
  })
  void brasilInvalidos(String raw) {
    assertThat(PhoneNormalizer.toE164Digits(raw, "55")).isEmpty();
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   ", "\t"})
  void nuloOVacio(String raw) {
    assertThat(PhoneNormalizer.toE164Digits(raw, "598")).isEmpty();
    assertThat(PhoneNormalizer.toE164Digits(raw, "55")).isEmpty();
  }

  @Test
  void paisDesconocidoSoloAceptaNumerosConCodigo() {
    assertThat(PhoneNormalizer.toE164Digits("099572537", "54")).isEmpty();
    assertThat(PhoneNormalizer.toE164Digits("+598 99 572 537", "54")).contains("59899572537");
  }

  @Test
  void resultadoSiempreEntraEnLaColumnaTelefono() {
    String[] casos = {"099572537", "24001234", "(11) 98765-4321", "11 3456-7890"};
    for (String c : casos) {
      PhoneNormalizer.toE164Digits(c, "598").ifPresent(v -> assertThat(v).matches("^[0-9]{7,15}$"));
      PhoneNormalizer.toE164Digits(c, "55").ifPresent(v -> assertThat(v).matches("^[0-9]{7,15}$"));
    }
  }
}

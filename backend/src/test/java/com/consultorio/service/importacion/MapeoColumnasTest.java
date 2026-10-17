package com.consultorio.service.importacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.consultorio.exception.ArchivoImportacionInvalidoException;
import com.consultorio.service.importacion.MapeoColumnas.Campo;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MapeoColumnasTest {

  @Test
  void encabezadosConTildesMayusculasYEspacios() {
    MapeoColumnas m = MapeoColumnas.desde(List.of(
        " Nombre ", "APELLIDOS", "Teléfono", "E-Mail", "Fecha_Nacimiento", "Notas"));
    List<String> fila = List.of("Ana", "López", "099111222", "a@b.com", "01/02/1990", "x");

    assertThat(m.valor(Campo.NOMBRE, fila)).isEqualTo("Ana");
    assertThat(m.valor(Campo.APELLIDO, fila)).isEqualTo("López");
    assertThat(m.valor(Campo.TELEFONO, fila)).isEqualTo("099111222");
    assertThat(m.valor(Campo.EMAIL, fila)).isEqualTo("a@b.com");
    assertThat(m.valor(Campo.FECHA_NACIMIENTO, fila)).isEqualTo("01/02/1990");
    assertThat(m.tiene(Campo.NOMBRE_COMPLETO)).isFalse();
  }

  @ParameterizedTest
  @CsvSource({
      "Clienta, Móvil, Cumpleaños",
      "Nombre y Apellido, WhatsApp, nacimiento",
      "PACIENTE, celular, Fecha de nacimiento",
      "nombre completo, tel, fecha nacimiento",
  })
  void aliasDeNombreCompletoTelefonoYFecha(String nombre, String telefono, String fecha) {
    MapeoColumnas m = MapeoColumnas.desde(List.of(nombre, telefono, fecha));
    assertThat(m.tiene(Campo.NOMBRE_COMPLETO)).isTrue();
    assertThat(m.tiene(Campo.TELEFONO)).isTrue();
    assertThat(m.tiene(Campo.FECHA_NACIMIENTO)).isTrue();
  }

  @Test
  void celdaFaltanteEnFilaCortaEsVacia() {
    MapeoColumnas m = MapeoColumnas.desde(List.of("nombre", "apellido", "telefono", "email"));
    assertThat(m.valor(Campo.EMAIL, List.of("Ana", "López", "099111222"))).isEmpty();
  }

  @Test
  void primeraColumnaGanaSiHayDosDelMismoCampo() {
    MapeoColumnas m = MapeoColumnas.desde(List.of("cliente", "telefono", "celular"));
    assertThat(m.valor(Campo.TELEFONO, List.of("Ana López", "1", "2"))).isEqualTo("1");
  }

  @Test
  void faltaTelefono_400ConEncabezadosDetectados() {
    assertThatThrownBy(() -> MapeoColumnas.desde(List.of("nombre", "apellido", "direccion")))
        .isInstanceOf(ArchivoImportacionInvalidoException.class)
        .hasMessageContaining("teléfono")
        .hasMessageContaining("\"direccion\"");
  }

  @Test
  void nombreSinApellidoNiNombreCompleto_esError() {
    assertThatThrownBy(() -> MapeoColumnas.desde(List.of("nombre", "telefono")))
        .isInstanceOf(ArchivoImportacionInvalidoException.class);
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', value = {
      "Ana María López  | Ana María | López",
      "Ana López        | Ana       | López",
      "  Ana   López    | Ana       | López",
      "Ana              | Ana       | ''",
  })
  void separaNombreCompletoEnElUltimoEspacio(String completo, String nombre, String apellido) {
    NombreCompleto n = NombreCompleto.separar(completo);
    assertThat(n.nombre()).isEqualTo(nombre);
    assertThat(n.apellido()).isEqualTo(apellido);
  }
}

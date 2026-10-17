package com.consultorio.service.importacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.consultorio.exception.ArchivoImportacionInvalidoException;
import com.consultorio.service.importacion.CsvLector.CsvDatos;
import com.consultorio.service.importacion.CsvLector.FilaCsv;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class CsvLectorTest {

  private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");

  @Test
  void utf8ConPuntoYComa() {
    CsvDatos d = CsvLector.leer("nombre;apellido;telefono\nMaría;Muñoz;099111222\n"
        .getBytes(StandardCharsets.UTF_8));

    assertThat(d.encabezados()).containsExactly("nombre", "apellido", "telefono");
    assertThat(d.filas()).containsExactly(new FilaCsv(1, List.of("María", "Muñoz", "099111222")));
  }

  @Test
  void utf8ConBom() {
    byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    byte[] cuerpo = "telefono,nombre completo\n099111222,Ana López\n".getBytes(StandardCharsets.UTF_8);
    byte[] todo = new byte[bom.length + cuerpo.length];
    System.arraycopy(bom, 0, todo, 0, bom.length);
    System.arraycopy(cuerpo, 0, todo, bom.length, cuerpo.length);

    CsvDatos d = CsvLector.leer(todo);

    assertThat(d.encabezados()).containsExactly("telefono", "nombre completo");
    assertThat(d.filas().get(0).celdas()).containsExactly("099111222", "Ana López");
  }

  @Test
  void windows1252ConEnieYTildes() {
    byte[] latin = "nombre;apellido;teléfono\nÁngela;Muñoz;099111222\n".getBytes(WINDOWS_1252);

    CsvDatos d = CsvLector.leer(latin);

    assertThat(d.encabezados()).containsExactly("nombre", "apellido", "teléfono");
    assertThat(d.filas().get(0).celdas()).containsExactly("Ángela", "Muñoz", "099111222");
  }

  @Test
  void comaComoSeparadorYComillas() {
    CsvDatos d = CsvLector.leer(
        "nombre,apellido,telefono\n\"Ana, María\",López,099111222\n".getBytes(StandardCharsets.UTF_8));

    assertThat(d.filas().get(0).celdas()).containsExactly("Ana, María", "López", "099111222");
  }

  @Test
  void tabComoSeparador() {
    CsvDatos d = CsvLector.leer("nombre\tapellido\ttelefono\r\nAna\tLópez\t099111222\r\n"
        .getBytes(StandardCharsets.UTF_8));

    assertThat(d.filas().get(0).celdas()).containsExactly("Ana", "López", "099111222");
  }

  @Test
  void celdasRecortadasYFilasVaciasSalteadasPeroContadas() {
    String csv = "nombre;apellido;telefono\n"
        + "  Ana  ; López ; 099111222 \n"
        + "\n"
        + " ; ; \n"
        + "Beto;Pérez;099333444\n";

    CsvDatos d = CsvLector.leer(csv.getBytes(StandardCharsets.UTF_8));

    assertThat(d.filas()).containsExactly(
        new FilaCsv(1, List.of("Ana", "López", "099111222")),
        new FilaCsv(4, List.of("Beto", "Pérez", "099333444")));
  }

  @Test
  void archivoVacioOSinEncabezado() {
    assertThatThrownBy(() -> CsvLector.leer(new byte[0]))
        .isInstanceOf(ArchivoImportacionInvalidoException.class).hasMessageContaining("vacío");
    assertThatThrownBy(() -> CsvLector.leer("  \n\n".getBytes(StandardCharsets.UTF_8)))
        .isInstanceOf(ArchivoImportacionInvalidoException.class);
  }

  @Test
  void soloEncabezadoNoTieneFilas() {
    CsvDatos d = CsvLector.leer("nombre;apellido;telefono\n".getBytes(StandardCharsets.UTF_8));
    assertThat(d.filas()).isEmpty();
  }

  @Test
  void hasta5000FilasSeAceptan() {
    assertThat(CsvLector.leer(conFilas(CsvLector.MAX_FILAS)).filas()).hasSize(CsvLector.MAX_FILAS);
  }

  @Test
  void masDe5000FilasEsError() {
    assertThatThrownBy(() -> CsvLector.leer(conFilas(CsvLector.MAX_FILAS + 1)))
        .isInstanceOf(ArchivoImportacionInvalidoException.class)
        .hasMessageContaining("5000");
  }

  @Test
  void comillaSinCerrarEsError() {
    assertThatThrownBy(() -> CsvLector.leer(
            "nombre;apellido;telefono\n\"Ana;López;099111222\n".getBytes(StandardCharsets.UTF_8)))
        .isInstanceOf(ArchivoImportacionInvalidoException.class);
  }

  @Test
  void detectaSeparador() {
    assertThat(CsvLector.detectarSeparador("a;b;c\n1,5;2;3")).isEqualTo(';');
    assertThat(CsvLector.detectarSeparador("a,b,c")).isEqualTo(',');
    assertThat(CsvLector.detectarSeparador("a\tb\tc")).isEqualTo('\t');
    assertThat(CsvLector.detectarSeparador("telefono")).isEqualTo(',');
  }

  private static byte[] conFilas(int n) {
    StringBuilder sb = new StringBuilder("nombre;apellido;telefono\n");
    for (int i = 0; i < n; i++) {
      sb.append("N").append(i).append(";A;099").append(100000 + i).append('\n');
    }
    return sb.toString().getBytes(StandardCharsets.UTF_8);
  }
}

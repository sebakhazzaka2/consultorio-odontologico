package com.consultorio.service.importacion;

import com.consultorio.exception.ArchivoImportacionInvalidoException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

/**
 * Lee un CSV exportado de otro sistema o de Excel y lo deja como encabezados + filas
 * de celdas recortadas. No interpreta el contenido (eso es {@link MapeoColumnas} y
 * {@code PacienteImportService}).
 *
 * <ul>
 *   <li><b>Codificación:</b> UTF-8 estricto; si hay bytes inválidos se reintenta como
 *       Windows-1252 (superconjunto de Latin-1, lo que guarda Excel en español). Se quita
 *       el BOM si viene.</li>
 *   <li><b>Separador:</b> el más frecuente entre {@code ;}, {@code ,} y tabulador en la
 *       línea de encabezado (Excel en español usa {@code ;}). Sin ninguno: {@code ,}.</li>
 *   <li><b>Filas:</b> la primera línea es el encabezado. Las filas totalmente vacías se
 *       saltean pero cuentan para la numeración, así que el número de fila coincide con
 *       la línea del archivo menos uno (fila 1 = primera línea después del encabezado),
 *       salvo celdas con saltos de línea entre comillas.</li>
 *   <li><b>Límite:</b> {@value #MAX_FILAS} filas de datos no vacías.</li>
 * </ul>
 */
public final class CsvLector {

  public static final int MAX_FILAS = 5000;

  private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");
  private static final char BOM = '﻿';

  private CsvLector() {}

  /** Fila de datos: número 1-based (sin contar el encabezado) y sus celdas recortadas. */
  public record FilaCsv(int numero, List<String> celdas) {}

  public record CsvDatos(List<String> encabezados, List<FilaCsv> filas) {}

  public static CsvDatos leer(byte[] bytes) {
    if (bytes == null || bytes.length == 0) {
      throw new ArchivoImportacionInvalidoException("El archivo está vacío");
    }
    String texto = decodificar(bytes);
    if (!texto.isEmpty() && texto.charAt(0) == BOM) {
      texto = texto.substring(1);
    }
    if (texto.isBlank()) {
      throw new ArchivoImportacionInvalidoException("El archivo está vacío");
    }

    CSVFormat formato = CSVFormat.DEFAULT.builder()
        .setDelimiter(detectarSeparador(texto))
        .setTrim(true)
        .setIgnoreEmptyLines(false)
        .get();

    List<String> encabezados = null;
    List<FilaCsv> filas = new ArrayList<>();
    try (CSVParser parser = CSVParser.parse(texto, formato)) {
      int numero = 0;
      for (CSVRecord registro : parser) {
        List<String> celdas = celdas(registro);
        if (encabezados == null) {
          if (todasVacias(celdas)) {
            continue; // líneas en blanco antes del encabezado
          }
          encabezados = celdas;
          continue;
        }
        numero++;
        if (todasVacias(celdas)) {
          continue;
        }
        if (filas.size() == MAX_FILAS) {
          throw new ArchivoImportacionInvalidoException(
              "El archivo supera el máximo de " + MAX_FILAS + " filas. Dividilo en partes más chicas.");
        }
        filas.add(new FilaCsv(numero, celdas));
      }
    } catch (IOException | UncheckedIOException | IllegalStateException e) {
      // commons-csv avisa así de comillas sin cerrar u otros CSV mal formados
      throw new ArchivoImportacionInvalidoException(
          "El archivo no es un CSV válido (revisá las comillas)");
    }

    if (encabezados == null) {
      throw new ArchivoImportacionInvalidoException("El archivo no tiene encabezado");
    }
    return new CsvDatos(encabezados, filas);
  }

  static String decodificar(byte[] bytes) {
    CharsetDecoder utf8 = StandardCharsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT);
    try {
      return utf8.decode(ByteBuffer.wrap(bytes)).toString();
    } catch (CharacterCodingException e) {
      return new String(bytes, WINDOWS_1252);
    }
  }

  static char detectarSeparador(String texto) {
    int fin = texto.indexOf('\n');
    String encabezado = fin < 0 ? texto : texto.substring(0, fin);
    int puntoYComa = contar(encabezado, ';');
    int coma = contar(encabezado, ',');
    int tab = contar(encabezado, '\t');
    if (puntoYComa == 0 && coma == 0 && tab == 0) {
      return ',';
    }
    if (puntoYComa >= coma && puntoYComa >= tab) {
      return ';';
    }
    return coma >= tab ? ',' : '\t';
  }

  private static int contar(String s, char c) {
    int n = 0;
    for (int i = 0; i < s.length(); i++) {
      if (s.charAt(i) == c) {
        n++;
      }
    }
    return n;
  }

  private static List<String> celdas(CSVRecord registro) {
    List<String> celdas = new ArrayList<>(registro.size());
    for (String valor : registro) {
      celdas.add(valor == null ? "" : valor.trim());
    }
    return celdas;
  }

  private static boolean todasVacias(List<String> celdas) {
    return celdas.stream().allMatch(String::isEmpty);
  }
}

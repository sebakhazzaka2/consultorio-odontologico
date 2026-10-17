package com.consultorio.service.importacion;

import com.consultorio.exception.ArchivoImportacionInvalidoException;
import java.text.Normalizer;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Qué columna del CSV corresponde a cada dato del paciente, a partir del encabezado.
 *
 * <p>Los encabezados se comparan normalizados: sin tildes, en minúsculas, {@code _}
 * como espacio y espacios colapsados ("Teléfono", "TELEFONO" y " telefono " son lo
 * mismo). Si dos columnas mapean al mismo campo, gana la primera. Las columnas que no
 * reconoce se ignoran.
 *
 * <p>Obligatorias: teléfono y (nombre + apellido, o nombre completo).
 *
 * <p>Para soportar otro export (Ágora u otro sistema) alcanza con sumar alias en
 * {@link #ALIAS}, ya normalizados.
 */
public final class MapeoColumnas {

  public enum Campo {
    NOMBRE, APELLIDO, NOMBRE_COMPLETO, TELEFONO, EMAIL, FECHA_NACIMIENTO
  }

  /** Tabla única de alias por campo, escritos ya normalizados (ver {@link #normalizar}). */
  static final Map<Campo, List<String>> ALIAS = new EnumMap<>(Map.of(
      Campo.NOMBRE, List.of("nombre", "nombres", "first name"),
      Campo.APELLIDO, List.of("apellido", "apellidos", "last name"),
      Campo.NOMBRE_COMPLETO, List.of(
          "nombre completo", "cliente", "clienta", "paciente", "nombre y apellido"),
      Campo.TELEFONO, List.of("telefono", "celular", "movil", "whatsapp", "tel"),
      Campo.EMAIL, List.of("email", "e-mail", "correo", "mail", "correo electronico"),
      Campo.FECHA_NACIMIENTO, List.of(
          "fecha de nacimiento", "nacimiento", "fecha nacimiento", "cumpleanos")));

  private final Map<Campo, Integer> indices;

  private MapeoColumnas(Map<Campo, Integer> indices) {
    this.indices = indices;
  }

  public static MapeoColumnas desde(List<String> encabezados) {
    Map<String, Campo> porAlias = new LinkedHashMap<>();
    ALIAS.forEach((campo, alias) -> alias.forEach(a -> porAlias.put(a, campo)));

    Map<Campo, Integer> indices = new EnumMap<>(Campo.class);
    for (int i = 0; i < encabezados.size(); i++) {
      Campo campo = porAlias.get(normalizar(encabezados.get(i)));
      if (campo != null) {
        indices.putIfAbsent(campo, i);
      }
    }

    boolean tieneNombre = (indices.containsKey(Campo.NOMBRE) && indices.containsKey(Campo.APELLIDO))
        || indices.containsKey(Campo.NOMBRE_COMPLETO);
    if (!indices.containsKey(Campo.TELEFONO) || !tieneNombre) {
      // Los encabezados no son datos personales: se pueden mostrar para ayudar a corregir
      String detectados = encabezados.stream()
          .filter(e -> !e.isBlank())
          .map(e -> "\"" + e + "\"")
          .collect(Collectors.joining(", "));
      throw new ArchivoImportacionInvalidoException(
          "Faltan columnas obligatorias: se necesita teléfono y (nombre y apellido, o nombre completo). "
              + "Encabezados detectados: " + (detectados.isEmpty() ? "ninguno" : detectados));
    }
    return new MapeoColumnas(indices);
  }

  public boolean tiene(Campo campo) {
    return indices.containsKey(campo);
  }

  /** Valor recortado de la celda del campo en esa fila; "" si no hay columna o celda. */
  public String valor(Campo campo, List<String> celdas) {
    Integer i = indices.get(campo);
    if (i == null || i >= celdas.size()) {
      return "";
    }
    return celdas.get(i);
  }

  static String normalizar(String encabezado) {
    if (encabezado == null) {
      return "";
    }
    String sinTildes = Normalizer.normalize(encabezado, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "");
    return sinTildes.toLowerCase(Locale.ROOT)
        .replace('_', ' ')
        .replaceAll("\\s+", " ")
        .trim();
  }
}

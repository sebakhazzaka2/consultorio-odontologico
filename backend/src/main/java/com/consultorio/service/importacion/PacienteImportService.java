package com.consultorio.service.importacion;

import com.consultorio.config.ClinicProperties;
import com.consultorio.dto.EstadoFilaImportacion;
import com.consultorio.dto.FilaImportacionResultado;
import com.consultorio.dto.ImportacionResultado;
import com.consultorio.exception.ArchivoImportacionInvalidoException;
import com.consultorio.model.Paciente;
import com.consultorio.model.PacienteOrigen;
import com.consultorio.repository.PacienteRepository;
import com.consultorio.service.importacion.CsvLector.CsvDatos;
import com.consultorio.service.importacion.CsvLector.FilaCsv;
import com.consultorio.service.importacion.MapeoColumnas.Campo;
import com.consultorio.util.EmailNormalizer;
import com.consultorio.util.PhoneNormalizer;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Importación de pacientes desde CSV en dos pasos sin estado en el servidor: el mismo
 * archivo se sube primero en simulación ({@code confirmar=false}, no escribe nada) y
 * después en confirmación ({@code confirmar=true}), que vuelve a analizar todo e inserta
 * solo las filas {@code OK} con {@code creado_por = IMPORT}.
 *
 * <p>Reglas por fila (ver {@code docs/IMPORTACION_PACIENTES.md}):
 * <ul>
 *   <li><b>Nombre:</b> columnas nombre/apellido si vienen; si no, nombre completo separado
 *       en el último espacio. Una sola palabra → {@code REVISAR}.</li>
 *   <li><b>Teléfono:</b> obligatorio y válido para el país de la instancia
 *       ({@link PhoneNormalizer}). Se guarda como lo escribió la persona, solo dígitos
 *       (igual que los pacientes cargados a mano hoy, formato local); si vino con prefijo
 *       internacional ({@code +} o {@code 00}) o no entra en 7–15 dígitos, se guarda el
 *       E.164. La detección de duplicados usa siempre el E.164.</li>
 *   <li><b>Email:</b> opcional, normalizado ({@link EmailNormalizer}). Nunca se inventa.</li>
 *   <li><b>Fecha de nacimiento:</b> opcional, {@code dd/MM/aaaa} ({@code d/M/aaaa} y
 *       {@code aaaa-MM-dd} también); no puede ser futura.</li>
 *   <li><b>Duplicados:</b> por email normalizado; si la fila no tiene email, por teléfono
 *       E.164. Contra filas anteriores del archivo → {@code DUPLICADO_ARCHIVO}; contra la
 *       base → {@code YA_EXISTE}. Nunca por nombre (PR #34).</li>
 * </ul>
 *
 * <p>Carrera: si entre el análisis y el insert alguien crea un paciente con el mismo
 * email, el constraint {@code uk_pacientes_email} hace fallar el flush, la transacción
 * entera se revierte (no queda una importación a medias) y el endpoint responde 409.
 * Se reintenta y esa fila sale como {@code YA_EXISTE}.
 *
 * <p>Privacidad: ni el reporte ni los logs incluyen nombres, teléfonos ni emails; los
 * logs llevan solo conteos.
 */
@Service
public class PacienteImportService {

  private static final Logger log = LoggerFactory.getLogger(PacienteImportService.class);

  private static final int MAX_LARGO_TEXTO = 255;
  // Además de @Email (que acepta "ana@gmail"), se exige un punto en el dominio:
  // en una importación masiva un typo así es más probable que un dominio sin punto.
  private static final Pattern EMAIL_CON_DOMINIO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
  private static final Pattern TELEFONO_COLUMNA = Pattern.compile("^[0-9]{7,15}$");
  private static final DateTimeFormatter FECHA_DMY =
      DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT);
  private static final DateTimeFormatter FECHA_ISO =
      DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);

  private final PacienteRepository pacienteRepository;
  private final ClinicProperties clinicProperties;
  private final Validator validator;

  public PacienteImportService(
      PacienteRepository pacienteRepository, ClinicProperties clinicProperties, Validator validator) {
    this.pacienteRepository = pacienteRepository;
    this.clinicProperties = clinicProperties;
    this.validator = validator;
  }

  @Transactional
  public ImportacionResultado importar(MultipartFile archivo, boolean confirmar) {
    CsvDatos datos = CsvLector.leer(bytes(archivo));
    MapeoColumnas mapeo = MapeoColumnas.desde(datos.encabezados());
    String pais = clinicProperties.getCountry();

    Existentes existentes = cargarExistentes(pais);
    Map<String, Integer> emailsArchivo = new HashMap<>();
    Map<String, Integer> telefonosArchivo = new HashMap<>();

    List<FilaImportacionResultado> filas = new ArrayList<>();
    List<Paciente> aInsertar = new ArrayList<>();
    int[] conteo = new int[EstadoFilaImportacion.values().length];

    for (FilaCsv fila : datos.filas()) {
      Analisis a = analizar(fila, mapeo, pais, existentes, emailsArchivo, telefonosArchivo);
      filas.add(new FilaImportacionResultado(fila.numero(), a.estado(), a.motivo()));
      conteo[a.estado().ordinal()]++;
      if (a.estado() == EstadoFilaImportacion.OK) {
        aInsertar.add(a.paciente());
      }
    }

    int insertados = 0;
    if (confirmar && !aInsertar.isEmpty()) {
      pacienteRepository.saveAll(aInsertar);
      pacienteRepository.flush(); // que un email duplicado por carrera falle acá (409)
      insertados = aInsertar.size();
    }

    ImportacionResultado resultado = new ImportacionResultado(
        datos.filas().size(),
        conteo[EstadoFilaImportacion.OK.ordinal()],
        conteo[EstadoFilaImportacion.ERROR.ordinal()],
        conteo[EstadoFilaImportacion.REVISAR.ordinal()],
        conteo[EstadoFilaImportacion.DUPLICADO_ARCHIVO.ordinal()],
        conteo[EstadoFilaImportacion.YA_EXISTE.ordinal()],
        insertados,
        confirmar,
        filas);
    log.info("Importación de pacientes (confirmar={}): total={}, ok={}, errores={}, revisar={}, "
            + "duplicados={}, ya_existen={}, insertados={}",
        confirmar, resultado.total(), resultado.ok(), resultado.errores(), resultado.revisar(),
        resultado.duplicados(), resultado.yaExisten(), insertados);
    return resultado;
  }

  private record Analisis(EstadoFilaImportacion estado, String motivo, Paciente paciente) {}

  /** Emails y teléfonos E.164 de los pacientes ya cargados (instancias chicas: una consulta). */
  private record Existentes(Set<String> emails, Set<String> telefonos) {}

  private Existentes cargarExistentes(String pais) {
    Set<String> emails = new HashSet<>();
    Set<String> telefonos = new HashSet<>();
    for (PacienteRepository.Contacto c : pacienteRepository.findAllContactos()) {
      String email = EmailNormalizer.normalizar(c.getEmail());
      if (email != null) {
        emails.add(email);
      }
      PhoneNormalizer.toE164Digits(c.getTelefono(), pais).ifPresent(telefonos::add);
    }
    return new Existentes(emails, telefonos);
  }

  private Analisis analizar(FilaCsv fila, MapeoColumnas mapeo, String pais, Existentes existentes,
      Map<String, Integer> emailsArchivo, Map<String, Integer> telefonosArchivo) {
    List<String> errores = new ArrayList<>();
    List<String> revisar = new ArrayList<>();
    List<String> celdas = fila.celdas();

    // Nombre: columnas explícitas ganan; si falta el apellido y hay nombre completo, se usa ese
    String nombreCol = colapsarEspacios(mapeo.valor(Campo.NOMBRE, celdas));
    String apellidoCol = colapsarEspacios(mapeo.valor(Campo.APELLIDO, celdas));
    String completo = mapeo.valor(Campo.NOMBRE_COMPLETO, celdas);
    String nombre;
    String apellido;
    if (!nombreCol.isEmpty() && (!apellidoCol.isEmpty() || completo.isEmpty())) {
      nombre = nombreCol;
      apellido = apellidoCol;
    } else if (!completo.isEmpty()) {
      NombreCompleto separado = NombreCompleto.separar(completo);
      nombre = separado.nombre();
      apellido = separado.apellido();
    } else {
      nombre = "";
      apellido = apellidoCol;
    }
    if (nombre.isEmpty()) {
      errores.add("Falta el nombre");
    } else if (apellido.isEmpty()) {
      revisar.add("Falta el apellido (el nombre tiene una sola palabra o la columna está vacía)");
    }
    if (nombre.length() > MAX_LARGO_TEXTO || apellido.length() > MAX_LARGO_TEXTO) {
      errores.add("Nombre o apellido demasiado largo (máximo " + MAX_LARGO_TEXTO + " caracteres)");
    }

    // Teléfono
    String telefonoRaw = mapeo.valor(Campo.TELEFONO, celdas);
    String e164 = null;
    String telefonoGuardado = null;
    if (telefonoRaw.isEmpty()) {
      errores.add("Falta el teléfono");
    } else {
      Optional<String> normalizado = PhoneNormalizer.toE164Digits(telefonoRaw, pais);
      if (normalizado.isEmpty()) {
        errores.add("Teléfono inválido para el país de la instancia");
      } else {
        e164 = normalizado.get();
        telefonoGuardado = telefonoParaGuardar(telefonoRaw, e164);
      }
    }

    // Email (opcional)
    String email = EmailNormalizer.normalizar(mapeo.valor(Campo.EMAIL, celdas));
    if (email != null && !emailValido(email)) {
      errores.add("Email inválido");
    }

    // Fecha de nacimiento (opcional)
    LocalDate fechaNacimiento = null;
    String fechaRaw = mapeo.valor(Campo.FECHA_NACIMIENTO, celdas);
    if (!fechaRaw.isEmpty()) {
      fechaNacimiento = parsearFecha(fechaRaw);
      if (fechaNacimiento == null) {
        errores.add("Fecha de nacimiento inválida (formato dd/MM/aaaa)");
      } else if (fechaNacimiento.isAfter(LocalDate.now())) {
        errores.add("Fecha de nacimiento en el futuro");
      }
    }

    Paciente paciente = null;
    if (errores.isEmpty()) {
      paciente = new Paciente(nombre, apellido.isEmpty() ? null : apellido, telefonoGuardado, email,
          fechaNacimiento);
      paciente.setCreadoPor(PacienteOrigen.IMPORT);
      // Reusa las constraints de la entidad (las mismas que el alta manual). Los mensajes
      // por defecto no incluyen el valor, así que no filtran datos personales.
      for (ConstraintViolation<Paciente> v : validator.validate(paciente)) {
        if ("apellido".equals(v.getPropertyPath().toString()) && apellido.isEmpty()) {
          continue; // ya está como REVISAR
        }
        errores.add("Dato inválido en " + v.getPropertyPath() + ": " + v.getMessage());
      }
    }

    if (!errores.isEmpty()) {
      errores.addAll(revisar);
      return new Analisis(EstadoFilaImportacion.ERROR, String.join("; ", errores), null);
    }

    if (email != null && existentes.emails().contains(email)) {
      return new Analisis(EstadoFilaImportacion.YA_EXISTE, "Ya existe un paciente con ese email", null);
    }
    if (email == null && existentes.telefonos().contains(e164)) {
      return new Analisis(EstadoFilaImportacion.YA_EXISTE,
          "Ya existe un paciente con ese teléfono (la fila no tiene email)", null);
    }
    if (email != null && emailsArchivo.containsKey(email)) {
      return new Analisis(EstadoFilaImportacion.DUPLICADO_ARCHIVO,
          "Mismo email que la fila " + emailsArchivo.get(email), null);
    }
    if (email == null && telefonosArchivo.containsKey(e164)) {
      return new Analisis(EstadoFilaImportacion.DUPLICADO_ARCHIVO,
          "Mismo teléfono que la fila " + telefonosArchivo.get(e164) + " (la fila no tiene email)", null);
    }

    // Las filas REVISAR también reservan su clave: si se corrigen, no deben chocar con otra
    if (email != null) {
      emailsArchivo.put(email, fila.numero());
    }
    telefonosArchivo.putIfAbsent(e164, fila.numero());

    if (!revisar.isEmpty()) {
      return new Analisis(EstadoFilaImportacion.REVISAR, String.join("; ", revisar), null);
    }
    return new Analisis(EstadoFilaImportacion.OK, null, paciente);
  }

  /**
   * Dígitos tal como los escribió la persona (formato local, igual que el alta manual de
   * hoy); E.164 si vino con prefijo internacional o si los dígitos no entran en la columna.
   */
  static String telefonoParaGuardar(String raw, String e164) {
    String digitos = raw.replaceAll("[^0-9]", "");
    boolean internacional = raw.trim().startsWith("+") || digitos.startsWith("00");
    if (internacional || !TELEFONO_COLUMNA.matcher(digitos).matches()) {
      return e164;
    }
    return digitos;
  }

  private boolean emailValido(String email) {
    return email.length() <= MAX_LARGO_TEXTO
        && EMAIL_CON_DOMINIO.matcher(email).matches()
        && validator.validateValue(Paciente.class, "email", email).isEmpty();
  }

  static LocalDate parsearFecha(String raw) {
    for (DateTimeFormatter f : List.of(FECHA_DMY, FECHA_ISO)) {
      try {
        return LocalDate.parse(raw, f);
      } catch (DateTimeParseException ignored) {
        // probar el siguiente formato
      }
    }
    return null;
  }

  private static String colapsarEspacios(String s) {
    return s.replaceAll("\\s+", " ").trim();
  }

  private static byte[] bytes(MultipartFile archivo) {
    if (archivo == null || archivo.isEmpty()) {
      throw new ArchivoImportacionInvalidoException("El archivo está vacío");
    }
    try {
      return archivo.getBytes();
    } catch (IOException e) {
      throw new ArchivoImportacionInvalidoException("No se pudo leer el archivo");
    }
  }
}

package com.consultorio.service.notificacion;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Token del link de baja de recordatorios: {@code <pacienteId>.<firma>}, con
 * firma = HMAC-SHA256(clave, "baja-recordatorios:" + pacienteId) en Base64 URL-safe sin
 * padding. Sin tabla ni vencimiento: el link de cualquier recordatorio sigue sirviendo.
 *
 * <p>La clave se deriva de JWT_SECRET (HMAC(JWT_SECRET, "baja-recordatorios")) para no
 * agregar otro secreto por instancia, y es distinta de la que firma los JWT. Rotar
 * JWT_SECRET invalida los links de baja ya enviados (aceptable: llegan nuevos con cada
 * recordatorio). La comparación de firmas es en tiempo constante.
 */
@Service
public class BajaTokenService {

  private static final String ALGORITMO = "HmacSHA256";
  private static final String CONTEXTO_CLAVE = "baja-recordatorios";
  private static final String CONTEXTO_FIRMA = "baja-recordatorios:";

  private final byte[] clave;

  public BajaTokenService(@Value("${jwt.secret}") String jwtSecret) {
    if (jwtSecret == null || jwtSecret.isBlank()) {
      throw new IllegalStateException("jwt.secret es necesario para firmar los links de baja");
    }
    this.clave = hmac(jwtSecret.getBytes(StandardCharsets.UTF_8), CONTEXTO_CLAVE);
  }

  public String generar(Long pacienteId) {
    if (pacienteId == null || pacienteId <= 0) {
      throw new IllegalArgumentException("pacienteId inválido");
    }
    return pacienteId + "." + firmar(pacienteId);
  }

  /** Id del paciente si el token es auténtico; vacío si está mal formado o la firma no coincide. */
  public Optional<Long> verificar(String token) {
    if (token == null || token.length() > 200) {
      return Optional.empty();
    }
    int punto = token.indexOf('.');
    if (punto <= 0 || punto == token.length() - 1) {
      return Optional.empty();
    }
    String idTexto = token.substring(0, punto);
    if (!idTexto.matches("[1-9][0-9]{0,18}")) {
      return Optional.empty();
    }
    long id;
    try {
      id = Long.parseLong(idTexto);
    } catch (NumberFormatException e) {
      return Optional.empty();
    }
    byte[] esperada = firmar(id).getBytes(StandardCharsets.US_ASCII);
    byte[] recibida = token.substring(punto + 1).getBytes(StandardCharsets.US_ASCII);
    return MessageDigest.isEqual(esperada, recibida) ? Optional.of(id) : Optional.empty();
  }

  private String firmar(long pacienteId) {
    byte[] firma = hmac(clave, CONTEXTO_FIRMA + pacienteId);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(firma);
  }

  private static byte[] hmac(byte[] clave, String datos) {
    try {
      Mac mac = Mac.getInstance(ALGORITMO);
      mac.init(new SecretKeySpec(clave, ALGORITMO));
      return mac.doFinal(datos.getBytes(StandardCharsets.UTF_8));
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("HmacSHA256 no disponible", e);
    }
  }
}

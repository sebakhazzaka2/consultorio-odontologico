package com.consultorio.repository;

import com.consultorio.model.Notificacion;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

  boolean existsByClaveDedupe(String claveDedupe);

  Optional<Notificacion> findByClaveDedupe(String claveDedupe);

  /**
   * Inserta una notificación PENDIENTE; si la clave de dedupe ya existe no hace nada
   * (devuelve 0) y NO lanza excepción. Por qué nativo y no {@code save()}: un
   * {@code DataIntegrityViolationException} atrapado dentro de una transacción JPA la deja
   * marcada rollback-only, y encolar corre dentro de la transacción del caller (reserva,
   * confirmación...): un duplicado no puede tirar abajo esa operación.
   * {@code ON DUPLICATE KEY UPDATE id = id} es un no-op que solo absorbe el choque de claves
   * únicas; a diferencia de {@code INSERT IGNORE}, cualquier otro error (FK, truncado) sigue
   * fallando. MySQL y H2 (modo MySQL) lo soportan.
   */
  @Modifying(flushAutomatically = true)
  @Query(
      nativeQuery = true,
      value =
          "INSERT INTO notificaciones (tipo, canal, clave_dedupe, cita_id, referencia_id,"
              + " destinatario_tipo, estado, intentos, proximo_intento_at, created_at)"
              + " VALUES (:tipo, :canal, :clave, :citaId, :referenciaId, :destinatario,"
              + " 'PENDIENTE', 0, :proximo, :creada)"
              + " ON DUPLICATE KEY UPDATE id = id")
  int insertarPendienteSiNoExiste(
      @Param("tipo") String tipo,
      @Param("canal") String canal,
      @Param("clave") String clave,
      @Param("citaId") Long citaId,
      @Param("referenciaId") Long referenciaId,
      @Param("destinatario") String destinatario,
      @Param("proximo") Instant proximo,
      @Param("creada") Instant creada);

  /**
   * Ids de pendientes vencidas, bloqueadas con {@code FOR UPDATE SKIP LOCKED} (MySQL 8):
   * dos corridas del despachador que se solapan se reparten filas distintas en vez de
   * esperar o tomar las mismas. Tiene que correr dentro de una transacción corta que
   * enseguida corre el {@code proximo_intento_at} (ver {@link #tomar}).
   */
  @Query(
      nativeQuery = true,
      value =
          "SELECT id FROM notificaciones WHERE estado = 'PENDIENTE' AND proximo_intento_at <= :ahora"
              + " ORDER BY proximo_intento_at, id LIMIT :limite FOR UPDATE SKIP LOCKED")
  List<Long> bloquearVencidas(@Param("ahora") Instant ahora, @Param("limite") int limite);

  /**
   * Marca las filas tomadas: suma el intento y corre {@code proximo_intento_at} al fin del
   * "lease". Si el proceso muere enviando, la fila vuelve a ser elegible al vencer el lease
   * (entrega al-menos-una-vez) y el intento ya quedó contado.
   */
  @Modifying
  @Query(
      nativeQuery = true,
      value =
          "UPDATE notificaciones SET intentos = intentos + 1, proximo_intento_at = :finLease"
              + " WHERE id IN (:ids)")
  int tomar(@Param("ids") Collection<Long> ids, @Param("finLease") Instant finLease);
}

# Lessons Learned

Patrones de errores y correcciones. Actualizar después de cada corrección del usuario.

---

## Convenciones Angular

_Ninguna lección registrada aún._

## Convenciones Spring Boot

_Ninguna lección registrada aún._

## Arquitectura y diseño

### [2026-06-11] Imágenes servidas por el backend salen con `no-store`
**Contexto:** se migraron las fotos del hero de imgur a self-hosting en el volumen de uploads, servidas por el backend vía `/api/uploads/**`.
**Error:** Spring Security agrega `Cache-Control: no-cache, no-store` a TODAS las respuestas, así que el navegador re-descargaba cada imagen en cada vista → carrusel "trabado" (mostraba la misma mientras bajaba la siguiente) y fotos lentas. Con imgur no pasaba porque venían de su CDN con caché propia.
**Regla:** las imágenes y assets estáticos se cachean en la capa de proxy (Caddy), no en Spring — mismo criterio que el rate limiting en nginx. Cache-busting = cambiar el nombre del archivo. La caché va acotada a `/api/uploads/*` y assets con hash; NUNCA al JSON de la API (debe seguir `no-store`).

## Workflow y proceso

### [2026-06-11] El repo del server queda "sucio" y rompe el deploy
**Contexto:** se aplicó un cambio al `Caddyfile` directo en el server por `scp` (sin commit). El deploy CI corría `git pull origin main`.
**Error:** un archivo versionado modificado localmente en el server (sin commitear) que también cambia en main hace abortar `git pull` → el deploy falla. El repo del server además arrastraba otros mods locales inertes (`docker-compose.yml`, `frontend/src/*`).
**Regla:** el deploy usa `git fetch + git reset --hard origin/main` (idempotente; `.env.prod` gitignored y `backups/` untracked no se tocan). El server es un checkout pristino de main: todo lo de entorno vive en `.env.prod` y volúmenes. No editar archivos versionados directo en el server.

### [2026-10-15] VPS de Hetzner dado de baja irrevocable por impago — sin backup off-site
**Contexto:** 3 avisos de pago ignorados → Hetzner canceló el contrato y borró todos los datos sin posibilidad de reversión. `scripts/backup.sh` guardaba los dumps en `backups/` del mismo servidor, así que el VPS perdido se llevó también los backups. No había copia off-site (P3-10 seguía pendiente en el roadmap).
**Error:** backup local-only = sin backup real ante pérdida total del servidor. Además, nunca se había ejercitado un reprovisioning desde cero, así que un bug de Flyway (`V5` duplicaba una columna ya presente en el baseline `V1`) quedó sin detectar durante meses — solo rompe en una DB completamente nueva.
**Regla:** (1) backup off-site obligatorio en un proveedor **distinto** al de cómputo, con alertas de billing propias — un solo proveedor cancelado no puede llevarse infra + backup a la vez. (2) ejercitar el disaster-recovery runbook (`docs/DEPLOY.md`) al menos una vez contra un servidor vacío real, no solo en papel — es la única forma de detectar bugs de "fresh install" como este. Migrado a DigitalOcean el mismo día.

### [2026-10-15] Reserva pública: race condition, PII leak y backlog de idempotencia
**Contexto:** revisión manual post-deploy de `PublicReservaService`/`CitaService` encontró un leak de PII (nombre real filtrado por dedupe de email), solapamiento inconsistente entre disponibilidad pública y validación de admin, y falta de lock ante reservas concurrentes para el mismo slot.
**Primer intento (PR #33), insuficiente:** `GET_LOCK` de MySQL por día dentro de `@Transactional`, liberado en un `finally` del mismo método. Una segunda revisión encontró dos problemas reales:
  1. **El lock no cerraba la race condition de verdad.** Spring commitea la transacción DESPUÉS de que el método retorna, pero `RELEASE_LOCK` corría en el `finally`, ANTES del commit. Ventana: thread B toma el lock, lee con `REPEATABLE READ` (default de MySQL) y no ve el insert todavía no comiteado de thread A → las dos pueden pasar el chequeo. Se verificó empíricamente con un test de 2 threads vía JDBC puro contra un MySQL descartable (no H2 — no soporta `GET_LOCK`), corrido 5 veces: con el lock mal ubicado el riesgo es real (razonamiento confirmado), con el fix (abajo) dio `1 cita insertada` las 5/5 veces.
  2. **El dedupe "solo reusar si el nombre coincide" rompía reservas legítimas.** `pacientes.email` nunca tuvo constraint único (confirmado en `V1__baseline_schema.sql`) y el resto de la app (`PacienteService.update`, el propio dedupe) asume `findByEmail` singular. Crear un segundo paciente con el mismo email ante un nombre distinto hace que cualquier `findByEmail` posterior para ese email reviente con `IncorrectResultSizeDataAccessException` — rompe "María" vs "Maria", un padre reservando para su hijo con el mismo mail familiar, y cualquier reserva futura legítima de la víctima. Estuvo en producción ~1h (desde el merge del PR #33 hasta el fix).
**Fix real:** lock pesimista de fila (`SELECT ... FOR UPDATE` sobre `disponibilidad_semanal`, vía `@Lock(PESSIMISTIC_WRITE)`), tomado al inicio de la transacción — lo libera MySQL automáticamente al hacer commit/rollback, sin ventana manual que gestionar. Reusado en `create()`/`update()`/`confirmar()` del admin (antes solo lo tenía la reserva pública). Dedupe por email vuelve a ser simple (reusar si existe, sin exigir nombre) — la verificación de identidad real (email de verdad, no solo "dice ser X") queda para cuando haya un flujo de confirmación por mail.
**Backlog explícito, no resuelto:** reintentar una reserva exitosa hoy da "horario no disponible" en vez de devolver la cita ya creada — falta una clave de idempotencia en el request (ej. UUID generado client-side, guardado en la cita, chequeado antes de insertar). Evaluar en S-Audit o S7 (portal paciente) si se justifica antes.
**Regla:** un lock que se libera "manualmente" en un `finally` dentro de un método `@Transactional` casi seguro se libera ANTES del commit real (que hace el proxy de Spring al volver del método) — para que un lock proteja hasta el commit, tiene que ser uno que el motor de la DB libere solo, atado a la transacción (`FOR UPDATE`), no uno de liberación explícita (`GET_LOCK`/`RELEASE_LOCK`). Y nunca asumir cardinalidad sin verificarla en el schema — sin constraint único, "un paciente por email" es una convención de la app, no una garantía de la base.

---

## Formato de entrada

Cuando se registre una lección, usar este formato:

```
### [fecha] Título corto del error
**Contexto:** qué se estaba haciendo
**Error:** qué salió mal o qué dijo el usuario
**Regla:** comportamiento correcto a seguir en el futuro
```

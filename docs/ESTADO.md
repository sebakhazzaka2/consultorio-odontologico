# ESTADO — Consultorio / TurnosUy

> Fecha: 2026-10-16 · Rama `main` (HEAD `17f059d`) · Documento vivo: se actualiza al cerrar cada sesión (sección "Estado actual" de `CLAUDE.md` + este archivo, sin tocar el bloque COMMON).
> Generado leyendo el repo; no se ejecutó código, tests ni comandos con efectos. No contiene secretos ni datos de clientes.
> `docs/estado-consultorio.md` es una auditoría anterior (2026-10-15); varias filas ya están resueltas (ver "Inconsistencias" abajo). Este archivo manda cuando difieren.

---

## 1. HECHOS VERIFICADOS

### Producto y operación
- Monorepo Spring Boot 3.2.5 / Java 17 + Angular 19 + MySQL 8. Evidencia: `backend/pom.xml`, `frontend/package.json`, `docker-compose.prod.yml`.
- Una instancia Docker por cliente; sin `tenant_id` en ninguna entidad ni migración. Evidencia: `backend/src/main/java/com/consultorio/model/*.java`, `backend/src/main/resources/db/migration/V1`–`V9`.
- CI: `mvn verify` → imágenes a GHCR con tag = SHA → deploy SSH por instancia (GitHub Environments, matriz `instance`) en cada push a `main`; rollback por SHA documentado en `docs/DEPLOY.md` (PR #49). Evidencia: `.github/workflows/ci.yml`. No re-auditado en detalle.
- Backup a Backblaze B2 (dump + uploads) y restore: `scripts/backup.sh`, `scripts/restore.sh`, `tasks/lessons.md` (verificación declarada, no reproducible desde el repo).
- Validación de arranque en prod: JWT ≥ 32 caracteres, CORS, admin ≠ valor de desarrollo y `CLINIC_*` obligatorias. Evidencia: `config/StartupEnvValidator.java`.
- `show-sql` apagado en prod; Actuator solo `health` en :8081. Evidencia: `application-prod.properties`.

### Funcionalidades implementadas
| Módulo | Evidencia |
|---|---|
| Login JWT admin + cambio de contraseña; credenciales inválidas → 401 | `AuthController`, `AuthService`, `GlobalExceptionHandler.handleBadCredentials` |
| Citas: CRUD, confirmar, cancelar, solapamiento solo contra PENDIENTE/CONFIRMADA | `CitaController`, `CitaService` |
| Disponibilidad semanal + fechas bloqueadas (una agenda por instancia) | `DisponibilidadController` (`/admin/disponibilidad`), `V7`, `DisponibilidadSemanal` (`UNIQUE dia_semana`) |
| Reserva pública sin login (crea cita PENDIENTE; alta de paciente con `creado_por=AGENDA`; lock `FOR UPDATE`; honeypot) | `PublicReservaController`, `PublicReservaService` |
| Pacientes CRUD (email obligatorio; unicidad solo en la app) | `PacienteController`, `PacienteService` |
| Servicios CRUD + toggle + foto | `ServicioController`, `FileStorageService` |
| Historial de procedimientos con `precio_aplicado` snapshot | `HistorialProcedimientosController` |
| Pagos (alta/baja, sin edición) y saldo calculado | `PagoController`, `SaldoController` |
| Página pública (home, servicios, reseñas Google) y config de marca por env | `PublicClinicController`, `PublicReviewsController`, `ClinicProperties`, `features/public/*` |
| Seed demo aislado por `app.seed-demo-data` | `config/DemoDataSeeder.java` |

### Endpoints disponibles (context path `/api`)
- **Públicos, sin auth:** `GET /public/config`, `GET /public/servicios`, `GET /public/slots`, `GET /public/reviews`, `POST /public/reservas`, `GET /uploads/**`, `POST /auth/login`.
- **JWT (cualquier usuario autenticado):** `/citas` (+ `/disponibilidad`, `/{id}/cancelar`, `/{id}/confirmar`), `/pacientes` (+ `/{id}/saldo`), `/historial-procedimientos` (+ `/paciente/{id}`, `/cita/{id}`), `/pagos` (+ `/paciente/{id}`), `/servicios` (+ `/activos`, `/{id}/toggle`, `/{id}/foto`), `/admin/disponibilidad/*`, `/auth/change-password`, `/auth/register`.
- Evidencia: `controller/*.java`, `security/SecurityConfig.java`. No hay OpenAPI/springdoc.

### Tests
- Backend: 33 `@Test` en 10 clases (controladores contra H2; servicios con Mockito; `backend/src/test/java/com/consultorio/mysql/` contra MySQL real vía Testcontainers: migraciones y reserva concurrente, PR #48). Frontend: 26 `it` en 6 specs, solo servicios y `brand-logo`. Evidencia: `backend/src/test`, `frontend/src/**/*.spec.ts`. CI corre solo `mvn verify`.

### Riesgos de seguridad visibles (sin valores)
- No hay autorización por rol: sin `@PreAuthorize`/`hasRole`; el rol `ADMIN` existe pero nada lo exige. Evidencia: grep en `backend/src/main`.
- `POST /auth/register` exige JWT pero cualquier admin autenticado puede crear otro ADMIN. Evidencia: `SecurityConfig`, `AuthControllerTest.register_autenticado_retorna201`.
- JWT en `localStorage` y sin revocación; sin lockout por cuenta (solo rate limit nginx por IP). Evidencia: `frontend/src/app/core/auth/auth.service.ts`, `frontend/nginx.conf`.
- Sin validación de fechas pasadas ni horizonte en reservas; el dedupe por email adjunta citas a un paciente existente sin verificar el email. Evidencia: `PublicReservaService`, `CitaService`.
- PII en logs: `PacienteService` registra email en WARN y nombre en INFO; `AuthService` registra el email del login. Evidencia: `PacienteService.java:39,45,63`, `AuthService.java:37`.
- El perfil base deja `logging.level.org.springframework=DEBUG` (prod lo baja a WARN) y define una contraseña admin de desarrollo en `application.properties` (el validador de prod la rechaza). Evidencia: `application.properties`, `StartupEnvValidator`.
- Sin CSP (pendiente); sin audit log; `findAll()` sin paginación en citas/pacientes/pagos. Evidencia: `Caddyfile`, `CitaService`.
- `docs/DEPLOY.md` (versionado) contiene IP pública, usuario SSH y ruta del servidor: no es secreto pero amplía la superficie de reconocimiento. Considerar sacarlo del repo público si el repo lo es.
- `DataIntegrityViolationException` siempre responde "No se puede eliminar…" aunque ocurra en un insert. Evidencia: `GlobalExceptionHandler`.

### Qué falta para soportar varios negocios (tenants)
1. `negocio_id` en todas las tablas, repos y locks; hoy la unicidad y el lock son por día de semana global (`DisponibilidadSemanalRepository.findByDiaSemanaForUpdate`).
2. Resolver el negocio por request (subdominio o API key) y llevarlo en el JWT; hoy el JWT no tiene claims de rol ni de negocio.
3. Entidad Profesional/Recurso: una agenda por instancia.
4. Marca data-driven: logo, paleta, moneda (`UY$`) y locale (`es-UY`) están hardcodeados; `FEATURE_*` no existen (`ClinicProperties.Feature` son tarjetas de marketing).
5. Vocabulario dental/médico en API y esquema (`/pacientes`, `/historial-procedimientos`).
6. Decisión vigente (`ROADMAP.md`, `backend/CLAUDE.md`): no migrar a multitenancy real hasta ~5 clientes pagando; la opción anotada es un schema MySQL por tenant.

### Superficie disponible para un futuro agente
- Utilizable hoy sin auth: catálogo (`/public/servicios`), disponibilidad (`/public/slots`), reserva (`POST /public/reservas`, queda PENDIENTE), config (`/public/config`).
- Falta: autenticación de máquina con scopes, identidad verificada del paciente por teléfono, cancelar/reagendar con token de cita, idempotency key, rate limit en backend (hoy solo nginx), webhooks entrada/salida, OpenAPI, auditoría de acciones del agente, y un campo de origen por canal (`PacienteOrigen` solo ADMIN/AGENDA).

---

## 2. HIPÓTESIS / DUDAS

- Que los 8 tests de servicio que fallaban en un JDK 25 local pasan con Temurin 17: no se ejecutaron en esta sesión; la corrida de CI no es visible desde el repo.
- Que producción corre el último `main` y que el restore a B2 se probó (lo afirman `CLAUDE.md` y `tasks/lessons.md`); no verificable desde el código.
- Estado real de `/api/auth/register` y lista de usuarios ADMIN en prod (consulta de solo lectura pendiente).
- Si la demo de Render expone el backend directo sin nginx ni rate limit (`render.yaml`, `frontend/nginx.render.conf`).
- Si `frontend/CONFIGURACION_EMAIL.md` describe algo implementado: no se leyó y no hay envío de email en el backend.
- Dependencias fuera de soporte (Spring Boot 3.2.x, MySQL 8.0, Angular 19, imágenes con tag flotante): sin `mvn dependency-check` ni `npm audit` ejecutados.
- Consentimiento y base legal para datos de salud (Ley 18.331 UY): no verificable desde el repo.
- Cantidad de datos reales en prod (dimensiona la falta de paginación) y duplicados de email (no hay índice único hasta `V10`).
- `Paciente.user_id` y `citas.google_event_id` parecen sin uso en el código (hay lectura de `Cita` pero no integración con Google Calendar).

### Inconsistencias README / documentos vs. código
- `README.md` § "Current status" y "Known technical debt" aún tratan F0 como "Next" y listan login 500 y defaults de cliente como deuda; `CLAUDE.md`/`ROADMAP.md` y el código muestran F0 cerrada (login → 401, `StartupEnvValidator` exige `CLINIC_*`).
- Bloque COMMON v1 (`CLAUDE.md`, § 8) decía "backups: restauración no verificada en ninguno"; para TurnosUy `tasks/lessons.md` registra restore probado (2026-10-15). Se editó esa línea en este repo por pedido explícito; falta replicarla en la fuente CONTRATO-COMUN.md.
- `README.md` y `ROADMAP.md` dicen 26 tests de backend en 7 clases; hay 33 `@Test` en 10 clases.
- Esta auditoría se escribió antes del PR #47 (texto del rubro por configuración): las menciones a texto dental fijo en `home`/`servicios` y a defaults de marketing en `ClinicProperties` pueden estar resueltas parcialmente; no re-verificado.
- `backend/CLAUDE.md` dice "Login fallido hoy responde 500"; el código ya devuelve 401 (`GlobalExceptionHandler`).
- `README.md` dice "Feature flags via env vars … (planned)": coherente, pero `docs/estado-consultorio.md` sigue marcando como pendientes cosas ya resueltas (show-sql en prod, defaults de cliente, restore probado).
- `ROADMAP.md` § "Hechos verificados" tiene filas de estado previo a F0 (marcadas como tales en la nota superior).
- `backend/CLAUDE.md` indica `jwt.expiration` 2h en prod (correcto) pero el perfil base usa 1h; no es contradicción, solo conviene saberlo.
- `ClinicProperties` conserva defaults de marketing odontológicos (imagen hero, tarjetas "Historial seguro", "Profesionales matriculados") que se verían en una instancia de otro rubro si no se sobrescriben.

---

## 3. PENDIENTE / DEUDA TÉCNICA (priorizada)

1. **F1 — Instancia de Guille + importador CSV + `V10`** (email nullable + único, índice): siguiente fase en `ROADMAP.md`.
2. **Recordatorios por email (F2a):** `NotificationService`, proveedor, cron idempotente (`recordatorio_enviado_at`), zona horaria por instancia, SPF/DKIM/DMARC. Es el dolor principal del cliente.
3. **Autorización por rol:** `@PreAuthorize`/claims en el JWT y cerrar `register` a un rol propietario antes de crear cualquier usuario no admin (portal de paciente, agente).
4. **Flags `FEATURE_*` reales** en `ClinicProperties` + `GET /public/config` (F3), más moneda y locale por configuración.
5. **Validación temporal de reservas** (rechazar pasado, horizonte máximo) y verificación de email/teléfono antes de adjuntar a un paciente existente.
6. **Quitar PII de logs** (`PacienteService`, `AuthService`) y bajar el nivel DEBUG del perfil base.
7. **CI:** correr `ng test` (hoy solo `mvn verify`). Las migraciones y el lock de reserva ya tienen tests contra MySQL real (PR #48); falta confirmar en la corrida de CI que se ejecutan y no se omiten.
8. **Paginación** de citas/pacientes/pagos y dashboard calculado en servidor.
9. **Hardening S9:** JWT en cookie HttpOnly, lockout, CSP, audit log, logs JSON con `correlationId`.
10. **Dependencias:** fijar tags de `mysql`/`uptime-kuma`, correr `npm audit`/OWASP y planear upgrade de Spring Boot y MySQL.

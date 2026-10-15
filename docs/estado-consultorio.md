# Estado del producto — auditoría de solo lectura

> Fecha: 2026-10-15 · Rama `main` (commit `ed54439`) · Auditoría sin cambios de código, config, dependencias ni BD.
> Único archivo creado: este. Único comando con efecto lateral: `mvn test` local (escribe en `backend/target/`, ignorado por git).
> Convención: **[HECHO]** = verificado leyendo el código citado. **[HIPÓTESIS]** = inferido o no verificable desde el repo.
> No se leyeron secretos. Existen: `.env.example` (plantilla, versionada), `.env.prod` en el servidor (gitignored, no accesible), `jwt.secret`/credenciales en `application*.properties` vía env vars, DSN de Sentry hardcodeado en `frontend/src/environments/environment.ts` (los DSN de frontend son públicos por diseño), password de demo en `frontend/src/environments/environment.demo.ts` (intencional, instancia demo).

---

## 1. Stack y arquitectura

**[HECHO]**

| Capa | Tecnología | Evidencia |
|---|---|---|
| Backend | Java 17, Spring Boot 3.2.5, Spring Security + JWT (jjwt 0.12.5), JPA/Hibernate, Flyway, Actuator, Sentry 7.14 | `backend/pom.xml` |
| BD | MySQL 8 (prod/local); H2 en modo MySQL (tests) | `docker-compose.prod.yml`, `backend/src/test/resources/application.properties` |
| Frontend | Angular 19.2 (standalone components), Angular Material 19, Chart.js/ng2-charts, Sentry Angular, Karma/Jasmine | `frontend/package.json` |
| Proxy/TLS | Caddy 2 (TLS automático, headers) → nginx (SPA + proxy `/api` + rate limit) → backend | `Caddyfile`, `frontend/nginx.conf` |
| Monitoreo | Uptime Kuma (contenedor) + Sentry | `docker-compose.prod.yml` |
| CI/CD | GitHub Actions: `mvn verify` → build/push imágenes a GHCR → deploy por SSH a DigitalOcean | `.github/workflows/ci.yml` |
| Demo | Render (Docker) + MySQL externo (Aiven), `SEED_DEMO_DATA=true` | `render.yaml`, `config/DemoDataSeeder.java` |

**Estructura**: monorepo `backend/` (paquetes `controller / service / repository / model / dto / security / config / exception`), `frontend/src/app/{core,features/{admin,public,auth},shared}`, `scripts/` (backup/restore/cron), `docs/`.

**Despliegue**: una instancia Docker Compose por cliente (db + backend + frontend/nginx + caddy + kuma), un solo código y una sola imagen; variación por variables de entorno (`CLINIC_*`, `FEATURE_*` según CLAUDE.md). Backup con `scripts/backup.sh` + rclone a Backblaze B2.

**[HIPÓTESIS]** Los flags `FEATURE_PUBLIC_PAGE/PATIENT_PORTAL/WHATSAPP` que CLAUDE.md describe **no existen en el código** (ver §5/§6). La documentación va por delante de la implementación.

---

## 2. Módulos existentes

| Módulo | Estado | Evidencia / notas |
|---|---|---|
| Autenticación admin (login, cambio de contraseña) | **Completo** (básico) | `AuthController`, `AuthService`, `JwtService`. Un solo rol efectivo (ADMIN). |
| Agenda / citas (CRUD, confirmar, cancelar, reagendar, solapamiento) | **Completo** | `CitaService`, `CitaController`, frontend `agenda-calendar`, `cita-form`, `citas-listado`. Estados `PENDIENTE/CONFIRMADA/CANCELADA/COMPLETADA` (`CitaEstado`). |
| Disponibilidad (horario semanal, pausa, fechas bloqueadas) | **Completo** para 1 recurso | `DisponibilidadSemanal` (UNIQUE `dia_semana`), `FechaBloqueada`, `DisponibilidadService`. |
| Reserva pública sin login | **Completo** | `PublicReservaController/Service`, frontend `public/reserva`. Crea cita `PENDIENTE` que el admin confirma. |
| Pacientes (CRUD) | **Completo** | `PacienteService`, frontend `pacientes-*`. |
| Historial de procedimientos | **Parcial** (log de texto, no ficha clínica) | `HistorialProcedimientos` — ver §3. |
| Pagos y saldo por paciente | **Parcial** | `Pago`, `PagoService.getSaldoPaciente`. Sin medios de pago, sin recibos, sin caja diaria. |
| Caja / cierre diario | **No existe** | Solo `dashboard` con ingresos del período (`dashboard.component.html:69`). |
| Estadísticas | **Parcial** | Dashboard con KPIs (citas semana, pendientes, ingresos) calculados **en el cliente** sobre `GET /citas` completo (`dashboard.component.ts:159-172`). |
| Servicios (catálogo con precio, duración, foto) | **Completo** | `Servicio`, `ServicioController`, `FileStorageService`. |
| Página pública (home, servicios, reseñas Google, WhatsApp FAB) | **Completo** | `features/public/*`, `GooglePlacesService`. |
| Portal del paciente (login paciente) | **Esqueleto** | Solo existe `Paciente.user` (FK a `users`) sin uso: no hay endpoints ni UI. Declarado "S7" en ROADMAP. |
| Notificaciones (mail / WhatsApp) | **No existe** | Ver §4. |
| Integración Google Calendar | **Esqueleto abandonado** | Solo la columna `citas.google_event_id` (V9) y el campo en `Cita`; ningún código lo usa. |
| Presupuestos, archivos, multi-profesional, audit log | **No existe** | Planificados en ROADMAP (S5). |

---

## 3. Historia clínica / ficha

**[HECHO]** No hay ficha clínica estructurada. Lo que existe:

- `Paciente` (`model/Paciente.java`): nombre, apellido, teléfono (7–15 dígitos), email (obligatorio), fecha de nacimiento, `creado_por` (ADMIN/AGENDA), `user_id` (sin uso).
- `HistorialProcedimientos` (tabla `historial_procedimientos`): paciente, cita (opcional), fecha/hora, `procedimiento` (texto ≤500), `notas` (≤2000), servicio (opcional), `precio_aplicado`, `foto_url` (columna presente; solo hay upload para servicios en `FileStorageService.storeServicioFoto` → **no hay upload de fotos clínicas**).
- UI: `paciente-detalle.component` (tabla de historial + pagos + saldo), `historial-form-dialog`.

**Faltan** (sin campos ni flujos): antecedentes/alergias/medicación, odontograma, consentimientos, adjuntos (radiografías), evolución por pieza dental, recetas, firma, historial de cambios (audit). Es un registro de "qué se hizo y cuánto costó", no una historia clínica.

**[HIPÓTESIS]** Para otras verticales sin necesidad clínica esto es una ventaja (no hay datos sensibles estructurados); para odontología seria sería insuficiente legalmente (Ley 18.331 UY — no verificado).

---

## 4. Recordatorios y notificaciones

**[HECHO]**

- **Email**: no hay `spring-boot-starter-mail`, `JavaMailSender` ni plantillas en `pom.xml` / `src/main`. Grep de `JavaMailSender|twilio|@Scheduled` → solo `DisponibilidadService.limpiarFechasPasadas` (limpia fechas bloqueadas, cron 03:00).
- **WhatsApp**: solo un enlace `https://wa.me/<número>` en la página pública (`public.component.html:350,402`). No hay API de envío, webhooks ni plantillas.
- **Reserva pública**: responde "Te contactaremos para confirmar tu turno" (`PublicReservaService`); el paciente no recibe ninguna confirmación automática.
- `frontend/CONFIGURACION_EMAIL.md` existe pero es documentación; no hay implementación backend (verificar si trata EmailJS u otro lado cliente — **[HIPÓTESIS]**, no leído en detalle).

**Falta para recordatorios automáticos**: (1) `NotificationService` con canales; (2) proveedor (Brevo/SES/Twilio/Meta Cloud API); (3) job programado con idempotencia (campo `recordatorio_enviado_at` en `Cita`, no existe); (4) consentimiento/opt-in y baja; (5) plantillas aprobadas (WhatsApp); (6) zona horaria explícita (hoy todo es `LocalDateTime` sin zona); (7) cola/reintentos y registro de entregas; (8) multi-instancia segura del scheduler (`@Scheduled` correría en cada réplica; hoy hay una sola).

---

## 5. Multi-negocio

**[HECHO] No hay separación de datos por negocio.** Ninguna entidad tiene `tenant_id`/`negocio_id` (`model/*.java`, `V1__baseline_schema.sql`). El aislamiento es **una base de datos + un contenedor por cliente**. La marca vive en variables de entorno (`ClinicProperties`, `GET /public/config`).

**Hardcodeos específicos de cliente / rubro:**

| Dónde | Qué |
|---|---|
| `ClinicProperties.java:25-56` | Defaults: "Dental Montecaseros", dirección, teléfono, WhatsApp, email reales, horario, imagen de Unsplash odontológica, features "Historial seguro / Profesionales matriculados / Tecnología actual". |
| `application.properties:51-63` | Mismos defaults de cliente real en `clinic.*` (`${CLINIC_NOMBRE:Dental Montecaseros}` …, teléfono y mail reales). Si falta una env var, **se publica el dato del primer cliente en otra instancia**. |
| `frontend/src/app/features/public/clinic.config.ts` | `CLINIC_CONFIG` con nombre, dirección, teléfono, WhatsApp y mail reales (fallback). |
| `frontend/src/index.html:5-9,18` | `<title>`, `description`, `og:title` "Dental Montecaseros — Odontología…", `preload` de imagen odontológica. No es configurable en runtime. |
| `DemoDataSeeder.java:64-76` | Servicios odontológicos (limpieza, ortodoncia, endodoncia, implantes…). Solo demo. |
| `ClinicProperties.labelServicio / labelHistorial` | Etiquetas parametrizables ("Servicio", "Historial clínico") — buen paso, pero solo llegan a parte de la UI. |
| Frontend admin | Vocabulario fijo: "Pacientes", "Tratamiento" (`paciente-detalle.component.ts:76,180`), moneda `UY$` hardcodeada en ≥8 sitios (`paciente-detalle.component.html:44-157`, `historial-form-dialog.component.html:52`), locale `es-UY`/`UYU` (`dashboard.component.html:5,69,107`), `LOCALE_ID='es'`. |
| `brand-logo.component.ts:27-33` | Logo SVG y wordmark "TurnosUy" hardcodeados (marca del producto, no del cliente). |
| `_design-system.scss` | Paleta navy fija ("TurnosUy Design System"). |
| `DisponibilidadSemanal` seed V7 | Lunes–Viernes 9–18 con pausa 13–14 (insertado por migración). |
| Clínica: texto "cita/paciente" en entidades y rutas API | `/pacientes`, `/citas`, `/historial-procedimientos`: el modelo de datos y la API están en vocabulario dental/médico. |

---

## 6. Candidatos a módulo / flag

**[HECHO]** Hoy solo hay un flag implícito: `reviewsEnabled = googlePlaceId no vacío` (`PublicClinicController:38`). El resto de la tabla de flags de CLAUDE.md no está implementado.

Funciones hoy mezcladas que son separables por negocio:

1. **Página pública + reserva online** (`/public/**`, `features/public`) — activable/desactivable.
2. **Historial de procedimientos** (ficha) — no aplica a muchas verticales (peluquería, canchas).
3. **Pagos / saldos** — módulo financiero opcional; acoplado a `HistorialProcedimientos` (deuda = suma de `precio_aplicado`, ver `PagoService.getSaldoPaciente`).
4. **Reseñas Google** (`GooglePlacesService`).
5. **Dashboard con ingresos** (depende de pagos).
6. **Fotos de servicio** / upload (`FileStorageService`).
7. **Disponibilidad y fechas bloqueadas** — núcleo reutilizable de agenda.
8. **Notificaciones** (futuro): canal email, canal WhatsApp.
9. **Demo seeder** (ya aislado por `app.seed-demo-data`).

---

## 7. Seguridad básica

**[HECHO] — bien resuelto**
- BCrypt (`SecurityConfig.passwordEncoder`), JWT HMAC firmado, expiración 2h en prod (`application-prod.properties`).
- `anyRequest().authenticated()`; público solo `POST /auth/login`, `GET /public/**`, `POST /public/reservas`, `GET /uploads/**` (`SecurityConfig:56-61`).
- `/auth/register` ya no es público: devuelve 401 sin token (`AuthControllerTest.register_sinAutenticar_retorna401`). Aun así, **cualquier usuario autenticado puede crear otro ADMIN**.
- Validador de arranque en prod (JWT ≥32 chars, admin ≠ `admin123`) (`StartupEnvValidator`, `ConsultorioApplication.validateProdEnvVars`).
- Validación de entrada en reserva pública: `@Size`, regex de teléfono, `@Email`, honeypot `website`; upload limitado a jpg/jpeg/png/webp con nombre UUID (`FileStorageService`), 5MB.
- Rate limit en nginx: login 5/min, reservas 6/min, lecturas públicas 60/min (`frontend/nginx.conf:1-9`); `X-Real-IP` sobreescrito en Caddy; headers HSTS/XFO/nosniff (`Caddyfile`). Actuator en puerto 8081 no expuesto. Contenedor backend con usuario no-root. MySQL sin puertos publicados.
- Concurrencia de reservas corregida con `SELECT … FOR UPDATE` (commits #33–#35) y test.

**[HECHO] — brechas**
1. **Sin autorización por rol**: `role` existe pero no hay `@PreAuthorize`/`hasRole` (`grep` → solo `User.getAuthorities`). El JWT no lleva claims de rol. Cuando exista portal de paciente, todo endpoint admin quedaría abierto a cualquier usuario autenticado.
2. **Login con credenciales inválidas responde 500** y se reporta como error no controlado (`AuthService` lanza `RuntimeException` → `GlobalExceptionHandler.handleGenericException`; el test lo fija: `AuthControllerTest.login_passwordIncorrecta_retorna500`). Contamina Sentry y rompe la semántica 401.
3. Sin lockout por cuenta ni contraseña mínima en login; solo rate limit por IP en nginx (en `local`/Render sin nginx no aplica; Render demo expone el backend directo — **[HIPÓTESIS]**).
4. **Sin revocación de JWT**; token en `localStorage` (`auth.service.ts:16-28`) → exfiltrable por XSS. `JwtAuthFilter` traga todas las excepciones.
5. **Reserva pública con email ajeno**: el dedupe por email reutiliza al paciente existente (`PublicReservaService.resolverPaciente`); un tercero puede adjuntar citas al historial de otra persona y consumir su slot. Decisión consciente documentada, pero sin verificación de email.
6. **Sin validación temporal**: no se rechazan fechas pasadas ni hay horizonte máximo de reserva (`CitaService.getDisponibilidad` no compara con `now`).
7. `GET /public/slots` sin rate limit en backend (solo nginx). `GET /uploads/**` público (por diseño).
8. Sin audit log de acciones admin ni de acceso a datos de pacientes (ROADMAP S5).
9. CSP pendiente (comentado en `Caddyfile`).
10. `spring.jpa.show-sql=true` y `logging.level.org.springframework=DEBUG` en el `application.properties` base (prod los baja a WARN; `show-sql` sigue activo en prod porque no se sobreescribe — **[HIPÓTESIS]**, verificar). El log de login registra el email en INFO.
11. `findAll()` sin paginación en citas/pacientes/pagos (DoS por volumen y exposición masiva ante cuenta comprometida).
12. `DataIntegrityViolationException` siempre responde "No se puede eliminar…" aunque ocurra en un insert.

**Dependencias [HIPÓTESIS, no ejecutado `npm audit`/`mvn dependency-check`]**: Spring Boot 3.2.5 salió de soporte OSS en nov-2024; Angular 19 salió de soporte LTS en 2026; MySQL 8.0 EOL abril 2026; `flyway-maven-plugin 9.22.3` (otra versión que el Flyway del starter). Imágenes `uptime-kuma:1` y `mysql:8.0` con tag flotante. Conviene correr `mvn versions:display-dependency-updates` + OWASP Dependency-Check + `npm audit`.

---

## 8. Tests y calidad

**[HECHO]** (ejecutado localmente con `mvn -o test`, JDK 25.0.2 local; CI usa JDK 17)

- Backend: 7 clases, 26 tests (`@Test`). **18 pasan, 8 con error** — todos en `CitaServiceTest` y `PublicReservaServiceTest` por `Mockito cannot mock class DisponibilidadService … Could not modify all classes` (ByteBuddy/Mockito vs JDK 25). **[HIPÓTESIS]**: es un problema del JDK local, no del código; en CI con Temurin 17 deberían pasar (no verificado — el estado de CI no es visible desde acá).
- Los tests de controlador corren contra H2 con `ddl-auto=create-drop` y **Flyway deshabilitado**: las migraciones V1–V9 **no se prueban en CI**, ni el comportamiento de `FOR UPDATE` bajo MySQL real (la corrección de la carrera se validó a mano según comentario en `PublicReservaService`).
- Sin tests: `DisponibilidadService`, pagos/saldo, historial, servicios, upload, `GooglePlacesService`, lockout/seguridad, cancelación/reagendar vía API.
- Frontend: 6 specs (~26 `it`), solo servicios y `brand-logo`; ningún componente de admin/agenda/reserva. `ng test` no figura en el CI (`ci.yml` solo corre `mvn verify`, y los jobs de imagen compilan el frontend con el Dockerfile). No hay linter ni E2E.
- Cobertura medida: ninguna (sin JaCoCo ni `karma-coverage` configurado en CI).
- `mvn -DskipTests` en el Dockerfile; el gate real es el job `backend-test`.

**[HIPÓTESIS]** `ng build`/`ng test` no se ejecutaron en esta auditoría; el estado del build del frontend se infiere por el despliegue exitoso en producción.

---

## 9. Frontend

**[HECHO]**
- **UI**: Angular Material 19 con el tema prebuilt `indigo-pink.css` (`styles.scss:2`) + capa propia de tokens CSS (`styles/_design-system.scss`, paleta navy/ink, Inter). Componentes custom en `shared/` (page-header, empty-state, status-chip, brand-logo).
- **Cambiar colores**: los tokens en `:root` (`--color-navy-*`, `--color-primary`) permiten re-skin con ~10 variables, pero (a) el tema Material sigue siendo `indigo-pink` prebuilt (no tema M3 parametrizable) y (b) no hay carga dinámica de paleta desde `/public/config` (S4 "Theming" postergado). Cambiar de cliente = rebuild o CSS override.
- **Logo**: SVG embebido en `brand-logo.component.ts` + wordmark "TurnosUy"; cambiar por cliente exige tocar código. `favicon.ico` único. No hay `logoUrl` en `ClinicConfigResponse`.
- **Textos**: españoles hardcodeados en templates (sin i18n, sin Transloco/`@angular/localize`). Solo ~6 campos de la marca vienen del backend (nombre, tagline, labels, features, nosotros…). El copy de la página pública está parcialmente en `public.component.html` (463 líneas).
- **Moneda/locale**: `UY$` y `es-UY` fijos.
- **Estado visual**: moderno en lo básico (Material 19, tokens, Inter, hero, cards, FAB WhatsApp, calendario propio). Evidencias de "producto genérico": tema Material por defecto indigo/pink con paleta navy sobrepuesta; capturas en `docs/screenshots/`. No se evaluó visualmente en vivo (**[HIPÓTESIS]** no he abierto el sitio). ROADMAP admite que falta "Fase Premium UX" (diseñador externo).
- Carga: `index.html` carga Roboto + Inter + Material Icons desde Google Fonts (3 requests bloqueantes, dependencia externa, sin self-hosting).
- Rutas admin con lazy loading parcial (varias con `component:` eager). Manejo de chunk obsoleto (`chunk-error.handler.ts`).

---

## 10. Deuda técnica y riesgos (por gravedad)

| # | Gravedad | Riesgo | Evidencia |
|---|---|---|---|
| 1 | **Alta** | Sin recordatorios ni confirmación automática: el no-show es el dolor principal y el producto promete "Web + WhatsApp" sin implementación. | §4 |
| 2 | **Alta** | Single-tenant duro: un recurso/agenda, sin `negocio_id`, sin profesional. Reutilizar = fork/instancia por cliente. | §5, `DisponibilidadSemanal` UNIQUE |
| 3 | **Alta** | Sin autorización por rol ni permisos finos; cualquier usuario autenticado es admin total. | §7.1 |
| 4 | **Alta** | Restore de backup nunca probado en VM limpia; alertas de Kuma sin confirmar (según CLAUDE.md). Datos de pacientes en un solo VPS. | CLAUDE.md "Operaciones mínimas" |
| 5 | **Media-Alta** | Defaults de cliente real embebidos (teléfono/mail) filtrables a otras instancias. | §5 |
| 6 | **Media** | Reserva permite fechas pasadas, sin verificación de email, dedupe adjunta a paciente ajeno. | §7.5-6 |
| 7 | **Media** | Tests: migraciones no probadas, 8/26 con error en JDK local, ningún test de frontend de UI, CI no corre `ng test`. | §8 |
| 8 | **Media** | Login inválido = 500 (y ruido en Sentry). | §7.2 |
| 9 | **Media** | Sin paginación ni consultas acotadas (`findAll`) y dashboard calculado en cliente. | `CitaService.findAll`, `dashboard.component.ts` |
| 10 | **Media** | Documentación divergente del código (flags `FEATURE_*`, "S3 sin mergear" mientras `main` ya contiene su contenido: register 401 + rate limit; `CLAUDE.md`/`ROADMAP.md` modificados sin commitear). | §1, `git status` |
| 11 | **Media** | Dependencias de versiones fuera de soporte (hipótesis). | §7 |
| 12 | **Baja** | Zona horaria implícita (`LocalDateTime`), slots fijos de 15 min, granularidad única. | `CitaService:getDisponibilidad` |
| 13 | **Baja** | `google_event_id` y `Paciente.user` sin usar; código muerto parcial. | §2 |
| 14 | **Baja** | Lock por día-de-semana serializa toda reserva de ese weekday (correcto con 1 agenda; no escala con N profesionales). | `DisponibilidadSemanalRepository.findByDiaSemanaForUpdate` |
| 15 | **Baja** | Credenciales de la demo embebidas en bundle (intencional). | `environment.demo.ts` |

---

## 11. Reutilización como núcleo de agenda (1 = muy bajo, 5 = muy alto)

| Criterio | Nota | Evidencia |
|---|---|---|
| Aislamiento por negocio | **1** | Sin `tenant_id`; aislamiento solo por instancia/BD. Migrar a multitenancy toca todas las tablas, repos, locks y el JWT. |
| Genericidad del modelo (turnos, servicios, profesionales) | **3** | `Cita` + `Servicio(duración, precio)` + disponibilidad semanal + bloqueos son genéricos y bien separados del historial. Pero: sin entidad Profesional/Recurso, una sola agenda, vocabulario "paciente" en API y esquema, historial/pagos acoplados. |
| Personalización de marca | **2** | Env vars para datos y textos principales; logo, paleta, moneda, locale, `index.html` y tema Material requieren tocar código y rebuild. |
| Seguridad | **3** | Base sólida (BCrypt, JWT, rate limit, hardening, validación de arranque, bloqueo de carrera). Faltan roles, revocación, lockout, audit, verificación de email. |
| Tests | **2** | Cobertura parcial en agenda/reserva (valiosa), nada en disponibilidad/pagos/UI, migraciones sin probar. |
| Costo de crear una vertical nueva | **3** (≈ 1–2 semanas por vertical **sin** multitenancy) | Con el esquema actual: copiar instancia, ajustar `CLINIC_*`, renombrar labels, cambiar seed y logo; esfuerzo bajo si el rubro encaja en "cliente reserva servicio en un horario de una sola agenda". Sube a ≥3–5 semanas si se necesita multi-profesional, multi-sede o ficha propia. |

**Resumen**: núcleo de agenda **aprovechable** para verticales de un solo profesional/recurso (peluquería, estética, psicología, canchas pequeñas). Para multi-profesional o SaaS multitenant requiere rediseño del modelo antes de venderlo como plataforma.

---

## 12. Superficie para un agente (p. ej. WhatsApp)

**[HECHO] Ya existe, sin autenticación, apto para un agente con límites:**
- `GET /api/public/servicios` — catálogo activo.
- `GET /api/public/slots?fecha&servicioId` — disponibilidad calculada en servidor.
- `POST /api/public/reservas` — crea cita `PENDIENTE` (con honeypot y rate limit nginx).
- `GET /api/public/config` — datos del negocio.

**Solo admin (JWT)**: `PATCH /citas/{id}/cancelar|confirmar`, `GET/POST/PUT/DELETE /citas`, `/pacientes`, etc.

**Qué falta para exponerlo de forma segura a un agente:**
1. **Identidad del negocio**: hoy la instancia = negocio; un agente multi-negocio necesita resolver tenant (API key por negocio / subdominio).
2. **Autenticación de máquina**: API key o token de scope limitado (`agent:read-availability`, `agent:book`, `agent:cancel-own`), **no** el JWT admin ni `/auth/register`.
3. **Identificación del paciente por canal**: verificar que quien cancela es dueño de la cita (hoy `cancelar` es admin; no existe "cancelar con código/teléfono verificado").
4. **Endpoints agent-friendly**: consultar mis citas por teléfono verificado, cancelar/reagendar con token de cita (magic link o código), idempotency key en reservas.
5. **Límites**: rate limit por número/IP/negocio en backend (hoy solo nginx), máximo de citas activas por paciente, horizonte de reserva, validación de fechas pasadas.
6. **Confirmación humana**: mantener `PENDIENTE` → confirmación del admin (ya existe el flujo) para acciones sensibles; registrar quién/qué canal creó la cita (`origen`: hoy `PacienteOrigen` solo ADMIN/AGENDA).
7. **Auditoría**: log inmutable de acciones del agente.
8. **Webhook de salida** (cita creada/cancelada) y **entrada** (mensajes WhatsApp) con verificación de firma.
9. **Contrato**: OpenAPI (no existe `springdoc`), respuestas de error semánticas (hoy 500 en login y mensajes en español libre).

---

## 13. Preguntas que solo se responden mirando producción

1. ¿Qué versión de imagen corre realmente en `neodentalmaster.turnosuy.com` (SHA vs `latest`) y está al día con `main`?
2. ¿`/api/auth/register` responde 401 en prod? ¿Se creó algún usuario `ADMIN` no esperado antes del cierre? (`SELECT email, role FROM users` — solo lectura).
3. ¿Cuántas citas/pacientes/pagos hay y cuál es el crecimiento? (dimensiona `findAll`).
4. ¿Hay pacientes duplicados por email en prod (el esquema no tiene UNIQUE) y citas en el pasado creadas por reserva pública?
5. ¿Qué proporción de citas son `PENDIENTE` sin atender y cuántas terminan `CANCELADA`/no-show? (valor de los recordatorios).
6. ¿Se probó un restore real del último backup de B2? ¿Hay alertas activas en Kuma y a quién notifican?
7. ¿Qué envía el cliente por la reserva (spam/bots)? Logs de nginx 429 y honeypot.
8. ¿`SHOW VARIABLES` de MySQL: `time_zone`, `transaction_isolation`, `innodb_lock_wait_timeout` coinciden con lo asumido en el código del lock?
9. ¿Qué eventos hay en Sentry (frecuencia de 500 por login erróneo)?
10. ¿Los defaults de `CLINIC_*` están sobrescritos en prod, o se está sirviendo el valor por defecto del primer cliente?
11. ¿La instancia Render demo está expuesta con el backend directo (sin nginx/rate limit) y con usuario demo con permisos de escritura?
12. ¿Se usa el campo `foto_url` de historial y existen archivos en `uploads_data` que no sean de servicios?
13. ¿Hay consentimiento/aviso de privacidad vigente para guardar datos de salud de los pacientes del cliente real?

---

## Resumen (10 líneas)

1. Producto funcional en producción (1 cliente real + demo): agenda, reserva pública, pacientes, servicios, pagos y dashboard.
2. Stack moderno y simple: Spring Boot 3.2 + Angular 19 + MySQL, una instancia Docker por cliente, CI/CD a DigitalOcean.
3. El núcleo de agenda (citas, disponibilidad, solapamiento, locks) es sólido y relativamente genérico.
4. No hay recordatorios, email ni WhatsApp de envío; solo un enlace `wa.me`. Los flags por paquete están documentados pero no implementados.
5. Single-tenant duro: sin `negocio_id`, sin profesionales, una sola agenda por instancia.
6. Seguridad base buena (BCrypt/JWT/rate limit/headers) pero sin roles, revocación ni audit; login erróneo devuelve 500.
7. Marca parcialmente parametrizable por env vars; logo, paleta, moneda y textos siguen hardcodeados con datos del primer cliente como defaults.
8. Tests: 18/26 backend pasan localmente (8 fallan por Mockito+JDK 25, CI usa 17); migraciones sin probar; frontend casi sin tests y CI sin `ng test`.
9. La "historia clínica" es un log de procedimientos con precio; no hay ficha, adjuntos ni consentimientos.
10. Reutilizable ya para verticales de 1 recurso (costo ~1–2 semanas/vertical vía instancia); no apto como plataforma multitenant sin rediseño.

## 5 brechas principales por objetivo

**(a) Recordatorios automáticos WhatsApp/email**
1. No existe `NotificationService`/proveedor (Brevo/Twilio/Meta) ni dependencia de mail.
2. No hay job con idempotencia (`recordatorio_enviado_at`) ni cola/reintentos/registro de entregas.
3. Sin opt-in/consentimiento ni baja; WhatsApp requiere plantillas aprobadas y costo por mensaje definido.
4. Zona horaria no modelada (`LocalDateTime`); riesgo de enviar a hora errónea.
5. Sin canal de respuesta (confirmar/cancelar desde el mensaje) ni verificación del contacto (email/teléfono sin verificar).

**(b) Reutilizar en otra vertical**
1. Sin `negocio_id`/multitenancy; aislamiento solo por instancia.
2. Sin entidad Profesional/Recurso; una agenda única (`dia_semana` UNIQUE).
3. Vocabulario y moneda dentales/UY hardcodeados en API, esquema y UI (`/pacientes`, `UY$`, `es-UY`).
4. Marca no data-driven (logo, paleta, `index.html`, tema Material) y defaults de cliente real embebidos.
5. Historial y pagos acoplados al núcleo: falta un sistema real de módulos/flags por negocio.

**(c) Conectar un agente de WhatsApp**
1. Sin autenticación de máquina/API keys por negocio ni scopes (solo JWT admin).
2. Sin identificación verificada del paciente por teléfono ni cancelar/reagendar con token de cita.
3. Sin límites en backend (rate limit por número, citas activas por paciente, horizonte, fechas pasadas) ni idempotencia.
4. Sin webhooks de entrada/salida, firma de mensajes ni auditoría de acciones del agente.
5. Sin contrato OpenAPI ni errores semánticos (login 500) para que el agente interprete resultados.

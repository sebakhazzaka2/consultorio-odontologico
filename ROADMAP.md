# Roadmap — Consultorio Odontológico

> **Última actualización:** 2026-05-04 — Balance + Gastos vuelve al pipeline (Sprint 5, tras MVP2); Sprint 6 = instancia demo + README pro para portfolio y demo comercial.

## Secuencia recomendada
```
Public page polish ✅ → Multi-rubro genérico → Mails (Brevo) → MVP2 (novia como piloto)
→ Balance + Gastos → Demo instance + README pro → SEO multi-subdominio → Observability+Rollback → WhatsApp → primer cliente pagando
```

## Estado por fase

| Fase | Estado |
|------|--------|
| MVP1 — Backend | ✅ Completo (mergeado a main) |
| MVP1 — Frontend | ✅ Completo (mergeado a main) |
| P1 — Hardening pre-producción | ✅ Completo (mergeado a main) |
| P2 — Infraestructura profesional | ✅ Completo (mergeado a main) |
| P3 Fase A — Producto usable | ✅ Completo (mergeado a main) |
| P3 Fase B — código deploy | ✅ Completo (mergeado a main) |
| Página pública clínica | ✅ Completo (mergeado a main) |
| Admin polish | ✅ Completo (mergeado a main) |
| Deploy real | ✅ Live en dentalmontecaseros.turnosuy.com (2026-04-21) |
| Post-deploy hardening | ✅ Completo (mergeado a main, PR #19, 2026-04-22) |
| **Public page polish** | ✅ Completo (mergeado a main, PR #20, 2026-04-28) |
| **CI — frontend lint/build job** | ⏳ Pendiente (agregar a `.github/workflows/ci.yml` antes del Sprint 1) |
| **Sprint 1 — Multi-rubro genérico** | ⏳ Próximo (2-3 días) — renombramientos + config por instancia |
| **Sprint 2 — Mails (Brevo)** | ⏳ Tras multi-rubro (5-8 días) — bloqueante de MVP2 |
| **Sprint 3-4 — MVP2 rol paciente** | ⏳ Tras Mails (3-4 semanas) — novia como usuario piloto |
| **Sprint 5 — Balance + Gastos** | ⏳ Tras MVP2 — novia como piloto activa genera el driver de feedback |
| **Sprint 6 — Demo instance + README pro** | ⏳ Tras Balance+Gastos — producto completo → instancia demo con datos fake + README portfolio |
| **Sprint 7 — SEO multi-subdominio** | ⏳ Tras demo (3-5 días) — con 2 subdominios reales el ROI es mayor |
| **Sprint 8 — Observability + rollback** | ⏳ Tras SEO (1 semana) |
| **Sprint 9 — WhatsApp automatizado** | ⏳ Tras observability (1-2 semanas) |
| 🎯 Primer cliente Web | ⏳ Tras MVP2 + Mails |
| 🎯 Cliente #2 | ⏳ Trigger para Terraform + Prometheus + script provisioning |
| Landing SaaS proveedor | ⏳ Post primer cliente pagando con caso de éxito |
| V5 — Multi-tenant SaaS | ⏳ Post ~5 clientes activos |

---

## Pre-ventas — checklist mínimo antes de vender

Lo que tiene que estar resuelto antes del primer cliente de pago (en orden de prioridad):

**Imprescindible — sin esto no se vende:**
- ⬜ Script de provisioning: dado dominio + datos de clínica, levanta la instancia en < 15 min
- ⬜ Configuración dinámica de clínica desde el admin (nombre, logo, horarios, tratamientos) — rama actual
- ⬜ Backup automático por cliente verificado (cron + test de restore)
- ⬜ HTTPS automático por dominio/subdominio (Caddy ya lo da)

**Necesario para paquete Web (paquete 2 y 3):**
- ⬜ MVP2 — login y pedir turno como paciente (sin esto la página pública es solo vitrina)
- ⬜ Email transaccional básico — confirmación de turno

**Necesario para paquete WhatsApp (paquete 3):**
- ⬜ WhatsApp Business API integrado (Twilio / UltraMsg / Meta directo)
- ⬜ Costo por mensaje trasladado al precio del paquete

---

## Detalle por fase

### P3 Fase B — código ✅ (`feat/production-deploy`, mergeado)
- ✅ CORS, rate limiting, JWT 2h, MySQL aislado, healthchecks, appuser, backup.sh, actuator
- ✅ Flip `push: true` en CI + secrets GHCR — hacer cuando haya server

### Página pública clínica ⏳ (`feat/public-page`)
- ✅`''` → `PublicComponent` — muestra tratamientos activos vía `GET /api/tratamientos`
- ✅Botón "Iniciar sesión" en navbar → `/login`
- ✅Base para MVP2 donde pacientes pedirán turno desde esta misma página

### Admin polish ⏳ (`feat/admin-polish`)
- ✅Login oscuro, sidebar oscuro (`#0F172A`), Inter font
- ✅Dashboard component en `/admin` con cards de turnos/pacientes
- ✅Tablas: íconos de acción, empty states, chips semánticos por estado de cita

### Deploy real ✅ (live 2026-04-21)
- ✅ Hetzner CX-23, Ubuntu 22.04 — `167.235.134.150`
- ✅ `dentalmontecaseros.turnosuy.com` — Caddy + TLS Let's Encrypt
- ✅ Admin seeding desde env vars, nginx proxy fix, CORS configurado

### Post-deploy hardening ✅ (mergeado a main, PR #19, 2026-04-22)
- ✅ Cron backup diario 2am — `scripts/backup.sh` + `scripts/restore.sh`
- ✅ Endpoint + UI cambio de password del admin
- ✅ CI/CD: push GHCR habilitado + deploy SSH automático + `workflow_dispatch`
- ✅ Sentry (Angular + Spring Boot) + Uptime Kuma (status subdomain vía Caddy)
- ✅ Validación fail-fast de env vars al startup (`StartupEnvValidator`)
- ✅ H2 en tests de CI (no requiere MySQL en pipeline)

### Public page polish ✅ (`feat/public-page-polish`, 13 commits, listo para PR)
- ✅ Backend: `ClinicProperties` + endpoint `GET /public/config` (datos de clínica configurables)
- ✅ Backend: `FileStorageService` + endpoint `PATCH /api/tratamientos/{id}/foto` (fotos de tratamientos)
- ✅ Backend: `GooglePlacesService` + endpoint `GET /public/reviews` con cache 24h + filtro 4★
- ✅ Frontend: `ClinicConfigService` — consume `/public/config` en lugar de `clinic.config.ts` hardcodeado
- ✅ Frontend: sección "Ubicación" (texto + foto exterior + mapa embebido) + sección "Reseñas" carousel
- ✅ Frontend: fotos en cards de tratamientos + upload en form dialog del admin (create y edit)
- ✅ Frontend: stats strip, WhatsApp FAB, rebrand TurnosUy, estado open/closed dinámico, features section configurable
- ✅ Docker: var `GOOGLE_PLACES_API_KEY` + volumen `uploads_data` en `docker-compose.prod.yml`
- ⚠️ Verificar tras deploy: encoding de acentos en `/api/public/config` (defaults Java rotos en local pero env vars `.env.prod` UTF-8 deberían sobreescribirlos)

---

## Sprints planificados (post merge `feat/public-page-polish`)

### Sprint 1 — Multi-rubro genérico (2-3 días)
**Por qué primero:** la novia (estética, cejas/pestañas) es el nuevo usuario piloto. Antes de mails y MVP2 necesitamos que el sistema hable "estética" y no "odontología". El código es ~60% genérico; el 40% restante es mecánico de renombrar.

**Relevamiento previo (2026-04-29):** el código es 60-70% genérico. Lo que requiere cambio:
- **Config/env (30 min):** `CLINIC_NOMBRE`, `CLINIC_TAGLINE`, features, lista de servicios pública en `servicios.component.ts`
- **Renombramientos mecánicos (3-4 horas):** `Tratamiento` → `Servicio` (entidad + DTO + controller + service + frontend models), endpoint `/api/tratamientos` → `/api/servicios`, `HistorialClinico` → `HistorialProcedimientos` o similar
- **Migración Flyway:** renombrar tabla `tratamientos` → `servicios` (V5)
- **UI labels:** "Tratamientos" → "Servicios", "Historial Clínico" → "Procedimientos realizados" en templates Angular
- **No requiere cambios de lógica:** citas, pagos, agenda, auth son completamente genéricos

**Resultado:** mismo codebase sirve para dental y estética solo cambiando variables de entorno + subdominio.

### Sprint 2 — Infraestructura de mail (5-8 días)
**Por qué segundo:** bloqueante de MVP2 (password reset sin mail = no hay registro de paciente). Stack: Brevo (free tier 300 mails/día) + Spring Boot Mail + Thymeleaf.
- `spring-boot-starter-mail` + Brevo SMTP en `application-prod.properties`
- Templates Thymeleaf base con header/footer compartido
- Mail confirmación turno agendado / cancelado / reagendado
- Mail entrada en historia clínica con detalle tratamiento + precio + deuda
- Password reset endpoint (token + expiración + UI)
- Cron backup: alerta de fallo (mail al admin si backup no se ejecutó)

### Sprint 3-4 — MVP2 patient portal (3-4 semanas)
**Driver:** la novia (usuaria piloto) necesita poder pedir turno por su cuenta. Sin MVP2, el sistema es solo admin-only y ella no puede interactuar.
- Registro/login paciente + JWT con rol PACIENTE + RBAC efectivo (`@PreAuthorize` + `roleGuard`)
- Pedir turno desde página pública → cita PENDIENTE → admin confirma
- Refresh tokens con rotación + revocación
- Página "Mi cuenta" (citas, historial resumido)
- Consentimiento explícito de datos (Ley 18.331)
- Política de privacidad + términos
- JWT en cookie HttpOnly + SameSite=Strict (mover desde localStorage)
- Notificación por mail (reusa Sprint 3)

### Sprint 5 — Balance + Gastos (1-2 semanas)
**Por qué después de MVP2:** con la novia usando el sistema activamente, el módulo financiero tiene un driver real de feedback. Registrar ingresos/egresos del negocio, balance mensual, gráficos.
- CRUD de gastos con categorías
- Balance mensual: Σ cobros − Σ gastos del período
- Vista resumen en dashboard admin
- Export básico (CSV o PDF)

### Sprint 6 — Demo instance + README pro (2-4 días)
**Por qué después de Balance+Gastos:** el producto está completo — tiene todo lo que hay que mostrar. Hacerlo antes implica rehacer los datos demo con cada sprint.
- Docker Compose separado (`docker-compose.demo.yml`) con datos fake realistas
- Subdominio `demo.turnosuy.com` con user/pass públicos en el README
- README pro: badges, screenshot/GIF del admin, arquitectura de deploy, decisiones de diseño, link live + demo
- Sirve doble: portfolio personal + demo comercial para clientes potenciales

### Sprint 7 — SEO multi-subdominio (3-5 días)
**Por qué después de demo:** con 2 subdominios reales (dental + estética) el ROI es inmediato y el JSON-LD necesita ser genérico (no solo `Dentist` schema). El código se hace una vez y aplica a todos los clientes futuros.
- Prerender estático con `@angular/ssr` (build-time, sin runtime overhead)
- Meta tags dinámicos desde `ClinicConfig` usando `Title` + `Meta` services
- JSON-LD genérico (`LocalBusiness` + tipo específico por instancia) desde `ClinicProperties`
- Endpoint backend `/sitemap.xml` por subdominio
- `robots.txt` estático con `Sitemap:`
- Headers HTTP de seguridad en `Caddyfile` (CSP, HSTS, X-Frame-Options, etc.)

### Sprint 8 — Observability + rollback (1 semana)
- Logs JSON + correlationId/MDC
- Micrometer + actuator/prometheus expuesto
- Smoke test post-deploy: `curl /api/actuator/health` con retry
- `scripts/rollback.sh` que vuelve al SHA anterior
- Tag de imagen por SHA en `docker-compose.prod.yml` (no `:latest`)
- Backup automático pre-deploy en CI/CD
- Notificación de deploy a Telegram/Discord
- Reglas Sentry + alertas Uptime Kuma a canal real

### Sprint 9 — WhatsApp automatizado (1-2 semanas)
- Twilio Business API o UltraMsg
- Recordatorio 24h antes de turno
- Notificación al cliente de cambios

### Sprint 10+ — Pre-cliente #2
- Script de provisioning (15 min para nueva clínica) — incluye Search Console + GBP en checklist
- Terraform módulo Hetzner Cloud + Cloudflare
- Prometheus + Grafana (free tier o self-hosted)

---

### MVP2 scope (referencia)
- Registro y login de pacientes
- Paciente elige slot → cita queda PENDIENTE
- Admin confirma (asigna duración), cancela o reagenda
- HTTPS, rate limiting, passwords fuertes
- `pacientes.user_id` FK ya está en la DB

### Backlog medio plazo (intercalado con sprints)
- Soft delete + auditoría acceso historial clínico (Ley 18.331)
- Paginación con `Pageable` en endpoints de listado
- Export PDF historial clínico + recetas
- Dark mode toggle
- Historial UX mejorado (cards, colores por tratamiento)
- Cobertura tests backend ~50% / frontend ~40%
- Subir `FileStorageService` a `BackblazeStorageServiceImpl` (S3-compatible) cuando los uploads escalen

### Backlog largo plazo (post 5 clientes activos)
- Multi-tenancy real (DB por tenant en una sola instancia, schema por cliente)
- i18n (es/pt/en)
- App móvil PWA
- Búsqueda global cross-módulo

### Landing SaaS proveedor ⏳ (repo `consultorio-landing`, post primer cliente con caso de éxito)
- No urgente — la página de ventas del software para atraer nuevas clínicas
- Stack: Astro + Tailwind v4, deploy en Cloudflare Pages (gratis)
- Formulario demo vía EmailJS / Resend
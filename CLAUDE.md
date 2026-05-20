# Consultorio Odontológico

## Stack
- **Backend:** Spring Boot 3.2.5, Java 17 — ver `backend/CLAUDE.md`
- **Frontend:** Angular 19 + Angular Material — ver `frontend/CLAUDE.md`
- **Base de datos:** MySQL (`consultorio_db`, `localhost:3306`)
- **Backend:** puerto 8080, context path `/api`

## Estructura del repositorio
```
consultorio-odontologico/
├── backend/          # Spring Boot — convenciones en backend/CLAUDE.md
├── frontend/         # Angular 19 — convenciones en frontend/CLAUDE.md
├── scripts/          # backup.sh, utilidades de deploy
├── CLAUDE.md         # este archivo — overview del proyecto
├── ROADMAP.md        # fases, estado y secuencia
└── WORKFLOWS.md      # orquestación, task management, principios
```

## Navegación rápida
| Qué necesitás | Dónde ir |
|--------------|----------|
| Convenciones Spring Boot, DB, diseño | `backend/CLAUDE.md` |
| Convenciones Angular, componentes, services | `frontend/CLAUDE.md` |
| Roadmap, fases, próximos pasos | `ROADMAP.md` |
| Cómo trabajar, subagents, task management | `WORKFLOWS.md` |
| Lecciones aprendidas, errores comunes | `tasks/lessons.md` |

---

## Git

### Convenciones
- Conventional commits: `feat:`, `fix:`, `refactor:`, `chore:`
- Un commit por módulo funcional completo y verificado
- PRs siempre con **Squash and Merge**

### Ramas activas
- `main` — rama estable

### Ramas mergeadas (referencia)
- `v2-angular-frontend`, `p1-hardening-backend`, `feat/agenda-calendar`, `feat/ux-essentials`, `feat/production-deploy`
- `feat/multi-rubro` (PR #21, 2026-05-11), `fix/safari-white-screen` (2026-05-11), `fix/admin-branding` (2026-05-11)

## Modelo de negocio

| Paquete | Qué incluye | Cobro |
|---------|-------------|-------|
| **Local** | Sistema local, sin página web, usuario único | Pago único |
| **Web** | Sistema + página pública + login paciente | Instalación + cuota mensual |
| **Web + WhatsApp** | Todo lo anterior + recordatorios automáticos WhatsApp | Instalación + cuota mensual mayor |

**Arquitectura de deploy:** una instancia Docker por cliente (no multitenancy todavía).
Trigger para migrar a multitenancy real: ~5 clientes activos pagando.

**Onboarding por rubro/cliente:** checklist único en `docs/ONBOARDING_CLIENTE.md` (entregable de S3.5 junto al script de provisioning). Consolida datos de clínica, env vars de branding (`BRAND_*`), feature flags (`FEATURE_*`), servicios seed, DNS, backup y smoke test. Hasta que exista, la info vive fragmentada entre este archivo, `ClinicProperties` y el detalle de Sprint 2.0.

### Feature flags por paquete

Un solo codebase, una sola imagen Docker. Cada paquete es una combinación de env vars. El backend expone los flags en `GET /api/public/config` vía `ClinicProperties`; el frontend los consume al arrancar y oculta rutas/botones según corresponda.

```
# Paquete Local
FEATURE_PUBLIC_PAGE=false
FEATURE_PATIENT_PORTAL=false
FEATURE_WHATSAPP=false

# Paquete Web
FEATURE_PUBLIC_PAGE=true
FEATURE_PATIENT_PORTAL=true
FEATURE_WHATSAPP=false

# Paquete Web + WhatsApp
FEATURE_PUBLIC_PAGE=true
FEATURE_PATIENT_PORTAL=true
FEATURE_WHATSAPP=true
```

**Regla:** al implementar cualquier feature nueva, agregar su flag en `ClinicProperties` desde el día uno. No hay ramas ni repos separados por paquete.

## Repos relacionados
- `consultorio-odontologico` — este repo (backend + frontend Angular + infra)
- `consultorio-landing` — landing de ventas del proveedor SaaS (futuro, post primer cliente)

---

## Estado actual (2026-05-19)

Completado: MVP1, P1 hardening, P2 infra, P3 Fase A/B, página pública, admin polish, deploy real (live 2026-04-21), post-deploy hardening (PR #19), public page polish (PR #20), Sprint 1 multi-rubro (PR #21, live 2026-05-11).

**Instancias en producción:**
- `neodentalmaster.turnosuy.com` — segunda instancia live (odontología, Montevideo)
- `dentalmontecaseros.turnosuy.com` — primera instancia (sin uso activo)

Siguiente: S2.0 (Theming foundations, ~2 días) → S2 (Disponibilidad + Reserva sin login) → S2.5 (Hardening urgente) → S3 (Diagnósticos + Presupuestos + Hardening seguridad) → S3.5 (Pre-cliente #3) → S4 (Mails) → S5 (WhatsApp) → S6 (Portal paciente).

Ver detalles completos y secuencia por sprint en `ROADMAP.md`. Decisiones estratégicas pendientes (P3 auditoría) en sección dedicada de ROADMAP.md.

## Operaciones mínimas (target post Sprint 2.5)

Checklist operacional antes de seguir agregando features sobre instancias live:

- ⬜ Restore de backup probado en VM limpia (P0-1)
- ⬜ Uptime monitor externo con alertas (P0-2)
- ⬜ Sentry capturando errores backend + frontend (P0-3)
- ⬜ Headers de seguridad HTTPS estrictos en Caddy (P0-4)
- ⬜ Audit log de acciones admin (P1-6, modelado en S3)
- ⬜ Logs JSON estructurados + correlationId (P1-8, en S3)
- ⬜ Backup off-site verificado (P3-10)

## Prioridades actuales (2026-05-19)

🔴 **Sprint 2.0 — Theming foundations** (~2 días, antes de S2). Migración Angular Material prebuilt → `@use mat`, capa `--brand-*` en design system, `applyBranding` en `ClinicConfigService`, env vars `BRAND_*` en backend. Desbloquea "mismo build Docker, color por cliente" y evita acoplar componentes nuevos de S2+ al navy hardcoded. Polish visual puro (skeletons, density, hero, agenda) NO entra acá — se aplica incremental en el PR del sprint que toca cada zona. Detalle en `frontend/CLAUDE.md` § Theming + § Polish backlog.
🔴 **Sprint 2 — Disponibilidad admin + Reserva sin login** (~2 semanas). Admin configura horarios disponibles y bloquea fechas. Cliente reserva desde página pública sin login; admin confirma. **Hace el producto vendible.**
🔴 **Sprint 2.5 — Hardening urgente** (~1.5 días, P0 auditoría 2026-05-19). Restore backup probado, uptime monitor, Sentry, headers seguridad Caddy, audit SQL injection, verificar S2 contra spam. Riesgos existenciales antes de seguir.
🔴 **Sprint 3 — Diagnósticos + Presupuestos + Archivos + Hardening seguridad** (~2 semanas). Producto: Presupuesto entidad + UI + archivos clínicos. Seguridad (P1 auditoría): JWT a cookie HttpOnly, rate limit login, paginación, CSP, audit log, password reset, logs JSON.
🔴 **Sprint 3.5 — Pre-cliente #3** (~2 semanas, P2 auditoría). E2E Playwright, export datos, soft delete, multi-usuario admin, script provisioning, SLA documentado, página status. Bloqueantes para firmar cliente #3.
🔴 **Sprint 4 — Mails transaccionales** (5-8 días). Brevo SMTP + Thymeleaf. Confirmación, cancelación, recordatorio 24h, notificación pago + saldo, envío presupuesto, alerta backup. (Password reset adelantado a S3.)
🔴 **Sprint 5 — WhatsApp automático** (1-2 sem). Twilio/UltraMsg. Antes: modelar costo y plantear como "X mensajes/mes incluidos" (P3-4).
🔴 **Sprint 6 — Portal paciente opcional** (2-3 sem). Login no obligatorio. JWT con `roles` claim unificando admin + paciente (P3-5). Refresh tokens con rotación.
🟡 Sprint 7 — Balance + Gastos (1-2 sem).
🟡 Sprint 8 — Demo instance + README pro. Antes: migración feature flags a tabla DB (P3-2). Theming ya implementado en S2.0 — acá solo se setean env vars de la demo.
🟡 Sprint 9 — SEO multi-subdominio. JSON-LD genérico, SSR, sitemap.
🟡 Sprint 10 — Observability + rollback (logs JSON ya en S3; aquí Micrometer, rollback.sh, alertas).
🟡 Sprint 11+ — Pre-cliente #2 ampliado: Terraform Hetzner+Cloudflare, Prometheus+Grafana, Docker secrets (P3-9).

**Postergados explícitamente:**
- **Fase Premium UX** (diseñador externo + delight: command palette, shared transitions, ilustraciones propias, etc.) — post 1+ cliente WhatsApp pagando 2+ meses sin churn. Antes de invertir en landing SaaS porque la landing tiene que mostrar este nivel. Detalle en `ROADMAP.md` § Fase Premium UX.
- Landing comercial `consultorio-landing` — post Fase Premium UX, con caso de éxito real.
- Multi-tenancy real — post 5 clientes activos.

**Aprendizaje devops (proyecto + paralelo):**
- En este proyecto: Prometheus/Grafana (Sprint 9+), Terraform (cliente #2).
- Paralelo: Kubernetes (k3s + helm + CKA) — no aplicar a este proyecto.

Todo el resto (prosa de snapshot, rutas de archivos clave, preguntas abiertas, tabla 4 futuro, secuencia narrativa) no se copia a CLAUDE.md. Vive o en el commit history o se regenera si hace falta.
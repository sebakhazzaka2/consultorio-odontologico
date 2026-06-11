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

**Onboarding por rubro/cliente:** checklist único en `docs/ONBOARDING_CLIENTE.md` (entregable de S9 junto al script de provisioning). Consolida datos de clínica, env vars de branding (`BRAND_*`), feature flags (`FEATURE_*`), servicios seed, DNS, backup y smoke test. Hasta que exista, la info vive fragmentada entre este archivo, `ClinicProperties` y el detalle de Sprint 4.

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

---

## Estado actual (2026-06-10)

Completado: MVP1, P1 hardening, P2 infra, P3 Fase A/B, página pública, admin polish, deploy real (live 2026-04-21), post-deploy hardening (PR #19), public page polish (PR #20), Sprint 1 multi-rubro (PR #21, live 2026-05-11), Sprint 2 Disponibilidad + Reserva sin login (2026-05-20), fix stale chunk reload (36930f1, 2026-05-20).

**Instancias en producción:**
- `neodentalmaster.turnosuy.com` — única instancia live (odontología, Montevideo). `dentalmontecaseros` dada de baja.

Siguiente (repriorizado 2026-06-10): S3 → S6 → S-Audit → S-Tests → Agenda polish → S5 → S7 → S8 → S9 (scope reducido) → S10. **S4 Theming postergado on-demand.**

Ver detalles completos y secuencia por sprint en `ROADMAP.md`. Decisiones estratégicas pendientes (P3 auditoría) en sección dedicada de ROADMAP.md.

## Operaciones mínimas (target post Sprint 3)

Checklist operacional antes de seguir agregando features sobre instancias live:

- ⬜ Restore de backup probado en VM limpia (P0-1)
- ⬜ Uptime monitor externo con alertas (P0-2)
- ⬜ Sentry capturando errores backend + frontend (P0-3)
- ⬜ Headers de seguridad HTTPS estrictos en Caddy (P0-4)
- ⬜ Audit log de acciones admin (P1-6, modelado en S5)
- ⬜ Logs JSON estructurados + correlationId (P1-8, en S9)
- ⬜ Backup off-site verificado (P3-10)

## Prioridades actuales (2026-06-10)

Secuencia: fundamentos (seguridad+calidad+tests) → features → WhatsApp. Ver `ROADMAP.md` para detalle completo.

🔴 **S3 — Hardening urgente** (~1.5 días, P0). Restore backup, uptime monitor, Sentry, headers Caddy. **← PRÓXIMO**
🔴 **S6 — Mails transaccionales** (~1 sem). Brevo SMTP + Thymeleaf + password reset admin + notificaciones reserva.
🔴 **S-Audit** (~3-5 días). Seguridad infra (puertos, UFW, fail2ban) + seguridad app (exposición de password en GET, paginación, SQL injection, JWT, rate limit) + calidad HTML/CSS + RAM + load test BD. Detalle en `ROADMAP.md` § S-Audit.
🔴 **S-Tests** (~1 sem). JUnit Services + E2E Playwright reserva pública + ESLint/Prettier/Spotless/Checkstyle.
🔴 **Agenda polish** (~1 día). Línea "ahora", colores por estado, tipografía bloque.
🔴 **S5 — Presupuestos + Archivos** (~1 sem).
🔴 **S7 — Portal paciente** (~2-3 sem). Login opcional, JWT unificado con `roles` claim.
🔴 **S8 — Balance + Gastos** (~1-2 sem).
🔴 **S9 — Hardening consolidado** (~1-2 sem, scope reducido por S-Audit/S-Tests).
🔴 **S10 — WhatsApp** (~1-2 sem). Post-hardening.
🟡 S11 Demo instance → S12 SEO → S13 Observability → cliente #3+
⏸️ **S4 — Theming foundations** — postergado on-demand. Solo si un cliente lo pide activamente.

**Postergados explícitamente:**
- **Fase Premium UX** (diseñador externo + delight: command palette, shared transitions, ilustraciones propias, etc.) — post 1+ cliente WhatsApp pagando 2+ meses sin churn. Antes de invertir en landing SaaS porque la landing tiene que mostrar este nivel. Detalle en `ROADMAP.md` § Fase Premium UX.
- Landing comercial `consultorio-landing` — post Fase Premium UX, con caso de éxito real.
- Multi-tenancy real — post 5 clientes activos.

**Aprendizaje devops (proyecto + paralelo):**
- En este proyecto: Prometheus/Grafana (Sprint 13+), Terraform (cliente #2).
- Paralelo: Kubernetes (k3s + helm + CKA) — no aplicar a este proyecto.

Todo el resto (prosa de snapshot, rutas de archivos clave, preguntas abiertas, tabla 4 futuro, secuencia narrativa) no se copia a CLAUDE.md. Vive o en el commit history o se regenera si hace falta.
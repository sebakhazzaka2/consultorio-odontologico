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
- `main` — rama estable, todo mergeado hasta P3 Fase B
- `feat/admin-polish` — admin UX: login oscuro, sidebar oscuro, dashboard, tablas ← **actual**
- `feat/public-page` — página pública de la clínica (próxima)

### Ramas mergeadas (referencia)
- `v2-angular-frontend`, `p1-hardening-backend`, `feat/agenda-calendar`, `feat/ux-essentials`, `feat/production-deploy`

## Repos relacionados
- `consultorio-odontologico` — este repo (backend + frontend Angular + infra)
- `consultorio-landing` — landing de ventas del proveedor SaaS (futuro, post primer cliente)

---

## Estado actual (2026-04-16)

**Rama:** `feat/admin-polish`

## Estado actual (2026-04-18)

Rama: feat/Admin-Polish-v2.
Completado: MVP1, P1 hardening, P2 infra, P3 Fase A UX, P3 Fase B código deploy, página pública, admin polish base.
En curso: Claude Design sobre el front (atractivo visual), arranque MVP2.

Ver detalles completos en `ROADMAP.md`.
## Prioridades actuales (pivote 2026-04-18)
Orden explícito, con criticidad:

🔴 Claude Design polish del front — más atractivo, activo ya, en paralelo con MVP2.
🔴 MVP2 — rol paciente — urgente, antes del deploy. Scope: registro/login paciente, JWT con rol PACIENTE, RBAC @PreAuthorize + roleGuard, paciente pide slot → cita PENDIENTE, refresh tokens, passwords fuertes, "Mi cuenta".
🔴 WhatsApp respuestas automáticas — inmediatamente después de MVP2. A investigar: provider (Twilio / WhatsApp Cloud API / Meta Business), casos (confirmación de cita, recordatorio, respuestas a FAQ).
🟡 Deploy real — pospuesto (hoy no, tal vez mañana). Bloqueantes técnicos siguen vigentes (ver abajo).
🟡 Legal / compliance Ley 18.331 — privacidad, términos, consentimiento, auditoría de acceso al historial. Antes del primer cliente real, no antes del deploy técnico.

# Bloqueantes pre-deploy (checklist vital)
Extracto condensado de la Tabla 1 del audit, sólo headlines accionables:

Reverse proxy HTTPS (Caddy/Traefik + Cloudflare Origin CA).
CI: flip push: true + secrets GHCR + workflow de deploy.
DEPLOY.md runbook (Hetzner + Cloudflare + dominio).
Validación fail-fast de envs (JWT_SECRET, DB_PASSWORD, CORS_ALLOWED_ORIGINS).
Endpoint + UI de cambio de password del admin.
Cron de backup diario + test de restore.
Páginas 404/500 Angular + error_page nginx.
Índice compuesto citas(paciente_id, fecha_hora_inicio).
Smoke test en staging antes de apuntar DNS.

## Post-deploy semana 1-2 (headlines)
Extracto de la Tabla 2, sólo lo que importa retener:

Sentry (Angular + Spring Boot).
Uptime monitoring (Uptime Kuma / UptimeRobot).
Logs estructurados JSON + correlationId.
Email transaccional (Brevo/Resend) — puerta de entrada para password reset y WhatsApp.
Paginación Pageable en listados.
Soft delete + auditoría de acceso a historial.

Todo el resto (prosa de snapshot, rutas de archivos clave, preguntas abiertas, tabla 4 futuro, secuencia narrativa) no se copia a CLAUDE.md. Vive o en el commit history o se regenera si hace falta.
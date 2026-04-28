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
- `main` — rama estable, todo mergeado incluyendo post-deploy hardening (PR #19)
- `feat/public-page-polish` — polish página pública: fotos, config dinámica, reseñas ← **actual, lista para PR a main**

### Ramas mergeadas (referencia)
- `v2-angular-frontend`, `p1-hardening-backend`, `feat/agenda-calendar`, `feat/ux-essentials`, `feat/production-deploy`

## Modelo de negocio

| Paquete | Qué incluye | Cobro |
|---------|-------------|-------|
| **Local** | Sistema local, sin página web, usuario único | Pago único |
| **Web** | Sistema + página pública + login paciente | Instalación + cuota mensual |
| **Web + WhatsApp** | Todo lo anterior + recordatorios automáticos WhatsApp | Instalación + cuota mensual mayor |

**Arquitectura de deploy:** una instancia Docker por cliente (no multitenancy todavía).
Trigger para migrar a multitenancy real: ~5 clientes activos pagando.

## Repos relacionados
- `consultorio-odontologico` — este repo (backend + frontend Angular + infra)
- `consultorio-landing` — landing de ventas del proveedor SaaS (futuro, post primer cliente)

---

## Estado actual (2026-04-27)

**Rama:** `feat/public-page-polish` — 13 commits listos, PR a main inminente.

Completado: MVP1, P1 hardening, P2 infra, P3 Fase A/B, página pública, admin polish, deploy real (live 2026-04-21), post-deploy hardening (PR #19), public page polish (incluye commit 7 Docker).

Siguiente: Balance + Gastos (Sprint 1) → SEO multi-subdominio (Sprint 2) → Mails (Sprint 3) → MVP2 (Sprint 4-5).

Ver detalles completos y secuencia por sprint en `ROADMAP.md`.

## Prioridades actuales (2026-04-27)

🔴 **Sprint 0** — PR `feat/public-page-polish` → main. Verificar acentos en prod tras deploy (hipótesis: defaults Java rotos en local pero env vars UTF-8 los sobreescriben en prod).
🔴 **Sprint 1 — Balance + Gastos** (3-5 días). Quick win: la base ya existe (Pagos, HistorialClinico, getSaldoPaciente, dashboard ingresos). Falta entidad Gasto + endpoint balance + pestaña admin.
🔴 **Sprint 2 — SEO multi-subdominio** (3-5 días). Prerender + meta tags dinámicos + JSON-LD `Dentist` + sitemap.xml por subdominio + headers HTTP de seguridad. **Escala automáticamente con cada cliente nuevo del paquete Web.**
🔴 **Sprint 3 — Mails (Brevo)** (5-8 días). Bloqueante de MVP2 (password reset). Eventos: turno agendado/cancelado/reagendado, entrada en historia clínica con detalle pago/deuda.
🔴 **Sprint 4-5 — MVP2 rol paciente** (3-4 sem). Registro/login, JWT PACIENTE, pedir turno → PENDIENTE, refresh tokens, "Mi cuenta", JWT en cookie HttpOnly, consentimiento Ley 18.331, política de privacidad + términos.
🟡 Sprint 6 — Observability + rollback (logs JSON + correlationId, Micrometer, smoke test post-deploy, rollback.sh, alertas).
🟡 Sprint 7 — WhatsApp automatizado (Twilio/UltraMsg).
🟡 Sprint 8+ — Pre-cliente #2: script provisioning, Terraform Hetzner+Cloudflare, Prometheus+Grafana.

**Postergados explícitamente:**
- Multi-rubro (psicólogos/nutricionistas/etc.) — hasta tener 2-3 clientes odontológicos.
- Landing comercial `consultorio-landing` — post primer cliente pagando con caso de éxito.
- Multi-tenancy real — post 5 clientes activos.

**Aprendizaje devops (proyecto + paralelo):**
- En este proyecto: Prometheus/Grafana (Sprint 6+), Terraform (cliente #2).
- Paralelo: Kubernetes (k3s + helm + CKA) — no aplicar a este proyecto.

Todo el resto (prosa de snapshot, rutas de archivos clave, preguntas abiertas, tabla 4 futuro, secuencia narrativa) no se copia a CLAUDE.md. Vive o en el commit history o se regenera si hace falta.
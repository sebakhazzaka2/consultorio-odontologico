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

## Estado actual (2026-05-15)

Completado: MVP1, P1 hardening, P2 infra, P3 Fase A/B, página pública, admin polish, deploy real (live 2026-04-21), post-deploy hardening (PR #19), public page polish (PR #20), Sprint 1 multi-rubro (PR #21, live 2026-05-11).

**Instancias en producción:**
- `neodentalmaster.turnosuy.com` — segunda instancia live (odontología, Montevideo)
- `dentalmontecaseros.turnosuy.com` — primera instancia (sin uso activo)

Siguiente: Disponibilidad + Reserva sin login (S2) → Mails (S3) → WhatsApp (S4) → Portal paciente opcional (S5).

Ver detalles completos y secuencia por sprint en `ROADMAP.md`.

## Prioridades actuales (2026-05-15)

🔴 **Sprint 2 — Disponibilidad admin + Reserva sin login** (~2 semanas). Admin configura horarios disponibles y bloquea fechas. Cliente reserva desde página pública sin login; admin confirma. Incluye fix deuda técnica `clinic.json`. **Hace el producto vendible.**
🔴 **Sprint 3 — Mails transaccionales** (5-8 días). Brevo SMTP + Thymeleaf. Confirmación, cancelación, recordatorio 24h, password reset (para S5), alerta backup.
🔴 **Sprint 4 — WhatsApp automático** (1-2 sem). Twilio/UltraMsg. Recordatorio 24h, confirmación, cancelación. Diferencial del paquete premium. Sube desde Sprint 9.
🔴 **Sprint 5 — Portal paciente opcional** (2-3 sem). Login no obligatorio. Ver citas, cancelar (72h), reagendar, historial. JWT PACIENTE, cookie HttpOnly, Ley 18.331.
🟡 Sprint 6 — Balance + Gastos (1-2 sem).
🟡 Sprint 7 — Demo instance + README pro. `demo.turnosuy.com`, README portfolio.
🟡 Sprint 8 — SEO multi-subdominio. JSON-LD genérico, SSR, sitemap.
🟡 Sprint 9 — Observability + rollback (logs JSON, Micrometer, rollback.sh, alertas).
🟡 Sprint 10+ — Pre-cliente #2: script provisioning, Terraform Hetzner+Cloudflare, Prometheus+Grafana.

**Postergados explícitamente:**
- Landing comercial `consultorio-landing` — post primer cliente pagando con caso de éxito.
- Multi-tenancy real — post 5 clientes activos.

**Aprendizaje devops (proyecto + paralelo):**
- En este proyecto: Prometheus/Grafana (Sprint 9+), Terraform (cliente #2).
- Paralelo: Kubernetes (k3s + helm + CKA) — no aplicar a este proyecto.

Todo el resto (prosa de snapshot, rutas de archivos clave, preguntas abiertas, tabla 4 futuro, secuencia narrativa) no se copia a CLAUDE.md. Vive o en el commit history o se regenera si hace falta.
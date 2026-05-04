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
- `main` — rama estable, PR #20 (public-page-polish) mergeado 2026-04-28

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

## Estado actual (2026-05-04)

Completado: MVP1, P1 hardening, P2 infra, P3 Fase A/B, página pública, admin polish, deploy real (live 2026-04-21), post-deploy hardening (PR #19), public page polish (PR #20, mergeado 2026-04-28).

**Driver de cambio:** la hermana (Samara) no está usando el sistema activamente → sin feedback. La novia (estética, cejas/pestañas) pasa a ser la usuaria piloto en su propio subdominio.

Siguiente: Multi-rubro (S1) → Mails (S2) → MVP2 (S3-4) → Balance+Gastos (S5) → Demo instance + README pro (S6) → SEO (S7).

Ver detalles completos y secuencia por sprint en `ROADMAP.md`.

## Prioridades actuales (2026-05-04)

🔴 **Sprint 1 — Multi-rubro genérico** (2-3 días). Renombrar `Tratamiento`→`Servicio`, `HistorialClinico`→`HistorialProcedimientos`, endpoint `/tratamientos`→`/servicios`, migración Flyway, labels Angular. El código es 60% genérico; el resto es mecánico. Ver detalle en ROADMAP.md Sprint 1.
🔴 **Sprint 2 — Mails (Brevo)** (5-8 días). Bloqueante de MVP2 (password reset). Eventos: turno agendado/cancelado/reagendado, historia + detalle pago/deuda.
🔴 **Sprint 3-4 — MVP2 rol paciente** (3-4 sem). Registro/login, JWT PACIENTE, pedir turno → PENDIENTE, refresh tokens, "Mi cuenta", JWT en cookie HttpOnly, consentimiento Ley 18.331, política de privacidad + términos. **La novia es el usuario piloto.**
🔴 **Sprint 5 — Balance + Gastos** (1-2 sem). Vuelve al pipeline con la novia como usuaria activa generando feedback real.
🔴 **Sprint 6 — Demo instance + README pro** (2-4 días). `docker-compose.demo.yml` con datos fake, `demo.turnosuy.com`, README nivel portfolio. Doble ROI: portfolio personal + demo comercial para clientes.
🟡 Sprint 7 — SEO multi-subdominio. Con 2 subdominios reales el ROI es inmediato; JSON-LD genérico (`LocalBusiness`) no solo `Dentist`.
🟡 Sprint 8 — Observability + rollback (logs JSON + correlationId, Micrometer, smoke test post-deploy, rollback.sh, alertas).
🟡 Sprint 9 — WhatsApp automatizado (Twilio/UltraMsg).
🟡 Sprint 10+ — Pre-cliente #2: script provisioning, Terraform Hetzner+Cloudflare, Prometheus+Grafana.

**Postergados explícitamente:**
- Landing comercial `consultorio-landing` — post primer cliente pagando con caso de éxito.
- Multi-tenancy real — post 5 clientes activos.

**Aprendizaje devops (proyecto + paralelo):**
- En este proyecto: Prometheus/Grafana (Sprint 6+), Terraform (cliente #2).
- Paralelo: Kubernetes (k3s + helm + CKA) — no aplicar a este proyecto.

Todo el resto (prosa de snapshot, rutas de archivos clave, preguntas abiertas, tabla 4 futuro, secuencia narrativa) no se copia a CLAUDE.md. Vive o en el commit history o se regenera si hace falta.
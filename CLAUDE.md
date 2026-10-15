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
- `feat/s3-hardening-urgente` (PR #26), fixes de reserva concurrente y PII (PR #32–#35, 2026-10-15)

## Modelo de negocio

| Paquete | Qué incluye | Cobro |
|---------|-------------|-------|
| **Local** | Sistema local, sin página web, usuario único | Pago único |
| **Web** | Sistema + página pública + login paciente | Instalación + cuota mensual |
| **Web + WhatsApp** | Todo lo anterior + recordatorios automáticos WhatsApp | Instalación + cuota mensual mayor |

**Arquitectura de deploy:** una instancia Docker por cliente (no multitenancy todavía).
Trigger para migrar a multitenancy real: ~5 clientes activos pagando.

**Onboarding por rubro/cliente:** checklist único en `docs/ONBOARDING_CLIENTE.md` (entregable de F3 junto a los presets por rubro). Consolida datos de clínica, colores y logo, feature flags (`FEATURE_*`), servicios seed, DNS, backup y smoke test. Hasta que exista, la info vive fragmentada entre este archivo, `ClinicProperties` y `docs/DEPLOY.md`.

### Feature flags por paquete

> **Estado (2026-10-15): los `FEATURE_*` todavía no existen en el código.** `ClinicProperties.Feature` son tarjetas de marketing de la página pública, no flags. Se implementan en F3 (ver `ROADMAP.md`). Lo de abajo es el diseño objetivo.

Un solo codebase, una sola imagen Docker. Cada paquete es una combinación de env vars. El backend expondrá los flags en `GET /api/public/config` vía `ClinicProperties`; el frontend los consumirá al arrancar y ocultará rutas/botones según corresponda.

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

## Estado actual (2026-10-15)

Completado: MVP1, P1 hardening, P2 infra, P3 Fase A/B, página pública, admin polish, deploy real (live 2026-04-21), post-deploy hardening (PR #19), public page polish (PR #20), Sprint 1 multi-rubro (PR #21), Sprint 2 Disponibilidad + Reserva sin login (2026-05-20), fix uploads caching (PR #25), S3 hardening (PR #26: register cerrado, headers Caddy, `X-Real-IP` no falsificable, rate limit nginx, honeypot), migración Hetzner → DigitalOcean (PR #28–#29), demo en Render (2026-08-19), reserva concurrente con `FOR UPDATE` como primera sentencia + PII fuera de los logs de reserva (PR #32–#35).

**Instancias en producción:**
- `neodentalmaster.turnosuy.com` — única instancia live (odontología, Montevideo), en DigitalOcean (verificado 2026-10-15: sitio responde, Uptime Kuma activo). `dentalmontecaseros` dada de baja.
- Demo pública en Render (datos fake).
- **Próxima:** Guille Blanchet Beauty (belleza, Uruguay) en un VPS nuevo con subdominio de turnosuy.com. Se levanta en F1 pero ella sigue en Ágora hasta tener recordatorios por email (F2a).

**Pendientes operativos:** alertas de Kuma confirmadas. Restore en VM limpia y backup off-site (B2) ya cerrados (2026-10-15, PR #37). Todo en F0.

Ver la secuencia completa, los hechos verificados en el código y las preguntas abiertas en `ROADMAP.md`.

## Operaciones mínimas

Checklist operacional antes de seguir agregando features sobre instancias live:

- ✅ Restore de backup probado en VM limpia (P0-1) — contenedor descartable, 2026-10-15
- ⚠️ Uptime monitor con alertas (P0-2) — Kuma desplegado, falta confirmar alertas (F0)
- ✅ Sentry capturando errores backend + frontend (P0-3)
- ✅ Headers de seguridad HTTPS en Caddy (P0-4, PR #26). CSP pendiente (S9)
- ⬜ Audit log de acciones admin (P1-6, S9)
- ⬜ Logs JSON estructurados + correlationId (P1-8, S9)
- ✅ Backup off-site a Backblaze B2 (P3-10) — mergeado y verificado end-to-end (PR #37, 2026-10-15): backup real subido a B2 y restore probado en contenedor descartable (DB + uploads)

## Prioridades actuales (2026-10-15)

Secuencia por fases (detalle, horas y criterios de salida en `ROADMAP.md`). Capacidad: 15 a 20 h por semana.

🔴 **F0 — Higiene** (12–18 h). Login 401, sacar defaults de Montecaseros y fallar si falta `CLINIC_*`, `og:` por instancia, restore probado, alertas Kuma. **← PRÓXIMO**
🔴 **F1 — Instancia de Guille en paralelo + importación CSV** (42–63 h). VPS nuevo, deploy multi-host, texto del rubro y colores por configuración, `V10` (email nullable + único), importador con simulación. Guille sigue en Ágora.
🔴 **F2a — Recordatorios por email** (20–28 h). `NotificationService` por canal, Brevo + Thymeleaf, cron idempotente con zona horaria por instancia, SPF/DKIM/DMARC.
🔴 **Corte de Guille** (3–5 h). Importar sus clientas y salir en vivo.
🟡 **F2b — WhatsApp** (23–33 h). Twilio, plantillas Meta, opt-in, E.164. Antes: P3-4 costo por mensaje.
🟡 **F3 — Flags y presets** (20–30 h). `FEATURE_*` reales, moneda y locale por configuración, presets por rubro, `ONBOARDING_CLIENTE.md`.
⏸️ **F3b — pt-BR** (30–45 h) — solo si firma un cliente de Brasil.
⏸️ **F4 — Fisioterapia como complemento de Vedius** — condicionada a la reunión. El timer PWA (12–16 h) puede adelantarse con un piloto pago. Profesional no se construye mientras Vedius tenga la agenda.
🟡 **F5 — Agente** (95–135 h) tras F2.
🟢 Backlog posterior: agenda polish, theming completo, S5, S7, S8, S9, S11–S14.

**Postergados explícitamente:**
- **Fase Premium UX** (diseñador externo + delight: command palette, shared transitions, ilustraciones propias, etc.) — post 1+ cliente WhatsApp pagando 2+ meses sin churn. Antes de invertir en landing SaaS porque la landing tiene que mostrar este nivel. Detalle en `ROADMAP.md` § Fase Premium UX.
- Landing comercial `consultorio-landing` — post Fase Premium UX, con caso de éxito real.
- Multi-tenancy real — post 5 clientes activos.

**Aprendizaje devops (proyecto + paralelo):**
- En este proyecto: Prometheus/Grafana (S13), Terraform (cliente #2+).
- Paralelo: Kubernetes (k3s + helm + CKA) — no aplicar a este proyecto.

Todo el resto (prosa de snapshot, rutas de archivos clave, preguntas abiertas, tabla 4 futuro, secuencia narrativa) no se copia a CLAUDE.md. Vive o en el commit history o se regenera si hace falta.
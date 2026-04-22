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
- `feat/public-page-polish` — polish página pública: fotos, config dinámica, reseñas ← **actual**

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

## Estado actual (2026-04-22)

**Rama:** `feat/public-page-polish`

Completado: MVP1, P1 hardening, P2 infra, P3 Fase A/B, página pública, admin polish, deploy real (live 2026-04-21), post-deploy hardening (PR #19).
En curso: polish de la página pública — fotos de tratamientos, config dinámica, sección de reseñas.
Siguiente: MVP2 (rol paciente).

Ver detalles completos en `ROADMAP.md`.

## Prioridades actuales (2026-04-22)

🔴 Public page polish (`feat/public-page-polish`) — fotos tratamientos, ClinicConfig dinámico, reseñas Google, sección Nosotros + mapa.
🔴 MVP2 — rol paciente — próximo ciclo grande. Scope: registro/login paciente, JWT PACIENTE, RBAC, pedir turno → PENDIENTE, refresh tokens, "Mi cuenta".
🟡 WhatsApp automatizado — después de MVP2.
🟡 Legal / compliance Ley 18.331 — antes del primer cliente real.
Logs estructurados JSON + correlationId.
Email transaccional (Brevo/Resend) — puerta de entrada para password reset y WhatsApp.
Paginación Pageable en listados.
Soft delete + auditoría de acceso a historial.

Todo el resto (prosa de snapshot, rutas de archivos clave, preguntas abiertas, tabla 4 futuro, secuencia narrativa) no se copia a CLAUDE.md. Vive o en el commit history o se regenera si hace falta.
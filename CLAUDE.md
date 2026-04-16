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

**Completado:**
- ✅ MVP1 backend + frontend
- ✅ P1 hardening, P2 infra Docker/CI, P3 Fase A UX, P3 Fase B deploy

**En curso:**
- ⏳ Admin polish (login oscuro, sidebar oscuro, dashboard, chips de estado)

**Próximo:**
- ⏳ `feat/public-page` — página pública con tratamientos activos
- ⏳ Deploy real (requiere pagar: Hetzner + Cloudflare + dominio)

Ver detalles completos en `ROADMAP.md`.

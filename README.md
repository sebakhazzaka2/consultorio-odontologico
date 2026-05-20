# Consultorio Odontológico — B2B SaaS

Practice management system for independent health professionals (dentistry, aesthetics, etc.). One Docker instance per client, per-client branding and feature flags via env vars, public booking page.

**Live in production:**

- [neodentalmaster.turnosuy.com](https://neodentalmaster.turnosuy.com) — dental clinic in Montevideo (active client since 2026-05-11)


| Public landing | Admin scheduling |
|---|---|
| ![Landing](docs/screenshots/public-page-turnosuy.png) | ![Admin](docs/screenshots/admin-turnosuy.png) |

---

## What it solves

Independent practitioners managing appointments through WhatsApp + paper notebooks + Excel, who need:

- A public site where patients book appointments without installing an app or creating an account.
- A real scheduling system with clinical history, treatments, payments and outstanding balance.
- Something that looks like *their* brand (their color, name, domain) — not a generic SaaS shell.

Commercial model in 3 tiers (Local / Web / Web + WhatsApp), single Docker image, differentiated by feature flags.

## Stack

| Layer | Tech |
|------|-----------|
| Backend | Spring Boot 3.2.5 · Java 17 · Spring Security · JWT · Flyway |
| Frontend | Angular 19 · Angular Material · SCSS with `--brand-*` tokens |
| Database | MySQL 8 |
| Infrastructure | Docker Compose · Caddy (reverse proxy + automatic Let's Encrypt TLS) · Hetzner Cloud |
| CI/CD | GitHub Actions — backend tests, image build & push to GHCR, SSH deploy to Hetzner on merge to `main` |
| Operations | Automated daily backups · Uptime Kuma monitoring · centralized logs |

## Technical decisions (and their trade-offs)

**One Docker instance per client, not real multitenancy.**
Simpler to operate and isolate failures with few clients; migration to real multitenancy planned once active paying clients pass ~5. Conscious decision: the cost of premature multitenancy (RLS, `tenant_id` everywhere, cross-tenant bugs) outweighs the operational cost of N instances while N stays small.

**Feature flags via env vars, not a DB table.**
The package catalog (Local / Web / Web+WhatsApp) lives in `ClinicProperties` and is exposed via `GET /api/public/config`. Switching tiers = `docker compose up -d` with different env vars. Migration to a DB-backed flag table is planned once runtime toggling is actually needed.

**Per-client branding via env vars (`BRAND_*`) + CSS `--brand-*` layer.**
Same Docker build, different color and logo per client. Migration from Angular Material prebuilt themes → `@use mat` is planned to avoid coupling new components to the hardcoded navy.

**Snapshot/ledger pattern in clinical history.**
`precio_aplicado` is copied from the treatment at the moment of registration — historical price is preserved even if the catalog changes later. Intentional denormalization; the rest of the schema is 3NF.

**Patient balance is computed, never stored.**
`Σ history.precio_aplicado − Σ payments`. Avoids inconsistent state and simplifies auditing.

**Appointment overlap validation:** only against `CONFIRMADA` and `PENDIENTE` states — `CANCELADA` frees the slot.

**`Instant` UTC for auditing, `LocalDateTime` for business logic.**
Explicit separation avoids timezone bugs in operations that have local meaning (scheduling) vs. operations that cross system boundaries (logs, backups).

**Flyway-versioned migrations from day one.**
No migration has ever been applied by hand in production.

## Known technical debt

Prioritized and planned in [`ROADMAP.md`](ROADMAP.md). Listing it here because the meaningful part of technical debt is knowing it exists:

- **Security (S3):** JWT in localStorage → HttpOnly cookie, rate limit on login, strict CSP, admin action audit log.
- **Operations (S2.5):** restore-from-backup tested on a clean VM, external uptime monitor, Sentry (backend + frontend), strict security headers in Caddy.
- **Quality (S3.5):** Playwright E2E tests, soft delete, per-client data export, multi-user admin.
- **Observability (S10):** structured JSON logs with `correlationId`, Micrometer + Prometheus + Grafana.
- **Scale (post 5 clients):** real multitenancy, Terraform for provisioning, Docker secrets.

Full roadmap, P0/P1/P2/P3 priorities and per-sprint gates in [`ROADMAP.md`](ROADMAP.md).

## Repository layout

```
consultorio-odontologico/
├── backend/          # Spring Boot — REST API at /api, port 8080
│   └── src/main/java/com/consultorio/
│       ├── controller/  # REST endpoints
│       ├── service/     # business logic
│       ├── repository/  # Spring Data JPA
│       ├── model/       # JPA entities
│       ├── dto/         # request/response, kept separate from model
│       ├── exception/   # global @ControllerAdvice
│       └── config/      # security, CORS, ClinicProperties
├── frontend/         # Angular 19 — SPA on port 4200
│   └── src/app/
│       ├── core/        # auth (service, interceptor, guard), ClinicConfigService
│       ├── features/    # admin layout, CRUDs, public page
│       └── shared/      # reusable components, branding tokens
├── scripts/          # backup.sh and deploy utilities
├── ROADMAP.md        # phases, sprints, priorities
└── WORKFLOWS.md      # how to work in this repo
```

Module-specific conventions in `backend/CLAUDE.md` and `frontend/CLAUDE.md`.

## Running locally

### Requirements

- Java 17+ · Maven 3.8+
- Node.js 20+ · npm
- MySQL 8 on `localhost:3306`

### Database

```sql
CREATE DATABASE IF NOT EXISTS consultorio_db;
```

Flyway runs migrations automatically on backend startup.

### Backend

```bash
cd backend
# Create src/main/resources/application-local.properties with:
# spring.datasource.password=YOUR_PASSWORD
# jwt.secret=YOUR_BASE64_SECRET
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

API at `http://localhost:8080/api`.

### Frontend

```bash
cd frontend
npm install
npm start
```

SPA at `http://localhost:4200`.

## Current status (2026-05-19)

| Block | Status |
|------|--------|
| MVP1 (CRUDs, scheduling, clinical history, payments) | Done, in production |
| P1 Hardening (validation, logging, Flyway, tests) | Done |
| P2 Infrastructure (Docker, Caddy, backups, real deploy) | Done |
| P3 Public page + admin polish | Done |
| Sprint 1 — Multi-vertical (genericity across dental/aesthetics) | Done (PR #21) |
| Sprint 2.0 — Theming foundations | In progress |
| Sprint 2 — Admin availability + login-less booking | Pending |
| Sprint 2.5 — Urgent hardening (P0 from audit) | Pending |
| Sprint 3 — Diagnoses + treatment plans + security hardening | Pending |

Full roadmap and per-sprint sequencing in [`ROADMAP.md`](ROADMAP.md).

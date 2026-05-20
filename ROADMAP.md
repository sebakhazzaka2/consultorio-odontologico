# Roadmap — Consultorio Odontológico

> **Última actualización:** 2026-05-20 — S2 completo. Secuencia reordenada: features primero (S5→S6→S7→S8), luego hardening consolidado (S9), luego WhatsApp (S10).

## Secuencia recomendada
```
S3 Hardening urgente (P0, ~1.5 días)  ← PRÓXIMO
→ S4 Theming foundations (~2 días)
→ S5 Presupuestos + Archivos (~1 semana, solo producto)
→ S6 Mails transaccionales (~1 semana)
→ S7 Portal paciente (~2-3 semanas)
→ S8 Balance + Gastos (~1-2 semanas)
→ S9 Hardening consolidado (seguridad P1 + pre-cliente #3 P2, ~2-3 semanas)
→ S10 WhatsApp (~1-2 semanas)
→ S11 Demo instance → S12 SEO → S13 Observability → cliente #3+
```

> **Lógica de la secuencia:** construir todas las pantallas y features primero, luego un solo sprint de hardening y refinamiento donde hay superficie conocida, luego integración externa (WhatsApp). Evita interrumpir el flujo de desarrollo con cambios de contexto a seguridad/infra.
>
> **Excepción S3:** los 6 items P0 son riesgos existenciales con 2 instancias en prod — van primero siempre.
>
> **Polish visual incremental:** los quick wins de UI (skeletons, density, hero pública, agenda, etc.) NO tienen sprint dedicado. Se aplican dentro del PR de cada sprint que toca esa zona. Ver inventario en `frontend/CLAUDE.md` § Polish backlog.

---

## Estado por fase

| Fase | Estado |
|------|--------|
| MVP1 — Backend | ✅ Completo |
| MVP1 — Frontend | ✅ Completo |
| P1 — Hardening pre-producción | ✅ Completo |
| P2 — Infraestructura profesional | ✅ Completo |
| P3 Fase A — Producto usable | ✅ Completo |
| P3 Fase B — código deploy | ✅ Completo |
| Página pública clínica | ✅ Completo |
| Admin polish | ✅ Completo |
| Deploy real | ✅ Live en dentalmontecaseros.turnosuy.com (2026-04-21) |
| Post-deploy hardening | ✅ Completo (PR #19, 2026-04-22) |
| Public page polish | ✅ Completo (PR #20, 2026-04-28) |
| fix/safari-white-screen | ✅ Completo (2026-05-11) |
| Sprint 1 — Multi-rubro genérico | ✅ Completo (2026-05-11) — live en neodentalmaster.turnosuy.com |
| fix/admin-branding | ✅ Completo (2026-05-11) |
| Sprint 2 — Disponibilidad + Reserva sin login | ✅ Completo (2026-05-20) |
| **Sprint 3 — Hardening urgente (P0)** | ⏳ Próximo (~1.5 días) |
| **Sprint 4 — Theming foundations** | ⏳ Tras S3 (~2 días) |
| **Sprint 5 — Presupuestos + Archivos** | ⏳ Tras S4 (~1 semana) |
| **Sprint 6 — Mails transaccionales** | ⏳ Tras S5 (~1 semana) |
| **Sprint 7 — Portal paciente opcional** | ⏳ Tras S6 (~2-3 semanas) |
| **Sprint 8 — Balance + Gastos** | ⏳ Tras S7 (~1-2 semanas) |
| **Sprint 9 — Hardening consolidado (seguridad P1 + pre-cliente #3)** | ⏳ Tras S8 (~2-3 semanas) |
| **Sprint 10 — WhatsApp automático** | ⏳ Tras S9 (~1-2 semanas) |
| **Sprint 11 — Demo instance + README pro** | ⏳ Tras S10 (2-4 días) |
| **Sprint 12 — SEO multi-subdominio** | ⏳ Tras S11 (3-5 días) |
| **Sprint 13 — Observability + rollback** | ⏳ Tras S12 (1 semana) |
| **Sprint 14 — Google Calendar sync** | ⏳ Diferido — infraestructura lista (S13) |
| 🎯 Primer cliente Web | ⏳ Tras S6 + S3 + S9 seguridad P1 (paquete básico vendible) |
| 🎯 Primer cliente Web + WhatsApp | ⏳ Tras S10 (paquete premium) |
| 🎯 Cliente #3 | ⏳ Tras S9 completo (pre-cliente #3 P2) |
| **Fase Premium UX (diseñador + delight)** | ⏳ Post 1+ cliente WhatsApp pagando 2+ meses, antes de landing SaaS |
| Landing SaaS proveedor | ⏳ Post Premium UX |
| V5 — Multi-tenant SaaS | ⏳ Post ~5 clientes activos |

---

## Pre-ventas — checklist mínimo antes de vender

**Paquete Local / Web básico (vendible tras S6 + hardening P1 de S9):**
- ✅ Reserva como invitado desde página pública (sin login requerido)
- ✅ Admin gestiona disponibilidad de horarios
- ✅ Admin confirma / cancela / reagenda desde dashboard
- ⬜ Email confirmación de turno al cliente (S6)
- ⬜ Script de provisioning: dado dominio + datos de clínica, levanta instancia en < 15 min (S9)
- ⬜ Backup automático verificado (cron + test de restore) (S3)
- ✅ HTTPS automático por subdominio (Caddy)

**Paquete Web + WhatsApp (vendible tras S10):**
- ⬜ Recordatorio automático 24h antes por WhatsApp
- ⬜ Notificación al cliente cuando admin confirma / cancela

**Portal paciente (valor agregado, tras S10):**
- ⬜ Registro/login opcional para ver historial, cancelar (72h), reagendar

---

## Completados — decisiones clave

### Sprint 2 — Disponibilidad + Reserva sin login ✅ (2026-05-20)

**Decisiones de diseño que no deben revertirse:**
- La duración de cada cita la define el **servicio** (`duracionMinutos`), no el paciente ni el admin al reservar. El backend calcula slots usando esa duración.
- Al confirmar una cita `PENDIENTE`, el admin puede editar la duración antes de confirmar (flexibilidad clínica). La agenda bloquea la duración final confirmada.
- El **email es obligatorio** en la reserva pública — necesario para confirmaciones (S6) y recordatorio 24h.
- Columna `google_event_id VARCHAR(255) NULL` en tabla `citas` (migración V9) para futura sincronización sin migración en prod.
- `clinic.json` / `docker-entrypoint.sh` / vars `CLINIC_*` eliminados. `ClinicConfigService` llama a `/api/public/config` — una sola fuente de verdad.

---

## Detalle por sprint

### Sprint 3 — Hardening urgente (~1.5 días)

> **Origen:** auditoría técnica 2026-05-19, prioridad P0. Riesgos existenciales con 2 instancias en producción.

- **[P0-1] Probar restore de backup en VM limpia (2-3 h).** Restaurar dump en VM Hetzner descartable, validar arranque + integridad. Sin esto un disco corrupto termina el negocio. Documentar en `scripts/`.
- **[P0-2] Uptime monitor externo (30 min).** Better Stack / UptimeRobot free tier, alertas a mail + Telegram.
- **[P0-3] Sentry backend + frontend (3-4 h).** Free tier, `release` por commit SHA, `environment` por subdominio.
- **[P0-4] Headers de seguridad en Caddy (1 h).** `Strict-Transport-Security`, `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, `Referrer-Policy`. Verificar que `allowed-origins` CORS no tenga wildcards.
- **[P0-5] Auditar `@Query` y `Specification` por SQL injection (1-2 h).** Grep en concatenación de strings en queries y Specification builders.
- **[P0-6] Verificar endpoint público de reserva contra spam/DoS (1-2 h).** Rate limit por IP + validación email. Sin esto una botnet llena la agenda.

**Total:** ~10-13 h (~1.5 días). Rama: `feat/s3-hardening-urgente`.

---

### Sprint 4 — Theming foundations (~2 días)

> **Motivación:** infraestructura SaaS para "mismo build Docker, color por cliente" vía env vars. No es refinamiento visual — es una feature de negocio. El polish visual se aplica incremental en cada sprint.

- **[F-1] Migración Angular Material prebuilt → `@use mat`** (4-5 h). Quitar `@import 'indigo-pink.css'`, configurar `mat.theme()` con typography Inter + density 0, override de tokens MDC apuntando a `--color-primary`. Riesgo medio: verificar todos los componentes Material en uso.
- **[F-2] Capa brand tokens en `_design-system.scss`** (1 h). `--brand-primary`, `--brand-primary-hover`, `--brand-primary-contrast`, `--brand-accent`, `--brand-surface-tint`, `--brand-focus-ring`. Defaults = navy actual → cero regresiones.
- **[F-3] `applyBranding` en `ClinicConfigService`** (3-4 h). Extender `ClinicConfig` con bloque opcional `branding`. En `load()`, setear CSS vars en `document.documentElement`. Helpers: `shade()`, `alpha()`, `bestContrast()` (WCAG).
- **[F-4] Backend `ClinicProperties.branding`** (2 h). Leer `BRAND_PRIMARY`, `BRAND_PRIMARY_HOVER`, `BRAND_ACCENT` del env. Incluir en `GET /api/public/config`. Si no están seteados → no enviar bloque → frontend usa defaults.
- **[F-5] Nuevos design tokens base** (2 h). Gradientes, shadows brand-aware, motion tokens, z-index scale en `_design-system.scss`.
- **[F-6] QA visual en 2 brand configs** (3-4 h). Navy actual + magenta hipotético. Admin completo + página pública. **No mergear sin F-6.**

**Total:** ~13-16 h (~2 días). Rama: `feat/s4-theming-foundations`.
**Dependencias:** F-1 desbloquea F-2/F-3. F-4/F-5 paralelos. F-6 al final.

---

### Sprint 5 — Presupuestos + Archivos (~1 semana)

> Solo producto. El hardening de seguridad asociado va en el sprint S9 consolidado post-S8.

**Presupuestos:**
- Entidad `Presupuesto`: paciente, fecha, descripción libre (texto), monto total, estado (`BORRADOR` / `ENVIADO`)
- CRUD de presupuestos desde la ficha del paciente en el admin
- Sin vinculación obligatoria a tratamientos ni aranceles — descripción libre
- El envío por mail se implementa en S6

**Archivos clínicos:**
- Entidad `ArchivoClinico`: paciente, nombre original, path en disco, tipo MIME, tamaño, fecha subida, referencia opcional a presupuesto
- `FileStorageService` (interfaz) + `LocalFileStorageServiceImpl`: guarda en `/app/uploads/pacientes/{id}/`
- Docker volume en `docker-compose.prod.yml`: `/opt/consultorio/uploads:/app/uploads`
- Endpoint `POST /api/admin/pacientes/{id}/archivos` (multipart) + `GET` para servir
- UI: sección "Archivos" en ficha del paciente — upload drag&drop + lista + descarga/borrar

> **Operaciones críticas:** agregar `/opt/consultorio/uploads` al cron de backup — los archivos no están en BD.
> Backlog: migrar `LocalFileStorageServiceImpl` → `BackblazeStorageServiceImpl` cuando escale.

---

### Sprint 6 — Mails transaccionales (~1 semana)

- `spring-boot-starter-mail` + Brevo SMTP (free tier: 300 mails/día)
- Templates Thymeleaf base con header/footer compartido (nombre + logo desde `ClinicProperties`)
- **Password reset admin** incluido acá (P1-7) — SMTP ya disponible, evita SSH a producción

**Notificaciones al paciente:**
1. **Cita:** confirmación, cancelación/reagendamiento, recordatorio 24h antes (cron)
2. **Presupuesto:** envío desde admin → estado cambia `BORRADOR` → `ENVIADO`
3. **Pago:** mail al paciente con monto abonado y saldo pendiente

---

### Sprint 7 — Portal paciente opcional (~2-3 semanas)

- Registro/login opcional — el cliente puede "reclamar" su historial de reservas
- Ver citas agendadas (próximas + historial), cancelar (hasta 72h antes), reagendar
- Ver procedimientos realizados (historial clínico resumido)
- JWT con rol PACIENTE + RBAC (`@PreAuthorize` + route guard)
- Refresh tokens con rotación
- JWT en cookie HttpOnly + SameSite=Strict
- Consentimiento explícito de datos (Ley 18.331)

> **Decisión P3-5:** unificar JWT ADMIN + JWT PACIENTE con `roles` claim al implementar este sprint. Dos sistemas de auth en paralelo es complejidad innecesaria.

---

### Sprint 8 — Balance + Gastos (~1-2 semanas)

- CRUD de gastos con categorías
- Balance mensual: Σ cobros − Σ gastos del período
- Vista resumen en dashboard admin
- Export básico (CSV)

---

### Sprint 9 — Hardening consolidado + Pre-cliente #3 (~2-3 semanas)

> Después de tener todas las pantallas y features construidas, un solo sprint de hardening y refinamiento sobre superficie conocida.

**Seguridad P1 (auditoría 2026-05-19):**

- **[P1-1] JWT de localStorage → cookie HttpOnly + SameSite=Strict (1-1.5 días).** XSS roba localStorage trivialmente. Habilitar Spring Security CSRF para endpoints state-changing. Aplica a JWT admin + JWT paciente (S7).
- **[P1-2] Rate limit + lockout en `/auth/login` (2-3 h).** Bucket4j, 5 intentos en 15 min → lockout temporal.
- **[P1-3] Paginación `Pageable` en listados (1 día).** Listado de pacientes sin paginar explota con 3000+ registros.
- **[P1-4] CSP — Content Security Policy (4-6 h).** Modo `Report-Only` primero, iterar con Angular Material, luego enforce.
- **[P1-5] Endurecer file upload (2-3 h).** `Content-Disposition: attachment` + `X-Content-Type-Options: nosniff` en archivos clínicos.
- **[P1-6] Audit log de acciones admin (1 día).** Tabla `audit_log` con `@EntityListeners` o AOP. Sin UI — modelar antes de multi-usuario.
- **[P1-8] Logs JSON + correlationId (2-3 h).** Logback `logstash-encoder` + filtro `X-Correlation-Id`.
- **[P1-9] Revisar `GET /api/public/config` (30 min).** No debe exponer versiones de libs, URLs internas o paths.
- **[P1-10] Spring Actuator en :8081 (30 min).** Bindear a `127.0.0.1:8081` o red Docker interna.

**Pre-cliente #3 P2 (bloqueantes comerciales):**

- **[P2-1] E2E happy path con Playwright (1-1.5 días).** Login → crear paciente → turno → agenda. Corriendo en CI.
- **[P2-2] Export datos del paciente JSON/CSV (4-6 h).** Primera pregunta de cualquier cliente serio: "¿me llevo mis datos?".
- **[P2-3] Soft delete en pacientes y turnos (3-4 h).** Flag `deleted_at`. Hoy borrar = ir al backup.
- **[P2-4] Multi-usuario admin — secretaria + dentista (2-3 días).** El primer cliente con secretaria lo pide.
- **[P2-5] Healthcheck que valide bundle frontend (2 h).** Caddy chequea HTML, no que Angular bootee.
- **[P2-6] Script de provisioning + `docs/ONBOARDING_CLIENTE.md` (1-2 días).** Cliente #3 no se monta a mano. Checklist único: datos clínica, `BRAND_*`, `FEATURE_*`, servicios seed, DNS, backup, SPF/DKIM, smoke test.
- **[P2-7] SLA documentado (4 h).** 99% mensual = 7h downtime. Necesario para firmar con expectativas alineadas.
- **[P2-8] Página de status manual (2 h).** Estática editable cuando algo falla.

**Total S9:** ~3 semanas. Bloquea cliente #3 y habilita paquete básico vendible completo.

---

### Sprint 10 — WhatsApp automático (~1-2 semanas)

> Después de hardening — no tiene sentido endurecer seguridad e integrar una API externa al mismo tiempo.

- Twilio Business API o UltraMsg (evaluar costo/mensaje antes — P3-4)
- Recordatorio 24h antes, confirmación, cancelación/cambio de horario
- Mismo trigger que S6 mails, canal distinto

> **Antes de implementar:** modelar costo por mensaje y definir "X mensajes/mes incluidos" en precio del paquete (P3-4). Twilio ~$0.005/msg UY. UltraMsg con número personal = riesgo de ban. **Antes de S10.**

---

### Sprint 11 — Demo instance + README pro (2-4 días)

- `docker-compose.demo.yml` con datos fake realistas
- Subdominio `demo.turnosuy.com` con usuario/contraseña públicos en README
- README pro: badges, screenshot/GIF, arquitectura de deploy, link live + demo
- Theming dinámico ya implementado desde S4 — acá solo se setean env vars de la demo

---

### Sprint 12 — SEO multi-subdominio (3-5 días)

- Prerender estático con `@angular/ssr` (build-time)
- Meta tags dinámicos desde `ClinicConfig` usando `Title` + `Meta` services
- JSON-LD genérico (`LocalBusiness` + tipo específico por instancia vía `ClinicProperties`)
- Endpoint backend `/sitemap.xml` por subdominio
- `robots.txt` estático con `Sitemap:`

---

### Sprint 13 — Observability + rollback (1 semana)

- Logs JSON + correlationId/MDC (adelantado a S9 si hace falta antes)
- Micrometer + `actuator/prometheus` expuesto
- `scripts/rollback.sh` que vuelve al SHA anterior
- Tag de imagen por SHA en `docker-compose.prod.yml` (no `:latest`)
- Backup automático pre-deploy en CI/CD
- Notificación de deploy a Telegram/Discord
- Reglas Sentry + alertas a canal real

### Sprint 13+ — Pre-cliente #2

- Terraform módulo Hetzner Cloud + Cloudflare
- Prometheus + Grafana (self-hosted)

---

## Decisiones estratégicas pendientes (P3 auditoría 2026-05-19)

No urgentes pero caras si no se deciden ahora. Revisar al planificar el sprint relevante.

- **[P3-1] Reevaluar trigger de multi-tenancy real (análisis ~1 día).** Hoy "post 5 clientes". El costo operativo (deploys, parches, migraciones x N instancias) probablemente hace que el trigger real sea 3. Modelar antes de cliente #3.
- **[P3-2] Feature flags: env vars → tabla DB (1-2 días).** Hoy 3 booleanos via env. Al primer pedido tipo "portal sí, reserva no" se vuelve incómodo. **Decidir antes de S11.**
- ~~**[P3-3] Estrategia theming Angular Material**~~ — ✅ resuelto en S4: opción (b) Material neutro + override tokens MDC vía CSS vars.
- **[P3-4] Costo por mensaje WhatsApp y modelo "X incluidos/mes" (4 h).** Modelar antes de prometer "WhatsApp ilimitado". **Antes de S10.**
- **[P3-5] Unificar JWT ADMIN + JWT PACIENTE con `roles` claim.** Integrado en S7.
- **[P3-6] Reservar 20% de cada sprint para incidentes/soporte.** Con 2 clientes en producción el roadmap lineal sin buffer es ilusorio.
- **[P3-7] SPF/DKIM/DMARC en onboarding de subdominio (2 h en S6).** Sin esto los mails van a spam. Parte del checklist de provisioning.
- **[P3-8] Rotación de secrets documentada (2 h).** Procedimiento para rotar `JWT_SECRET`, `DB_PASSWORD`. Sin blacklist JWT, rotación = logout masivo.
- **[P3-9] Docker secrets en lugar de env vars (1 día a partir de 3+ clientes).** Hoy `docker inspect` revela secrets.
- **[P3-10] Backup off-site verificado (4-6 h).** Verificar retención, encriptación en reposo, y que el restore (P0-1) funcione desde copia off-site.

---

## Backlog medio plazo

- ~~Soft delete + auditoría acceso historial clínico~~ — soft delete en P2-3 (S9), audit log en P1-6 (S9).
- ~~Paginación con `Pageable` en endpoints de listado~~ — en S9 como P1-3.
- Export PDF historial clínico
- Dark mode toggle
- Cobertura tests backend ~50% / frontend ~40%
- `FileStorageService` → `BackblazeStorageServiceImpl` cuando uploads escalen

## Fase Premium UX — diferido post primer cliente WhatsApp (antes de landing SaaS)

> **Motivación:** S4 + polish incremental llevan el producto a "profesional y vendible" (~percentil 75 del SaaS B2B regional). Para "alto valor percibido" falta una capa de delight + diseño visual profesional que no se construye con tokens, se construye con criterio de diseñador y trabajo dedicado.
>
> **Cuándo:** post S10 + 1-2 clientes pagando cuota mensual sin churn. Antes de invertir en `consultorio-landing`. Financiado por ingresos reales.
>
> **No incluido:** features funcionales. Solo capa de percepción y delight.

**Bloque A — Diseño visual profesional (1 semana diseñador externo + 3-5 días integración):**
- Contratar diseñador UI senior (~USD 800-1500) o usar plantilla curada premium (Untitled UI Angular kit, USD 349)
- Entregables: paleta refinada por rubro, ilustraciones propias para empty states, jerarquía tipográfica dramática, iconografía custom (Lucide/Phosphor)

**Bloque B — Delight & interacciones premium (~1-2 sem):**
- Command palette (Cmd+K), atajos de teclado documentados
- Autoguardado con indicador discreto, undo/redo en acciones destructivas
- Shared element transitions, micro-feedback, skeletons con shimmer real
- Empty states con personalidad: ilustración + microcopy + CTA contextual

**Bloque C — Detalles de pulido fino (~3-5 días):**
- Modo "focus" en agenda (presentación al paciente), print stylesheets, animaciones de éxito

**Trigger explícito:** "tengo al menos 1 cliente WhatsApp pagando cuota mensual hace 2+ meses sin churn".

---

## Backlog largo plazo (post 5 clientes activos)

- Multi-tenancy real (schema MySQL por cliente)
- i18n (es/pt/en)
- App móvil PWA
- Búsqueda global cross-módulo

## Landing SaaS proveedor ⏳ (repo `consultorio-landing`)

Post primer cliente con caso de éxito. Stack: Astro + Tailwind v4, deploy Cloudflare Pages.

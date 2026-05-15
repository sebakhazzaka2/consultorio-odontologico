# Roadmap — Consultorio Odontológico

> **Última actualización:** 2026-05-15 — Repriorización post Sprint 1. Reserva sin login + WhatsApp suben; MVP2 portal baja a Sprint 5.

## Secuencia recomendada
```
Multi-rubro ✅ → Deuda técnica + Disponibilidad admin + Reserva sin login (S2)
→ Mails transaccionales (S3) → WhatsApp automático (S4)
→ Portal paciente opcional (S5) → Balance + Gastos (S6)
→ Demo instance + README pro (S7) → SEO (S8) → Observability (S9) → primer cliente pagando
```

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
| **Sprint 2 — Disponibilidad + Reserva sin login** | ⏳ Próximo (~2 semanas) |
| **Sprint 3 — Mails transaccionales** | ⏳ Tras S2 (5-8 días) |
| **Sprint 4 — WhatsApp automático** | ⏳ Tras S3 (1-2 semanas) |
| **Sprint 5 — Portal paciente opcional** | ⏳ Tras S4 (2-3 semanas) |
| **Sprint 6 — Balance + Gastos** | ⏳ Tras S5 (1-2 semanas) |
| **Sprint 7 — Demo instance + README pro** | ⏳ Tras S6 (2-4 días) |
| **Sprint 8 — SEO multi-subdominio** | ⏳ Tras S7 (3-5 días) |
| **Sprint 9 — Observability + rollback** | ⏳ Tras S8 (1 semana) |
| 🎯 Primer cliente Web | ⏳ Tras S2 + S3 (paquete básico vendible) |
| 🎯 Primer cliente Web + WhatsApp | ⏳ Tras S4 (paquete premium vendible) |
| 🎯 Cliente #2 | ⏳ Trigger para script provisioning + Terraform |
| Landing SaaS proveedor | ⏳ Post primer cliente pagando con caso de éxito |
| V5 — Multi-tenant SaaS | ⏳ Post ~5 clientes activos |

---

## Pre-ventas — checklist mínimo antes de vender

**Paquete Local / Web básico (vendible tras S2 + S3):**
- ⬜ Reserva como invitado desde página pública (sin login requerido)
- ⬜ Admin gestiona disponibilidad de horarios
- ⬜ Admin confirma / cancela / reagenda desde dashboard
- ⬜ Email confirmación de turno al cliente
- ⬜ Script de provisioning: dado dominio + datos de clínica, levanta instancia en < 15 min
- ⬜ Backup automático verificado (cron + test de restore)
- ✅ HTTPS automático por subdominio (Caddy)

**Paquete Web + WhatsApp (vendible tras S4):**
- ⬜ Recordatorio automático 24h antes por WhatsApp
- ⬜ Notificación al cliente cuando admin confirma / cancela

**Portal paciente (valor agregado, tras S5):**
- ⬜ Registro/login opcional para ver historial, cancelar (72h), reagendar

---

## Detalle por sprint

### Sprint 2 — Disponibilidad admin + Reserva sin login (~2 semanas)

**Motivación:** cambio de enfoque — el admin siempre confirma, por lo tanto el cliente no necesita login para reservar. Esto simplifica todo el funnel de venta y hace el producto vendible antes.

**Deuda técnica resuelta (2026-05-15):** ✅ `clinic.json` / `docker-entrypoint.sh` / vars `CLINIC_*` eliminados. El core `ClinicConfigService` ahora llama a `/api/public/config` igual que la página pública. Una sola fuente de verdad en Spring Boot.

**Gestión de disponibilidad (admin):**
- Admin configura franjas horarias disponibles por día de semana (ej: L-V 09:00–18:00, pausa 13:00–14:00)
- Admin puede bloquear fechas específicas (feriados, vacaciones)
- El backend expone slots disponibles considerando la config + citas existentes
- Frontend admin: sección de configuración de horarios

**Reserva como invitado (página pública):**
- Formulario en página pública: nombre, teléfono, email (opcional), servicio, fecha/hora
- El cliente elige de los slots disponibles expuestos por el backend
- Cita queda en estado `PENDIENTE` en el dashboard del admin
- Admin confirma, cancela, o propone otro horario desde el dashboard
- Sin login requerido para el cliente

### Sprint 3 — Mails transaccionales (5-8 días)

**Scope (acotado a lo necesario para vender):**
- `spring-boot-starter-mail` + Brevo SMTP (free tier: 300 mails/día) en `application-prod.properties`
- Templates Thymeleaf base con header/footer compartido (nombre + logo de clínica desde `ClinicProperties`)
- Mail confirmación de turno al cliente (cuando admin confirma)
- Mail cancelación / reagendamiento al cliente
- Mail recordatorio 24h antes del turno (cron job)
- Mail password reset (token + expiración + UI) — necesario para el portal de S5
- Mail alerta de fallo de backup al admin (cron existente)

### Sprint 4 — WhatsApp automático (1-2 semanas)

**Motivación:** diferencial clave del paquete premium. Con la infraestructura de mails en S3, WhatsApp es el mismo trigger en otro canal.

- Twilio Business API o UltraMsg (evaluar costo por mensaje para trasladar al precio del paquete)
- Recordatorio 24h antes del turno
- Notificación cuando admin confirma la reserva
- Notificación de cancelación / cambio de horario
- Costo por mensaje: documentar y reflejar en precio del paquete Web + WhatsApp

### Sprint 5 — Portal paciente opcional (2-3 semanas)

**Cambio respecto al plan anterior:** ya no es bloqueante de ventas. El cliente puede reservar sin login. El portal es valor agregado para retención y experiencia.

- Registro/login opcional — el cliente puede "reclamar" su historial de reservas
- Ver citas agendadas (próximas + historial)
- Cancelar cita (con restricción: solo hasta 72h antes)
- Reagendar cita
- Ver procedimientos realizados (historial clínico resumido)
- JWT con rol PACIENTE + RBAC (`@PreAuthorize` + route guard)
- Refresh tokens con rotación
- JWT en cookie HttpOnly + SameSite=Strict (mover desde localStorage)
- Consentimiento explícito de datos (Ley 18.331)
- Política de privacidad + términos de uso

### Sprint 6 — Balance + Gastos (1-2 semanas)

- CRUD de gastos con categorías
- Balance mensual: Σ cobros − Σ gastos del período
- Vista resumen en dashboard admin
- Export básico (CSV)

### Sprint 7 — Demo instance + README pro (2-4 días)

**Por qué después de Balance+Gastos:** el producto está completo — tiene todo lo que mostrar. Hacerlo antes implica actualizar los datos demo con cada sprint.

- `docker-compose.demo.yml` con datos fake realistas
- Subdominio `demo.turnosuy.com` con usuario/contraseña públicos en README
- README pro: badges, screenshot/GIF del admin, arquitectura de deploy, decisiones de diseño, link live + demo
- Sirve doble: portfolio personal + demo comercial para clientes potenciales

### Sprint 8 — SEO multi-subdominio (3-5 días)

Con 2+ subdominios reales (dental + estética) el ROI es inmediato; el código se hace una vez y aplica a todos los clientes futuros.

- Prerender estático con `@angular/ssr` (build-time)
- Meta tags dinámicos desde `ClinicConfig` usando `Title` + `Meta` services
- JSON-LD genérico (`LocalBusiness` + tipo específico por instancia vía `ClinicProperties`)
- Endpoint backend `/sitemap.xml` por subdominio
- `robots.txt` estático con `Sitemap:`
- Headers HTTP de seguridad en `Caddyfile` (CSP, HSTS, X-Frame-Options)

### Sprint 9 — Observability + rollback (1 semana)

- Logs JSON + correlationId/MDC
- Micrometer + `actuator/prometheus` expuesto
- Smoke test post-deploy: `curl /api/actuator/health` con retry
- `scripts/rollback.sh` que vuelve al SHA anterior
- Tag de imagen por SHA en `docker-compose.prod.yml` (no `:latest`)
- Backup automático pre-deploy en CI/CD
- Notificación de deploy a Telegram/Discord
- Reglas Sentry + alertas Uptime Kuma a canal real

### Sprint 10+ — Pre-cliente #2

- Script de provisioning (< 15 min para nueva clínica)
- Terraform módulo Hetzner Cloud + Cloudflare
- Prometheus + Grafana (self-hosted)

---

## Backlog medio plazo

- Soft delete + auditoría acceso historial clínico (Ley 18.331)
- Paginación con `Pageable` en endpoints de listado
- Export PDF historial clínico
- Dark mode toggle
- Cobertura tests backend ~50% / frontend ~40%
- `FileStorageService` → `BackblazeStorageServiceImpl` cuando uploads escalen

## Backlog largo plazo (post 5 clientes activos)

- Multi-tenancy real (schema MySQL por cliente)
- i18n (es/pt/en)
- App móvil PWA
- Búsqueda global cross-módulo

## Landing SaaS proveedor ⏳ (repo `consultorio-landing`)

Post primer cliente con caso de éxito. Stack: Astro + Tailwind v4, deploy Cloudflare Pages.
